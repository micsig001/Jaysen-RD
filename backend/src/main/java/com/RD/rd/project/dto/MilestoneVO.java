package com.RD.rd.project.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 里程碑返回 VO
 */
@Data
public class MilestoneVO {

    private Long id;
    private Long projectId;
    private String name;
    private String phase;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate plannedStart;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate plannedEnd;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualStart;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualEnd;

    private BigDecimal progress;
    private String dependencies;        // JSON 字符串
    private String ownerUserid;
    private String ownerName;            // 关联 sys_user 冗余
    private String status;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
