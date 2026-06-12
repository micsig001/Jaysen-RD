package com.RD.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等性注解
 *
 * <p>标注在 Controller 方法上，要求请求必须携带 {@code X-Idempotency-Key} 请求头，
 * 防止重复提交（网络重试、用户误操作）。</p>
 *
 * <p>工作机制（详见 {@link IdempotencyAspect}）：</p>
 * <ol>
 *   <li>拦截方法执行</li>
 *   <li>从请求头获取 X-Idempotency-Key</li>
 *   <li>Redis SETNX 检查 Key 是否已存在
 *     <ul>
 *       <li>存在：返回缓存结果（重复请求）</li>
 *       <li>不存在：执行方法，结果写入 Redis（TTL 24h）</li>
 *     </ul>
 *   </li>
 *   <li>业务异常时删除 Key，允许客户端重试</li>
 * </ol>
 *
 * <p>配合 {@code sys_idempotency_key} 数据库表做持久化兜底。</p>
 *
 * @author Mavis
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 操作类型（用于日志和审计）
     * 示例：CREATE_TASK、UPDATE_USER_ROLE
     */
    String operationType() default "";

    /**
     * Key 过期时间（秒）
     * 默认 24 小时，与 sys_idempotency_key 表 expires_at 对齐
     */
    long ttlSeconds() default 86400L;
}
