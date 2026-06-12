package com.RD.rd.defect.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 缺陷展示 VO
 */
@Data
public class DefectVO {

    private Long id;
    private String defectNumber;
    private String title;

    /** CRITICAL / MAJOR / MINOR / TRIVIAL */
    private String severity;
    /** HIGH / MEDIUM / LOW */
    private String priority;
    /** NEW / ANALYZING / FIX_IN_PROGRESS / FIXED / VERIFIED / CLOSED / REOPENED */
    private String status;

    /** EVT / DVT / PVT / MP */
    private String phaseFound;

    private String rootCause;
    private String correctiveAction;
    private String preventiveAction;

    private String reporterUserid;
    private String reporterName;

    private String assigneeUserid;
    private String assigneeName;

    private String verifierUserid;
    private String verifierName;

    private Long projectId;
    private String projectName;

    private Long sprintTaskId;
    private String sprintTaskTitle;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate foundDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate resolvedDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate verifiedDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate closedDate;

    /** 前端友好: 解析 attachments JSON 数组 */
    private List<Attachment> attachments;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 附件
     */
    @Data
    public static class Attachment {
        private String name;
        private String url;
        private Long size;
    }
}
