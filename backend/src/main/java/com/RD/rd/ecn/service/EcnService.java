package com.RD.rd.ecn.service;

import com.RD.common.BusinessException;
import com.RD.entity.EcnApproval;
import com.RD.entity.EcnChange;
import com.RD.entity.SysUser;
import com.RD.mapper.EcnApprovalMapper;
import com.RD.mapper.EcnChangeMapper;
import com.RD.mapper.SysUserMapper;
import com.RD.rd.ecn.dto.CreateEcnRequest;
import com.RD.rd.ecn.dto.EcnApprovalVO;
import com.RD.rd.ecn.dto.EcnChangeVO;
import com.RD.rd.ecn.dto.EcnQuery;
import com.RD.rd.ecn.listener.EcnApprovalNotifyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * ECN 业务服务
 *
 * <p>职责：</p>
 * <ul>
 *   <li>ECN 增删改查 + 分页查询（按角色过滤）</li>
 *   <li>提交审批（{@code submitForReview}）：调 Flowable 启动流程实例，写 ecn_approval 记录</li>
 *   <li>Flowable TaskListener 处理审批结果（Phase 2 后续完善）</li>
 * </ul>
 *
 * <p>状态机：</p>
 * <pre>
 *   DRAFT → submitForReview → UNDER_REVIEW（Flowable 接管）
 *   UNDER_REVIEW → all approved → APPROVED
 *   UNDER_REVIEW → any rejected → REJECTED
 *   APPROVED → manual mark IMPLEMENTED
 *   * → manual CANCELLED (only from DRAFT)
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcnService {

    private final EcnChangeMapper ecnChangeMapper;
    private final EcnApprovalMapper ecnApprovalMapper;
    private final SysUserMapper userMapper;
    private final EcnApprovalNotifyService notifyService;

    /** 注入 Flowable 引擎（由 FlowableConfig.processEngine() Bean 提供） */
    private final ProcessEngine processEngine;

    /** ECN 业务编号前缀 */
    private static final String ECN_NO_PREFIX = "ECN";
    private static final DateTimeFormatter ECN_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ============================================
    // CRUD
    // ============================================

    /**
     * 分页查询 ECN 列表
     *
     * <p>数据权限：</p>
     * <ul>
     *   <li>ADMIN：全部</li>
     *   <li>MANAGER：本部门成员发起的</li>
     *   <li>EMPLOYEE：仅自己发起的</li>
     * </ul>
     */
    public Page<EcnChangeVO> listEcn(EcnQuery query, SysUser currentUser) {
        if (currentUser == null) {
            throw BusinessException.unauthorized("未登录");
        }
        LambdaQueryWrapper<EcnChange> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(EcnChange::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getChangeType())) {
            wrapper.eq(EcnChange::getChangeType, query.getChangeType());
        }
        if (StringUtils.hasText(query.getUrgency())) {
            wrapper.eq(EcnChange::getUrgency, query.getUrgency());
        }
        if (StringUtils.hasText(query.getRequesterUserid())) {
            wrapper.eq(EcnChange::getRequesterUserid, query.getRequesterUserid());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(EcnChange::getEcnNumber, kw).or().like(EcnChange::getTitle, kw));
        }
        applyDataPermissionFilter(wrapper, currentUser);
        wrapper.orderByDesc(EcnChange::getCreatedAt);

        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1
                ? 20
                : Math.min(query.getPageSize(), 200);
        Page<EcnChange> page = new Page<>(pageNum, pageSize);
        Page<EcnChange> result = ecnChangeMapper.selectPage(page, wrapper);

        Page<EcnChangeVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        if (result.getRecords().isEmpty()) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        // 批量查发起人姓名
        Set<String> requesterIds = result.getRecords().stream()
                .map(EcnChange::getRequesterUserid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, String> nameMap = requesterIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectByUserIds(new java.util.ArrayList<>(requesterIds)).stream()
                        .collect(Collectors.toMap(SysUser::getUserId, SysUser::getName));

        voPage.setRecords(result.getRecords().stream()
                .map(e -> toChangeVO(e, nameMap.get(e.getRequesterUserid())))
                .collect(Collectors.toList()));
        return voPage;
    }

    /**
     * ECN 详情（带权限校验）
     */
    public EcnChangeVO getEcnById(Long id, SysUser currentUser) {
        EcnChange ecn = ecnChangeMapper.selectById(id);
        if (ecn == null) {
            throw BusinessException.notFound("ECN 不存在: " + id);
        }
        if (!hasDataPermission(ecn, currentUser)) {
            throw BusinessException.notFound("ECN 不存在: " + id);
        }
        return toChangeVO(ecn, lookupName(ecn.getRequesterUserid()));
    }

    /**
     * 通过 ecn_number 查 ECN 详情（带权限校验）
     *
     * <p>给 Flowable TaskListener / TaskService 用 —— BPMN business key 是 ecn_number。</p>
     */
    public EcnChangeVO getEcnByEcnNumber(String ecnNumber, SysUser currentUser) {
        if (!StringUtils.hasText(ecnNumber)) {
            throw BusinessException.badRequest("ecnNumber 不能为空");
        }
        EcnChange ecn = ecnChangeMapper.selectOne(
                new LambdaQueryWrapper<EcnChange>()
                        .eq(EcnChange::getEcnNumber, ecnNumber)
                        .last("LIMIT 1"));
        if (ecn == null) {
            return null;
        }
        if (!hasDataPermission(ecn, currentUser)) {
            return null;
        }
        return toChangeVO(ecn, lookupName(ecn.getRequesterUserid()));
    }

    /**
     * ECN 审批记录列表
     */
    public List<EcnApprovalVO> listApprovals(Long ecnId, SysUser currentUser) {
        // 触发权限检查
        getEcnById(ecnId, currentUser);

        List<EcnApproval> approvals = ecnApprovalMapper.selectList(
                new LambdaQueryWrapper<EcnApproval>()
                        .eq(EcnApproval::getEcnId, ecnId)
                        .orderByAsc(EcnApproval::getStepOrder));
        if (approvals.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> approverIds = approvals.stream()
                .map(EcnApproval::getApproverUserid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, String> nameMap = approverIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectByUserIds(new java.util.ArrayList<>(approverIds)).stream()
                        .collect(Collectors.toMap(SysUser::getUserId, SysUser::getName));
        return approvals.stream().map(a -> toApprovalVO(a, nameMap.get(a.getApproverUserid())))
                .collect(Collectors.toList());
    }

    /**
     * 创建 ECN（DRAFT 状态）
     */
    @Transactional(rollbackFor = Exception.class)
    public EcnChangeVO createEcn(CreateEcnRequest req, SysUser currentUser) {
        validateCreate(req);
        EcnChange ecn = new EcnChange();
        ecn.setEcnNumber(generateEcnNumber());
        ecn.setTitle(req.getTitle());
        ecn.setChangeType(req.getChangeType());
        ecn.setUrgency(StringUtils.hasText(req.getUrgency()) ? req.getUrgency() : "NORMAL");
        ecn.setReason(req.getReason());
        ecn.setDescription(req.getDescription());
        ecn.setImpactAnalysis(req.getImpactAnalysis());
        ecn.setAffectedBomIds(req.getAffectedBomIds());
        ecn.setRequesterUserid(currentUser.getUserId());
        ecn.setProjectId(req.getProjectId());
        ecn.setStatus("DRAFT");
        ecn.setPriority(req.getPriority() == null ? 0 : req.getPriority());
        ecn.setTargetDate(req.getTargetDate());
        ecn.setCreatedAt(LocalDateTime.now());
        ecn.setUpdatedAt(LocalDateTime.now());

        ecnChangeMapper.insert(ecn);
        log.info("[ECN] 创建: id={}, ecnNumber={}, title={}, requester={}",
                ecn.getId(), ecn.getEcnNumber(), ecn.getTitle(), currentUser.getUserId());
        return getEcnById(ecn.getId(), currentUser);
    }

    /**
     * 更新 ECN（仅 DRAFT 状态 + 仅发起人可改）
     */
    @Transactional(rollbackFor = Exception.class)
    public EcnChangeVO updateEcn(Long id, CreateEcnRequest req, SysUser currentUser) {
        EcnChange existing = ecnChangeMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("ECN 不存在: " + id);
        }
        if (!"DRAFT".equals(existing.getStatus())) {
            throw BusinessException.badRequest("只有草稿状态的 ECN 可编辑");
        }
        if (!Objects.equals(existing.getRequesterUserid(), currentUser.getUserId())
                && !"ADMIN".equals(currentUser.getRole())) {
            throw BusinessException.forbidden("只能修改自己创建的 ECN");
        }

        existing.setTitle(req.getTitle());
        existing.setChangeType(req.getChangeType());
        existing.setUrgency(StringUtils.hasText(req.getUrgency()) ? req.getUrgency() : "NORMAL");
        existing.setReason(req.getReason());
        existing.setDescription(req.getDescription());
        existing.setImpactAnalysis(req.getImpactAnalysis());
        existing.setAffectedBomIds(req.getAffectedBomIds());
        existing.setProjectId(req.getProjectId());
        existing.setPriority(req.getPriority() == null ? 0 : req.getPriority());
        existing.setTargetDate(req.getTargetDate());
        existing.setUpdatedAt(LocalDateTime.now());

        ecnChangeMapper.updateById(existing);
        log.info("[ECN] 更新: id={}, operator={}", id, currentUser.getUserId());
        return getEcnById(id, currentUser);
    }

    /**
     * 提交审批（DRAFT → UNDER_REVIEW）
     *
     * <p>步骤：</p>
     * <ol>
     *   <li>校验 ECN 状态 + 权限</li>
     *   <li>写 ecn_approval 初始步骤记录（待 Flowable 任务创建后回填 task_id）</li>
     *   <li>启动 Flowable 流程实例（process key = "ecn-default"）</li>
     *   <li>回填 process_instance_id 到 ecn_change</li>
     *   <li>更新状态为 UNDER_REVIEW</li>
     * </ol>
     */
    @Transactional(rollbackFor = Exception.class)
    public EcnChangeVO submitForReview(Long id, SysUser currentUser) {
        EcnChange ecn = ecnChangeMapper.selectById(id);
        if (ecn == null) {
            throw BusinessException.notFound("ECN 不存在: " + id);
        }
        if (!"DRAFT".equals(ecn.getStatus())) {
            throw BusinessException.badRequest("只有草稿状态的 ECN 可提交审批（当前: " + ecn.getStatus() + "）");
        }
        if (!Objects.equals(ecn.getRequesterUserid(), currentUser.getUserId())
                && !"ADMIN".equals(currentUser.getRole())) {
            throw BusinessException.forbidden("只能提交自己创建的 ECN");
        }

        // 1) 启动 Flowable 流程
        Map<String, Object> variables = new HashMap<>();
        variables.put("ecnId", ecn.getId());
        variables.put("ecnNumber", ecn.getEcnNumber());
        variables.put("requesterUserid", ecn.getRequesterUserid());
        variables.put("urgency", ecn.getUrgency());

        RepositoryService repositoryService = processEngine.getRepositoryService();
        RuntimeService runtimeService = processEngine.getRuntimeService();

        // 校验流程定义已部署（Phase 2 启动期自动部署）
        if (repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("ecn-default").latestVersion().singleResult() == null) {
            throw BusinessException.badRequest("ECN 流程定义未部署，请联系管理员（process key: ecn-default）");
        }

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "ecn-default", ecn.getEcnNumber(), variables);

        // 2) 更新 ECN 状态
        ecn.setStatus("UNDER_REVIEW");
        ecn.setProcessInstanceId(processInstance.getId());
        ecn.setUpdatedAt(LocalDateTime.now());
        ecnChangeMapper.updateById(ecn);

        log.info("[ECN] 提交审批: id={}, ecnNumber={}, processInstanceId={}",
                id, ecn.getEcnNumber(), processInstance.getId());
        return getEcnById(id, currentUser);
    }

    /**
     * 撤回 ECN（仅 DRAFT 状态）
     */
    @Transactional(rollbackFor = Exception.class)
    public EcnChangeVO cancelEcn(Long id, SysUser currentUser) {
        EcnChange ecn = ecnChangeMapper.selectById(id);
        if (ecn == null) {
            throw BusinessException.notFound("ECN 不存在: " + id);
        }
        if (!"DRAFT".equals(ecn.getStatus())) {
            throw BusinessException.badRequest("已进入审批流程的 ECN 不能撤回（请走 Flowable 流程）");
        }
        if (!Objects.equals(ecn.getRequesterUserid(), currentUser.getUserId())
                && !"ADMIN".equals(currentUser.getRole())) {
            throw BusinessException.forbidden("只能撤回自己创建的 ECN");
        }

        ecn.setStatus("CANCELLED");
        ecn.setUpdatedAt(LocalDateTime.now());
        ecnChangeMapper.updateById(ecn);
        log.info("[ECN] 撤回: id={}, operator={}", id, currentUser.getUserId());
        return getEcnById(id, currentUser);
    }

    /**
     * 标记实施完成（APPROVED → IMPLEMENTED）
     *
     * <p>仅发起人或 ADMIN 可操作。实施完成意味着 ECN 已落地（修改了 BOM / 文档 / 工艺）。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public EcnChangeVO markImplemented(Long id, SysUser currentUser) {
        EcnChange ecn = ecnChangeMapper.selectById(id);
        if (ecn == null) {
            throw BusinessException.notFound("ECN 不存在: " + id);
        }
        if (!"APPROVED".equals(ecn.getStatus())) {
            throw BusinessException.badRequest("只有审批通过的 ECN 可标记实施完成（当前: " + ecn.getStatus() + "）");
        }
        if (!Objects.equals(ecn.getRequesterUserid(), currentUser.getUserId())
                && !"ADMIN".equals(currentUser.getRole())) {
            throw BusinessException.forbidden("只能标记自己创建的 ECN 实施完成");
        }

        ecn.setStatus("IMPLEMENTED");
        ecn.setCompletedAt(LocalDateTime.now());
        ecn.setUpdatedAt(LocalDateTime.now());
        ecnChangeMapper.updateById(ecn);
        log.info("[ECN] 实施完成: id={}, operator={}", id, currentUser.getUserId());
        // 通知（占位 log）
        notifyService.notifyImplemented(id);
        return getEcnById(id, currentUser);
    }

    /**
     * Flowable Task 完成后的回调（Phase 2 后续从 TaskListener 调用）
     *
     * <p>本回合先暴露方法,TaskListener 暂不实现（见 Round 2）。</p>
     */
    public void onApprovalCompleted(Long ecnId, boolean approved) {
        EcnChange ecn = ecnChangeMapper.selectById(ecnId);
        if (ecn == null || !"UNDER_REVIEW".equals(ecn.getStatus())) {
            return;
        }
        if (approved) {
            // 简化：单步通过直接 APPROVED，多步用 Flowable multi-instance
            // Round 2: 配合 processInstance 状态判断 + 子流程
            ecn.setStatus("APPROVED");
            ecn.setCompletedAt(LocalDateTime.now());
        } else {
            ecn.setStatus("REJECTED");
            ecn.setCompletedAt(LocalDateTime.now());
        }
        ecn.setUpdatedAt(LocalDateTime.now());
        ecnChangeMapper.updateById(ecn);
    }

    // ============================================
    // 内部
    // ============================================

    private void validateCreate(CreateEcnRequest req) {
        if (req == null) {
            throw BusinessException.badRequest("ECN 数据不能为空");
        }
        if (!StringUtils.hasText(req.getTitle())) {
            throw BusinessException.badRequest("标题不能为空");
        }
        if (!StringUtils.hasText(req.getChangeType())
                || !Set.of("DESIGN", "MATERIAL", "PROCESS", "DOCUMENT").contains(req.getChangeType())) {
            throw BusinessException.badRequest("变更类型非法");
        }
        if (!StringUtils.hasText(req.getReason())) {
            throw BusinessException.badRequest("变更原因不能为空");
        }
        if (!StringUtils.hasText(req.getDescription())) {
            throw BusinessException.badRequest("变更描述不能为空");
        }
        if (StringUtils.hasText(req.getUrgency())
                && !Set.of("NORMAL", "URGENT", "CRITICAL").contains(req.getUrgency())) {
            throw BusinessException.badRequest("紧急程度非法");
        }
    }

    private String generateEcnNumber() {
        String datePart = LocalDate.now().format(ECN_DATE_FMT);
        int randomPart = ThreadLocalRandom.current().nextInt(10_000);
        return ECN_NO_PREFIX + datePart + String.format("%04d", randomPart);
    }

    private String lookupName(String userId) {
        if (!StringUtils.hasText(userId)) return null;
        SysUser u = userMapper.selectByUserId(userId);
        return u != null ? u.getName() : null;
    }

    private void applyDataPermissionFilter(LambdaQueryWrapper<EcnChange> wrapper, SysUser currentUser) {
        String role = currentUser.getRole();
        String userId = currentUser.getUserId();

        if ("ADMIN".equals(role)) {
            return;
        }
        if ("MANAGER".equals(role)) {
            if (currentUser.getDepartmentId() == null) {
                wrapper.eq(EcnChange::getId, -1L);
                return;
            }
            List<String> deptUserIds = userMapper.selectUserIdsByDeptId(currentUser.getDepartmentId());
            if (deptUserIds == null || deptUserIds.isEmpty()) {
                wrapper.eq(EcnChange::getId, -1L);
                return;
            }
            wrapper.in(EcnChange::getRequesterUserid, deptUserIds);
            return;
        }
        // EMPLOYEE
        wrapper.eq(EcnChange::getRequesterUserid, userId);
    }

    private boolean hasDataPermission(EcnChange ecn, SysUser currentUser) {
        String role = currentUser.getRole();
        String userId = currentUser.getUserId();
        if ("ADMIN".equals(role)) {
            return true;
        }
        if ("MANAGER".equals(role)) {
            if (currentUser.getDepartmentId() == null) return false;
            List<String> deptUserIds = userMapper.selectUserIdsByDeptId(currentUser.getDepartmentId());
            if (deptUserIds == null || deptUserIds.isEmpty()) return false;
            return deptUserIds.contains(ecn.getRequesterUserid());
        }
        return userId.equals(ecn.getRequesterUserid());
    }

    private EcnChangeVO toChangeVO(EcnChange e, String requesterName) {
        EcnChangeVO vo = new EcnChangeVO();
        vo.setId(e.getId());
        vo.setEcnNumber(e.getEcnNumber());
        vo.setTitle(e.getTitle());
        vo.setChangeType(e.getChangeType());
        vo.setUrgency(e.getUrgency());
        vo.setReason(e.getReason());
        vo.setDescription(e.getDescription());
        vo.setImpactAnalysis(e.getImpactAnalysis());
        vo.setAffectedBomIds(e.getAffectedBomIds());
        vo.setRequesterUserid(e.getRequesterUserid());
        vo.setRequesterName(requesterName);
        vo.setProjectId(e.getProjectId());
        vo.setStatus(e.getStatus());
        vo.setPriority(e.getPriority());
        vo.setTargetDate(e.getTargetDate());
        vo.setCompletedAt(e.getCompletedAt());
        vo.setProcessInstanceId(e.getProcessInstanceId());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setUpdatedAt(e.getUpdatedAt());
        return vo;
    }

    private EcnApprovalVO toApprovalVO(EcnApproval a, String approverName) {
        EcnApprovalVO vo = new EcnApprovalVO();
        vo.setId(a.getId());
        vo.setEcnId(a.getEcnId());
        vo.setApproverUserid(a.getApproverUserid());
        vo.setApproverName(approverName);
        vo.setDepartment(a.getDepartment());
        vo.setRole(a.getRole());
        vo.setStepOrder(a.getStepOrder());
        vo.setStatus(a.getStatus());
        vo.setComment(a.getComment());
        vo.setSignatureUrl(a.getSignatureUrl());
        vo.setSignedAt(a.getSignedAt());
        vo.setTaskId(a.getTaskId());
        vo.setCreatedAt(a.getCreatedAt());
        return vo;
    }
}
