package com.RD.audit;

import lombok.Data;

/**
 * 审计日志上下文
 *
 * 在 AuditLogAspect 中构造，传递给 AuditLogService 异步持久化
 *
 * @author Mavis
 */
@Data
public class AuditLogContext {

    /** 操作人 UserID（来自 SecurityContext principal） */
    private String operatorId;

    /** 操作类型 */
    private String operationType;

    /** 资源类型 */
    private String resourceType;

    /** 资源 ID（Long） */
    private Long resourceId;

    /** 操作描述 */
    private String description;

    /** 变更前数据快照（仅业务参数） */
    private Object beforeSnapshot;

    /** 变更后数据快照（方法返回 or 异常信息） */
    private Object afterSnapshot;

    /** 操作 IP */
    private String ipAddress;

    /** User-Agent */
    private String userAgent;
}
