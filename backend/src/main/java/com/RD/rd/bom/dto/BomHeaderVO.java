package com.RD.rd.bom.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * BOM 表头展示 VO
 */
@Data
public class BomHeaderVO {

    private Long id;
    private String bomCode;
    private String productName;
    private String productModel;
    private String version;
    private Long parentBomId;
    private Long projectId;
    private String projectName;
    /** DRAFT / UNDER_REVIEW / APPROVED / OBSOLETE */
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;
    private String createdBy;
    private String createdByName;
    private String approvedBy;
    private String approvedByName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /** 该 BOM 直接物料行数（不含子阶） */
    private Integer itemCount;
}
