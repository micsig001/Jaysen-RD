package com.RD.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备预约实体
 *
 * <p>对应表 {@code equipment_reservation}（V1 SQL 4.2 节）</p>
 */
@Data
@TableName("equipment_reservation")
public class EquipmentReservation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备 ID */
    private Long equipmentId;

    /** 预约人 UserID（企微） */
    private String userId;

    /** 关联项目 ID（可空：预约设备做非项目相关的事） */
    private Long projectId;

    /** 使用目的 */
    private String purpose;

    /** 预约开始时间 */
    private LocalDateTime startTime;

    /** 预约结束时间 */
    private LocalDateTime endTime;

    /** 状态：PENDING / CONFIRMED / IN_USE / COMPLETED / CANCELLED / NO_SHOW */
    private String status;

    /** 实际开始时间 */
    private LocalDateTime actualStartTime;

    /** 实际结束时间 */
    private LocalDateTime actualEndTime;

    /** 备注 */
    private String notes;

    /** 审批人 UserID */
    private String approvedBy;

    /** 审批时间 */
    private LocalDateTime approvedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
