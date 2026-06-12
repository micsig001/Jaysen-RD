package com.RD.rd.sprint.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建 Sprint 请求
 */
@Data
public class CreateSprintRequest {

    @NotNull(message = "项目 ID 不能为空")
    private Long projectId;

    @NotBlank(message = "Sprint 名称不能为空")
    @Size(max = 256, message = "Sprint 名称不能超过 256 字符")
    private String name;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @Size(max = 2000, message = "Sprint 目标不能超过 2000 字符")
    private String goal;
}
