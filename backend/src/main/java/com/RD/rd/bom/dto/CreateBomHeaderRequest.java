package com.RD.rd.bom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建/更新 BOM 表头请求
 */
@Data
public class CreateBomHeaderRequest {

    @NotBlank(message = "BOM 编号不能为空")
    @Size(max = 64, message = "BOM 编号不能超过 64 字符")
    private String bomCode;

    @NotBlank(message = "产品名称不能为空")
    @Size(max = 256, message = "产品名称不能超过 256 字符")
    private String productName;

    @Size(max = 128, message = "产品型号不能超过 128 字符")
    private String productModel;

    @Size(max = 32, message = "版本号不能超过 32 字符")
    private String version;

    /** 父 BOM ID（null 或 0 = 顶层） */
    private Long parentBomId;

    /** 关联项目 ID（可空） */
    private Long projectId;

    private LocalDate effectiveDate;
    private LocalDate expiryDate;
}
