package com.RD.rd.defect.dto;

import lombok.Data;

/**
 * 缺陷统计 VO (按 severity / status 聚合, 给仪表盘用)
 */
@Data
public class DefectStatsVO {

    private Long total;
    private Long open;             // 未关闭 (status != CLOSED)
    /** 严重度分布 */
    private Long criticalCount;
    private Long majorCount;
    private Long minorCount;
    private Long trivialCount;
    /** 状态分布 */
    private Long newCount;
    private Long analyzingCount;
    private Long fixInProgressCount;
    private Long fixedCount;
    private Long verifiedCount;
    private Long closedCount;
    private Long reopenedCount;
    /** 趋势 */
    private Long openedThisWeek;
    private Long closedThisWeek;
}
