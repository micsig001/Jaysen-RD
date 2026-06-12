package com.RD.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 幂等性 Key 实体
 *
 * <p>对应表 {@code sys_idempotency_key}（V1 SQL 定义）</p>
 *
 * <p>持久化兜底：Redis SETNX 失败时通过 UNIQUE 约束确保幂等性。</p>
 *
 * @author Mavis
 */
@Data
@TableName("sys_idempotency_key")
public class SysIdempotencyKey {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 幂等 Key（UUID v4 格式） */
    private String idempotencyKey;

    /** 操作类型（CREATE_TASK / UPDATE_USER_ROLE / ...） */
    private String operationType;

    /** 请求体哈希（防同 Key 不同 body） */
    private String requestHash;

    /** 首次返回结果（JSON） */
    private String responseData;

    /** 状态：IN_PROGRESS / COMPLETED / FAILED */
    private String status;

    /** 操作人 UserID */
    private String operatorId;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 过期时间（超过即视为可清理） */
    private LocalDateTime expiresAt;
}
