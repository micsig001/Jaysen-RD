package com.RD.task.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务展示 VO
 *
 * <p>对应后端 {@code Task} 实体；用于列表/详情响应。
 * 前端展示用，不包含敏感字段。</p>
 */
@Data
public class TaskVO {

    private Long id;
    private String taskNo;
    private String title;
    private String description;
    private String sourceRemark;

    private String creatorId;
    private String creatorName;        // 关联 sys_user 冗余

    private String assigneeId;
    private String assigneeName;       // 关联 sys_user 冗余

    /** 1-最高 / 2-高 / 3-中 / 4-低 */
    private Integer priority;

    /** PENDING_ACCEPT / IN_PROGRESS / PENDING_VERIFY / COMPLETED / REJECTED / WITHDRAWN */
    private String status;

    private Boolean selfAssigned;
    private Integer estimatedDuration;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actualStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actualDeadline;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actualEndTime;

    private String rejectReason;
    private Integer version;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
