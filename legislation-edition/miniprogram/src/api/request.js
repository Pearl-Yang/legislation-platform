import { clearSession } from "../utils/session.js";
export const BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8083/api";
export const getBaseUrl = () => uni.getStorageSync("apiBaseUrl") || BASE_URL;
let redirecting = false;
function expireSession() {
  clearSession();
  if (redirecting) return;
  redirecting = true;
  uni.showToast({ title: "请登录后继续", icon: "none" });
  uni.reLaunch({ url: "/pages/index/index" });
  setTimeout(() => {
    redirecting = false;
  }, 1000);
}
export function request(options) {
  const { url, method = "GET", data, header = {}, hideError = false } = options;
  const token = uni.getStorageSync("token");
  return new Promise((resolve, reject) =>
    uni.request({
      url: getBaseUrl() + url,
      method,
      data,
      timeout: 15000,
      header: {
        ...(token && token !== "demo-token"
          ? { Authorization: "Bearer " + token }
          : {}),
        ...header,
      },
      success(res) {
        const body = res.data || {};
        if (
          res.statusCode === 401 ||
          body.code === 401 ||
          (body.code >= 40100 && body.code < 40200)
        ) {
          if (url !== "/auth/login") expireSession();
          reject(new Error(body.message || "登录已过期，请重新登录"));
          return;
        }
        if (
          res.statusCode >= 200 &&
          res.statusCode < 300 &&
          body.code === 200
        ) {
          resolve(body.data);
          return;
        }
        const message =
          body.message ||
          (res.statusCode === 403
            ? "当前账号无权执行此操作"
            : "服务暂不可用，请稍后重试");
        if (!hideError) uni.showToast({ title: message, icon: "none" });
        reject(new Error(message));
      },
      fail() {
        const message = "无法连接服务，请检查网络或服务地址";
        if (!hideError) uni.showToast({ title: message, icon: "none" });
        reject(new Error(message));
      },
    }),
  );
}
export default request;
