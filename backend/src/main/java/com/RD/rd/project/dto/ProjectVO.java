package com.RD.rd.project.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目返回 VO
 */
@Data
public class ProjectVO {

    private Long id;
    private String code;
    private String name;
    private String type;
    private String phase;
    private String managerUserid;
    private String managerName;        // 关联 sys_user 冗余展示

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualStartDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualEndDate;

    private String status;
    private BigDecimal progress;
    private String description;
    private String tags;                // JSON 字符串，前端解析
    private String createdBy;
    private String createdByName;        // 关联 sys_user 冗余

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
