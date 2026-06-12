package com.RD.controller;

import com.RD.audit.AuditLog;
import com.RD.common.Result;
import com.RD.entity.SysUser;
import com.RD.mapper.SysUserMapper;
import com.RD.task.service.TaskStateMachineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 任务状态机 Controller
 *
 * <p>与 {@code TaskController}（CRUD）独立，专门处理状态流转。</p>
 *
 * <p>接口列表：</p>
 * <ul>
 *   <li>POST /api/tasks/{id}/accept    — 接收方确认接收（PENDING_ACCEPT → IN_PROGRESS）</li>
 *   <li>POST /api/tasks/{id}/submit    — 执行方提交完成（IN_PROGRESS → PENDING_VERIFY）</li>
 *   <li>POST /api/tasks/{id}/complete  — 发起方验收（→ COMPLETED）</li>
 *   <li>POST /api/tasks/{id}/reject    — 发起方驳回（PENDING_VERIFY → IN_PROGRESS，需原因）</li>
 *   <li>POST /api/tasks/{id}/cancel    — 发起方撤回（PENDING_ACCEPT/IN_PROGRESS → WITHDRAWN）</li>
 * </ul>
 *
 * <p>权限：每个端点都校验操作人身份（接收方 / 发起方），由状态机服务内部 ensureOperator 兜底。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks/{id}")
@RequiredArgsConstructor
@Tag(name = "任务状态机", description = "任务状态流转操作（accept/submit/complete/reject/cancel）")
public class TaskStateMachineController {

    private final TaskStateMachineService stateMachineService;
    private final SysUserMapper userMapper;

    /**
     * 接收方确认接收任务
     */
    @PostMapping("/accept")
    @AuditLog(operationType = "ACCEPT", resourceType = "TASK",
            resourceIdParam = "#id", description = "接收任务")
    @Operation(summary = "接收任务（PENDING_ACCEPT → IN_PROGRESS）")
    public Result<Void> accept(@PathVariable Long id) {
        stateMachineService.acceptTask(id, currentUserId());
        return Result.success();
    }

    /**
     * 执行方提交完成
     *
     * <p>Body: {@code {"remark": "提交说明"}}，remark 可空</p>
     */
    @PostMapping("/submit")
    @AuditLog(operationType = "SUBMIT", resourceType = "TASK",
            resourceIdParam = "#id", description = "提交任务")
    @Operation(summary = "提交任务（IN_PROGRESS → PENDING_VERIFY）")
    public Result<Void> submit(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String remark = body == null ? null : body.getOrDefault("remark", null);
        stateMachineService.submitTask(id, currentUserId(), remark);
        return Result.success();
    }

    /**
     * 发起方验收
     */
    @PostMapping("/complete")
    @AuditLog(operationType = "COMPLETE", resourceType = "TASK",
            resourceIdParam = "#id", description = "验收任务")
    @Operation(summary = "验收任务（PENDING_VERIFY → COMPLETED）")
    public Result<Void> complete(@PathVariable Long id) {
        stateMachineService.completeTask(id, currentUserId());
        return Result.success();
    }

    /**
     * 发起方驳回
     *
     * <p>Body: {@code {"reason": "驳回原因"}}，reason 必填</p>
     */
    @PostMapping("/reject")
    @AuditLog(operationType = "REJECT", resourceType = "TASK",
            resourceIdParam = "#id", description = "驳回任务")
    @Operation(summary = "驳回任务（PENDING_VERIFY → IN_PROGRESS）")
    public Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        stateMachineService.rejectTask(id, currentUserId(), reason);
        return Result.success();
    }

    /**
     * 发起方撤回
     *
     * <p>Body: {@code {"reason": "撤回原因"}}，reason 可空</p>
     */
    @PostMapping("/cancel")
    @AuditLog(operationType = "CANCEL", resourceType = "TASK",
            resourceIdParam = "#id", description = "撤回任务")
    @Operation(summary = "撤回任务（PENDING_ACCEPT/IN_PROGRESS → WITHDRAWN）")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        stateMachineService.cancelTask(id, currentUserId(), reason);
        return Result.success();
    }

    // ============================================
    // 工具方法
    // ============================================

    private String currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal == null) {
            throw com.RD.common.BusinessException.unauthorized("未登录");
        }
        return principal.toString();
    }
}
