const KEYS = [
  "token",
  "userInfo",
  "userId",
  "userName",
  "userRole",
  "userDept",
];
export function clearSession() {
  KEYS.forEach((key) => uni.removeStorageSync(key));
}
export function saveSession(data) {
  if (!data?.token) throw new Error("登录响应缺少凭证，请重试");
  const info = data.userInfo || data;
  uni.setStorageSync("token", data.token);
  uni.setStorageSync("userInfo", {
    id: info.id || info.userId,
    username: info.username,
    displayName: info.displayName,
    role: info.role,
    department: info.department,
  });
  uni.setStorageSync("userId", info.id || info.userId);
  uni.setStorageSync("userName", info.displayName || info.username || "");
  uni.setStorageSync("userRole", info.role || "ROLE_USER");
  uni.setStorageSync("userDept", info.department || "");
}
export const isLoggedIn = () =>
  !!uni.getStorageSync("token") && uni.getStorageSync("token") !== "demo-token";
export const roleLabel = (role) =>
  ({
    ROLE_ADMIN: "系统管理员",
    ROLE_LEADER: "法规处负责人",
    ROLE_USER: "立法工作者",
    ROLE_REVIEWER: "审查员",
    ROLE_EVALUATOR: "评估员",
  })[role] ||
  role ||
  "未登录";
