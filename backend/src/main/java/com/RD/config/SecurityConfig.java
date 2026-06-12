package com.RD.config;

import com.RD.auth.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 配置
 *
 * <p>JWT 无状态认证：</p>
 * <ul>
 *   <li>禁用 CSRF（用 JWT 替代 CSRF Token）</li>
 *   <li>按环境配置 CORS（dev 默认 localhost，prod 通过 ALLOWED_ORIGINS 配）</li>
 *   <li>Session 策略：STATELESS（每次请求带 Token）</li>
 *   <li>公开端点：/api/auth/**、/actuator/health、Swagger UI</li>
 *   <li>JWT 过滤器加在 UsernamePasswordAuthenticationFilter 之前</li>
 *   <li>启用 {@code @EnableMethodSecurity} 支持 {@code @PreAuthorize}</li>
 * </ul>
 *
 * @author Mavis
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（JWT 模式下不需要）
            .csrf(csrf -> csrf.disable())
            // 配置 CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 无状态 Session
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 授权规则
            .authorizeHttpRequests(auth -> auth
                // 公开端点：认证相关
                .requestMatchers("/api/auth/**").permitAll()
                // 企微 OAuth 回调（GET /api/auth/wework/callback）
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/auth/wework/callback").permitAll()
                // Swagger 文档（dev 环境）
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // 健康检查
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // 其他所有请求需要认证
                .anyRequest().authenticated()
            )
            // JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 按环境配置允许的 origin，避免生产环境全通配
        String allowedOrigins = System.getenv("ALLOWED_ORIGINS");
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            // 默认：仅允许开发环境域名
            configuration.setAllowedOrigins(List.of(
                    "http://localhost:5173",
                    "http://localhost:5174",
                    "http://127.0.0.1:5173",
                    "http://127.0.0.1:5174"
            ));
        } else {
            configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
