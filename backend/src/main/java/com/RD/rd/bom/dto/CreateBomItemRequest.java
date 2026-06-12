package com.RD.rd.bom.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建/更新 BOM 物料行请求
 */
@Data
public class CreateBomItemRequest {

    /** 所属 bom_id（创建时必填，更新时可空表示沿用） */
    private Long bomId;

    @NotNull(message = "行号不能为空")
    private Integer lineNo;

    @NotBlank(message = "物料编码不能为空")
    @Size(max = 64, message = "物料编码不能超过 64 字符")
    private String itemCode;

    @NotBlank(message = "物料名称不能为空")
    @Size(max = 256, message = "物料名称不能超过 256 字符")
    private String itemName;

    @Size(max = 512, message = "规格不能超过 512 字符")
    private String specification;

    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0.0", message = "数量不能为负")
    private BigDecimal quantity;

    @NotBlank(message = "单位不能为空")
    @Size(max = 32, message = "单位不能超过 32 字符")
    private String unit;

    @Size(max = 256, message = "供应商不能超过 256 字符")
    private String supplier;

    private BigDecimal unitPrice;
    private String remark;

    /** 子 BOM ID（多阶 BOM 关键字段，可空） */
    private Long subBomId;
}
