package com.RD.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ECN 工程变更主表
 *
 * <p>对应表 {@code ecn_change}（V1 SQL 5.3 节）</p>
 *
 * <p>状态机：</p>
 * <pre>
 *   DRAFT → UNDER_REVIEW（提交审批，Flowable 启动）
 *   UNDER_REVIEW → APPROVED（所有审批人通过）/ REJECTED（任一驳回）
 *   APPROVED → IMPLEMENTED（实施完成）/ CANCELLED（发起人撤回）
 * </pre>
 */
@Data
@TableName("ecn_change")
public class EcnChange {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** ECN 业务编号（{@code ECN + yyyyMMdd + 4 位随机}） */
    @TableField("ecn_number")
    private String ecnNumber;

    private String title;

    /** 变更类型：DESIGN / MATERIAL / PROCESS / DOCUMENT */
    @TableField("change_type")
    private String changeType;

    /** 紧急程度：NORMAL / URGENT / CRITICAL */
    private String urgency;

    private String reason;
    private String description;
    @TableField("impact_analysis")
    private String impactAnalysis;

    /** 受影响 BOM ID 列表（JSON 数组字符串） */
    @TableField("affected_bom_ids")
    private String affectedBomIds;

    /** 发起人 UserID */
    @TableField("requester_userid")
    private String requesterUserid;

    /** 关联项目 ID（可空） */
    @TableField("project_id")
    private Long projectId;

    /** DRAFT / UNDER_REVIEW / APPROVED / REJECTED / IMPLEMENTED / CANCELLED */
    private String status;

    private Integer priority;

    @TableField("target_date")
    private LocalDate targetDate;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    /** Flowable 流程实例 ID（提交审批后回填） */
    @TableField("process_instance_id")
    private String processInstanceId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
