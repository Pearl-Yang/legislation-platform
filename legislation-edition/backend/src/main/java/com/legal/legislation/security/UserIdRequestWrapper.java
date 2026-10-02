package com.legal.legislation.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * 轻量 HttpServletRequestWrapper:
 *  - 把 userId / role 同步回 X-User-Id / X-Role 请求头
 *  - 让现有业务代码(直接 @RequestHeader("X-User-Id"))无须改动即可拿到登录身份
 */
public class UserIdRequestWrapper extends HttpServletRequestWrapper {

    private final String userId;
    private final String role;

    public UserIdRequestWrapper(HttpServletRequest request, String userId, String role) {
        super(request);
        this.userId = userId;
        this.role = role;
    }

    @Override
    public String getHeader(String name) {
        if (XUserIdAuthFilter.HEADER_USER_ID.equalsIgnoreCase(name)) {
            return userId;
        }
        if (XUserIdAuthFilter.HEADER_ROLE.equalsIgnoreCase(name)) {
            return role;
        }
        return super.getHeader(name);
    }
}
