package com.RD.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * BOM 明细（物料行）
 *
 * <p>对应表 {@code bom_item}（V1 SQL 5.2 节）</p>
 *
 * <p>{@code sub_bom_id} 引用另一个 {@code bom_header.id} —— 多阶 BOM 的核心字段。
 * 如果该物料本身是个子组件（例如"主板"有自己独立的 BOM），就指向那个子 BOM 的 id，
 * 前端递归展开成嵌套树。</p>
 */
@Data
@TableName("bom_item")
public class BomItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属 bom_header.id */
    @TableField("bom_id")
    private Long bomId;

    /** 行号（同 bom 内唯一） */
    @TableField("line_no")
    private Integer lineNo;

    @TableField("item_code")
    private String itemCode;

    @TableField("item_name")
    private String itemName;

    private String specification;
    private BigDecimal quantity;
    private String unit;
    private String supplier;
    @TableField("unit_price")
    private BigDecimal unitPrice;
    @TableField("total_price")
    private BigDecimal totalPrice;
    private String remark;

    /** 子 BOM ID（多阶 BOM 关键字段，可空） */
    @TableField("sub_bom_id")
    private Long subBomId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
