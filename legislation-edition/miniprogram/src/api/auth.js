/**
 * 认证接口
 */
import request from "./request.js";

export const login = (username, password) =>
  request({ url: "/auth/login", method: "POST", data: { username, password } });

export const me = (token) =>
  request({
    url: "/auth/me",
    header: token ? { Authorization: "Bearer " + token } : {},
  });

export const refresh = (token) =>
  request({ url: "/auth/refresh", method: "POST", data: { token } });

export const logout = () => request({ url: "/auth/logout", method: "POST" });

export const health = () => request({ url: "/auth/health" });
