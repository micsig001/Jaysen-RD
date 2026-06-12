package com.RD.rd.defect.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 更新缺陷请求 (PUT 全量替换, 排除 reporter/foundDate 这种不可改字段)
 */
@Data
public class UpdateDefectRequest {

    @Size(max = 512, message = "缺陷标题不能超过 512 字符")
    private String title;

    @Pattern(regexp = "CRITICAL|MAJOR|MINOR|TRIVIAL",
            message = "严重度必须是 CRITICAL/MAJOR/MINOR/TRIVIAL")
    private String severity;

    @Pattern(regexp = "HIGH|MEDIUM|LOW",
            message = "优先级必须是 HIGH/MEDIUM/LOW")
    private String priority;

    @Pattern(regexp = "NEW|ANALYZING|FIX_IN_PROGRESS|FIXED|VERIFIED|CLOSED|REOPENED",
            message = "状态枚举非法")
    private String status;

    @Size(max = 64)
    private String phaseFound;

    private String rootCause;
    private String correctiveAction;
    private String preventiveAction;

    private String assigneeUserid;
    private String verifierUserid;

    private Long projectId;
    private Long sprintTaskId;

    private LocalDate foundDate;
    private LocalDate resolvedDate;
    private LocalDate verifiedDate;
    private LocalDate closedDate;

    private List<DefectVO.Attachment> attachments;
}
