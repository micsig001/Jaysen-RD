package com.RD.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtTokenProvider 单元测试
 *
 * <p>不依赖 Spring Context，直接 {@code new JwtTokenProvider(secret, expiration, refreshExpiration)}。
 * 测三件事：签发 access token、签发 refresh token、验签失败抛 IllegalArgumentException。</p>
 *
 * @author Mavis
 */
@DisplayName("JwtTokenProvider 单元测试")
class JwtTokenProviderTest {

    private static final String SECRET = "test-jwt-secret-32-chars-or-more-padding";
    private static final long EXPIRATION = 7_200_000L;        // 2h
    private static final long REFRESH_EXPIRATION = 604_800_000L; // 7d

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, EXPIRATION, REFRESH_EXPIRATION);
    }

    @Test
    @DisplayName("生成 access token + 解析出 userId / name / role")
    void generateAndParseAccessToken() {
        String token = provider.generateAccessToken("alice", "Alice Wang", "ADMIN");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // JWT 三段式

        assertThat(provider.getUserIdFromToken(token)).isEqualTo("alice");
        assertThat(provider.getRoleFromToken(token)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("生成 refresh token + 解析出 userId")
    void generateAndParseRefreshToken() {
        String token = provider.generateRefreshToken("bob");

        assertThat(token).isNotBlank();
        assertThat(provider.getUserIdFromToken(token)).isEqualTo("bob");
        // refresh token 没 role 字段
        assertThatThrownBy(() -> provider.getRoleFromToken(token))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("篡改 token → 验签失败抛 IllegalArgumentException")
    void tamperedToken_throws() {
        String token = provider.generateAccessToken("alice", "Alice", "EMPLOYEE");
        // 篡改 signature 段
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThatThrownBy(() -> provider.validateToken(tampered))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("空 token → 验签失败")
    void emptyToken_throws() {
        assertThatThrownBy(() -> provider.validateToken(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("不同 secret 签发的 token → 验签失败")
    void differentSecret_throws() {
        String token = provider.generateAccessToken("alice", "Alice", "EMPLOYEE");
        // 用不同 secret 构造另一个 provider
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                "other-secret-32-chars-padding-padding-x", EXPIRATION, REFRESH_EXPIRATION);

        assertThatThrownBy(() -> otherProvider.validateToken(token))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
