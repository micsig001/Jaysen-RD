package com.RD.rd.defect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建缺陷请求
 */
@Data
public class CreateDefectRequest {

    @NotBlank(message = "缺陷标题不能为空")
    @Size(max = 512, message = "缺陷标题不能超过 512 字符")
    private String title;

    @NotBlank(message = "严重度不能为空")
    @Pattern(regexp = "CRITICAL|MAJOR|MINOR|TRIVIAL",
            message = "严重度必须是 CRITICAL/MAJOR/MINOR/TRIVIAL")
    private String severity;

    @Pattern(regexp = "HIGH|MEDIUM|LOW",
            message = "优先级必须是 HIGH/MEDIUM/LOW")
    private String priority = "MEDIUM";

    /** EVT / DVT / PVT / MP */
    @Size(max = 64)
    private String phaseFound;

    /** 可空, 创建时默认 NEW */
    @Pattern(regexp = "NEW|ANALYZING|FIX_IN_PROGRESS|FIXED|VERIFIED|CLOSED|REOPENED",
            message = "状态枚举非法")
    private String status;

    private String rootCause;
    private String correctiveAction;
    private String preventiveAction;

    /** 处理人 (assignee) */
    private String assigneeUserid;
    /** 验证人 (verifier) */
    private String verifierUserid;

    @NotNull(message = "项目不能为空")
    private Long projectId;

    /** 关联看板任务 (可空) */
    private Long sprintTaskId;

    @NotNull(message = "发现日期不能为空")
    private LocalDate foundDate;

    /** 前端直传附件数组, 后端转 JSON 存 attachments */
    private List<DefectVO.Attachment> attachments;
}
