import assert from "node:assert/strict";
import { beforeEach, test } from "node:test";
import { readFileSync } from "node:fs";
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
const { parse } = require("@vue/compiler-sfc");
const encode = (text) =>
  "data:text/javascript;base64," + Buffer.from(text).toString("base64");
const source = (file) =>
  readFileSync(new URL("../src/" + file, import.meta.url), "utf8");
const sessionUrl = encode(source("utils/session.js"));
const session = await import(sessionUrl);
const connection = await import(encode(source("utils/connection.js")));
const requestSource = source("api/request.js")
  .replace(/['"]\.\.\/utils\/session\.js['"]/, JSON.stringify(sessionUrl))
  .replace("import.meta.env.VITE_API_BASE_URL", "undefined");
const { request } = await import(encode(requestSource));
let storage, calls;
beforeEach(() => {
  storage = new Map();
  calls = { requests: [], redirects: [], toasts: [] };
  globalThis.uni = {
    getStorageSync: (key) => storage.get(key),
    setStorageSync: (key, value) => storage.set(key, value),
    removeStorageSync: (key) => storage.delete(key),
    showToast: (data) => calls.toasts.push(data),
    reLaunch: (data) => calls.redirects.push(data),
    request: (options) => {
      calls.requests.push(options);
      options.success({ statusCode: 200, data: { code: 200, data: [] } });
    },
  };
});
test("flat backend login response preserves actual user identity and role", () => {
  session.saveSession({
    token: "token",
    userId: 7,
    username: "reviewer",
    displayName: "审查员",
    role: "ROLE_REVIEWER",
    department: "法制科",
  });
  assert.equal(storage.get("userId"), 7);
  assert.equal(storage.get("userRole"), "ROLE_REVIEWER");
  assert.equal(storage.get("userName"), "审查员");
  assert.equal(storage.get("userDept"), "法制科");
  assert.equal(session.isLoggedIn(), true);
});
test("missing token cannot produce fake logged-in state", () => {
  assert.throws(() => session.saveSession({ userId: 1 }), /缺少凭证/);
  assert.equal(session.isLoggedIn(), false);
});
test("logout clears all account data while preserving connection settings", () => {
  session.saveSession({ token: "t", userId: 3, username: "admin" });
  storage.set("apiBaseUrl", "http://192.168.1.2:8083/api");
  session.clearSession();
  assert.equal(session.isLoggedIn(), false);
  assert.equal(storage.has("userInfo"), false);
  assert.equal(storage.has("userDept"), false);
  assert.equal(storage.get("apiBaseUrl"), "http://192.168.1.2:8083/api");
});
test("API uses saved real token and configurable base URL", async () => {
  storage.set("token", "jwt");
  storage.set("apiBaseUrl", "http://192.168.1.2:8083/api");
  await request({ url: "/legislative-project/list" });
  assert.equal(calls.requests[0].header.Authorization, "Bearer jwt");
  assert.equal(
    calls.requests[0].url,
    "http://192.168.1.2:8083/api/legislative-project/list",
  );
  assert.equal(calls.requests[0].header["X-User-Id"], undefined);
});
test("demo token is never sent as authentication", async () => {
  storage.set("token", "demo-token");
  await request({ url: "/auth/health" });
  assert.equal(calls.requests[0].header.Authorization, undefined);
  assert.equal(session.isLoggedIn(), false);
});
test("expired credentials clear the session and return to login", async () => {
  session.saveSession({ token: "expired", userId: 2 });
  uni.request = (options) =>
    options.success({ statusCode: 401, data: { message: "登录已过期" } });
  await assert.rejects(
    request({ url: "/library/material/list" }),
    /登录已过期/,
  );
  assert.equal(session.isLoggedIn(), false);
  assert.equal(calls.redirects[0].url, "/pages/index/index");
});
test("wrong password keeps login available rather than entering demo mode", async () => {
  uni.request = (options) =>
    options.success({
      statusCode: 200,
      data: { code: 40101, message: "密码错误" },
    });
  await assert.rejects(request({ url: "/auth/login" }), /密码错误/);
  assert.equal(calls.redirects.length, 0);
  assert.equal(session.isLoggedIn(), false);
});
test("network failure is rejected with a retryable message", async () => {
  uni.request = (options) => options.fail({ errMsg: "network error" });
  await assert.rejects(request({ url: "/consultation/list" }), /检查网络/);
});
test("connection validation accepts LAN URLs and rejects malformed URLs", () => {
  assert.equal(
    connection.normalizeApiUrl(" http://192.168.1.2:8083/api/ "),
    "http://192.168.1.2:8083/api",
  );
  for (const url of [
    "localhost:8083",
    "javascript:alert(1)",
    "https://server/api?token=x",
    "http:// bad/api",
  ]) {
    assert.throws(() => connection.normalizeApiUrl(url));
  }
});
function libraryVM(api) {
  const { descriptor } = parse(source("pages/library/index.vue"));
  const script = descriptor.script.content
    .replace(/import[\s\S]*?from\s+['"][^'"]+['"];?\s*\n/g, "")
    .replace("export default", "return");
  const options = Function(
    "PageHeading",
    "DataState",
    "SearchBar",
    "FilterPills",
    "Empty",
    "LoadingBlock",
    "MATERIAL_TYPE",
    "ellipsis",
    "libraryApi",
    script,
  )({}, {}, {}, {}, {}, {}, {}, String, api);
  const vm = options.data();
  for (const [name, method] of Object.entries(options.methods))
    vm[name] = method.bind(vm);
  return vm;
}
test("rapid searches retain the latest result even if earlier request finishes later", async () => {
  const resolvers = [];
  const vm = libraryVM({
    searchMaterials: (kw) =>
      new Promise((resolve) => resolvers.push({ kw, resolve })),
  });
  vm.kw = "旧查询";
  const oldRequest = vm.reload();
  vm.kw = "新查询";
  const newRequest = vm.reload();
  resolvers[1].resolve([{ id: 2, title: "新结果" }]);
  await newRequest;
  resolvers[0].resolve([{ id: 1, title: "旧结果" }]);
  await oldRequest;
  assert.equal(vm.materials[0].title, "新结果");
  assert.equal(vm.loading, false);
});
test("list load failure shows error separately from empty data and allows retry", async () => {
  let fails = true;
  const vm = libraryVM({
    listMaterials: async () => {
      if (fails) throw new Error("连接失败");
      return [{ id: 9, title: "法规" }];
    },
  });
  await vm.reload();
  assert.equal(vm.error, "连接失败");
  fails = false;
  await vm.reload();
  assert.equal(vm.error, "");
  assert.equal(vm.materials.length, 1);
});
