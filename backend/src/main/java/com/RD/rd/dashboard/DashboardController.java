package com.RD.rd.dashboard;

import com.RD.common.Result;
import com.RD.rd.dashboard.dto.DashboardOverviewVO;
import com.RD.rd.dashboard.dto.EquipmentDashboardVO;
import com.RD.rd.dashboard.dto.SprintDashboardVO;
import com.RD.rd.dashboard.dto.TaskDashboardVO;
import com.RD.rd.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 研发仪表盘 Controller
 *
 * <p>接口列表（4 个聚合端点）：</p>
 * <ul>
 *   <li>GET /api/dashboard/overview   — 总览（4 数字 + 7 日趋势）</li>
 *   <li>GET /api/dashboard/tasks      — 任务（状态/优先级 + 逾期 Top + 处理人负载）</li>
 *   <li>GET /api/dashboard/equipment  — 设备（状态/类别 + 7 日预约 + 校准到期）</li>
 *   <li>GET /api/dashboard/sprint     — Sprint（活跃 Sprint 进度）</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "研发仪表盘", description = "聚合统计 + ECharts 数据源")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/api/dashboard/overview")
    @Operation(summary = "总览（4 块大数字 + 4 个 7 日趋势）")
    public Result<DashboardOverviewVO> overview() {
        return Result.success(dashboardService.getOverview());
    }

    @GetMapping("/api/dashboard/tasks")
    @Operation(summary = "任务仪表盘（状态/优先级分布 + 逾期 + 处理人负载）")
    public Result<TaskDashboardVO> tasks() {
        return Result.success(dashboardService.getTaskDashboard());
    }

    @GetMapping("/api/dashboard/equipment")
    @Operation(summary = "设备仪表盘（状态/类别 + 7 日预约热力 + 校准到期）")
    public Result<EquipmentDashboardVO> equipment() {
        return Result.success(dashboardService.getEquipmentDashboard());
    }

    @GetMapping("/api/dashboard/sprint")
    @Operation(summary = "Sprint 仪表盘（活跃 Sprint 进度）")
    public Result<SprintDashboardVO> sprint() {
        return Result.success(dashboardService.getSprintDashboard());
    }
}
