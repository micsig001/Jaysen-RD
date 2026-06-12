package com.RD.task.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务状态历史 VO
 *
 * <p>对应 {@code task_status_history} 表。前端用 {@code STATUS_LABELS} map
 * 自行把 {@code fromStatus} / {@code toStatus} 转中文显示。</p>
 */
@Data
public class TaskStatusHistoryVO {

    private Long id;

    private Long taskId;

    /** 原状态（创建时为 null） */
    private String fromStatus;

    /** 新状态 */
    private String toStatus;

    /** 操作人 UserID */
    private String operatorId;

    /** 操作人姓名（关联 sys_user 冗余） */
    private String operatorName;

    /** 备注 / 驳回原因 / 撤回原因等 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
