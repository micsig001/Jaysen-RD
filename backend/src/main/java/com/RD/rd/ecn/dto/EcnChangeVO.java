package com.RD.rd.ecn.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ECN 变更展示 VO
 */
@Data
public class EcnChangeVO {

    private Long id;
    private String ecnNumber;
    private String title;

    /** DESIGN / MATERIAL / PROCESS / DOCUMENT */
    private String changeType;
    /** NORMAL / URGENT / CRITICAL */
    private String urgency;

    private String reason;
    private String description;
    private String impactAnalysis;
    private String affectedBomIds;

    private String requesterUserid;
    private String requesterName;

    private Long projectId;

    /** DRAFT / UNDER_REVIEW / APPROVED / REJECTED / IMPLEMENTED / CANCELLED */
    private String status;

    private Integer priority;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate targetDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    /** Flowable 流程实例 ID（提交审批后回填） */
    private String processInstanceId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
