package com.RD.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ECN 审批记录
 *
 * <p>对应表 {@code ecn_approval}（V1 SQL 5.4 节）</p>
 *
 * <p>多对一：每次 {@code ecn_change} 提交审批会批量插入若干条，
 * 字段 {@code step_order} 标识是第几步（1 / 2 / 3...）。</p>
 */
@Data
@TableName("ecn_approval")
public class EcnApproval {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("ecn_id")
    private Long ecnId;

    @TableField("approver_userid")
    private String approverUserid;

    /** 审批人所在部门（冗余,流程节点配置可读） */
    private String department;

    /** 审批人角色（如 部门负责人 / 技术总工 / 质量经理） */
    private String role;

    /** 步骤序号（1 / 2 / 3...） */
    @TableField("step_order")
    private Integer stepOrder;

    /** PENDING / APPROVED / REJECTED / SKIPPED */
    private String status;

    private String comment;

    @TableField("signature_url")
    private String signatureUrl;

    @TableField("signed_at")
    private LocalDateTime signedAt;

    /** Flowable Task ID（流程任务 ID） */
    @TableField("task_id")
    private String taskId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
