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
 * BOM 表头
 *
 * <p>对应表 {@code bom_header}（V1 SQL 5.1 节）</p>
 *
 * <p>多阶 BOM 表达方式：每个 bom_header 是一阶，bom_item.sub_bom_id 引用子阶 bom_header。
 * 树的根 {@code parent_bom_id = 0}。</p>
 *
 * <p>状态机：</p>
 * <pre>
 *   DRAFT → UNDER_REVIEW → APPROVED → OBSOLETE (被新版本取代时)
 * </pre>
 */
@Data
@TableName("bom_header")
public class BomHeader {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("bom_code")
    private String bomCode;

    @TableField("product_name")
    private String productName;

    @TableField("product_model")
    private String productModel;

    private String version;

    /** 父 BOM ID（0 = 顶层根 BOM） */
    @TableField("parent_bom_id")
    private Long parentBomId;

    @TableField("project_id")
    private Long projectId;

    /** DRAFT / UNDER_REVIEW / APPROVED / OBSOLETE */
    private String status;

    @TableField("effective_date")
    private LocalDate effectiveDate;

    @TableField("expiry_date")
    private LocalDate expiryDate;

    @TableField("created_by")
    private String createdBy;

    @TableField("approved_by")
    private String approvedBy;

    @TableField("approved_at")
    private LocalDateTime approvedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
