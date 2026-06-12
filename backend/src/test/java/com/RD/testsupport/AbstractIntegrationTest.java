package com.RD.testsupport;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * 集成测试基类
 *
 * <p>所有需要 Spring Context 的测试继承此类。</p>
 *
 * <p>配置：</p>
 * <ul>
 *   <li>{@code @ActiveProfiles("test")} → 加载 {@code application-test.yml}</li>
 *   <li>Redis 自动配置在 application-test.yml 中被排除，
 *       {@code @MockBean} 注入 {@link StringRedisTemplate} 占位</li>
 *   <li>H2 schema 由 {@code spring.sql.init} 在启动时执行
 *       {@code classpath:db/test/schema-h2.sql}</li>
 *   <li>Flowable / archive 调度器在测试中关闭</li>
 * </ul>
 *
 * @author Mavis
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    /**
     * Mock Redis：避免测试启动时连接真实 Redis。
     * <p>RedisAutoConfiguration 已在 application-test.yml 排除，
     * 此处 @MockBean 仅用于让 StringRedisTemplate 类型可注入。</p>
     */
    @MockBean
    protected StringRedisTemplate stringRedisTemplate;
}
