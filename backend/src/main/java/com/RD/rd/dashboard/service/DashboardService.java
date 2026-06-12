package com.RD.rd.dashboard.service;

import com.RD.entity.Defect;
import com.RD.entity.EquipmentReservation;
import com.RD.entity.LabEquipment;
import com.RD.entity.Project;
import com.RD.entity.Sprint;
import com.RD.entity.SprintTask;
import com.RD.entity.SysUser;
import com.RD.entity.Task;
import com.RD.entity.TaskStatusHistory;
import com.RD.mapper.DefectMapper;
import com.RD.mapper.EquipmentReservationMapper;
import com.RD.mapper.LabEquipmentMapper;
import com.RD.mapper.ProjectMapper;
import com.RD.mapper.SprintMapper;
import com.RD.mapper.SprintTaskMapper;
import com.RD.mapper.SysUserMapper;
import com.RD.mapper.TaskMapper;
import com.RD.mapper.TaskStatusHistoryMapper;
import com.RD.rd.dashboard.dto.DashboardOverviewVO;
import com.RD.rd.dashboard.dto.EquipmentDashboardVO;
import com.RD.rd.dashboard.dto.SprintDashboardVO;
import com.RD.rd.dashboard.dto.TaskDashboardVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 仪表盘服务
 *
 * <p>所有聚合为实时计算（无缓存 / 无 ETL），适合单团队 / 中小数据量场景（每张表 < 10 万行）。</p>
 *
 * <p>性能预算：单端点 P95 < 200ms（5 张表全表扫 + 内存分组）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProjectMapper projectMapper;
    private final TaskMapper taskMapper;
    private final TaskStatusHistoryMapper taskStatusHistoryMapper;
    private final DefectMapper defectMapper;
    private final SprintMapper sprintMapper;
    private final SprintTaskMapper sprintTaskMapper;
    private final LabEquipmentMapper labEquipmentMapper;
    private final EquipmentReservationMapper equipmentReservationMapper;
    private final SysUserMapper userMapper;

    // ============================================
    // 1. 总览
    // ============================================

    public DashboardOverviewVO getOverview() {
        DashboardOverviewVO vo = new DashboardOverviewVO();

        // --- 4 块大数字 ---
        List<Project> projects = projectMapper.selectList(null);
        vo.setProjectCount((long) projects.size());
        vo.setActiveProjectCount(projects.stream()
                .filter(p -> "IN_PROGRESS".equals(p.getStatus()))
                .count());

        List<Task> tasks = taskMapper.selectList(null);
        vo.setTaskCount((long) tasks.size());
        vo.setOpenTaskCount(tasks.stream()
                .filter(t -> !"COMPLETED".equals(t.getStatus()) && !"WITHDRAWN".equals(t.getStatus()))
                .count());

        List<Defect> defects = defectMapper.selectList(null);
        vo.setDefectCount((long) defects.size());
        vo.setOpenDefectCount(defects.stream()
                .filter(d -> !"CLOSED".equals(d.getStatus()))
                .count());

        vo.setActiveSprintCount(sprintMapper.selectCount(
                new LambdaQueryWrapper<Sprint>().eq(Sprint::getStatus, "ACTIVE")));

        List<LabEquipment> equipment = labEquipmentMapper.selectList(null);
        vo.setEquipmentCount((long) equipment.size());
        vo.setAvailableEquipmentCount(equipment.stream()
                .filter(e -> "AVAILABLE".equals(e.getStatus()))
                .count());

        // --- 7 日趋势 ---
        vo.setRecent7Days(buildRecent7Days());
        vo.setTaskCreatedTrend(buildTaskCreatedTrend(tasks, vo.getRecent7Days()));
        vo.setTaskCompletedTrend(buildTaskCompletedTrend(tasks, vo.getRecent7Days()));
        vo.setDefectCreatedTrend(buildDefectCreatedTrend(defects, vo.getRecent7Days()));
        vo.setDefectClosedTrend(buildDefectClosedTrend(defects, vo.getRecent7Days()));

        return vo;
    }

    // ============================================
    // 2. 任务仪表盘
    // ============================================

    public TaskDashboardVO getTaskDashboard() {
        TaskDashboardVO vo = new TaskDashboardVO();
        List<Task> tasks = taskMapper.selectList(null);

        // 状态分布
        Map<String, Long> statusMap = new LinkedHashMap<>();
        for (String s : List.of("PENDING_ACCEPT", "IN_PROGRESS", "PENDING_VERIFY", "COMPLETED", "REJECTED", "WITHDRAWN")) {
            statusMap.put(s, 0L);
        }
        for (Task t : tasks) {
            statusMap.merge(t.getStatus(), 1L, Long::sum);
        }
        vo.setStatusDistribution(statusMap);

        // 优先级分布 (priority TINYINT: 1-高 2-中 3-低 4-闲)
        Map<String, Long> priorityMap = new LinkedHashMap<>();
        priorityMap.put("1", 0L);
        priorityMap.put("2", 0L);
        priorityMap.put("3", 0L);
        priorityMap.put("4", 0L);
        for (Task t : tasks) {
            String key = t.getPriority() == null ? "3" : String.valueOf(t.getPriority());
            priorityMap.merge(key, 1L, Long::sum);
        }
        vo.setPriorityDistribution(priorityMap);

        // 7 日趋势
        vo.setRecent7Days(buildRecent7Days());
        vo.setCreatedTrend(buildTaskCreatedTrend(tasks, vo.getRecent7Days()));
        vo.setCompletedTrend(buildTaskCompletedTrend(tasks, vo.getRecent7Days()));

        // 逾期 Top 10
        LocalDateTime now = LocalDateTime.now();
        List<TaskDashboardVO.OverdueTask> overdue = tasks.stream()
                .filter(t -> !"COMPLETED".equals(t.getStatus())
                        && !"WITHDRAWN".equals(t.getStatus())
                        && !"REJECTED".equals(t.getStatus())
                        && t.getActualDeadline() != null
                        && t.getActualDeadline().isBefore(now))
                .sorted(Comparator.comparing(Task::getActualDeadline))
                .limit(10)
                .map(t -> {
                    TaskDashboardVO.OverdueTask ot = new TaskDashboardVO.OverdueTask();
                    ot.setId(t.getId());
                    ot.setTaskNo(t.getTaskNo());
                    ot.setTitle(t.getTitle());
                    ot.setStatus(t.getStatus());
                    ot.setPriority(t.getPriority() == null ? "" : String.valueOf(t.getPriority()));
                    ot.setAssigneeUserid(t.getAssigneeId());
                    ot.setActualDeadline(t.getActualDeadline().toString());
                    ot.setOverdueDays(ChronoUnit.DAYS.between(t.getActualDeadline(), now));
                    return ot;
                })
                .collect(Collectors.toList());
        vo.setOverdueCount(tasks.stream()
                .filter(t -> !"COMPLETED".equals(t.getStatus())
                        && !"WITHDRAWN".equals(t.getStatus())
                        && t.getActualDeadline() != null
                        && t.getActualDeadline().isBefore(now))
                .count());
        vo.setOverdueTop(overdue);

        // 补 assigneeName
        Set<String> assigneeIds = overdue.stream()
                .map(TaskDashboardVO.OverdueTask::getAssigneeUserid)
                .filter(uid -> StringUtils.hasText(uid)).collect(Collectors.toSet());
        Map<String, String> nameMap = batchUserNames(assigneeIds);
        overdue.forEach(ot -> ot.setAssigneeName(nameMap.get(ot.getAssigneeUserid())));

        // 处理人负载 Top 10
        Map<String, Long[]> loadMap = new HashMap<>();   // userId -> [open, overdue]
        for (Task t : tasks) {
            if (!StringUtils.hasText(t.getAssigneeId())) continue;
            if ("COMPLETED".equals(t.getStatus()) || "WITHDRAWN".equals(t.getStatus())) continue;
            Long[] arr = loadMap.computeIfAbsent(t.getAssigneeId(), k -> new Long[]{0L, 0L});
            arr[0]++;
            if (t.getActualDeadline() != null && t.getActualDeadline().isBefore(now)) {
                arr[1]++;
            }
        }
        Set<String> loadUserIds = loadMap.keySet();
        Map<String, String> loadNameMap = batchUserNames(loadUserIds);
        List<TaskDashboardVO.AssigneeLoad> top = loadMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .limit(10)
                .map(e -> {
                    TaskDashboardVO.AssigneeLoad al = new TaskDashboardVO.AssigneeLoad();
                    al.setAssigneeUserid(e.getKey());
                    al.setAssigneeName(loadNameMap.get(e.getKey()));
                    al.setOpenCount(e.getValue()[0]);
                    al.setOverdueCount(e.getValue()[1]);
                    return al;
                })
                .collect(Collectors.toList());
        vo.setAssigneeTop(top);

        return vo;
    }

    // ============================================
    // 3. 设备仪表盘
    // ============================================

    public EquipmentDashboardVO getEquipmentDashboard() {
        EquipmentDashboardVO vo = new EquipmentDashboardVO();
        List<LabEquipment> equipment = labEquipmentMapper.selectList(null);

        // 状态分布
        Map<String, Long> statusMap = new LinkedHashMap<>();
        for (String s : List.of("AVAILABLE", "IN_USE", "MAINTENANCE", "CALIBRATION_OVERDUE", "SCRAPPED")) {
            statusMap.put(s, 0L);
        }
        for (LabEquipment e : equipment) {
            String s = e.getStatus() == null ? "AVAILABLE" : e.getStatus();
            statusMap.merge(s, 1L, Long::sum);
        }
        vo.setStatusDistribution(statusMap);

        // 类别分布
        Map<String, Long> categoryMap = equipment.stream()
                .filter(e -> StringUtils.hasText(e.getCategory()))
                .collect(Collectors.groupingBy(LabEquipment::getCategory, Collectors.counting()));
        vo.setCategoryDistribution(categoryMap);

        // 7 日预约热力
        List<String> days = buildRecent7Days();
        vo.setRecent7Days(days);
        LocalDateTime from = LocalDate.now().minusDays(6).atStartOfDay();
        LocalDateTime to = LocalDate.now().plusDays(1).atStartOfDay();
        List<EquipmentReservation> reservations = equipmentReservationMapper.selectList(
                new LambdaQueryWrapper<EquipmentReservation>()
                        .ge(EquipmentReservation::getStartTime, from)
                        .lt(EquipmentReservation::getStartTime, to));
        // 设备维度
        Map<Long, Long[]> usageByEquipment = new HashMap<>();
        for (EquipmentReservation r : reservations) {
            if (r.getEquipmentId() == null) continue;
            if ("CANCELLED".equals(r.getStatus())) continue;
            Long[] arr = usageByEquipment.computeIfAbsent(r.getEquipmentId(), k -> new Long[7]);
            int idx = (int) ChronoUnit.DAYS.between(
                    from.toLocalDate(), r.getStartTime().toLocalDate());
            if (idx >= 0 && idx < 7) arr[idx] = (arr[idx] == null ? 0L : arr[idx]) + 1;
        }
        // 转成 EquipmentUsage 列表
        List<EquipmentDashboardVO.EquipmentUsage> usageList = new ArrayList<>();
        for (LabEquipment e : equipment) {
            if (e.getId() == null) continue;
            Long[] arr = usageByEquipment.getOrDefault(e.getId(), new Long[7]);
            List<Long> daily = new ArrayList<>(7);
            long total = 0L;
            for (int i = 0; i < 7; i++) {
                long v = arr[i] == null ? 0L : arr[i];
                daily.add(v);
                total += v;
            }
            if (total > 0) {        // 只展示有使用的设备
                EquipmentDashboardVO.EquipmentUsage u = new EquipmentDashboardVO.EquipmentUsage();
                u.setEquipmentId(e.getId());
                u.setEquipmentName(e.getName());
                u.setAssetCode(e.getAssetCode());
                u.setCategory(e.getCategory());
                u.setDailyCounts(daily);
                usageList.add(u);
            }
        }
        vo.setUsage(usageList);

        // 利用率 Top 5
        List<EquipmentDashboardVO.EquipmentUsage> top = new ArrayList<>(usageList);
        top.sort((a, b) -> Long.compare(
                b.getDailyCounts().stream().mapToLong(Long::longValue).sum(),
                a.getDailyCounts().stream().mapToLong(Long::longValue).sum()));
        vo.setTopUsage(top.size() > 5 ? top.subList(0, 5) : top);

        // 即将到期校准 (30 天内)
        LocalDate today = LocalDate.now();
        List<EquipmentDashboardVO.CalibrationDue> calList = equipment.stream()
                .filter(e -> e.getCalibrationDueDate() != null)
                .filter(e -> !e.getCalibrationDueDate().isBefore(today.minusDays(60)))  // 已过期 60 天内的也展示
                .sorted(Comparator.comparing(LabEquipment::getCalibrationDueDate))
                .limit(10)
                .map(e -> {
                    EquipmentDashboardVO.CalibrationDue c = new EquipmentDashboardVO.CalibrationDue();
                    c.setEquipmentId(e.getId());
                    c.setEquipmentName(e.getName());
                    c.setAssetCode(e.getAssetCode());
                    c.setCalibrationDueDate(e.getCalibrationDueDate().toString());
                    c.setDaysUntilDue(ChronoUnit.DAYS.between(today, e.getCalibrationDueDate()));
                    return c;
                })
                .collect(Collectors.toList());
        vo.setCalibrationDue(calList);

        return vo;
    }

    // ============================================
    // 4. Sprint 仪表盘
    // ============================================

    public SprintDashboardVO getSprintDashboard() {
        SprintDashboardVO vo = new SprintDashboardVO();
        List<Sprint> activeSprints = sprintMapper.selectList(
                new LambdaQueryWrapper<Sprint>().eq(Sprint::getStatus, "ACTIVE"));
        vo.setActiveSprintCount((long) activeSprints.size());

        if (activeSprints.isEmpty()) {
            vo.setActiveSprints(Collections.emptyList());
            return vo;
        }

        // 批量查项目名
        Set<Long> projectIds = activeSprints.stream().map(Sprint::getProjectId).collect(Collectors.toSet());
        Map<Long, String> projectNames = projectIds.isEmpty() ? Collections.emptyMap()
                : projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(Project::getId, Project::getName, (a, b) -> a));

        // 一次性查所有 ACTIVE sprint 的任务
        Set<Long> sprintIds = activeSprints.stream().map(Sprint::getId).collect(Collectors.toSet());
        List<SprintTask> allTasks = sprintTaskMapper.selectList(
                new LambdaQueryWrapper<SprintTask>().in(SprintTask::getSprintId, sprintIds));

        Map<Long, List<SprintTask>> tasksBySprint = allTasks.stream()
                .filter(t -> t.getSprintId() != null)
                .collect(Collectors.groupingBy(SprintTask::getSprintId));

        List<SprintDashboardVO.SprintProgress> progresses = new ArrayList<>();
        for (Sprint s : activeSprints) {
            List<SprintTask> tasks = tasksBySprint.getOrDefault(s.getId(), Collections.emptyList());
            int total = tasks.size();
            int done = (int) tasks.stream().filter(t -> "DONE".equals(t.getStatus())).count();
            int totalSP = tasks.stream().filter(t -> t.getStoryPoints() != null)
                    .mapToInt(SprintTask::getStoryPoints).sum();
            int doneSP = tasks.stream()
                    .filter(t -> "DONE".equals(t.getStatus()) && t.getStoryPoints() != null)
                    .mapToInt(SprintTask::getStoryPoints).sum();
            int percent = totalSP == 0 ? 0 : (int) Math.round((double) doneSP * 100 / totalSP);

            SprintDashboardVO.SprintProgress p = new SprintDashboardVO.SprintProgress();
            p.setSprintId(s.getId());
            p.setSprintName(s.getName());
            p.setProjectId(s.getProjectId());
            p.setProjectName(projectNames.get(s.getProjectId()));
            p.setTotalTasks(total);
            p.setDoneTasks(done);
            p.setTotalStoryPoints(totalSP);
            p.setDoneStoryPoints(doneSP);
            p.setStartDate(s.getStartDate() == null ? null : s.getStartDate().toString());
            p.setEndDate(s.getEndDate() == null ? null : s.getEndDate().toString());
            p.setProgressPercent(percent);
            progresses.add(p);
        }
        vo.setActiveSprints(progresses);
        return vo;
    }

    // ============================================
    // 内部: 时间相关
    // ============================================

    /**
     * 生成最近 7 天日期 (含今天), 顺序 [day-6, ..., day0]
     */
    private List<String> buildRecent7Days() {
        List<String> days = new ArrayList<>(7);
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            days.add(today.minusDays(i).toString());
        }
        return days;
    }

    private Map<String, Long> buildTaskCreatedTrend(List<Task> tasks, List<String> days) {
        return buildDateCountMap(days, daysList -> tasks.stream()
                .filter(t -> t.getCreatedAt() != null)
                .map(t -> t.getCreatedAt().toLocalDate().toString())
                .filter(d -> daysList.contains(d))
                .collect(Collectors.groupingBy(d -> d, Collectors.counting())));
    }

    private Map<String, Long> buildTaskCompletedTrend(List<Task> tasks, List<String> days) {
        // 用 task_status_history: 流转到 COMPLETED 的 created_at
        List<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toList());
        if (taskIds.isEmpty()) return emptyMap(days);
        List<TaskStatusHistory> histories = taskStatusHistoryMapper.selectList(
                new LambdaQueryWrapper<TaskStatusHistory>()
                        .in(TaskStatusHistory::getTaskId, taskIds)
                        .eq(TaskStatusHistory::getToStatus, "COMPLETED"));
        return buildDateCountMap(days, daysList -> histories.stream()
                .filter(h -> h.getCreatedAt() != null)
                .map(h -> h.getCreatedAt().toLocalDate().toString())
                .filter(d -> daysList.contains(d))
                .collect(Collectors.groupingBy(d -> d, Collectors.counting())));
    }

    private Map<String, Long> buildDefectCreatedTrend(List<Defect> defects, List<String> days) {
        return buildDateCountMap(days, daysList -> defects.stream()
                .filter(d -> d.getFoundDate() != null)
                .map(d -> d.getFoundDate().toString())
                .filter(d -> daysList.contains(d))
                .collect(Collectors.groupingBy(d -> d, Collectors.counting())));
    }

    private Map<String, Long> buildDefectClosedTrend(List<Defect> defects, List<String> days) {
        return buildDateCountMap(days, daysList -> defects.stream()
                .filter(d -> "CLOSED".equals(d.getStatus()) && d.getClosedDate() != null)
                .map(d -> d.getClosedDate().toString())
                .filter(d -> daysList.contains(d))
                .collect(Collectors.groupingBy(d -> d, Collectors.counting())));
    }

    @FunctionalInterface
    private interface GroupingFunction {
        Map<String, Long> apply(List<String> daysList);
    }

    private Map<String, Long> buildDateCountMap(List<String> days, GroupingFunction fn) {
        Map<String, Long> result = new LinkedHashMap<>();
        Map<String, Long> raw = fn.apply(days);
        for (String d : days) {
            result.put(d, raw.getOrDefault(d, 0L));
        }
        return result;
    }

    private Map<String, Long> emptyMap(List<String> days) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (String d : days) result.put(d, 0L);
        return result;
    }

    private Map<String, String> batchUserNames(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyMap();
        List<SysUser> users = userMapper.selectByUserIds(new ArrayList<>(userIds));
        return users.stream().collect(Collectors.toMap(SysUser::getUserId, SysUser::getName, (a, b) -> a));
    }
}
