package com.RD.rd.sprint.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Sprint 任务 VO（看板卡片用）
 */
@Data
public class SprintTaskVO {

    private Long id;
    private Long projectId;
    private Long sprintId;

    private String title;
    private String description;

    /** FEATURE / BUG / OPTIMIZATION / TEST */
    private String type;
    /** LOW / MEDIUM / HIGH / CRITICAL */
    private String priority;
    /** BACKLOG / TODO / IN_PROGRESS / REVIEW / DONE */
    private String status;

    private String assigneeUserid;
    private String assigneeName;

    private String reporterUserid;
    private String reporterName;

    private BigDecimal estimatedHours;
    private BigDecimal actualHours;
    private Integer storyPoints;

    /** 前端友好: 解析 tags JSON 数组 */
    private List<String> tags;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    private Integer orderNum;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
