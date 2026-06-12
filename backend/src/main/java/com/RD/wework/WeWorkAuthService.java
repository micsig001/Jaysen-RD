package com.RD.wework;

import com.fasterxml.jackson.databind.JsonNode;
import com.RD.auth.JwtTokenProvider;
import com.RD.common.BusinessException;
import com.RD.entity.SysUser;
import com.RD.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 企业微信认证服务
 *
 * <p>处理 OAuth2.0 授权流程、用户信息同步和 JWT Token 签发。</p>
 *
 * <p>{@link WeWorkApiClient} 注入的是 Spring 容器中的 Bean，由 {@link WeWorkApiConfig}
 * 根据 profile 决定后续是 {@link FakeWeWorkApiClient}（dev/test）还是真实实现（prod）。
 * 业务侧无需感知。</p>
 *
 * @author Mavis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeWorkAuthService {

    private final WeWorkApiClient weWorkApiClient;
    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserMapper userMapper;

    /**
     * 登录返回的 Token 集合（access + refresh）
     */
    public record TokenPair(String accessToken, String refreshToken) { }

    /**
     * 通过企微授权码进行登录
     */
    @Transactional(rollbackFor = Exception.class)
    public TokenPair loginByCode(String code) {
        // 1. 通过授权码获取用户 ID
        JsonNode userInfo = weWorkApiClient.getUserInfoByCode(code);
        if (userInfo == null || !userInfo.has("UserId")) {
            throw new BusinessException(401, "授权码无效或已过期");
        }

        String userId = userInfo.get("UserId").asText();
        log.info("企微授权登录, UserID: {}, client={}", userId,
                weWorkApiClient.getClass().getSimpleName());

        // 2. 获取用户详细信息
        JsonNode userDetail = weWorkApiClient.getUserDetail(userId);
        if (userDetail == null) {
            throw new BusinessException(500, "获取用户详细信息失败");
        }

        // 3. 同步用户信息到本地数据库（Upsert）
        SysUser user = syncUserFromWeWork(userDetail);

        // 4. 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }

        // 5. 生成 access + refresh Token
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getUserId(),
                user.getName(),
                user.getRole()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());

        log.info("用户 {} 登录成功", user.getName());
        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * 从企微同步用户信息到本地数据库
     */
    private SysUser syncUserFromWeWork(JsonNode userDetail) {
        String userId = userDetail.get("userid").asText();

        SysUser existingUser = userMapper.selectByUserId(userId);

        if (existingUser != null) {
            // 更新用户信息
            existingUser.setName(userDetail.has("name") ? userDetail.get("name").asText() : userId);
            existingUser.setMobile(userDetail.has("mobile") ? userDetail.get("mobile").asText() : null);
            existingUser.setEmail(userDetail.has("email") ? userDetail.get("email").asText() : null);
            existingUser.setAvatarUrl(userDetail.has("avatar") ? userDetail.get("avatar").asText() : null);

            if (userDetail.has("department") && userDetail.get("department").isArray()
                    && userDetail.get("department").size() > 0) {
                existingUser.setDepartmentId(userDetail.get("department").get(0).asLong());
            }

            existingUser.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(existingUser);

            log.debug("更新用户信息: {}", userId);
            return existingUser;
        } else {
            // 创建新用户
            SysUser newUser = new SysUser();
            newUser.setUserId(userId);
            newUser.setName(userDetail.has("name") ? userDetail.get("name").asText() : userId);
            newUser.setMobile(userDetail.has("mobile") ? userDetail.get("mobile").asText() : null);
            newUser.setEmail(userDetail.has("email") ? userDetail.get("email").asText() : null);
            newUser.setAvatarUrl(userDetail.has("avatar") ? userDetail.get("avatar").asText() : null);
            newUser.setStatus(1);

            newUser.setRole("EMPLOYEE");
            newUser.setManualRole(false);

            if (userDetail.has("department") && userDetail.get("department").isArray()
                    && userDetail.get("department").size() > 0) {
                newUser.setDepartmentId(userDetail.get("department").get(0).asLong());
            }

            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(newUser);

            log.info("创建新用户: {}", userId);
            return newUser;
        }
    }

    /**
     * 刷新 Token（access + refresh 同时轮换）
     */
    public TokenPair refreshToken(String refreshToken) {
        // validateToken 在 token 无效/过期时抛 IllegalArgumentException，原先 `if (... == null)` 是死代码
        // 现在显式捕获并转为 401
        try {
            jwtTokenProvider.validateToken(refreshToken);
        } catch (IllegalArgumentException e) {
            log.warn("刷新令牌无效或已过期: {}", e.getMessage());
            throw new BusinessException(401, "刷新令牌无效或已过期");
        }

        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        SysUser user = userMapper.selectByUserId(userId);

        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }

        String newAccess = jwtTokenProvider.generateAccessToken(user.getUserId(), user.getName(), user.getRole());
        String newRefresh = jwtTokenProvider.generateRefreshToken(user.getUserId());
        return new TokenPair(newAccess, newRefresh);
    }

    /**
     * 为旧版单 token 接口保留的 Map 形式（仅向后兼容 — 新代码请用 TokenPair）
     */
    public Map<String, String> loginByCodeAsMap(String code) {
        TokenPair pair = loginByCode(code);
        Map<String, String> m = new HashMap<>();
        m.put("access_token", pair.accessToken());
        m.put("refresh_token", pair.refreshToken());
        m.put("token_type", "Bearer");
        return m;
    }
}
