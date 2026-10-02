package com.legal.legislation.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器。
 *
 * 流程:
 *  1) 从 Authorization: Bearer xxx 读 token
 *  2) JwtUtil 解析 claims(userId / role / sub)
 *  3) 构造 Authentication 塞入 SecurityContext
 *  4) 同时把 userId 写回请求头 X-User-Id(兼容现有业务代码读 X-User-Id)
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String AUTH_HEADER = "Authorization";
    public static final String AUTH_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(AUTH_HEADER);
        if (header != null && header.startsWith(AUTH_PREFIX)) {
            String token = header.substring(AUTH_PREFIX.length()).trim();
            Claims claims = jwtUtil.parse(token);
            if (claims != null) {
                String username = claims.getSubject();
                String role     = (String) claims.get("role");
                Object userIdObj = claims.get("userId");
                if (role == null || role.isBlank()) role = "ROLE_USER";

                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(role))
                    );
                SecurityContextHolder.getContext().setAuthentication(auth);

                // 兼容:把 userId / role 同步回请求头,业务代码继续读 X-User-Id 即可
                if (userIdObj != null) {
                    request = new UserIdRequestWrapper(request, String.valueOf(userIdObj), role);
                }
            }
        }
        chain.doFilter(request, response);
    }
}
