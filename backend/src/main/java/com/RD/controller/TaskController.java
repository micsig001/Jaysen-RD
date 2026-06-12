package com.RD.controller;

import com.RD.audit.AuditLog;
import com.RD.common.Result;
import com.RD.entity.SysUser;
import com.RD.mapper.SysUserMapper;
import com.RD.task.dto.CreateTaskRequest;
import com.RD.task.dto.TaskQuery;
import com.RD.task.dto.TaskVO;
import com.RD.task.service.TaskService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务 Controller
 *
 * <p>接口列表（Phase 2 范围，仅 CRUD + 数据权限过滤）：</p>
 * <ul>
 *   <li>GET  /api/tasks              — 分页查询（自动按角色过滤可见范围）</li>
 *   <li>GET  /api/tasks/{id}         — 任务详情</li>
 *   <li>POST /api/tasks              — 创建任务（仅 PENDING_ACCEPT 状态）</li>
 *   <li>PUT  /api/tasks/{id}         — 更新任务（仅创建者 + PENDING_ACCEPT 状态）</li>
 * </ul>
 *
 * <p>状态流转（accept / submit / complete / reject / cancel）由独立的
 * {@code TaskStateMachineController} 负责 —— Phase 2.5 接入。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "任务管理", description = "任务 CRUD + 数据权限过滤")
public class TaskController {

    private final TaskService taskService;
    private final SysUserMapper userMapper;

    /**
     * 分页查询任务列表
     */
    @GetMapping
    @Operation(summary = "分页查询任务（按角色自动过滤）")
    public Result<Page<TaskVO>> list(TaskQuery query) {
        return Result.success(taskService.listTasks(query, currentUser()));
    }

    /**
     * 任务详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "任务详情（带权限校验，无权时返回 404 不暴露存在性）")
    public Result<TaskVO> detail(@PathVariable Long id) {
        return Result.success(taskService.getTaskById(id, currentUser()));
    }

    /**
     * 创建任务
     */
    @PostMapping
    @AuditLog(operationType = "CREATE", resourceType = "TASK",
            description = "创建任务")
    @Operation(summary = "创建任务")
    public Result<TaskVO> create(@Valid @RequestBody CreateTaskRequest req) {
        return Result.success(taskService.createTask(req, currentUser()));
    }

    /**
     * 更新任务
     */
    @PutMapping("/{id}")
    @AuditLog(operationType = "UPDATE", resourceType = "TASK",
            resourceIdParam = "#id", description = "更新任务")
    @Operation(summary = "更新任务（仅 PENDING_ACCEPT + 仅创建者）")
    public Result<TaskVO> update(@PathVariable Long id, @Valid @RequestBody CreateTaskRequest req) {
        return Result.success(taskService.updateTask(id, req, currentUser()));
    }

    // ============================================
    // 工具方法
    // ============================================

    private SysUser currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal == null) {
            throw com.RD.common.BusinessException.unauthorized("未登录");
        }
        SysUser user = userMapper.selectByUserId(principal.toString());
        if (user == null) {
            throw com.RD.common.BusinessException.unauthorized("用户不存在: " + principal);
        }
        return user;
    }
}
