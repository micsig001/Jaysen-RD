package com.RD.task.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建任务请求
 */
@Data
public class CreateTaskRequest {

    @NotBlank(message = "任务标题不能为空")
    @Size(max = 256, message = "任务标题不能超过 256 字符")
    private String title;

    @Size(max = 5000, message = "任务描述不能超过 5000 字符")
    private String description;

    @Size(max = 512, message = "来源备注不能超过 512 字符")
    private String sourceRemark;

    /** 接收人 UserID（必填,自指派时也填自己） */
    @NotBlank(message = "执行人不能为空")
    private String assigneeId;

    @NotNull(message = "优先级不能为空")
    @Min(value = 1, message = "优先级最小为 1")
    @Max(value = 4, message = "优先级最大为 4")
    private Integer priority;

    /** 预估时长（小时），可空 */
    @Min(value = 1, message = "预估时长必须大于 0")
    private Integer estimatedDuration;
}
