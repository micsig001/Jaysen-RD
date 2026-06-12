package com.RD.rd.sprint.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建/更新 Sprint 任务请求（看板卡片用）
 */
@Data
public class CreateSprintTaskRequest {

    @NotNull(message = "项目 ID 不能为空")
    private Long projectId;

    /** 所属冲刺 ID（可空：BACKLOG 任务） */
    private Long sprintId;

    @NotBlank(message = "标题不能为空")
    @Size(max = 512, message = "标题不能超过 512 字符")
    private String title;

    @Size(max = 5000, message = "描述不能超过 5000 字符")
    private String description;

    /** FEATURE / BUG / OPTIMIZATION / TEST (默认 FEATURE) */
    private String type;

    /** LOW / MEDIUM / HIGH / CRITICAL (默认 MEDIUM) */
    private String priority;

    /** BACKLOG / TODO / IN_PROGRESS / REVIEW / DONE (默认 BACKLOG) */
    private String status;

    private String assigneeUserid;

    @Min(value = 0, message = "预估工时不能为负")
    private BigDecimal estimatedHours;

    @Min(value = 0, message = "故事点不能为负")
    @Max(value = 100, message = "故事点不能超过 100")
    private Integer storyPoints;

    private String tags;

    private LocalDate dueDate;

    private Integer orderNum;
}
