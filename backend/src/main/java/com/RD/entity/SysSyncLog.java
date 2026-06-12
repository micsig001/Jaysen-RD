package com.RD.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 同步日志实体
 *
 * <p>对应表 {@code sys_sync_log}（V1 SQL 1.4 节）</p>
 */
@Data
@TableName("sys_sync_log")
public class SysSyncLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 同步类型：USER / DEPARTMENT */
    private String syncType;

    /** 触发方式：MANUAL / SCHEDULED / INCREMENTAL / FULL */
    private String triggerType;

    /** 执行状态：RUNNING / SUCCESS / FAILED / PARTIAL */
    private String status;

    /** 总条数 */
    private Integer totalCount;

    /** 成功条数 */
    private Integer successCount;

    /** 失败条数 */
    private Integer failedCount;

    /** 错误信息 */
    private String errorMessage;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** 结束时间 */
    private LocalDateTime finishedAt;

    /** 耗时（毫秒） */
    private Long durationMs;

    private LocalDateTime createdAt;
}
