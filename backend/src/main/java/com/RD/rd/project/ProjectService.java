package com.RD.rd.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.RD.common.BusinessException;
import com.RD.entity.Milestone;
import com.RD.entity.Project;
import com.RD.entity.SysUser;
import com.RD.mapper.MilestoneMapper;
import com.RD.mapper.ProjectMapper;
import com.RD.mapper.SysUserMapper;
import com.RD.rd.common.RdConstants;
import com.RD.rd.project.dto.MilestoneVO;
import com.RD.rd.project.dto.ProjectQuery;
import com.RD.rd.project.dto.ProjectVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 项目服务（项目 + 里程碑）
 *
 * <p>数据权限：</p>
 * <ul>
 *   <li>ADMIN：看全部</li>
 *   <li>MANAGER：仅看自己负责的项目</li>
 *   <li>EMPLOYEE：仅看自己作为创建人 / 负责人的项目</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final MilestoneMapper milestoneMapper;
    private final SysUserMapper userMapper;

    // ============================================
    // Project CRUD
    // ============================================

    /**
     * 分页查询项目（带数据权限过滤）
     */
    public Page<ProjectVO> listProjects(ProjectQuery query, SysUser currentUser) {
        if (currentUser == null) {
            throw BusinessException.unauthorized("未登录");
        }

        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();

        // 关键词模糊匹配
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(Project::getCode, query.getKeyword())
                    .or().like(Project::getName, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getType())) {
            wrapper.eq(Project::getType, query.getType());
        }
        if (StringUtils.hasText(query.getPhase())) {
            wrapper.eq(Project::getPhase, query.getPhase());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Project::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getManagerUserid())) {
            wrapper.eq(Project::getManagerUserid, query.getManagerUserid());
        }

        // 数据权限过滤
        applyDataPermissionFilter(wrapper, currentUser);

        // 排序：按创建时间倒序
        wrapper.orderByDesc(Project::getCreatedAt);

        Page<Project> page = new Page<>(
                query.getPageNum() != null && query.getPageNum() > 0 ? query.getPageNum() : 1,
                query.getPageSize() != null && query.getPageSize() > 0 ? query.getPageSize() : 10);

        Page<Project> result = projectMapper.selectPage(page, wrapper);
        return convertProjectPage(result);
    }

    /**
     * 根据 ID 查询项目详情
     */
    public ProjectVO getProjectById(Long id, SysUser currentUser) {
        if (id == null) {
            throw BusinessException.badRequest("项目 ID 不能为空");
        }
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw BusinessException.notFound("项目不存在: " + id);
        }
        // 权限校验
        checkProjectPermission(project, currentUser, "查看");
        return toProjectVO(project);
    }

    /**
     * 创建项目
     */
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO createProject(Project project, SysUser currentUser) {
        validateProject(project);
        // 编号去重
        if (projectMapper.selectByCode(project.getCode()) != null) {
            throw BusinessException.badRequest("项目编号已存在: " + project.getCode());
        }
        project.setCreatedBy(currentUser.getUserId());
        project.setStatus(RdConstants.ProjectStatus.PLANNING);
        if (project.getProgress() == null) {
            project.setProgress(java.math.BigDecimal.ZERO);
        }
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.insert(project);
        log.info("[项目] 创建项目: id={}, code={}, name={}, by={}",
                project.getId(), project.getCode(), project.getName(), currentUser.getUserId());
        return toProjectVO(project);
    }

    /**
     * 更新项目
     */
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO updateProject(Long id, Project project, SysUser currentUser) {
        Project existing = projectMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("项目不存在: " + id);
        }
        checkProjectPermission(existing, currentUser, "修改");

        // 不允许改编号
        if (StringUtils.hasText(project.getCode()) && !project.getCode().equals(existing.getCode())) {
            throw BusinessException.badRequest("项目编号不可修改");
        }
        // 不允许改 createdBy
        project.setCreatedBy(null);
        // 主键 ID 必须由请求体传入
        project.setId(id);
        project.setUpdatedAt(LocalDateTime.now());

        projectMapper.updateById(project);
        log.info("[项目] 更新项目: id={}, operator={}", id, currentUser.getUserId());
        return toProjectVO(projectMapper.selectById(id));
    }

    /**
     * 删除项目（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long id, SysUser currentUser) {
        Project existing = projectMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("项目不存在: " + id);
        }
        checkProjectPermission(existing, currentUser, "删除");

        // 物理删除里程碑（避免悬挂）
        milestoneMapper.delete(new LambdaQueryWrapper<Milestone>().eq(Milestone::getProjectId, id));
        projectMapper.deleteById(id);
        log.info("[项目] 删除项目: id={}, operator={}", id, currentUser.getUserId());
    }

    // ============================================
    // Milestone CRUD
    // ============================================

    /**
     * 查询项目里程碑列表
     */
    public List<MilestoneVO> listMilestones(Long projectId, SysUser currentUser) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BusinessException.notFound("项目不存在: " + projectId);
        }
        checkProjectPermission(project, currentUser, "查看");

        List<Milestone> milestones = milestoneMapper.selectByProjectId(projectId);
        if (milestones.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量查责任人姓名
        Set<String> ownerIds = milestones.stream()
                .map(Milestone::getOwnerUserid)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Map<String, String> ownerNameMap = batchLookupUserNames(ownerIds);

        return milestones.stream().map(m -> toMilestoneVO(m, ownerNameMap)).collect(Collectors.toList());
    }

    /**
     * 创建里程碑
     */
    @Transactional(rollbackFor = Exception.class)
    public MilestoneVO createMilestone(Long projectId, Milestone milestone, SysUser currentUser) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BusinessException.notFound("项目不存在: " + projectId);
        }
        checkProjectPermission(project, currentUser, "修改");

        validateMilestone(milestone);
        milestone.setProjectId(projectId);
        if (milestone.getStatus() == null) {
            milestone.setStatus(RdConstants.MilestoneStatus.NOT_STARTED);
        }
        if (milestone.getProgress() == null) {
            milestone.setProgress(java.math.BigDecimal.ZERO);
        }
        milestone.setCreatedAt(LocalDateTime.now());
        milestone.setUpdatedAt(LocalDateTime.now());
        milestoneMapper.insert(milestone);

        // 更新项目进度（按所有里程碑平均）
        updateProjectProgressFromMilestones(projectId);

        log.info("[里程碑] 创建: id={}, projectId={}, name={}",
                milestone.getId(), projectId, milestone.getName());
        return toMilestoneVO(milestone, batchLookupUserNames(Set.of(milestone.getOwnerUserid())));
    }

    /**
     * 删除里程碑
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMilestone(Long milestoneId, SysUser currentUser) {
        Milestone existing = milestoneMapper.selectById(milestoneId);
        if (existing == null) {
            throw BusinessException.notFound("里程碑不存在: " + milestoneId);
        }
        Project project = projectMapper.selectById(existing.getProjectId());
        if (project != null) {
            checkProjectPermission(project, currentUser, "修改");
        }
        milestoneMapper.deleteById(milestoneId);
        updateProjectProgressFromMilestones(existing.getProjectId());
        log.info("[里程碑] 删除: id={}, operator={}", milestoneId, currentUser.getUserId());
    }

    // ============================================
    // 内部方法
    // ============================================

    /**
     * 数据权限过滤
     */
    private void applyDataPermissionFilter(LambdaQueryWrapper<Project> wrapper, SysUser currentUser) {
        String role = currentUser.getRole();
        if ("ADMIN".equals(role)) {
            return; // 管理员看全部
        }
        if ("MANAGER".equals(role)) {
            // 经理：仅看自己负责的项目
            wrapper.eq(Project::getManagerUserid, currentUser.getUserId());
            return;
        }
        // 普通员工：仅看自己作为创建人或负责人的项目
        wrapper.and(w -> w.eq(Project::getManagerUserid, currentUser.getUserId())
                .or().eq(Project::getCreatedBy, currentUser.getUserId()));
    }

    /**
     * 单条项目权限校验
     */
    private void checkProjectPermission(Project project, SysUser currentUser, String operation) {
        if (currentUser == null) {
            throw BusinessException.unauthorized("未登录");
        }
        String role = currentUser.getRole();
        if ("ADMIN".equals(role)) {
            return;
        }
        boolean isManager = project.getManagerUserid() != null
                && project.getManagerUserid().equals(currentUser.getUserId());
        boolean isCreator = project.getCreatedBy() != null
                && project.getCreatedBy().equals(currentUser.getUserId());
        if (!isManager && !isCreator) {
            log.warn("[项目] 用户 {} 无权{}项目 {}（非负责人/创建人）",
                    currentUser.getUserId(), operation, project.getId());
            throw BusinessException.forbidden("无权" + operation + "该项目");
        }
    }

    /**
     * 校验项目字段
     */
    private void validateProject(Project project) {
        if (project == null) {
            throw BusinessException.badRequest("项目数据不能为空");
        }
        if (!StringUtils.hasText(project.getCode())) {
            throw BusinessException.badRequest("项目编号不能为空");
        }
        if (!StringUtils.hasText(project.getName())) {
            throw BusinessException.badRequest("项目名称不能为空");
        }
        if (!StringUtils.hasText(project.getType())
                || !java.util.Set.of("HARDWARE", "FIRMWARE", "SOFTWARE", "MIXED").contains(project.getType())) {
            throw BusinessException.badRequest("项目类型必须是 HARDWARE / FIRMWARE / SOFTWARE / MIXED");
        }
        if (StringUtils.hasText(project.getPhase())
                && !java.util.Set.of("EVT", "DVT", "PVT", "MP").contains(project.getPhase())) {
            throw BusinessException.badRequest("项目阶段必须是 EVT / DVT / PVT / MP");
        }
        if (project.getStartDate() != null && project.getEndDate() != null
                && project.getEndDate().isBefore(project.getStartDate())) {
            throw BusinessException.badRequest("结束日期不能早于开始日期");
        }
    }

    /**
     * 校验里程碑字段
     */
    private void validateMilestone(Milestone m) {
        if (m == null) {
            throw BusinessException.badRequest("里程碑数据不能为空");
        }
        if (!StringUtils.hasText(m.getName())) {
            throw BusinessException.badRequest("里程碑名称不能为空");
        }
        if (!StringUtils.hasText(m.getPhase())
                || !java.util.Set.of("EVT", "DVT", "PVT", "MP").contains(m.getPhase())) {
            throw BusinessException.badRequest("阶段必须是 EVT / DVT / PVT / MP");
        }
        if (m.getPlannedStart() == null || m.getPlannedEnd() == null) {
            throw BusinessException.badRequest("计划开始/结束日期不能为空");
        }
        if (m.getPlannedEnd().isBefore(m.getPlannedStart())) {
            throw BusinessException.badRequest("计划结束日期不能早于开始日期");
        }
    }

    /**
     * 项目进度 = 所有里程碑 progress 的平均值
     */
    private void updateProjectProgressFromMilestones(Long projectId) {
        List<Milestone> milestones = milestoneMapper.selectByProjectId(projectId);
        if (milestones.isEmpty()) {
            return;
        }
        double avg = milestones.stream()
                .mapToDouble(m -> m.getProgress() == null ? 0.0 : m.getProgress().doubleValue())
                .average()
                .orElse(0.0);
        java.math.BigDecimal progress = java.math.BigDecimal.valueOf(avg)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        Project project = projectMapper.selectById(projectId);
        if (project != null) {
            project.setProgress(progress);
            project.setUpdatedAt(LocalDateTime.now());
            projectMapper.updateById(project);
        }
    }

    /**
     * 批量查用户姓名
     */
    private Map<String, String> batchLookupUserNames(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUser> users = userMapper.selectByUserIds(new java.util.ArrayList<>(userIds));
        Map<String, String> map = new HashMap<>(users.size() * 2);
        for (SysUser u : users) {
            map.put(u.getUserId(), u.getName());
        }
        return map;
    }

    /**
     * 分页结果转换
     */
    private Page<ProjectVO> convertProjectPage(Page<Project> result) {
        Page<ProjectVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<Project> records = result.getRecords();
        if (records.isEmpty()) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }
        // 批量查负责人 + 创建人姓名
        Set<String> userIds = records.stream()
                .flatMap(p -> java.util.stream.Stream.of(p.getManagerUserid(), p.getCreatedBy()))
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Map<String, String> userNameMap = batchLookupUserNames(userIds);

        List<ProjectVO> voList = records.stream()
                .map(p -> toProjectVO(p, userNameMap))
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    private ProjectVO toProjectVO(Project p) {
        return toProjectVO(p, batchLookupUserNames(
                java.util.stream.Stream.of(p.getManagerUserid(), p.getCreatedBy())
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toSet())));
    }

    private ProjectVO toProjectVO(Project p, Map<String, String> userNameMap) {
        ProjectVO vo = new ProjectVO();
        vo.setId(p.getId());
        vo.setCode(p.getCode());
        vo.setName(p.getName());
        vo.setType(p.getType());
        vo.setPhase(p.getPhase());
        vo.setManagerUserid(p.getManagerUserid());
        vo.setManagerName(userNameMap.get(p.getManagerUserid()));
        vo.setStartDate(p.getStartDate());
        vo.setEndDate(p.getEndDate());
        vo.setActualStartDate(p.getActualStartDate());
        vo.setActualEndDate(p.getActualEndDate());
        vo.setStatus(p.getStatus());
        vo.setProgress(p.getProgress());
        vo.setDescription(p.getDescription());
        vo.setTags(p.getTags());
        vo.setCreatedBy(p.getCreatedBy());
        vo.setCreatedByName(userNameMap.get(p.getCreatedBy()));
        vo.setCreatedAt(p.getCreatedAt());
        vo.setUpdatedAt(p.getUpdatedAt());
        return vo;
    }

    private MilestoneVO toMilestoneVO(Milestone m, Map<String, String> ownerNameMap) {
        MilestoneVO vo = new MilestoneVO();
        vo.setId(m.getId());
        vo.setProjectId(m.getProjectId());
        vo.setName(m.getName());
        vo.setPhase(m.getPhase());
        vo.setPlannedStart(m.getPlannedStart());
        vo.setPlannedEnd(m.getPlannedEnd());
        vo.setActualStart(m.getActualStart());
        vo.setActualEnd(m.getActualEnd());
        vo.setProgress(m.getProgress());
        vo.setDependencies(m.getDependencies());
        vo.setOwnerUserid(m.getOwnerUserid());
        vo.setOwnerName(m.getOwnerUserid() != null ? ownerNameMap.get(m.getOwnerUserid()) : null);
        vo.setStatus(m.getStatus());
        vo.setDescription(m.getDescription());
        vo.setCreatedAt(m.getCreatedAt());
        vo.setUpdatedAt(m.getUpdatedAt());
        return vo;
    }
}
