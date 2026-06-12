package com.RD.rd.dashboard.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 任务仪表盘
 *
 * <p>按状态/优先级/类型 分布 + 7 日趋势 + 当前逾期 Top 10</p>
 */
@Data
public class TaskDashboardVO {

    // ========== 状态分布 (status -> count) ==========
    private Map<String, Long> statusDistribution;
    // ========== 优先级分布 ==========
    private Map<String, Long> priorityDistribution;
    // ========== 类型分布 ==========
    private Map<String, Long> typeDistribution;
    // ========== 7 日趋势 ==========
    private List<String> recent7Days;
    private Map<String, Long> createdTrend;
    private Map<String, Long> completedTrend;
    // ========== 当前逾期任务 (Top 10) ==========
    private Long overdueCount;
    private List<OverdueTask> overdueTop;
    // ========== 按处理人 (Top 10) ==========
    private List<AssigneeLoad> assigneeTop;

    /**
     * 逾期任务
     */
    @Data
    public static class OverdueTask {
        private Long id;
        private String taskNo;
        private String title;
        private String status;
        private String priority;
        private String assigneeUserid;
        private String assigneeName;
        private String actualDeadline;
        private Long overdueDays;        // 逾期天数
    }

    /**
     * 处理人负载
     */
    @Data
    public static class AssigneeLoad {
        private String assigneeUserid;
        private String assigneeName;
        private Long openCount;          // 进行中/待办/待验收 总数
        private Long overdueCount;
    }
}
