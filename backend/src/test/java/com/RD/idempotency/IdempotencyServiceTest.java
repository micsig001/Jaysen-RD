package com.RD.idempotency;

import com.RD.common.BusinessException;
import com.RD.mapper.SysIdempotencyKeyMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * IdempotencyService 单元测试
 *
 * <p>不依赖 Spring Context，直接 mock 依赖。覆盖：</p>
 * <ol>
 *   <li>X-Idempotency-Key 提取与校验</li>
 *   <li>Redis 路径预留 Key（SETNX 成功）</li>
 *   <li>Redis 路径失败时回退到数据库兜底</li>
 * </ol>
 *
 * @author Mavis
 */
@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private SysIdempotencyKeyMapper idempotencyKeyMapper;

    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        service = new IdempotencyService(redisTemplate, idempotencyKeyMapper, new ObjectMapper());
    }

    // ============================================
    // extractKey
    // ============================================

    @Test
    @DisplayName("extractKey：缺少 X-Idempotency-Key → 400")
    void extractKey_missing_throws400() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Idempotency-Key")).thenReturn(null);

        assertThatThrownBy(() -> service.extractKey(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("缺少幂等性 Key");
    }

    @Test
    @DisplayName("extractKey：长度 5（<8）→ 400")
    void extractKey_tooShort_throws400() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Idempotency-Key")).thenReturn("short");

        assertThatThrownBy(() -> service.extractKey(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("8-64");
    }

    @Test
    @DisplayName("extractKey：非 UUID v4 格式（36 字符但格式错）→ 400")
    void extractKey_notUUIDv4_throws400() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Idempotency-Key"))
                .thenReturn("zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz");

        assertThatThrownBy(() -> service.extractKey(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("UUID v4");
    }

    @Test
    @DisplayName("extractKey：合法 UUID v4 → 返回原值")
    void extractKey_validUUIDv4_returnsKey() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        String validUUID = "550e8400-e29b-41d4-a716-446655440000";
        when(request.getHeader("X-Idempotency-Key")).thenReturn(validUUID);

        assertThat(service.extractKey(request)).isEqualTo(validUUID);
    }

    @Test
    @DisplayName("extractKey：非 UUID 格式（10 字符）→ 返回原值（不走 UUID 校验）")
    void extractKey_shortNonUUID_returnsKey() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Idempotency-Key")).thenReturn("customkey01");

        assertThat(service.extractKey(request)).isEqualTo("customkey01");
    }

    // ============================================
    // tryReserve
    // ============================================

    @Test
    @DisplayName("tryReserve：Redis SETNX 成功 → 返回 true")
    void tryReserve_redisSucceeds() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);

        boolean reserved = service.tryReserve("test-key-001", "CREATE_TASK", 86400L);

        assertThat(reserved).isTrue();
    }

    @Test
    @DisplayName("tryReserve：Redis SETNX 失败（Key 已存在）+ DB 查询也命中 → 返回 false")
    void tryReserve_redisFailsAndDbHits() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(false);
        when(idempotencyKeyMapper.selectByKey("test-key-001"))
                .thenReturn(new com.RD.entity.SysIdempotencyKey());

        boolean reserved = service.tryReserve("test-key-001", "CREATE_TASK", 86400L);

        assertThat(reserved).isFalse();
    }
}
