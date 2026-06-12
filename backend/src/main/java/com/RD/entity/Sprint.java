package com.RD.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 敏捷冲刺
 *
 * <p>对应表 {@code sprint}（V1 SQL 3.3 节）</p>
 *
 * <p>状态机：</p>
 * <pre>
 *   PLANNED → ACTIVE → COMPLETED
 *   PLANNED/ACTIVE → CANCELLED
 * </pre>
 */
@Data
@TableName("sprint")
public class Sprint {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目 ID */
    @TableField("project_id")
    private Long projectId;

    private String name;

    @TableField("start_date")
    private LocalDate startDate;

    @TableField("end_date")
    private LocalDate endDate;

    private String goal;

    /** PLANNED / ACTIVE / COMPLETED / CANCELLED */
    private String status;

    @TableField("created_by")
    private String createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
