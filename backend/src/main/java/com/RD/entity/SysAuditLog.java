package com.RD.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志实体
 *
 * 对应表 {@code sys_audit_log}（V1 SQL 末段定义）
 *
 * @author Mavis
 */
@Data
@TableName("sys_audit_log")
public class SysAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人 UserID（来自 SecurityContext principal） */
    private String operatorId;

    /** 操作人姓名（冗余） */
    private String operatorName;

    /** 操作类型：CREATE / UPDATE / DELETE / ACCEPT / REJECT / SUBMIT ... */
    private String operationType;

    /** 资源类型：TASK / PROJECT / ECN / USER / EQUIPMENT / BOM ... */
    private String resourceType;

    /** 资源 ID（V1 SQL 是 VARCHAR(128)，本字段用 String 兼容 Long/String） */
    private String resourceId;

    /** 操作描述 */
    private String description;

    /** 变更前数据快照（JSON） */
    private String beforeData;

    /** 变更后数据快照（JSON） */
    private String afterData;

    /** 操作 IP */
    private String ipAddress;

    /** User-Agent */
    private String userAgent;

    /** 操作时间（V1 SQL 字段名 operation_time） */
    private LocalDateTime operationTime;
}
