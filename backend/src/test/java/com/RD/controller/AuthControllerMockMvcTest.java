package com.RD.controller;

import com.RD.auth.JwtAuthenticationFilter;
import com.RD.auth.JwtTokenProvider;
import com.RD.wework.WeWorkAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController MockMvc 集成测试
 *
 * <p>覆盖 4 个端点（生产路径）：</p>
 * <ul>
 *   <li>POST /api/auth/token — 企微 code 换 JWT</li>
 *   <li>GET  /api/auth/wework/callback — OAuth 回调 + 重定向</li>
 *   <li>POST /api/auth/refresh — 刷新 Token</li>
 *   <li>POST /api/auth/logout — 退出登录</li>
 * </ul>
 *
 * <p>dev-login 的端到端测试在 {@code DevAuthControllerIntegrationTest}
 * （用 {@code AbstractIntegrationTest} + H2，验证 Upsert + JWT 签发）。</p>
 *
 * <p>本测试不加载 SecurityConfig（{@code excludeAutoConfiguration}），
 * JwtAuthenticationFilter 用 @MockBean 占位，避免 SecurityConfig 装配依赖。</p>
 *
 * @author Mavis
 */
@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.frontend-base-url=http://localhost:5173",
        "jwt.secret=test-jwt-secret-for-authcontroller-mockmvc-32chars-padding",
        "jwt.expiration=7200000",
        "jwt.refresh-expiration=604800000"
})
class AuthControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WeWorkAuthService weWorkAuthService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ============================================
    // POST /api/auth/token
    // ============================================

    @Test
    @DisplayName("POST /token：合法 code → 200 + access_token")
    void postToken_validCode_returns200() throws Exception {
        when(weWorkAuthService.loginByCode("valid_code")).thenReturn("mocked.jwt.token");

        Map<String, String> body = new HashMap<>();
        body.put("code", "valid_code");

        mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.access_token").value("mocked.jwt.token"))
                .andExpect(jsonPath("$.data.token_type").value("Bearer"));
    }

    @Test
    @DisplayName("POST /token：空 code → 400")
    void postToken_emptyCode_returns400() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("code", "");

        mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /token：无效 code → 401")
    void postToken_invalidCode_returns401() throws Exception {
        when(weWorkAuthService.loginByCode("invalid_code"))
                .thenThrow(new com.RD.common.BusinessException(401, "授权码无效"));

        Map<String, String> body = new HashMap<>();
        body.put("code", "invalid_code");

        mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // ============================================
    // GET /api/auth/wework/callback
    // ============================================

    @Test
    @DisplayName("GET /wework/callback：合法 code → 302 重定向到前端 /login/callback")
    void callback_validCode_redirectsToFrontend() throws Exception {
        when(weWorkAuthService.loginByCode("oauth_code")).thenReturn("mocked.jwt.token");

        mockMvc.perform(get("/api/auth/wework/callback").param("code", "oauth_code"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("/login/callback?token=mocked.jwt.token")));
    }

    @Test
    @DisplayName("GET /wework/callback：无效 code → 302 重定向到前端 /login?error=...")
    void callback_invalidCode_redirectsToLoginError() throws Exception {
        when(weWorkAuthService.loginByCode("bad_code"))
                .thenThrow(new com.RD.common.BusinessException(401, "授权码无效"));

        mockMvc.perform(get("/api/auth/wework/callback").param("code", "bad_code"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("/login?error=")));
    }

    // ============================================
    // POST /api/auth/refresh
    // ============================================

    @Test
    @DisplayName("POST /refresh：合法 refresh_token → 200 + 新 access_token")
    void refresh_validToken_returns200() throws Exception {
        when(weWorkAuthService.refreshToken("valid_refresh")).thenReturn("new.jwt.token");

        Map<String, String> body = new HashMap<>();
        body.put("refresh_token", "valid_refresh");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.access_token").value("new.jwt.token"));
    }

    @Test
    @DisplayName("POST /refresh：空 refresh_token → 400")
    void refresh_emptyToken_returns400() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("refresh_token", "");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ============================================
    // POST /api/auth/logout
    // ============================================

    @Test
    @DisplayName("POST /logout：带 Authorization → 200")
    void logout_withAuth_returns200() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer some.jwt.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("POST /logout：无 Authorization → 200（幂等）")
    void logout_withoutAuth_returns200() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
