package com.RD.controller;

import com.RD.common.Result;
import com.RD.entity.SysUser;
import com.RD.mapper.SysUserMapper;
import com.RD.auth.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 【开发专用】认证控制器
 *
 * <p>仅在 {@code dev} profile 下注册到 Spring 容器。
 * 提供本地绕过企微 OAuth 直接登录的端点，方便本地开发不配 corp 也能跑通登录流程。</p>
 *
 * <p>Phase 2 可扩展：
 *   <ul>
 *     <li>默认 mock 用户列表（admin / manager / employee 各一个）</li>
 *     <li>支持任意 username + role 直接签发 JWT</li>
 *   </ul>
 * </p>
 *
 * <p>⚠️ 部署到 prod 之前请确认 {@code spring.profiles.active=prod}，本 Controller 不会启动。</p>
 *
 * @author Mavis
 */
@Slf4j
@Profile("dev")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class DevAuthController {

    private final SysUserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 本地绕过 OAuth 直接登录
     *
     * <p>请求：</p>
     * <pre>
     * POST /api/auth/dev-login
     * { "username": "alice", "role": "ADMIN" }
     * </pre>
     *
     * <p>行为：</p>
     * <ol>
     *   <li>Upsert 用户（已存在则更新，不存在则创建）</li>
     *   <li>签发 JWT Token</li>
     *   <li>返回 token + userId + name + role</li>
     * </ol>
     */
    @PostMapping("/dev-login")
    public Result<Map<String, String>> devLogin(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        if (!StringUtils.hasText(username)) {
            return Result.badRequest("username 不能为空");
        }
        String role = request.getOrDefault("role", "ADMIN").toUpperCase();
        if (!role.matches("ADMIN|MANAGER|EMPLOYEE")) {
            return Result.badRequest("role 必须是 ADMIN / MANAGER / EMPLOYEE");
        }

        SysUser user = userMapper.selectByUserId(username);
        if (user == null) {
            user = new SysUser();
            user.setUserId(username);
            user.setCreatedAt(LocalDateTime.now());
            user.setManualRole(true);
        }
        user.setName(username);
        user.setRole(role);
        user.setStatus(1);
        user.setUpdatedAt(LocalDateTime.now());
        if (user.getId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }

        String token = jwtTokenProvider.generateAccessToken(
                user.getUserId(), user.getName(), user.getRole());

        Map<String, String> resp = new HashMap<>();
        resp.put("access_token", token);
        resp.put("token_type", "Bearer");
        resp.put("userId", user.getUserId());
        resp.put("name", user.getName());
        resp.put("role", user.getRole());
        log.info("[Dev-Login] user={} role={}", username, role);
        return Result.success(resp);
    }
}
