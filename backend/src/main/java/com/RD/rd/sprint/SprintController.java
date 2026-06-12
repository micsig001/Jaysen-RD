package com.RD.rd.sprint;

import com.RD.audit.AuditLog;
import com.RD.common.Result;
import com.RD.entity.SysUser;
import com.RD.mapper.SysUserMapper;
import com.RD.rd.sprint.dto.CreateSprintRequest;
import com.RD.rd.sprint.dto.CreateSprintTaskRequest;
import com.RD.rd.sprint.dto.SprintTaskVO;
import com.RD.rd.sprint.dto.SprintVO;
import com.RD.rd.sprint.service.SprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Sprint 看板 Controller
 *
 * <p>接口列表：</p>
 * <ul>
 *   <li>GET    /api/sprints                        — Sprint 列表</li>
 *   <li>GET    /api/sprints/{id}                   — Sprint 详情</li>
 *   <li>POST   /api/sprints                        — 创建 Sprint</li>
 *   <li>POST   /api/sprints/{id}/activate           — 启动 Sprint (PLANNED → ACTIVE)</li>
 *   <li>POST   /api/sprints/{id}/complete           — 完成 Sprint (ACTIVE → COMPLETED)</li>
 *   <li>GET    /api/sprint-tasks                   — 任务列表 (sprintId / projectId / status 过滤)</li>
 *   <li>GET    /api/sprint-tasks/{id}              — 任务详情</li>
 *   <li>POST   /api/sprint-tasks                   — 创建任务</li>
 *   <li>PUT    /api/sprint-tasks/{id}              — 更新任务</li>
 *   <li>DELETE /api/sprint-tasks/{id}              — 删除任务 (仅 reporter / ADMIN)</li>
 *   <li>POST   /api/sprint-tasks/{id}/move         — 拖拽移动 (status + order_num)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Sprint 看板", description = "敏捷 Sprint + 任务管理（看板视图）")
public class SprintController {

    private final SprintService sprintService;
    private final SysUserMapper userMapper;

    // ========== Sprint ==========

    @GetMapping("/api/sprints")
    @Operation(summary = "Sprint 列表（按 projectId/status 过滤）")
    public Result<List<SprintVO>> listSprints(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status) {
        return Result.success(sprintService.listSprints(projectId, status, currentUser()));
    }

    @GetMapping("/api/sprints/{id}")
    @Operation(summary = "Sprint 详情（含任务统计）")
    public Result<SprintVO> getSprint(@PathVariable Long id) {
        return Result.success(sprintService.getSprintById(id, currentUser()));
    }

    @PostMapping("/api/sprints")
    @AuditLog(operationType = "CREATE", resourceType = "SPRINT",
            description = "创建 Sprint")
    @Operation(summary = "创建 Sprint（PLANNED 状态）")
    public Result<SprintVO> createSprint(@Valid @RequestBody CreateSprintRequest req) {
        return Result.success(sprintService.createSprint(req, currentUser()));
    }

    @PostMapping("/api/sprints/{id}/activate")
    @AuditLog(operationType = "ACTIVATE", resourceType = "SPRINT",
            resourceIdParam = "#id", description = "启动 Sprint")
    @Operation(summary = "启动 Sprint（PLANNED → ACTIVE）")
    public Result<SprintVO> activateSprint(@PathVariable Long id) {
        return Result.success(sprintService.activateSprint(id, currentUser()));
    }

    @PostMapping("/api/sprints/{id}/complete")
    @AuditLog(operationType = "COMPLETE", resourceType = "SPRINT",
            resourceIdParam = "#id", description = "完成 Sprint")
    @Operation(summary = "完成 Sprint（ACTIVE → COMPLETED）")
    public Result<SprintVO> completeSprint(@PathVariable Long id) {
        return Result.success(sprintService.completeSprint(id, currentUser()));
    }

    // ========== Sprint Task ==========

    @GetMapping("/api/sprint-tasks")
    @Operation(summary = "Sprint 任务列表（按 order_num 升序）")
    public Result<List<SprintTaskVO>> listTasks(
            @RequestParam(required = false) Long sprintId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status) {
        return Result.success(sprintService.listSprintTasks(sprintId, projectId, status, currentUser()));
    }

    @GetMapping("/api/sprint-tasks/{id}")
    @Operation(summary = "Sprint 任务详情")
    public Result<SprintTaskVO> getTask(@PathVariable Long id) {
        return Result.success(sprintService.getSprintTaskById(id));
    }

    @PostMapping("/api/sprint-tasks")
    @AuditLog(operationType = "CREATE", resourceType = "SPRINT_TASK",
            description = "创建 Sprint 任务")
    @Operation(summary = "创建 Sprint 任务")
    public Result<SprintTaskVO> createTask(@Valid @RequestBody CreateSprintTaskRequest req) {
        return Result.success(sprintService.createSprintTask(req, currentUser()));
    }

    @PutMapping("/api/sprint-tasks/{id}")
    @AuditLog(operationType = "UPDATE", resourceType = "SPRINT_TASK",
            resourceIdParam = "#id", description = "更新 Sprint 任务")
    @Operation(summary = "更新 Sprint 任务")
    public Result<SprintTaskVO> updateTask(@PathVariable Long id,
                                          @Valid @RequestBody CreateSprintTaskRequest req) {
        return Result.success(sprintService.updateSprintTask(id, req, currentUser()));
    }

    @DeleteMapping("/api/sprint-tasks/{id}")
    @AuditLog(operationType = "DELETE", resourceType = "SPRINT_TASK",
            resourceIdParam = "#id", description = "删除 Sprint 任务")
    @Operation(summary = "删除 Sprint 任务（仅 reporter / ADMIN）")
    public Result<Void> deleteTask(@PathVariable Long id) {
        sprintService.deleteSprintTask(id, currentUser());
        return Result.success();
    }

    @PostMapping("/api/sprint-tasks/{id}/move")
    @AuditLog(operationType = "MOVE", resourceType = "SPRINT_TASK",
            resourceIdParam = "#id", description = "拖拽移动 Sprint 任务")
    @Operation(summary = "拖拽移动任务（设置新 status + order_num，可改 sprintId）")
    public Result<SprintTaskVO> moveTask(@PathVariable Long id,
                                        @RequestBody java.util.Map<String, Object> body) {
        Long newSprintId = body.get("sprintId") instanceof Number n ? n.longValue() : null;
        String newStatus = body.get("status") instanceof String s ? s : null;
        Integer newOrderNum = body.get("orderNum") instanceof Number n ? n.intValue() : null;
        return Result.success(sprintService.moveTask(id, newSprintId, newStatus, newOrderNum, currentUser()));
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
