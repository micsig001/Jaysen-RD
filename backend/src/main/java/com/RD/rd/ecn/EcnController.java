package com.RD.rd.ecn;

import com.RD.audit.AuditLog;
import com.RD.common.Result;
import com.RD.entity.SysUser;
import com.RD.mapper.SysUserMapper;
import com.RD.rd.ecn.dto.CreateEcnRequest;
import com.RD.rd.ecn.dto.EcnApprovalVO;
import com.RD.rd.ecn.dto.EcnChangeVO;
import com.RD.rd.ecn.dto.EcnQuery;
import com.RD.rd.ecn.service.EcnService;
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

import java.util.List;

/**
 * ECN 工程变更 Controller
 *
 * <p>接口列表（Phase 2 范围，CRUD + 提交审批）：</p>
 * <ul>
 *   <li>GET  /api/ecn                    — 分页查询（按角色过滤）</li>
 *   <li>GET  /api/ecn/{id}               — ECN 详情</li>
 *   <li>GET  /api/ecn/{id}/approvals     — 审批记录列表</li>
 *   <li>POST /api/ecn                    — 创建 ECN（DRAFT）</li>
 *   <li>PUT  /api/ecn/{id}               — 更新 ECN（仅 DRAFT + 仅发起人）</li>
 *   <li>POST /api/ecn/{id}/submit        — 提交审批（启动 Flowable 流程）</li>
 *   <li>POST /api/ecn/{id}/cancel        — 撤回 ECN（仅 DRAFT）</li>
 * </ul>
 *
 * <p>Flowable Task 操作（complete / delegate / claim）由独立的
 * {@code EcnTaskController} 处理 —— Phase 2 后续接入。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/ecn")
@RequiredArgsConstructor
@Tag(name = "ECN 工程变更", description = "ECN CRUD + Flowable 审批流")
public class EcnController {

    private final EcnService ecnService;
    private final SysUserMapper userMapper;

    @GetMapping
    @Operation(summary = "分页查询 ECN（按角色自动过滤）")
    public Result<Page<EcnChangeVO>> list(EcnQuery query) {
        return Result.success(ecnService.listEcn(query, currentUser()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ECN 详情（带权限校验）")
    public Result<EcnChangeVO> detail(@PathVariable Long id) {
        return Result.success(ecnService.getEcnById(id, currentUser()));
    }

    @GetMapping("/{id}/approvals")
    @Operation(summary = "ECN 审批记录列表（按步骤顺序）")
    public Result<List<EcnApprovalVO>> approvals(@PathVariable Long id) {
        return Result.success(ecnService.listApprovals(id, currentUser()));
    }

    @PostMapping
    @AuditLog(operationType = "CREATE", resourceType = "ECN",
            description = "创建 ECN")
    @Operation(summary = "创建 ECN（DRAFT 状态）")
    public Result<EcnChangeVO> create(@Valid @RequestBody CreateEcnRequest req) {
        return Result.success(ecnService.createEcn(req, currentUser()));
    }

    @PutMapping("/{id}")
    @AuditLog(operationType = "UPDATE", resourceType = "ECN",
            resourceIdParam = "#id", description = "更新 ECN")
    @Operation(summary = "更新 ECN（仅 DRAFT + 仅发起人）")
    public Result<EcnChangeVO> update(@PathVariable Long id, @Valid @RequestBody CreateEcnRequest req) {
        return Result.success(ecnService.updateEcn(id, req, currentUser()));
    }

    @PostMapping("/{id}/submit")
    @AuditLog(operationType = "SUBMIT", resourceType = "ECN",
            resourceIdParam = "#id", description = "提交 ECN 审批")
    @Operation(summary = "提交 ECN 审批（DRAFT → UNDER_REVIEW，启动 Flowable 流程）")
    public Result<EcnChangeVO> submit(@PathVariable Long id) {
        return Result.success(ecnService.submitForReview(id, currentUser()));
    }

    @PostMapping("/{id}/cancel")
    @AuditLog(operationType = "CANCEL", resourceType = "ECN",
            resourceIdParam = "#id", description = "撤回 ECN")
    @Operation(summary = "撤回 ECN（仅 DRAFT 状态）")
    public Result<EcnChangeVO> cancel(@PathVariable Long id) {
        return Result.success(ecnService.cancelEcn(id, currentUser()));
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
