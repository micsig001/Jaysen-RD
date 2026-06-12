package com.RD.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 里程碑实体（硬件甘特图）
 *
 * <p>对应表 {@code milestone}（V1 SQL 3.2 节）</p>
 */
@Data
@TableName("milestone")
public class Milestone {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目 ID */
    private Long projectId;

    /** 里程碑名称 */
    private String name;

    /** 阶段：EVT / DVT / PVT / MP */
    private String phase;

    /** 计划开始日期 */
    private LocalDate plannedStart;

    /** 计划结束日期 */
    private LocalDate plannedEnd;

    /** 实际开始日期 */
    private LocalDate actualStart;

    /** 实际结束日期 */
    private LocalDate actualEnd;

    /** 完成百分比 0-100 */
    private BigDecimal progress;

    /** 前置里程碑 ID 列表（JSON 字符串，由业务层序列化/反序列化） */
    private String dependencies;

    /** 责任人 UserID */
    private String ownerUserid;

    /** 状态：NOT_STARTED / IN_PROGRESS / COMPLETED / DELAYED */
    private String status;

    /** 描述 */
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
