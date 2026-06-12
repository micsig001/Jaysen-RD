package com.RD.rd.ecn.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ECN 审批记录 VO
 */
@Data
public class EcnApprovalVO {

    private Long id;
    private Long ecnId;

    private String approverUserid;
    private String approverName;

    private String department;
    private String role;

    /** 步骤序号（1 / 2 / 3...） */
    private Integer stepOrder;

    /** PENDING / APPROVED / REJECTED / SKIPPED */
    private String status;

    private String comment;
    private String signatureUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime signedAt;

    /** Flowable Task ID */
    private String taskId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
