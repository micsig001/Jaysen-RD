package com.RD.task.service;

import com.RD.common.BusinessException;
import com.RD.entity.SysUser;
import com.RD.entity.Task;
import com.RD.mapper.SysUserMapper;
import com.RD.mapper.TaskMapper;
import com.RD.task.dto.CreateTaskRequest;
import com.RD.task.dto.TaskQuery;
import com.RD.task.dto.TaskVO;
import com.RD.task.util.TaskNoGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 任务服务（CRUD 层）
 *
 * <p>职责：</p>
 * <ul>
 *   <li>分页查询（带数据权限过滤）</li>
 *   <li>详情查询（带权限校验）</li>
 *   <li>创建 / 更新任务</li>
 * </ul>
 *
 * <p>状态流转（accept / submit / complete / reject / cancel）由独立的
 * {@code TaskStateMachineService} 负责 —— Phase 2.5 接入，本期不实现。</p>
 *
 * <p>数据权限规则：</p>
 * <ul>
 *   <li>ADMIN：全部</li>
 *   <li>MANAGER：本部门所有成员作为创建者/接收者的任务</li>
 *   <li>EMPLOYEE：仅自己作为创建者/接收者的任务</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;
    private final SysUserMapper userMapper;
    private final TaskNoGenerator taskNoGenerator;

    // ============================================
    // 查询
    // ============================================

    /**
     * 分页查询任务列表（带数据权限过滤）
     */
    public Page<TaskVO> listTasks(TaskQuery query, SysUser currentUser) {
        if (currentUser == null) {
            throw BusinessException.unauthorized("未登录");
        }

        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Task::getStatus, query.getStatus());
        }
        if (query.getPriority() != null) {
            wrapper.eq(Task::getPriority, query.getPriority());
        }
        if (StringUtils.hasText(query.getCreatorId())) {
            wrapper.eq(Task::getCreatorId, query.getCreatorId());
        }
        if (StringUtils.hasText(query.getAssigneeId())) {
            wrapper.eq(Task::getAssigneeId, query.getAssigneeId());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(Task::getTaskNo, kw).or().like(Task::getTitle, kw));
        }

        // 关键：数据权限过滤
        applyDataPermissionFilter(wrapper, currentUser);

        // 排序：优先级 ↑ → 截止时间 ↑ → 创建时间 ↓
        wrapper.orderByAsc(Task::getPriority)
                .orderByAsc(Task::getActualDeadline)
                .orderByDesc(Task::getCreatedAt);

        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1
                ? 20
                : Math.min(query.getPageSize(), 200);
        Page<Task> page = new Page<>(pageNum, pageSize);
        Page<Task> result = taskMapper.selectPage(page, wrapper);

        Page<TaskVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        if (result.getRecords().isEmpty()) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        // 批量查用户名（creator + assignee 合并去重）
        Set<String> userIds = result.getRecords().stream()
                .flatMap(t -> java.util.stream.Stream.of(t.getCreatorId(), t.getAssigneeId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, String> nameMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectByUserIds(new java.util.ArrayList<>(userIds)).stream()
                        .collect(Collectors.toMap(SysUser::getUserId, SysUser::getName));

        voPage.setRecords(result.getRecords().stream()
                .map(t -> toVO(t, nameMap.get(t.getCreatorId()), nameMap.get(t.getAssigneeId())))
                .collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 任务详情（带权限校验）
     */
    public TaskVO getTaskById(Long id, SysUser currentUser) {
        if (id == null) {
            throw BusinessException.badRequest("任务 ID 不能为空");
        }
        if (currentUser == null) {
            throw BusinessException.unauthorized("未登录");
        }
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw BusinessException.notFound("任务不存在: " + id);
        }
        if (!hasDataPermission(task, currentUser)) {
            log.warn("[任务] 用户 {} 无权查看任务 {}", currentUser.getUserId(), id);
            // 不暴露存在性：跟"不存在"返回同样的 404
            throw BusinessException.notFound("任务不存在: " + id);
        }

        String creatorName = lookupName(task.getCreatorId());
        String assigneeName = lookupName(task.getAssigneeId());
        return toVO(task, creatorName, assigneeName);
    }

    // ============================================
    // 写操作
    // ============================================

    /**
     * 创建任务
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskVO createTask(CreateTaskRequest req, SysUser currentUser) {
        validateCreate(req);
        Task task = new Task();
        task.setTaskNo(taskNoGenerator.generate());
        task.setTitle(req.getTitle());
        task.setDescription(req.getDescription());
        task.setSourceRemark(req.getSourceRemark());
        task.setCreatorId(currentUser.getUserId());
        task.setAssigneeId(req.getAssigneeId());
        task.setPriority(req.getPriority());
        task.setEstimatedDuration(req.getEstimatedDuration());
        task.setStatus("PENDING_ACCEPT");
        task.setSelfAssigned(Objects.equals(currentUser.getUserId(), req.getAssigneeId()));
        task.setVersion(0);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        taskMapper.insert(task);
        log.info("[任务] 创建: id={}, taskNo={}, title={}, creator={}, assignee={}",
                task.getId(), task.getTaskNo(), task.getTitle(),
                task.getCreatorId(), task.getAssigneeId());
        return getTaskById(task.getId(), currentUser);
    }

    /**
     * 更新任务（仅 PENDING_ACCEPT 状态 + 仅创建者可改）
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskVO updateTask(Long id, CreateTaskRequest req, SysUser currentUser) {
        Task existing = taskMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("任务不存在: " + id);
        }
        if (!"PENDING_ACCEPT".equals(existing.getStatus())) {
            throw BusinessException.badRequest("只有待接收状态的任务可以编辑");
        }
        if (!Objects.equals(existing.getCreatorId(), currentUser.getUserId())) {
            throw BusinessException.forbidden("只能修改自己创建的任务");
        }

        // 只更新可编辑字段
        existing.setTitle(req.getTitle());
        existing.setDescription(req.getDescription());
        existing.setSourceRemark(req.getSourceRemark());
        existing.setAssigneeId(req.getAssigneeId());
        existing.setPriority(req.getPriority());
        existing.setEstimatedDuration(req.getEstimatedDuration());
        existing.setSelfAssigned(Objects.equals(currentUser.getUserId(), req.getAssigneeId()));
        existing.setUpdatedAt(LocalDateTime.now());

        int affected = taskMapper.updateById(existing);
        if (affected == 0) {
            throw new BusinessException(409, "任务已被其他请求修改，请刷新后重试");
        }
        log.info("[任务] 更新: id={}, operator={}", id, currentUser.getUserId());
        return getTaskById(id, currentUser);
    }

    // ============================================
    // 内部
    // ============================================

    private void validateCreate(CreateTaskRequest req) {
        if (req == null) {
            throw BusinessException.badRequest("任务数据不能为空");
        }
        if (!StringUtils.hasText(req.getTitle())) {
            throw BusinessException.badRequest("任务标题不能为空");
        }
        if (req.getPriority() == null || req.getPriority() < 1 || req.getPriority() > 4) {
            throw BusinessException.badRequest("优先级必须在 1-4 之间");
        }
        if (!StringUtils.hasText(req.getAssigneeId())) {
            throw BusinessException.badRequest("执行人不能为空");
        }
        if (req.getEstimatedDuration() != null && req.getEstimatedDuration() <= 0) {
            throw BusinessException.badRequest("预估时长必须大于 0");
        }
    }

    /**
     * 数据权限过滤（按角色限制可见范围）
     */
    private void applyDataPermissionFilter(LambdaQueryWrapper<Task> wrapper, SysUser currentUser) {
        String role = currentUser.getRole();
        String userId = currentUser.getUserId();

        if ("ADMIN".equals(role)) {
            return;
        }

        if ("MANAGER".equals(role)) {
            if (currentUser.getDepartmentId() == null) {
                log.warn("[任务] MANAGER {} 未关联部门，返回空集", userId);
                wrapper.eq(Task::getId, -1L);
                return;
            }
            List<String> deptUserIds = userMapper.selectUserIdsByDeptId(currentUser.getDepartmentId());
            if (deptUserIds == null || deptUserIds.isEmpty()) {
                wrapper.eq(Task::getId, -1L);
                return;
            }
            wrapper.and(w -> w.in(Task::getCreatorId, deptUserIds)
                    .or().in(Task::getAssigneeId, deptUserIds));
            return;
        }

        // EMPLOYEE：仅自己作为创建者或接收者
        wrapper.and(w -> w.eq(Task::getCreatorId, userId)
                .or().eq(Task::getAssigneeId, userId));
    }

    /**
     * 单条任务权限校验
     */
    private boolean hasDataPermission(Task task, SysUser currentUser) {
        String role = currentUser.getRole();
        String userId = currentUser.getUserId();

        if ("ADMIN".equals(role)) {
            return true;
        }

        if ("MANAGER".equals(role)) {
            if (currentUser.getDepartmentId() == null) {
                return false;
            }
            List<String> deptUserIds = userMapper.selectUserIdsByDeptId(currentUser.getDepartmentId());
            if (deptUserIds == null || deptUserIds.isEmpty()) {
                return false;
            }
            return deptUserIds.contains(task.getCreatorId())
                    || deptUserIds.contains(task.getAssigneeId());
        }

        // EMPLOYEE
        return userId.equals(task.getCreatorId())
                || userId.equals(task.getAssigneeId());
    }

    private String lookupName(String userId) {
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        SysUser u = userMapper.selectByUserId(userId);
        return u != null ? u.getName() : null;
    }

    private TaskVO toVO(Task t, String creatorName, String assigneeName) {
        TaskVO vo = new TaskVO();
        vo.setId(t.getId());
        vo.setTaskNo(t.getTaskNo());
        vo.setTitle(t.getTitle());
        vo.setDescription(t.getDescription());
        vo.setSourceRemark(t.getSourceRemark());
        vo.setCreatorId(t.getCreatorId());
        vo.setCreatorName(creatorName);
        vo.setAssigneeId(t.getAssigneeId());
        vo.setAssigneeName(assigneeName);
        vo.setPriority(t.getPriority());
        vo.setStatus(t.getStatus());
        vo.setSelfAssigned(t.getSelfAssigned());
        vo.setEstimatedDuration(t.getEstimatedDuration());
        vo.setActualStartTime(t.getActualStartTime());
        vo.setActualDeadline(t.getActualDeadline());
        vo.setActualEndTime(t.getActualEndTime());
        vo.setRejectReason(t.getRejectReason());
        vo.setVersion(t.getVersion());
        vo.setCreatedAt(t.getCreatedAt());
        vo.setUpdatedAt(t.getUpdatedAt());
        return vo;
    }
}
