package com.RD.wework;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WeWork API 客户端 Bean 注册配置
 *
 * <p>根据 Spring profile 选择激活的 {@link WeWorkApiClient} 实现：</p>
 * <ul>
 *   <li>{@code dev} / {@code test} —— 注册 {@link FakeWeWorkApiClient}（extends WeWorkApiClient），
 *       不发网络请求, 所有接口返回 mock 数据</li>
 *   <li>其它 profile（{@code prod}、未设置等）—— 注册真实 {@link WeWorkApiClient}，调企微 API</li>
 * </ul>
 *
 * <p>切换方法：环境变量 {@code SPRING_PROFILES_ACTIVE=dev} 或启动参数
 * {@code -Dspring.profiles.active=dev}。生产部署不设此变量即可走真实 client。</p>
 *
 * @author Mavis
 */
@Slf4j
@Configuration
public class WeWorkApiConfig {

    /**
     * 真实客户端（prod / 默认环境）
     */
    @Bean
    @Profile("!dev & !test")
    public WeWorkApiClient realWeWorkApiClient(
            WebClient.Builder webClientBuilder,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        log.info("[WeWork] 注册 真实 WeWorkApiClient（prod profile）");
        return new WeWorkApiClient(webClientBuilder, redisTemplate, objectMapper);
    }

    /**
     * Fake 客户端（dev / test 环境）
     */
    @Bean
    @Profile({"dev", "test"})
    public WeWorkApiClient fakeWeWorkApiClient(
            WebClient.Builder webClientBuilder,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        log.info("[WeWork] 注册 FakeWeWorkApiClient（dev/test profile）");
        return new FakeWeWorkApiClient(webClientBuilder, redisTemplate, objectMapper);
    }
}
