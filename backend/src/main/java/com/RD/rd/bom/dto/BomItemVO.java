package com.RD.rd.bom.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * BOM 物料行展示 VO
 *
 * <p>含 {@code subBom} 嵌套字段 —— 如果该物料本身有子 BOM，
 * 后端会递归查询并嵌入，前端 el-tree 直接展开。</p>
 */
@Data
public class BomItemVO {

    private Long id;
    private Long bomId;
    private Integer lineNo;
    private String itemCode;
    private String itemName;
    private String specification;
    private BigDecimal quantity;
    private String unit;
    private String supplier;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String remark;

    /** 子 BOM ID（可空：有子组件时才填） */
    private Long subBomId;

    /**
     * 子 BOM 完整树（递归）
     * <p>如果 subBomId 非空, 后端会查该子 BOM 的 header + items, items 里再查 subBom...</p>
     */
    private BomTreeNode subBom;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
