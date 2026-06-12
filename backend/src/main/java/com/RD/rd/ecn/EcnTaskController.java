package com.RD.rd.ecn;

import com.RD.common.Result;
import com.RD.entity.SysUser;
import com.RD.mapper.SysUserMapper;
import com.RD.rd.ecn.dto.EcnChangeVO;
import com.RD.rd.ecn.service.EcnTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * ECN 流程任务 Controller
 *
 * <p>接口列表：</p>
 * <ul>
 *   <li>GET  /api/ecn/tasks/my-pending  — 当前用户待办（assignee）</li>
 *   <li>GET  /api/ecn/tasks/my-candidate — 当前用户候选（candidate）</li>
 *   <li>POST /api/ecn/tasks/{taskId}/claim  — claim 任务</li>
 *   <li>POST /api/ecn/tasks/{taskId}/complete — 完成审批 (body: approved, comment)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/ecn/tasks")
@RequiredArgsConstructor
@Tag(name = "ECN 流程任务", description = "Flowable Task 操作 (claim/complete)")
public class EcnTaskController {

    private final EcnTaskService ecnTaskService;
    private final SysUserMapper userMapper;

    @GetMapping("/my-pending")
    @Operation(summary = "当前用户 ECN 待办（assignee 命中）")
    public Result<List<EcnChangeVO>> myPending() {
        return Result.success(ecnTaskService.listMyPendingTasks(currentUser()));
    }

    @GetMapping("/my-candidate")
    @Operation(summary = "当前用户 ECN 候选（candidateUsers 命中，Phase 2 暂返回空）")
    public Result<List<EcnChangeVO>> myCandidate() {
        return Result.success(ecnTaskService.listMyCandidateTasks(currentUser()));
    }

    @PostMapping("/{taskId}/claim")
    @Operation(summary = "claim 任务（多候选人场景）")
    public Result<Void> claim(@PathVariable String taskId) {
        ecnTaskService.claimTask(taskId, currentUser());
        return Result.success();
    }

    @PostMapping("/{taskId}/complete")
    @Operation(summary = "完成审批 (body: { approved: true/false, comment?: '...' })")
    public Result<Void> complete(@PathVariable String taskId, @RequestBody Map<String, Object> body) {
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        String comment = body.get("comment") instanceof String s ? s : null;
        ecnTaskService.completeTask(taskId, approved, comment, currentUser());
        return Result.success();
    }

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
