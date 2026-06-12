package com.RD.rd.defect.service;

import com.RD.common.BusinessException;
import com.RD.entity.Defect;
import com.RD.entity.Project;
import com.RD.entity.SprintTask;
import com.RD.entity.SysUser;
import com.RD.mapper.DefectMapper;
import com.RD.mapper.ProjectMapper;
import com.RD.mapper.SprintTaskMapper;
import com.RD.mapper.SysUserMapper;
import com.RD.rd.defect.dto.CreateDefectRequest;
import com.RD.rd.defect.dto.DefectStatsVO;
import com.RD.rd.defect.dto.DefectVO;
import com.RD.rd.defect.dto.UpdateDefectRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 缺陷跟踪服务
 *
 * <p>职责：</p>
 * <ul>
 *   <li>缺陷 CRUD</li>
 *   <li>状态机校验：NEW → ANALYZING → FIX_IN_PROGRESS → FIXED → VERIFIED → CLOSED, 可 REOPENED</li>
 *   <li>统计数据 (按 severity/status/severity 分组)</li>
 * </ul>
 *
 * <p>字段 {@code attachments} 是 JSON 字符串, 存 [{name,url,size}, ...], 通过 Jackson 转换。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefectService {

    private final DefectMapper defectMapper;
    private final ProjectMapper projectMapper;
    private final SprintTaskMapper sprintTaskMapper;
    private final SysUserMapper userMapper;
    private final ObjectMapper objectMapper;

    // 状态机允许的转移关系 (from -> allowed to)
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "NEW",              Set.of("ANALYZING", "FIX_IN_PROGRESS", "CLOSED"),
            "ANALYZING",        Set.of("FIX_IN_PROGRESS", "CLOSED"),
            "FIX_IN_PROGRESS",  Set.of("FIXED", "ANALYZING", "CLOSED"),
            "FIXED",            Set.of("VERIFIED", "REOPENED"),
            "VERIFIED",         Set.of("CLOSED", "REOPENED"),
            "CLOSED",           Set.of("REOPENED"),
            "REOPENED",         Set.of("ANALYZING", "FIX_IN_PROGRESS", "FIXED", "CLOSED")
    );

    // ============================================
    // 列表 / 详情
    // ============================================

    /**
     * 缺陷列表 (按多条件过滤)
     *
     * @param projectId    按项目
     * @param status       按状态
     * @param severity     按严重度
     * @param assigneeUserid 按处理人
     * @param reporterUserid 按报告人
     * @param keyword      模糊搜索 title / defectNumber
     */
    public List<DefectVO> listDefects(Long projectId, String status, String severity,
                                       String assigneeUserid, String reporterUserid, String keyword) {
        LambdaQueryWrapper<Defect> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(Defect::getProjectId, projectId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Defect::getStatus, status);
        }
        if (StringUtils.hasText(severity)) {
            wrapper.eq(Defect::getSeverity, severity);
        }
        if (StringUtils.hasText(assigneeUserid)) {
            wrapper.eq(Defect::getAssigneeUserid, assigneeUserid);
        }
        if (StringUtils.hasText(reporterUserid)) {
            wrapper.eq(Defect::getReporterUserid, reporterUserid);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Defect::getTitle, keyword)
                    .or().like(Defect::getDefectNumber, keyword));
        }
        // 优先级: CRITICAL/MAJOR 优先, 同优先级按 foundDate 倒序
        wrapper.last("ORDER BY FIELD(severity, 'CRITICAL', 'MAJOR', 'MINOR', 'TRIVIAL'), found_date DESC, id DESC");
        List<Defect> list = defectMapper.selectList(wrapper);
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        return enrichVOList(list);
    }

    /**
     * 缺陷详情
     */
    public DefectVO getDefectById(Long id) {
        Defect d = defectMapper.selectById(id);
        if (d == null) {
            throw BusinessException.notFound("缺陷不存在: " + id);
        }
        return enrichVO(d);
    }

    // ============================================
    // CRUD
    // ============================================

    /**
     * 创建缺陷 (默认状态 NEW, 自动生成 defect_number)
     */
    @Transactional(rollbackFor = Exception.class)
    public DefectVO createDefect(CreateDefectRequest req, SysUser currentUser) {
        validateCreate(req);

        // 项目存在性
        Project project = projectMapper.selectById(req.getProjectId());
        if (project == null) {
            throw BusinessException.badRequest("项目不存在: " + req.getProjectId());
        }

        // 看板任务存在性
        if (req.getSprintTaskId() != null) {
            SprintTask task = sprintTaskMapper.selectById(req.getSprintTaskId());
            if (task == null) {
                throw BusinessException.badRequest("关联看板任务不存在: " + req.getSprintTaskId());
            }
        }

        Defect d = new Defect();
        d.setDefectNumber(generateDefectNumber());
        d.setTitle(req.getTitle());
        d.setSeverity(req.getSeverity());
        d.setPriority(StringUtils.hasText(req.getPriority()) ? req.getPriority() : "MEDIUM");
        d.setStatus(StringUtils.hasText(req.getStatus()) ? req.getStatus() : "NEW");
        d.setPhaseFound(req.getPhaseFound());
        d.setRootCause(req.getRootCause());
        d.setCorrectiveAction(req.getCorrectiveAction());
        d.setPreventiveAction(req.getPreventiveAction());
        d.setReporterUserid(currentUser.getUserId());
        d.setAssigneeUserid(req.getAssigneeUserid());
        d.setVerifierUserid(req.getVerifierUserid());
        d.setProjectId(req.getProjectId());
        d.setSprintTaskId(req.getSprintTaskId());
        d.setFoundDate(req.getFoundDate());
        d.setAttachments(serializeAttachments(req.getAttachments()));

        LocalDateTime now = LocalDateTime.now();
        d.setCreatedAt(now);
        d.setUpdatedAt(now);
        defectMapper.insert(d);
        log.info("[缺陷] 创建: defectNumber={}, reporter={}", d.getDefectNumber(), currentUser.getUserId());
        return enrichVO(d);
    }

    /**
     * 全量更新缺陷 (PUT)
     *
     * <p>不允许改 reporter / createdAt; defectNumber 也不让改。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public DefectVO updateDefect(Long id, UpdateDefectRequest req, SysUser currentUser) {
        Defect d = defectMapper.selectById(id);
        if (d == null) {
            throw BusinessException.notFound("缺陷不存在: " + id);
        }
        if (StringUtils.hasText(req.getTitle())) {
            d.setTitle(req.getTitle());
        }
        if (StringUtils.hasText(req.getSeverity())) {
            d.setSeverity(req.getSeverity());
        }
        if (StringUtils.hasText(req.getPriority())) {
            d.setPriority(req.getPriority());
        }
        if (StringUtils.hasText(req.getStatus())) {
            validateTransition(d.getStatus(), req.getStatus());
            d.setStatus(req.getStatus());
            // 自动维护日期
            LocalDate today = LocalDate.now();
            if ("FIXED".equals(req.getStatus()) && d.getResolvedDate() == null) {
                d.setResolvedDate(today);
            }
            if ("VERIFIED".equals(req.getStatus()) && d.getVerifiedDate() == null) {
                d.setVerifiedDate(today);
            }
            if ("CLOSED".equals(req.getStatus()) && d.getClosedDate() == null) {
                d.setClosedDate(today);
            }
            if ("REOPENED".equals(req.getStatus())) {
                // 重开后, resolved/verified/closed 日期全部清空
                d.setResolvedDate(null);
                d.setVerifiedDate(null);
                d.setClosedDate(null);
            }
        }
        if (req.getPhaseFound() != null) {
            d.setPhaseFound(req.getPhaseFound());
        }
        if (req.getRootCause() != null) {
            d.setRootCause(req.getRootCause());
        }
        if (req.getCorrectiveAction() != null) {
            d.setCorrectiveAction(req.getCorrectiveAction());
        }
        if (req.getPreventiveAction() != null) {
            d.setPreventiveAction(req.getPreventiveAction());
        }
        if (req.getAssigneeUserid() != null) {
            d.setAssigneeUserid(req.getAssigneeUserid());
        }
        if (req.getVerifierUserid() != null) {
            d.setVerifierUserid(req.getVerifierUserid());
        }
        if (req.getProjectId() != null) {
            d.setProjectId(req.getProjectId());
        }
        if (req.getSprintTaskId() != null) {
            d.setSprintTaskId(req.getSprintTaskId());
        }
        if (req.getFoundDate() != null) {
            d.setFoundDate(req.getFoundDate());
        }
        if (req.getResolvedDate() != null) {
            d.setResolvedDate(req.getResolvedDate());
        }
        if (req.getVerifiedDate() != null) {
            d.setVerifiedDate(req.getVerifiedDate());
        }
        if (req.getClosedDate() != null) {
            d.setClosedDate(req.getClosedDate());
        }
        if (req.getAttachments() != null) {
            d.setAttachments(serializeAttachments(req.getAttachments()));
        }

        d.setUpdatedAt(LocalDateTime.now());
        defectMapper.updateById(d);
        log.info("[缺陷] 更新: id={}, operator={}", id, currentUser.getUserId());
        return enrichVO(d);
    }

    /**
     * 软删除缺陷
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDefect(Long id, SysUser currentUser) {
        Defect d = defectMapper.selectById(id);
        if (d == null) {
            throw BusinessException.notFound("缺陷不存在: " + id);
        }
        // 权限: reporter / assignee / MANAGER / ADMIN
        boolean isOwner = currentUser.getUserId().equals(d.getReporterUserid())
                || currentUser.getUserId().equals(d.getAssigneeUserid());
        boolean isPrivileged = "ADMIN".equals(currentUser.getRole()) || "MANAGER".equals(currentUser.getRole());
        if (!isOwner && !isPrivileged) {
            throw BusinessException.forbidden("只有报告人/处理人/管理员可删除缺陷");
        }
        defectMapper.deleteById(id);
        log.info("[缺陷] 删除: id={}, operator={}", id, currentUser.getUserId());
    }

    // ============================================
    // 状态机便捷方法
    // ============================================

    /**
     * 状态转移: 进入下一阶段
     * 用于前端只点一个按钮 (例如"开始修复"), 后端算下一态
     */
    @Transactional(rollbackFor = Exception.class)
    public DefectVO transitionDefect(Long id, String targetStatus, SysUser currentUser) {
        Defect d = defectMapper.selectById(id);
        if (d == null) {
            throw BusinessException.notFound("缺陷不存在: " + id);
        }
        validateTransition(d.getStatus(), targetStatus);
        d.setStatus(targetStatus);
        LocalDate today = LocalDate.now();
        if ("FIXED".equals(targetStatus) && d.getResolvedDate() == null) {
            d.setResolvedDate(today);
        }
        if ("VERIFIED".equals(targetStatus) && d.getVerifiedDate() == null) {
            d.setVerifiedDate(today);
        }
        if ("CLOSED".equals(targetStatus) && d.getClosedDate() == null) {
            d.setClosedDate(today);
        }
        if ("REOPENED".equals(targetStatus)) {
            d.setResolvedDate(null);
            d.setVerifiedDate(null);
            d.setClosedDate(null);
        }
        d.setUpdatedAt(LocalDateTime.now());
        defectMapper.updateById(d);
        log.info("[缺陷] 状态转移: id={} {} -> {}, operator={}", id, d.getStatus(), targetStatus, currentUser.getUserId());
        return enrichVO(d);
    }

    // ============================================
    // 统计
    // ============================================

    /**
     * 缺陷统计 (仪表盘用)
     */
    public DefectStatsVO getStats(Long projectId) {
        LambdaQueryWrapper<Defect> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(Defect::getProjectId, projectId);
        }
        List<Defect> all = defectMapper.selectList(wrapper);
        DefectStatsVO vo = new DefectStatsVO();
        vo.setTotal((long) all.size());
        long open = 0;
        long openedThisWeek = 0;
        long closedThisWeek = 0;
        LocalDate oneWeekAgo = LocalDate.now().minusDays(7);
        for (Defect d : all) {
            if (!"CLOSED".equals(d.getStatus())) open++;
            if (d.getFoundDate() != null && !d.getFoundDate().isBefore(oneWeekAgo)) openedThisWeek++;
            if ("CLOSED".equals(d.getStatus()) && d.getClosedDate() != null
                    && !d.getClosedDate().isBefore(oneWeekAgo)) closedThisWeek++;
            switch (d.getSeverity()) {
                case "CRITICAL" -> vo.setCriticalCount(nz(vo.getCriticalCount()) + 1);
                case "MAJOR"    -> vo.setMajorCount(nz(vo.getMajorCount()) + 1);
                case "MINOR"    -> vo.setMinorCount(nz(vo.getMinorCount()) + 1);
                case "TRIVIAL"  -> vo.setTrivialCount(nz(vo.getTrivialCount()) + 1);
                default -> {}
            }
            switch (d.getStatus()) {
                case "NEW"             -> vo.setNewCount(nz(vo.getNewCount()) + 1);
                case "ANALYZING"       -> vo.setAnalyzingCount(nz(vo.getAnalyzingCount()) + 1);
                case "FIX_IN_PROGRESS" -> vo.setFixInProgressCount(nz(vo.getFixInProgressCount()) + 1);
                case "FIXED"           -> vo.setFixedCount(nz(vo.getFixedCount()) + 1);
                case "VERIFIED"        -> vo.setVerifiedCount(nz(vo.getVerifiedCount()) + 1);
                case "CLOSED"          -> vo.setClosedCount(nz(vo.getClosedCount()) + 1);
                case "REOPENED"        -> vo.setReopenedCount(nz(vo.getReopenedCount()) + 1);
                default -> {}
            }
        }
        vo.setOpen(open);
        vo.setOpenedThisWeek(openedThisWeek);
        vo.setClosedThisWeek(closedThisWeek);
        return vo;
    }

    // ============================================
    // 内部: 校验
    // ============================================

    private void validateCreate(CreateDefectRequest req) {
        if (req == null) {
            throw BusinessException.badRequest("缺陷数据不能为空");
        }
        if (!StringUtils.hasText(req.getTitle())) {
            throw BusinessException.badRequest("缺陷标题不能为空");
        }
        if (!StringUtils.hasText(req.getSeverity())) {
            throw BusinessException.badRequest("严重度不能为空");
        }
        if (req.getProjectId() == null) {
            throw BusinessException.badRequest("项目 ID 不能为空");
        }
        if (req.getFoundDate() == null) {
            throw BusinessException.badRequest("发现日期不能为空");
        }
        if (req.getFoundDate().isAfter(LocalDate.now())) {
            throw BusinessException.badRequest("发现日期不能晚于今天");
        }
    }

    /**
     * 校验状态转移是否合法
     */
    private void validateTransition(String from, String to) {
        if (from == null || to == null) return;
        if (from.equals(to)) return;
        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Collections.emptySet());
        if (!allowed.contains(to)) {
            throw BusinessException.badRequest("不允许的状态转移: " + from + " → " + to
                    + "（允许: " + allowed + "）");
        }
    }

    /**
     * 自动生成缺陷编号: DF-yyyyMMdd-{6位随机}
     */
    private String generateDefectNumber() {
        return "DF-" + LocalDate.now().toString().replace("-", "")
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private Long nz(Long v) { return v == null ? 0L : v; }

    // ============================================
    // 内部: VO 转换 + 关联填充
    // ============================================

    private List<DefectVO> enrichVOList(List<Defect> list) {
        // 批量查项目名 / 用户名 / 看板任务标题
        Set<Long> projectIds = list.stream().map(Defect::getProjectId)
                .filter(p -> p != null).collect(Collectors.toSet());
        Set<String> userIds = list.stream().flatMap(d -> java.util.stream.Stream.of(
                        d.getReporterUserid(), d.getAssigneeUserid(), d.getVerifierUserid()))
                .filter(u -> u != null && !u.isBlank()).collect(Collectors.toSet());
        Set<Long> sprintTaskIds = list.stream().map(Defect::getSprintTaskId)
                .filter(s -> s != null).collect(Collectors.toSet());

        Map<Long, String> projectNames = batchProjectNames(projectIds);
        Map<String, String> userNames = batchUserNames(userIds);
        Map<Long, String> sprintTaskTitles = batchSprintTaskTitles(sprintTaskIds);

        return list.stream().map(d -> toVO(d,
                projectNames.get(d.getProjectId()),
                userNames.get(d.getReporterUserid()),
                userNames.get(d.getAssigneeUserid()),
                userNames.get(d.getVerifierUserid()),
                sprintTaskTitles.get(d.getSprintTaskId()))).collect(Collectors.toList());
    }

    private DefectVO enrichVO(Defect d) {
        return enrichVOList(java.util.Collections.singletonList(d)).get(0);
    }

    private DefectVO toVO(Defect d, String projectName,
                          String reporterName, String assigneeName, String verifierName,
                          String sprintTaskTitle) {
        DefectVO vo = new DefectVO();
        vo.setId(d.getId());
        vo.setDefectNumber(d.getDefectNumber());
        vo.setTitle(d.getTitle());
        vo.setSeverity(d.getSeverity());
        vo.setPriority(d.getPriority());
        vo.setStatus(d.getStatus());
        vo.setPhaseFound(d.getPhaseFound());
        vo.setRootCause(d.getRootCause());
        vo.setCorrectiveAction(d.getCorrectiveAction());
        vo.setPreventiveAction(d.getPreventiveAction());
        vo.setReporterUserid(d.getReporterUserid());
        vo.setReporterName(reporterName);
        vo.setAssigneeUserid(d.getAssigneeUserid());
        vo.setAssigneeName(assigneeName);
        vo.setVerifierUserid(d.getVerifierUserid());
        vo.setVerifierName(verifierName);
        vo.setProjectId(d.getProjectId());
        vo.setProjectName(projectName);
        vo.setSprintTaskId(d.getSprintTaskId());
        vo.setSprintTaskTitle(sprintTaskTitle);
        vo.setFoundDate(d.getFoundDate());
        vo.setResolvedDate(d.getResolvedDate());
        vo.setVerifiedDate(d.getVerifiedDate());
        vo.setClosedDate(d.getClosedDate());
        vo.setAttachments(parseAttachments(d.getAttachments()));
        vo.setCreatedAt(d.getCreatedAt());
        vo.setUpdatedAt(d.getUpdatedAt());
        return vo;
    }

    private String serializeAttachments(List<DefectVO.Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(attachments);
        } catch (Exception e) {
            log.warn("[缺陷] 序列化 attachments 失败: {}", e.getMessage());
            return null;
        }
    }

    private List<DefectVO.Attachment> parseAttachments(String json) {
        if (!StringUtils.hasText(json)) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<DefectVO.Attachment>>() {});
        } catch (Exception e) {
            log.warn("[缺陷] 反序列化 attachments 失败: {}", e.getMessage());
            return Collections.emptyList();
        }
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

    private Map<Long, String> batchSprintTaskTitles(Set<Long> sprintTaskIds) {
        if (sprintTaskIds == null || sprintTaskIds.isEmpty()) return Collections.emptyMap();
        List<SprintTask> tasks = sprintTaskMapper.selectBatchIds(sprintTaskIds);
        return tasks.stream().collect(Collectors.toMap(SprintTask::getId, SprintTask::getTitle, (a, b) -> a));
    }
}
