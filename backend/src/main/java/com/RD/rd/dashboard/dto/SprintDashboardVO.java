package com.RD.rd.dashboard.dto;

import lombok.Data;

import java.util.List;

/**
 * Sprint 仪表盘
 *
 * <p>当前活跃 Sprint 列表 + 进度 (totalStoryPoints / doneStoryPoints)</p>
 */
@Data
public class SprintDashboardVO {

    private Long activeSprintCount;
    private List<SprintProgress> activeSprints;

    /**
     * 单个 Sprint 进度
     */
    @Data
    public static class SprintProgress {
        private Long sprintId;
        private String sprintName;
        private Long projectId;
        private String projectName;
        private Integer totalTasks;
        private Integer doneTasks;
        private Integer totalStoryPoints;
        private Integer doneStoryPoints;
        private String startDate;
        private String endDate;
        /** 0-100 进度百分比 */
        private Integer progressPercent;
    }
}
