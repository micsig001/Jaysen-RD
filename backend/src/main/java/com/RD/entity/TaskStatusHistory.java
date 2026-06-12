package com.RD.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务状态流转历史
 *
 * <p>对应表 {@code task_status_history}（V1 SQL 2.2 节）。
 * 每次状态变更由 {@code TaskStateMachineService}（Phase 2.5 接入）写入。</p>
 */
@Data
@TableName("task_status_history")
public class TaskStatusHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private Long taskId;

    @TableField("from_status")
    private String fromStatus;

    @TableField("to_status")
    private String toStatus;

    @TableField("operator_id")
    private String operatorId;

    @TableField("operator_name")
    private String operatorName;

    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
