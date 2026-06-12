package com.RD.auth;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * JWT 认证过滤器
 *
 * <p>从请求头提取并验证 JWT Token，将用户信息存入 SecurityContext。</p>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>认证失败时不再直接写 JSON 响应，委托给 {@link HandlerExceptionResolver}，
 *       由 {@code @RestControllerAdvice} 统一处理，输出标准 {@code Result} JSON</li>
 *   <li>容器关闭钩子用 {@code @PreDestroy}，避免覆盖
 *       {@code GenericFilterBean.destroy()} 的 Spring 容器语义</li>
 *   <li>Token 黑名单：登出时调 {@link #blacklistToken} 将 token 写入 Redis，
 *       后续请求命中黑名单直接拒绝</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    /**
     * 注入 Spring MVC 的 HandlerExceptionResolver（典型实现是
     * {@code ExceptionHandlerExceptionResolver}），用于在 Filter 链中将
     * 业务异常/认证异常委托给 {@code @RestControllerAdvice} 统一处理。
     * 显式 @Qualifier 避免与 {@code errorAttributes} bean 冲突。
     */
    private final HandlerExceptionResolver resolver;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   StringRedisTemplate redisTemplate,
                                   @Qualifier("handlerExceptionResolver")
                                   HandlerExceptionResolver resolver) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = resolveToken(request);

            if (token != null) {
                // 检查 Token 是否在黑名单中
                if (isTokenBlacklisted(token)) {
                    log.warn("Token 已在黑名单中: {}",
                            token.substring(0, Math.min(20, token.length())));
                    resolver.resolveException(request, response, null,
                            new BadCredentialsException("Token 已失效"));
                    return;
                }

                // 验证 Token 并获取用户信息
                String userId = jwtTokenProvider.getUserIdFromToken(token);
                String role = jwtTokenProvider.getRoleFromToken(token);

                // 构建认证对象
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                // 将认证信息存入 SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("用户认证成功: userId={}, role={}", userId, role);
            }

            filterChain.doFilter(request, response);

        } catch (IllegalArgumentException e) {
            log.warn("JWT 验证失败: {}", e.getMessage());
            resolver.resolveException(request, response, null,
                    new BadCredentialsException("无效的 Token", e));
        } catch (AuthenticationException e) {
            log.warn("JWT 认证失败: {}", e.getMessage());
            resolver.resolveException(request, response, null, e);
        } catch (Exception e) {
            log.error("JWT 认证处理异常", e);
            resolver.resolveException(request, response, null,
                    new BadCredentialsException("认证失败", e));
        }
    }

    /**
     * 从请求头中提取 Token
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(TOKEN_HEADER);
        if (bearerToken != null && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    /**
     * 检查 Token 是否在黑名单中
     */
    private boolean isTokenBlacklisted(String token) {
        String blacklistKey = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }

    /**
     * 将 Token 加入黑名单
     */
    public void blacklistToken(String token, long expirationMillis) {
        String blacklistKey = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(blacklistKey, "1", expirationMillis, TimeUnit.MILLISECONDS);
        log.info("Token 已加入黑名单, 有效期: {}ms", expirationMillis);
    }

    /**
     * Bean 销毁阶段清理 SecurityContext
     */
    @PreDestroy
    public void cleanup() {
        SecurityContextHolder.clearContext();
    }
}
