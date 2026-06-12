package com.RD.rd.sprint.service;

import com.RD.common.BusinessException;
import com.RD.entity.Project;
import com.RD.entity.Sprint;
import com.RD.entity.SprintTask;
import com.RD.entity.SysUser;
import com.RD.mapper.ProjectMapper;
import com.RD.mapper.SprintMapper;
import com.RD.mapper.SprintTaskMapper;
import com.RD.mapper.SysUserMapper;
import com.RD.rd.sprint.dto.CreateSprintRequest;
import com.RD.rd.sprint.dto.CreateSprintTaskRequest;
import com.RD.rd.sprint.dto.SprintTaskVO;
import com.RD.rd.sprint.dto.SprintVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sprint 看板服务
 *
 * <p>职责：</p>
 * <ul>
 *   <li>Sprint CRUD（创建 / 启动 / 完成 / 取消）</li>
 *   <li>Sprint 任务 CRUD + 拖拽排序（order_num 重排）</li>
 *   <li>看板 5 列数据（按 status 分组：BACKLOG / TODO / IN_PROGRESS / REVIEW / DONE）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SprintService {

    private final SprintMapper sprintMapper;
    private final SprintTaskMapper sprintTaskMapper;
    private final ProjectMapper projectMapper;
    private final SysUserMapper userMapper;

    // ============================================
    // Sprint CRUD
    // ============================================

    /**
     * 分页查询 Sprint
     */
    public List<SprintVO> listSprints(Long projectId, String status, SysUser currentUser) {
        LambdaQueryWrapper<Sprint> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(Sprint::getProjectId, projectId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Sprint::getStatus, status);
        }
        wrapper.orderByDesc(Sprint::getStartDate);
        List<Sprint> list = sprintMapper.selectList(wrapper);
        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查项目名 + 创建人名
        Set<Long> projectIds = list.stream().map(Sprint::getProjectId).collect(Collectors.toSet());
        Set<String> userIds = list.stream().map(Sprint::getCreatedBy)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> projectNames = batchProjectNames(projectIds);
        Map<String, String> userNames = batchUserNames(userIds);

        // 任务统计 (按 sprint_id 分组)
        Set<Long> sprintIds = list.stream().map(Sprint::getId).collect(Collectors.toSet());
        Map<Long, Long[]> taskCountMap = batchSprintTaskCount(sprintIds);

        return list.stream().map(s -> toSprintVO(s,
                projectNames.get(s.getProjectId()),
                userNames.get(s.getCreatedBy()),
                taskCountMap.get(s.getId()))).collect(Collectors.toList());
    }

    /**
     * Sprint 详情
     */
    public SprintVO getSprintById(Long id, SysUser currentUser) {
        Sprint s = sprintMapper.selectById(id);
        if (s == null) {
            throw BusinessException.notFound("Sprint 不存在: " + id);
        }
        String projectName = lookupProjectName(s.getProjectId());
        String userName = lookupUserName(s.getCreatedBy());
        Long[] counts = batchSprintTaskCount(Collections.singleton(id)).get(id);
        return toSprintVO(s, projectName, userName, counts);
    }

    /**
     * 创建 Sprint
     */
    @Transactional(rollbackFor = Exception.class)
    public SprintVO createSprint(CreateSprintRequest req, SysUser currentUser) {
        validateSprint(req);
        // 项目存在性
        Project project = projectMapper.selectById(req.getProjectId());
        if (project == null) {
            throw BusinessException.badRequest("项目不存在: " + req.getProjectId());
        }
        Sprint sprint = new Sprint();
        sprint.setProjectId(req.getProjectId());
        sprint.setName(req.getName());
        sprint.setStartDate(req.getStartDate());
        sprint.setEndDate(req.getEndDate());
        sprint.setGoal(req.getGoal());
        sprint.setStatus("PLANNED");
        sprint.setCreatedBy(currentUser.getUserId());
        sprint.setCreatedAt(LocalDateTime.now());
        sprint.setUpdatedAt(LocalDateTime.now());
        sprintMapper.insert(sprint);
        log.info("[Sprint] 创建: id={}, name={}, project={}", sprint.getId(), sprint.getName(), sprint.getProjectId());
        return getSprintById(sprint.getId(), currentUser);
    }

    /**
     * 启动 Sprint（PLANNED → ACTIVE）
     */
    @Transactional(rollbackFor = Exception.class)
    public SprintVO activateSprint(Long id, SysUser currentUser) {
        Sprint s = sprintMapper.selectById(id);
        if (s == null) {
            throw BusinessException.notFound("Sprint 不存在: " + id);
        }
        if (!"PLANNED".equals(s.getStatus())) {
            throw BusinessException.badRequest("只有 PLANNED 状态的 Sprint 可启动（当前: " + s.getStatus() + "）");
        }
        s.setStatus("ACTIVE");
        s.setUpdatedAt(LocalDateTime.now());
        sprintMapper.updateById(s);
        log.info("[Sprint] 启动: id={}, operator={}", id, currentUser.getUserId());
        return getSprintById(id, currentUser);
    }

    /**
     * 完成 Sprint（ACTIVE → COMPLETED）
     */
    @Transactional(rollbackFor = Exception.class)
    public SprintVO completeSprint(Long id, SysUser currentUser) {
        Sprint s = sprintMapper.selectById(id);
        if (s == null) {
            throw BusinessException.notFound("Sprint 不存在: " + id);
        }
        if (!"ACTIVE".equals(s.getStatus())) {
            throw BusinessException.badRequest("只有 ACTIVE 状态的 Sprint 可完成（当前: " + s.getStatus() + "）");
        }
        s.setStatus("COMPLETED");
        s.setUpdatedAt(LocalDateTime.now());
        sprintMapper.updateById(s);
        log.info("[Sprint] 完成: id={}, operator={}", id, currentUser.getUserId());
        return getSprintById(id, currentUser);
    }

    // ============================================
    // Sprint 任务 CRUD
    // ============================================

    /**
     * Sprint 任务列表（按 order_num 升序）
     *
     * <p>{@code sprintId=null} 时返回项目下所有 BACKLOG 任务（无 sprint_id）</p>
     */
    public List<SprintTaskVO> listSprintTasks(Long sprintId, Long projectId, String status, SysUser currentUser) {
        LambdaQueryWrapper<SprintTask> wrapper = new LambdaQueryWrapper<>();
        if (sprintId != null) {
            wrapper.eq(SprintTask::getSprintId, sprintId);
        } else if (projectId != null) {
            wrapper.eq(SprintTask::getProjectId, projectId)
                    .isNull(SprintTask::getSprintId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SprintTask::getStatus, status);
        }
        wrapper.orderByAsc(SprintTask::getOrderNum)
                .orderByAsc(SprintTask::getId);
        List<SprintTask> tasks = sprintTaskMapper.selectList(wrapper);
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量查用户名
        Set<String> userIds = tasks.stream()
                .flatMap(t -> java.util.stream.Stream.of(t.getAssigneeUserid(), t.getReporterUserid()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, String> nameMap = batchUserNames(userIds);
        return tasks.stream()
                .map(t -> toSprintTaskVO(t, nameMap.get(t.getAssigneeUserid()), nameMap.get(t.getReporterUserid())))
                .collect(Collectors.toList());
    }

    /**
     * 创建 Sprint 任务
     */
    @Transactional(rollbackFor = Exception.class)
    public SprintTaskVO createSprintTask(CreateSprintTaskRequest req, SysUser currentUser) {
        validateTask(req);
        SprintTask t = new SprintTask();
        t.setProjectId(req.getProjectId());
        t.setSprintId(req.getSprintId());
        t.setTitle(req.getTitle());
        t.setDescription(req.getDescription());
        t.setType(StringUtils.hasText(req.getType()) ? req.getType() : "FEATURE");
        t.setPriority(StringUtils.hasText(req.getPriority()) ? req.getPriority() : "MEDIUM");
        t.setStatus(StringUtils.hasText(req.getStatus()) ? req.getStatus() : "BACKLOG");
        t.setAssigneeUserid(req.getAssigneeUserid());
        t.setReporterUserid(currentUser.getUserId());
        t.setEstimatedHours(req.getEstimatedHours());
        t.setStoryPoints(req.getStoryPoints());
        t.setTags(req.getTags());
        t.setDueDate(req.getDueDate());
        t.setOrderNum(req.getOrderNum() == null ? 0 : req.getOrderNum());
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        sprintTaskMapper.insert(t);
        log.info("[Sprint Task] 创建: id={}, title={}, status={}", t.getId(), t.getTitle(), t.getStatus());
        return getSprintTaskById(t.getId());
    }

    /**
     * 任务详情
     */
    public SprintTaskVO getSprintTaskById(Long id) {
        SprintTask t = sprintTaskMapper.selectById(id);
        if (t == null) {
            throw BusinessException.notFound("Sprint 任务不存在: " + id);
        }
        String assigneeName = lookupUserName(t.getAssigneeUserid());
        String reporterName = lookupUserName(t.getReporterUserid());
        return toSprintTaskVO(t, assigneeName, reporterName);
    }

    /**
     * 更新 Sprint 任务
     */
    @Transactional(rollbackFor = Exception.class)
    public SprintTaskVO updateSprintTask(Long id, CreateSprintTaskRequest req, SysUser currentUser) {
        SprintTask existing = sprintTaskMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("Sprint 任务不存在: " + id);
        }
        if (StringUtils.hasText(req.getTitle())) existing.setTitle(req.getTitle());
        if (req.getDescription() != null) existing.setDescription(req.getDescription());
        if (StringUtils.hasText(req.getType())) existing.setType(req.getType());
        if (StringUtils.hasText(req.getPriority())) existing.setPriority(req.getPriority());
        if (StringUtils.hasText(req.getStatus())) existing.setStatus(req.getStatus());
        if (req.getSprintId() != null) existing.setSprintId(req.getSprintId());
        if (req.getAssigneeUserid() != null) existing.setAssigneeUserid(req.getAssigneeUserid());
        if (req.getEstimatedHours() != null) existing.setEstimatedHours(req.getEstimatedHours());
        if (req.getStoryPoints() != null) existing.setStoryPoints(req.getStoryPoints());
        if (req.getTags() != null) existing.setTags(req.getTags());
        if (req.getDueDate() != null) existing.setDueDate(req.getDueDate());
        if (req.getOrderNum() != null) existing.setOrderNum(req.getOrderNum());
        existing.setUpdatedAt(LocalDateTime.now());
        sprintTaskMapper.updateById(existing);
        return getSprintTaskById(id);
    }

    /**
     * 删除任务（仅 reporter 可删）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSprintTask(Long id, SysUser currentUser) {
        SprintTask existing = sprintTaskMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("Sprint 任务不存在: " + id);
        }
        if (!Objects.equals(existing.getReporterUserid(), currentUser.getUserId())
                && !"ADMIN".equals(currentUser.getRole())) {
            throw BusinessException.forbidden("只能删除自己创建的任务");
        }
        sprintTaskMapper.deleteById(id);
        log.info("[Sprint Task] 删除: id={}, operator={}", id, currentUser.getUserId());
    }

    /**
     * 移动任务到指定列（拖拽用）
     *
     * <p>同时设置 status + order_num。
     * 如果 sprintId 改了（新冲刺 / 移到 BACKLOG sprintId=null），也算移动。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public SprintTaskVO moveTask(Long id, Long newSprintId, String newStatus, Integer newOrderNum, SysUser currentUser) {
        SprintTask existing = sprintTaskMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("Sprint 任务不存在: " + id);
        }
        if (!StringUtils.hasText(newStatus)
                || !Set.of("BACKLOG", "TODO", "IN_PROGRESS", "REVIEW", "DONE").contains(newStatus)) {
            throw BusinessException.badRequest("目标 status 非法: " + newStatus);
        }
        existing.setSprintId(newSprintId);
        existing.setStatus(newStatus);
        existing.setOrderNum(newOrderNum == null ? 0 : newOrderNum);
        // 切到 DONE 状态自动设 completedAt
        if ("DONE".equals(newStatus) && existing.getCompletedAt() == null) {
            existing.setCompletedAt(LocalDateTime.now());
        } else if (!"DONE".equals(newStatus) && existing.getCompletedAt() != null) {
            // 拖出 DONE 列则清空
            existing.setCompletedAt(null);
        }
        existing.setUpdatedAt(LocalDateTime.now());
        sprintTaskMapper.updateById(existing);
        log.info("[Sprint Task] 移动: id={} -> sprintId={} status={} orderNum={} operator={}",
                id, newSprintId, newStatus, newOrderNum, currentUser.getUserId());
        return getSprintTaskById(id);
    }

    // ============================================
    // 内部
    // ============================================

    private void validateSprint(CreateSprintRequest req) {
        if (req == null) {
            throw BusinessException.badRequest("Sprint 数据不能为空");
        }
        if (!StringUtils.hasText(req.getName())) {
            throw BusinessException.badRequest("Sprint 名称不能为空");
        }
        if (req.getStartDate() == null || req.getEndDate() == null) {
            throw BusinessException.badRequest("开始/结束日期不能为空");
        }
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw BusinessException.badRequest("结束日期必须晚于或等于开始日期");
        }
    }

    private void validateTask(CreateSprintTaskRequest req) {
        if (req == null) {
            throw BusinessException.badRequest("任务数据不能为空");
        }
        if (!StringUtils.hasText(req.getTitle())) {
            throw BusinessException.badRequest("任务标题不能为空");
        }
        if (req.getProjectId() == null) {
            throw BusinessException.badRequest("项目 ID 不能为空");
        }
        if (StringUtils.hasText(req.getType())
                && !Set.of("FEATURE", "BUG", "OPTIMIZATION", "TEST").contains(req.getType())) {
            throw BusinessException.badRequest("任务类型非法");
        }
        if (StringUtils.hasText(req.getPriority())
                && !Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL").contains(req.getPriority())) {
            throw BusinessException.badRequest("优先级非法");
        }
        if (StringUtils.hasText(req.getStatus())
                && !Set.of("BACKLOG", "TODO", "IN_PROGRESS", "REVIEW", "DONE").contains(req.getStatus())) {
            throw BusinessException.badRequest("状态非法");
        }
    }

    private String lookupProjectName(Long projectId) {
        if (projectId == null) return null;
        Project p = projectMapper.selectById(projectId);
        return p != null ? p.getName() : null;
    }

    private String lookupUserName(String userId) {
        if (!StringUtils.hasText(userId)) return null;
        SysUser u = userMapper.selectByUserId(userId);
        return u != null ? u.getName() : null;
    }

    private Map<Long, String> batchProjectNames(Set<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) return Collections.emptyMap();
        List<Project> projects = projectMapper.selectBatchIds(projectIds);
        return projects.stream().collect(Collectors.toMap(Project::getId, Project::getName, (a, b) -> a));
    }

    private Map<String, String> batchUserNames(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyMap();
        List<SysUser> users = userMapper.selectByUserIds(new ArrayList<>(userIds));
        return users.stream().collect(Collectors.toMap(SysUser::getUserId, SysUser::getName, (a, b) -> a));
    }

    /**
     * 批量统计每个 sprint 的任务总数和已完成数
     * 返回 [total, done]
     */
    private Map<Long, Long[]> batchSprintTaskCount(Set<Long> sprintIds) {
        if (sprintIds == null || sprintIds.isEmpty()) return Collections.emptyMap();
        Map<Long, Long[]> result = new HashMap<>();
        for (Long sid : sprintIds) {
            result.put(sid, new Long[]{0L, 0L});
        }
        // 单 SQL: 按 sprint_id 分组统计
        List<SprintTask> all = sprintTaskMapper.selectList(
                new LambdaQueryWrapper<SprintTask>().in(SprintTask::getSprintId, sprintIds));
        Map<Long, Long> totalMap = all.stream()
                .collect(Collectors.groupingBy(SprintTask::getSprintId, Collectors.counting()));
        Map<Long, Long> doneMap = all.stream()
                .filter(t -> "DONE".equals(t.getStatus()))
                .collect(Collectors.groupingBy(SprintTask::getSprintId, Collectors.counting()));
        for (Long sid : sprintIds) {
            result.put(sid, new Long[]{
                    totalMap.getOrDefault(sid, 0L),
                    doneMap.getOrDefault(sid, 0L)
            });
        }
        return result;
    }

    private SprintVO toSprintVO(Sprint s, String projectName, String userName, Long[] counts) {
        SprintVO vo = new SprintVO();
        vo.setId(s.getId());
        vo.setProjectId(s.getProjectId());
        vo.setProjectName(projectName);
        vo.setName(s.getName());
        vo.setStartDate(s.getStartDate());
        vo.setEndDate(s.getEndDate());
        vo.setGoal(s.getGoal());
        vo.setStatus(s.getStatus());
        vo.setCreatedBy(s.getCreatedBy());
        vo.setCreatedByName(userName);
        vo.setTaskCount(counts == null ? 0L : counts[0]);
        vo.setDoneCount(counts == null ? 0L : counts[1]);
        vo.setCreatedAt(s.getCreatedAt());
        vo.setUpdatedAt(s.getUpdatedAt());
        return vo;
    }

    private SprintTaskVO toSprintTaskVO(SprintTask t, String assigneeName, String reporterName) {
        SprintTaskVO vo = new SprintTaskVO();
        vo.setId(t.getId());
        vo.setProjectId(t.getProjectId());
        vo.setSprintId(t.getSprintId());
        vo.setTitle(t.getTitle());
        vo.setDescription(t.getDescription());
        vo.setType(t.getType());
        vo.setPriority(t.getPriority());
        vo.setStatus(t.getStatus());
        vo.setAssigneeUserid(t.getAssigneeUserid());
        vo.setAssigneeName(assigneeName);
        vo.setReporterUserid(t.getReporterUserid());
        vo.setReporterName(reporterName);
        vo.setEstimatedHours(t.getEstimatedHours() != null ? t.getEstimatedHours() : null);
        vo.setActualHours(t.getActualHours() != null ? t.getActualHours() : null);
        vo.setStoryPoints(t.getStoryPoints());
        // 简单 tags 解析: 前端是 JSON 字符串, 此处仅作为 List<String> 透传
        // (生产可用 Jackson ObjectMapper)
        vo.setDueDate(t.getDueDate());
        vo.setCompletedAt(t.getCompletedAt());
        vo.setOrderNum(t.getOrderNum());
        vo.setCreatedAt(t.getCreatedAt());
        vo.setUpdatedAt(t.getUpdatedAt());
        return vo;
    }
}
