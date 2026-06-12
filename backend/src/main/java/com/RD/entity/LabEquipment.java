package com.RD.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 实验室设备台账实体
 *
 * <p>对应表 {@code lab_equipment}（V1 SQL 4.1 节）</p>
 */
@Data
@TableName("lab_equipment")
public class LabEquipment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 资产编号（唯一） */
    private String assetCode;

    /** 设备名称 */
    private String name;

    /** 型号 */
    private String model;

    /** 制造商 */
    private String manufacturer;

    /** 设备类别：SPECTRUM_ANALYZER / OSCILLOSCOPE / SIGNAL_GENERATOR / NETWORK_ANALYZER / POWER_METER / OTHER */
    private String category;

    /** 存放位置 */
    private String location;

    /** 采购日期 */
    private LocalDate purchaseDate;

    /** 保修到期日 */
    private LocalDate warrantyExpiry;

    /** 下次校准日期 */
    private LocalDate calibrationDueDate;

    /** 校准周期（月） */
    private Integer calibrationIntervalMonths;

    /** 状态：AVAILABLE / IN_USE / MAINTENANCE / CALIBRATION_OVERDUE / SCRAPPED */
    private String status;

    /** 技术参数（JSON 字符串） */
    private String specifications;

    /** 设备手册 URL */
    private String manualUrl;

    /** 二维码图片 URL（用于扫码查看设备详情） */
    private String qrCode;

    /** 备注 */
    private String notes;

    /** 创建人 UserID */
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
