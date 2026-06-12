package com.RD.rd.equipment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备返回 VO
 */
@Data
public class LabEquipmentVO {

    private Long id;
    private String assetCode;
    private String name;
    private String model;
    private String manufacturer;
    private String category;
    private String location;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate warrantyExpiry;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate calibrationDueDate;

    private Integer calibrationIntervalMonths;
    private String status;
    private String specifications;        // JSON 字符串
    private String manualUrl;
    private String qrCode;
    private String notes;
    private String createdBy;
    private String createdByName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
