package com.RD.rd.sprint.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Sprint 展示 VO
 */
@Data
public class SprintVO {

    private Long id;
    private Long projectId;
    private String projectName;
    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String goal;

    /** PLANNED / ACTIVE / COMPLETED / CANCELLED */
    private String status;

    private String createdBy;
    private String createdByName;

    /** 该 Sprint 下任务统计（前端展示用） */
    private Long taskCount;
    private Long doneCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
