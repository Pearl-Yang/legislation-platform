package com.legal.legislation.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * 简化版用户识别过滤器。
 *
 * 当前立法版不接 JWT（注释说明 by gov_edition），改为从请求头 X-User-Id 直接读取登录态。
 *   - 由前端在登录后从 localStorage 取出 userId，写到 Axios 拦截器里
 *   - 后端拿到 X-User-Id 之后注入到 SecurityContext
 *   - 没有 X-User-Id 的请求视为匿名，可访问 /auth/** 与 Swagger 资源
 *
 * 后续接入 legislation_commons 后，本类会被 JwtTokenFilter 覆盖（同一 Bean 名）。
 */
public class XUserIdAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID  = "X-User-Id";
    public static final String HEADER_ROLE     = "X-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String userId = request.getHeader(HEADER_USER_ID);
        String role  = request.getHeader(HEADER_ROLE);

        if (userId != null && !userId.isBlank()) {
            String roleAuthority = (role == null || role.isBlank()) ? "ROLE_USER" : role;
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(roleAuthority))
                );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(request, response);
    }
}