package com.RD.rd.dashboard.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 设备仪表盘
 *
 * <p>状态分布 + 类别分布 + 7 日预约热力(按设备聚合) + 即将到期校准</p>
 */
@Data
public class EquipmentDashboardVO {

    /** 状态分布 (AVAILABLE/IN_USE/MAINTENANCE/CALIBRATION_OVERDUE/SCRAPPED) */
    private Map<String, Long> statusDistribution;

    /** 类别分布 (SPECTRUM_ANALYZER/OSCILLOSCOPE/...) */
    private Map<String, Long> categoryDistribution;

    /** 7 日预约热力 (date -> equipmentId -> count) */
    private List<String> recent7Days;
    private List<EquipmentUsage> usage;            // 设备维度的 7 日预约次数

    /** 即将到期校准 (30 天内) Top 10 */
    private List<CalibrationDue> calibrationDue;

    /** 利用率最高的设备 Top 5 (7 日预约次数排序) */
    private List<EquipmentUsage> topUsage;

    /**
     * 单个设备的 7 日预约数据
     */
    @Data
    public static class EquipmentUsage {
        private Long equipmentId;
        private String equipmentName;
        private String assetCode;
        private String category;
        /** 7 个数字, 索引对应 recent7Days */
        private List<Long> dailyCounts;
    }

    /**
     * 即将到期校准
     */
    @Data
    public static class CalibrationDue {
        private Long equipmentId;
        private String equipmentName;
        private String assetCode;
        private String calibrationDueDate;
        private Long daysUntilDue;     // 负数=已过期
    }
}
