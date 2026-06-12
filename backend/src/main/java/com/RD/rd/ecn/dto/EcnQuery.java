package com.RD.rd.ecn.dto;

import lombok.Data;

/**
 * ECN 变更查询参数
 */
@Data
public class EcnQuery {

    /** 状态过滤 */
    private String status;

    /** 变更类型 */
    private String changeType;

    /** 紧急程度 */
    private String urgency;

    /** 发起人 UserID 精确过滤 */
    private String requesterUserid;

    /** 关键字（匹配 ecn_number / title） */
    private String keyword;

    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
