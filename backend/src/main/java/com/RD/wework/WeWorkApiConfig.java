package com.RD.wework;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WeWork API 客户端 Bean 注册配置
 *
 * <p>根据 Spring profile 选择激活的 {@link WeWorkApiClient} 实现：</p>
 * <ul>
 *   <li>{@code dev} / {@code test} —— 注册 {@link FakeWeWorkApiClient}（{@code @Primary}），
 *       端到端跑通 OAuth 流程无需企微 corp 账号</li>
 *   <li>其它 profile（{@code prod}、未设置等）—— 注册真实 {@link WeWorkApiClient}</li>
 * </ul>
 *
 * <p>Phase 1 状态：仅注册真实 WeWorkApiClient；FakeWeWorkApiClient 待 Phase 2 接入测试时引入。</p>
 *
 * @author Mavis
 */
@Slf4j
@Configuration
public class WeWorkApiConfig {

    /**
     * 真实客户端（prod / 默认环境）—— 非 Primary
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(WeWorkApiClient.class)
    public WeWorkApiClient realWeWorkApiClient(
            WebClient.Builder webClientBuilder,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        log.info("[WeWork] 注册 真实 WeWorkApiClient（默认/prod profile）");
        return new WeWorkApiClient(webClientBuilder, redisTemplate, objectMapper);
    }
}
