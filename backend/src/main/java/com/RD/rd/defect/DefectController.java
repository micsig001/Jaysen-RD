package com.RD.rd.defect;

import com.RD.audit.AuditLog;
import com.RD.common.Result;
import com.RD.entity.SysUser;
import com.RD.mapper.SysUserMapper;
import com.RD.rd.defect.dto.CreateDefectRequest;
import com.RD.rd.defect.dto.DefectStatsVO;
import com.RD.rd.defect.dto.DefectVO;
import com.RD.rd.defect.dto.UpdateDefectRequest;
import com.RD.rd.defect.service.DefectService;
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
 * 缺陷跟踪 Controller
 *
 * <p>接口列表：</p>
 * <ul>
 *   <li>GET    /api/defects                       — 缺陷列表 (projectId/status/severity/assignee/reporter/keyword)</li>
 *   <li>GET    /api/defects/{id}                  — 缺陷详情</li>
 *   <li>POST   /api/defects                       — 创建缺陷</li>
 *   <li>PUT    /api/defects/{id}                  — 全量更新 (含状态/严重度等)</li>
 *   <li>DELETE /api/defects/{id}                  — 软删除 (reporter/assignee/ADMIN/MANAGER)</li>
 *   <li>POST   /api/defects/{id}/transition       — 状态机转移 (按允许的 next 状态)</li>
 *   <li>GET    /api/defects/stats                 — 缺陷统计 (按 severity/status/趋势)</li>
 *   <li>GET    /api/defects/enums                 — 枚举常量 (供前端下拉用)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "缺陷跟踪", description = "缺陷上报 + 状态机 + 统计")
public class DefectController {

    private final DefectService defectService;
    private final SysUserMapper userMapper;

    @GetMapping("/api/defects")
    @Operation(summary = "缺陷列表（多条件过滤）")
    public Result<List<DefectVO>> listDefects(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String assigneeUserid,
            @RequestParam(required = false) String reporterUserid,
            @RequestParam(required = false) String keyword) {
        return Result.success(defectService.listDefects(
                projectId, status, severity, assigneeUserid, reporterUserid, keyword));
    }

    @GetMapping("/api/defects/stats")
    @Operation(summary = "缺陷统计（仪表盘用）")
    public Result<DefectStatsVO> getStats(@RequestParam(required = false) Long projectId) {
        return Result.success(defectService.getStats(projectId));
    }

    @GetMapping("/api/defects/enums")
    @Operation(summary = "枚举常量（严重度/状态/优先级/阶段）")
    public Result<EnumBundle> getEnums() {
        return Result.success(new EnumBundle(
                List.of("CRITICAL", "MAJOR", "MINOR", "TRIVIAL"),
                List.of("HIGH", "MEDIUM", "LOW"),
                List.of("NEW", "ANALYZING", "FIX_IN_PROGRESS", "FIXED", "VERIFIED", "CLOSED", "REOPENED"),
                List.of("EVT", "DVT", "PVT", "MP")
        ));
    }

    @GetMapping("/api/defects/{id}")
    @Operation(summary = "缺陷详情")
    public Result<DefectVO> getDefect(@PathVariable Long id) {
        return Result.success(defectService.getDefectById(id));
    }

    @PostMapping("/api/defects")
    @AuditLog(operationType = "CREATE", resourceType = "DEFECT",
            description = "创建缺陷")
    @Operation(summary = "创建缺陷")
    public Result<DefectVO> createDefect(@Valid @RequestBody CreateDefectRequest req) {
        return Result.success(defectService.createDefect(req, currentUser()));
    }

    @PutMapping("/api/defects/{id}")
    @AuditLog(operationType = "UPDATE", resourceType = "DEFECT",
            resourceIdParam = "#id", description = "更新缺陷")
    @Operation(summary = "全量更新缺陷（含状态/严重度等）")
    public Result<DefectVO> updateDefect(@PathVariable Long id,
                                          @Valid @RequestBody UpdateDefectRequest req) {
        return Result.success(defectService.updateDefect(id, req, currentUser()));
    }

    @PostMapping("/api/defects/{id}/transition")
    @AuditLog(operationType = "TRANSITION", resourceType = "DEFECT",
            resourceIdParam = "#id", description = "缺陷状态机转移")
    @Operation(summary = "状态机转移 (targetStatus in body)")
    public Result<DefectVO> transition(@PathVariable Long id,
                                         @RequestBody java.util.Map<String, String> body) {
        String target = body.get("targetStatus");
        return Result.success(defectService.transitionDefect(id, target, currentUser()));
    }

    @DeleteMapping("/api/defects/{id}")
    @AuditLog(operationType = "DELETE", resourceType = "DEFECT",
            resourceIdParam = "#id", description = "软删除缺陷")
    @Operation(summary = "软删除缺陷（reporter/assignee/ADMIN/MANAGER）")
    public Result<Void> deleteDefect(@PathVariable Long id) {
        defectService.deleteDefect(id, currentUser());
        return Result.success();
    }

    // ========== 工具 ==========

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

    /**
     * 枚举常量打包（前端下拉用）
     */
    public record EnumBundle(
            List<String> severities,
            List<String> priorities,
            List<String> statuses,
            List<String> phases
    ) { }
}
