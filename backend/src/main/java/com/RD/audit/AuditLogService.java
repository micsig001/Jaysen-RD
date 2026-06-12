package com.RD.audit;

import com.RD.entity.SysAuditLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.RD.mapper.SysAuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 审计日志服务
 *
 * 异步写入 sys_audit_log 表，不阻塞主业务线程
 *
 * @author Mavis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final SysAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    /**
     * 异步记录审计日志
     */
    @Async
    public void recordAsync(AuditLogContext context) {
        try {
            SysAuditLog record = new SysAuditLog();
            record.setOperatorId(context.getOperatorId());
            record.setOperationType(context.getOperationType());
            record.setResourceType(context.getResourceType());
            record.setResourceId(context.getResourceId() != null
                    ? context.getResourceId().toString() : null);
            record.setDescription(context.getDescription());
            record.setBeforeData(toJson(context.getBeforeSnapshot()));
            record.setAfterData(toJson(context.getAfterSnapshot()));
            record.setIpAddress(context.getIpAddress());
            record.setUserAgent(truncate(context.getUserAgent(), 512));
            record.setOperationTime(LocalDateTime.now());

            auditLogMapper.insert(record);
            log.debug("[审计] 已记录: operator={}, op={}, resource={}/{}",
                    context.getOperatorId(), context.getOperationType(),
                    context.getResourceType(), context.getResourceId());
        } catch (Exception e) {
            // 审计日志写入失败不能影响主业务
            log.error("[审计] 写入失败", e);
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("[审计] 序列化快照失败: {}", e.getMessage());
            return obj.toString();
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
