package com.RD.rd.ecn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建/更新 ECN 变更请求
 *
 * <p>状态字段、流程实例 ID、审批步骤均由后端管理,不允许前端传入。</p>
 */
@Data
public class CreateEcnRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 512, message = "标题不能超过 512 字符")
    private String title;

    /** DESIGN / MATERIAL / PROCESS / DOCUMENT */
    @NotBlank(message = "变更类型不能为空")
    private String changeType;

    /** NORMAL / URGENT / CRITICAL */
    private String urgency;

    @NotBlank(message = "变更原因不能为空")
    private String reason;

    @NotBlank(message = "变更描述不能为空")
    private String description;

    private String impactAnalysis;

    /** 受影响 BOM ID 列表（JSON 数组字符串，可空） */
    private String affectedBomIds;

    /** 关联项目 ID（可空） */
    private Long projectId;

    private Integer priority;

    private LocalDate targetDate;
}
