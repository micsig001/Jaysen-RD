package com.RD.rd.project;

import com.RD.audit.AuditLog;
import com.RD.common.Result;
import com.RD.entity.Milestone;
import com.RD.entity.Project;
import com.RD.entity.SysUser;
import com.RD.mapper.SysUserMapper;
import com.RD.rd.project.dto.MilestoneVO;
import com.RD.rd.project.dto.ProjectQuery;
import com.RD.rd.project.dto.ProjectVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 项目管理 Controller
 *
 * <p>接口列表：</p>
 * <ul>
 *   <li>GET    /api/projects              分页查询（带数据权限过滤）</li>
 *   <li>GET    /api/projects/{id}         项目详情</li>
 *   <li>POST   /api/projects              创建项目（仅 ADMIN/MANAGER）</li>
 *   <li>PUT    /api/projects/{id}         更新项目（需权限校验）</li>
 *   <li>DELETE /api/projects/{id}         删除项目（需权限校验）</li>
 *   <li>GET    /api/projects/{id}/milestones       里程碑列表</li>
 *   <li>POST   /api/projects/{id}/milestones       创建里程碑</li>
 *   <li>DELETE /api/projects/{id}/milestones/{mid} 删除里程碑</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "项目管理", description = "项目 / 里程碑 CRUD + 数据权限过滤")
public class ProjectController {

    private final ProjectService projectService;
    private final SysUserMapper userMapper;

    // ============================================
    // Project CRUD
    // ============================================

    @GetMapping
    @Operation(summary = "分页查询项目", description = "带数据权限过滤：ADMIN 看全部，MANAGER 看自己负责，EMPLOYEE 看自己相关")
    public Result<Page<ProjectVO>> listProjects(ProjectQuery query) {
        return Result.success(projectService.listProjects(query, currentUser()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "项目详情")
    public Result<ProjectVO> getProject(@PathVariable Long id) {
        return Result.success(projectService.getProjectById(id, currentUser()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @AuditLog(operationType = "CREATE", resourceType = "PROJECT",
            resourceIdParam = "#project.id", description = "创建项目")
    @Operation(summary = "创建项目")
    public Result<ProjectVO> createProject(@RequestBody Project project) {
        return Result.success(projectService.createProject(project, currentUser()));
    }

    @PutMapping("/{id}")
    @AuditLog(operationType = "UPDATE", resourceType = "PROJECT",
            resourceIdParam = "#id", description = "更新项目")
    @Operation(summary = "更新项目")
    public Result<ProjectVO> updateProject(@PathVariable Long id, @RequestBody Project project) {
        return Result.success(projectService.updateProject(id, project, currentUser()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @AuditLog(operationType = "DELETE", resourceType = "PROJECT",
            resourceIdParam = "#id", description = "删除项目")
    @Operation(summary = "删除项目")
    public Result<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id, currentUser());
        return Result.success();
    }

    // ============================================
    // Milestone CRUD
    // ============================================

    @GetMapping("/{id}/milestones")
    @Operation(summary = "项目里程碑列表")
    public Result<List<MilestoneVO>> listMilestones(@PathVariable Long id) {
        return Result.success(projectService.listMilestones(id, currentUser()));
    }

    @PostMapping("/{id}/milestones")
    @AuditLog(operationType = "CREATE", resourceType = "MILESTONE",
            resourceIdParam = "#milestone.id", description = "创建里程碑")
    @Operation(summary = "创建里程碑")
    public Result<MilestoneVO> createMilestone(@PathVariable Long id, @RequestBody Milestone milestone) {
        return Result.success(projectService.createMilestone(id, milestone, currentUser()));
    }

    @DeleteMapping("/{id}/milestones/{mid}")
    @AuditLog(operationType = "DELETE", resourceType = "MILESTONE",
            resourceIdParam = "#mid", description = "删除里程碑")
    @Operation(summary = "删除里程碑")
    public Result<Void> deleteMilestone(@PathVariable Long id, @PathVariable Long mid) {
        projectService.deleteMilestone(mid, currentUser());
        return Result.success();
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
