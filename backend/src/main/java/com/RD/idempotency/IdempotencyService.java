package com.RD.idempotency;

import com.RD.common.BusinessException;
import com.RD.entity.SysIdempotencyKey;
import com.RD.mapper.SysIdempotencyKeyMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 幂等性服务
 *
 * <p>封装 Redis 和数据库的幂等性检查逻辑：</p>
 * <ol>
 *   <li>{@link #tryReserve} - 尝试预留 Key（SETNX）</li>
 *   <li>{@link #getCachedResult} - 获取缓存结果</li>
 *   <li>{@link #saveResult} - 保存执行结果</li>
 *   <li>{@link #release} - 释放 Key（异常时调用）</li>
 * </ol>
 *
 * @author Mavis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    /** Redis Key 前缀 */
    private static final String REDIS_KEY_PREFIX = "idempotency:key:";

    /** UUID v4 格式校验正则 */
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final StringRedisTemplate redisTemplate;
    private final SysIdempotencyKeyMapper idempotencyKeyMapper;
    private final ObjectMapper objectMapper;

    /**
     * 尝试预留幂等性 Key
     *
     * <p>双重检查：先 Redis SETNX（快路径），再数据库 UNIQUE 约束（兜底）</p>
     *
     * @return true=首次请求（可执行业务），false=重复请求（应返回缓存）
     */
    public boolean tryReserve(String idempotencyKey, String operationType, long ttlSeconds) {
        String redisKey = REDIS_KEY_PREFIX + idempotencyKey;
        Boolean reserved = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PROCESSING", Duration.ofSeconds(ttlSeconds));
        if (Boolean.TRUE.equals(reserved)) {
            return true;
        }

        // Redis 路径失败或 Key 已存在，尝试数据库兜底
        log.debug("[幂等] Redis 路径已存在 Key，尝试数据库兜底: {}", idempotencyKey);
        return tryReserveByDatabase(idempotencyKey, operationType, ttlSeconds);
    }

    /**
     * 数据库兜底：通过 UNIQUE 约束判断是否已存在
     */
    private boolean tryReserveByDatabase(String idempotencyKey, String operationType, long ttlSeconds) {
        SysIdempotencyKey existing = idempotencyKeyMapper.selectByKey(idempotencyKey);
        if (existing != null) {
            log.info("[幂等] 数据库兜底检测到已存在 Key: {}", idempotencyKey);
            return false;
        }

        try {
            SysIdempotencyKey record = new SysIdempotencyKey();
            record.setIdempotencyKey(idempotencyKey);
            record.setOperationType(operationType);
            record.setStatus("IN_PROGRESS");
            record.setCreatedAt(LocalDateTime.now());
            record.setExpiresAt(LocalDateTime.now().plusSeconds(ttlSeconds));
            idempotencyKeyMapper.insert(record);
            return true;
        } catch (Exception e) {
            // 唯一键冲突 = 已存在
            log.info("[幂等] 数据库插入冲突，Key 已存在: {}", idempotencyKey);
            return false;
        }
    }

    /**
     * 获取缓存的响应数据
     */
    public Optional<String> getCachedResult(String idempotencyKey) {
        String redisKey = REDIS_KEY_PREFIX + idempotencyKey;
        String value = redisTemplate.opsForValue().get(redisKey);
        if (value == null || "PROCESSING".equals(value)) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    /**
     * 保存执行结果到 Redis 和数据库
     */
    public void saveResult(String idempotencyKey, Object result, long ttlSeconds) {
        String redisKey = REDIS_KEY_PREFIX + idempotencyKey;
        try {
            String json = objectMapper.writeValueAsString(result);
            // Redis：缓存响应结果
            redisTemplate.opsForValue().set(redisKey, json, Duration.ofSeconds(ttlSeconds));
            // 数据库：更新 response_data 字段
            idempotencyKeyMapper.updateResponseData(idempotencyKey, json,
                    LocalDateTime.now().plusSeconds(ttlSeconds));
        } catch (JsonProcessingException e) {
            log.error("[幂等] 序列化响应结果失败: {}", e.getMessage());
        }
    }

    /**
     * 释放 Key（异常时调用，允许客户端重试）
     *
     * <p>同时清理 Redis 缓存和数据库记录（让客户端可以立即重试）</p>
     */
    public void release(String idempotencyKey) {
        String redisKey = REDIS_KEY_PREFIX + idempotencyKey;
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.warn("[幂等] 释放 Redis Key 失败，依赖 TTL 自动过期: {}", e.getMessage());
        }
        try {
            idempotencyKeyMapper.deleteByKey(idempotencyKey);
        } catch (Exception e) {
            log.warn("[幂等] 释放数据库 Key 失败: {}", e.getMessage());
        }
    }

    /**
     * 获取并校验 X-Idempotency-Key 请求头
     *
     * <p>校验规则：</p>
     * <ul>
     *   <li>非空（缺失时抛 400）</li>
     *   <li>长度 8-64 字符</li>
     *   <li>如果是 UUID 格式，必须符合 v4</li>
     * </ul>
     */
    public String extractKey(HttpServletRequest request) {
        String key = request.getHeader("X-Idempotency-Key");
        if (key == null || key.isBlank()) {
            throw BusinessException.badRequest("缺少幂等性 Key: X-Idempotency-Key 请求头");
        }
        if (key.length() < 8 || key.length() > 64) {
            throw BusinessException.badRequest("X-Idempotency-Key 长度必须在 8-64 之间");
        }
        if (key.length() == 36 && !UUID_PATTERN.matcher(key).matches()) {
            throw BusinessException.badRequest("X-Idempotency-Key 格式不合法（UUID v4 必须是 8-4-4-4-12 十六进制）");
        }
        return key;
    }
}
