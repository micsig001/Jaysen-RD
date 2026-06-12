package com.RD.rd.dashboard.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘总览
 *
 * <p>4 个全局数字 + 7 日趋势(任务/缺陷)</p>
 */
@Data
public class DashboardOverviewVO {

    // ========== 4 块大数字 ==========
    private Long projectCount;
    private Long activeProjectCount;
    private Long taskCount;
    private Long openTaskCount;
    private Long defectCount;
    private Long openDefectCount;
    private Long activeSprintCount;
    private Long equipmentCount;
    private Long availableEquipmentCount;

    // ========== 7 日趋势 (key=日期 yyyy-MM-dd, value=数量) ==========
    private List<String> recent7Days;                       // 日期
    private Map<String, Long> taskCreatedTrend;            // 任务新增
    private Map<String, Long> taskCompletedTrend;          // 任务完成
    private Map<String, Long> defectCreatedTrend;          // 缺陷新增
    private Map<String, Long> defectClosedTrend;           // 缺陷关闭
}
