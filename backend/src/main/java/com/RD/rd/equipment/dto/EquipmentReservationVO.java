package com.RD.rd.equipment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备预约返回 VO
 */
@Data
public class EquipmentReservationVO {

    private Long id;
    private Long equipmentId;
    private String equipmentName;        // 关联 lab_equipment 冗余
    private String equipmentAssetCode;    // 关联 lab_equipment 冗余

    private String userId;
    private String userName;            // 关联 sys_user 冗余

    private Long projectId;
    private String projectName;            // 关联 project 冗余（可空）

    private String purpose;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actualStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actualEndTime;

    private String notes;
    private String approvedBy;
    private String approvedByName;        // 关联 sys_user 冗余

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
