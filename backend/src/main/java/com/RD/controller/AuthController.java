package com.RD.controller;

import com.RD.auth.JwtAuthenticationFilter;
import com.RD.auth.JwtTokenProvider;
import com.RD.common.Result;
import com.RD.wework.WeWorkAuthService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 *
 * <p>处理企业微信 OAuth2.0 登录、Token 刷新和退出登录。</p>
 *
 * <p>接口列表：</p>
 * <ul>
 *   <li>POST /api/auth/token        — 用企微 code 换 JWT Token</li>
 *   <li>GET  /api/auth/wework/callback — 企微 OAuth 回调（GET 方式）</li>
 *   <li>POST /api/auth/refresh      — 刷新 Token</li>
 *   <li>POST /api/auth/logout       — 退出登录（Token 进黑名单）</li>
 *   <li>POST /api/auth/dev-login    — 【dev only】本地绕过 OAuth 直接登录</li>
 * </ul>
 *
 * @author Mavis
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final WeWorkAuthService weWorkAuthService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    /**
     * 通过企微授权码换取 JWT Token
     */
    @PostMapping("/token")
    public Result<Map<String, String>> getToken(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        if (code == null || code.isEmpty()) {
            return Result.badRequest("授权码不能为空");
        }

        try {
            String accessToken = weWorkAuthService.loginByCode(code);
            Map<String, String> response = new HashMap<>();
            response.put("access_token", accessToken);
            response.put("token_type", "Bearer");
            return Result.success(response);
        } catch (Exception e) {
            log.error("登录失败", e);
            return Result.error(401, e.getMessage());
        }
    }

    /**
     * 企微 OAuth2.0 回调接口（GET）
     *
     * <p>流程：</p>
     * <ol>
     *   <li>企微工作台点击应用 → 企微服务器重定向到本接口（带 code）</li>
     *   <li>用 code 换取用户信息 + JWT Token</li>
     *   <li>重定向回前端，URL 带上 token</li>
     * </ol>
     */
    @GetMapping("/wework/callback")
    public void weworkCallback(@RequestParam("code") String code,
                                @RequestParam(value = "state", required = false) String state,
                                HttpServletResponse response) throws IOException {
        log.info("[企微 OAuth] 收到回调, code={}, state={}", code, state);

        try {
            String accessToken = weWorkAuthService.loginByCode(code);
            String redirectUrl = frontendBaseUrl + "/login/callback"
                    + "?token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
            if (state != null) {
                redirectUrl += "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
            }
            log.info("[企微 OAuth] 登录成功，重定向到前端: {}", redirectUrl);
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("[企微 OAuth] 登录失败", e);
            String errorUrl = frontendBaseUrl + "/login?error="
                    + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            response.sendRedirect(errorUrl);
        }
    }

    /**
     * 刷新 Token
     */
    @PostMapping("/refresh")
    public Result<Map<String, String>> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refresh_token");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return Result.badRequest("刷新令牌不能为空");
        }

        try {
            String accessToken = weWorkAuthService.refreshToken(refreshToken);
            Map<String, String> response = new HashMap<>();
            response.put("access_token", accessToken);
            response.put("token_type", "Bearer");
            return Result.success(response);
        } catch (Exception e) {
            log.error("刷新 Token 失败", e);
            return Result.error(401, e.getMessage());
        }
    }

    /**
     * 退出登录（Token 进黑名单）
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                Claims claims = jwtTokenProvider.validateToken(token);
                long expirationTime = claims.getExpiration().getTime();
                long remainingTime = expirationTime - System.currentTimeMillis();
                if (remainingTime > 0) {
                    jwtAuthenticationFilter.blacklistToken(token, remainingTime);
                    log.info("用户退出登录, Token 已加入黑名单");
                }
            } catch (Exception e) {
                log.warn("Token 验证失败，但仍执行退出操作", e);
            }
        }
        return Result.success();
    }
}
