package com.RD.rd.ecn.listener;

import com.RD.entity.EcnApproval;
import com.RD.entity.EcnChange;
import com.RD.entity.SysDepartment;
import com.RD.entity.SysUser;
import com.RD.mapper.EcnApprovalMapper;
import com.RD.mapper.EcnChangeMapper;
import com.RD.mapper.SysDepartmentMapper;
import com.RD.mapper.SysUserMapper;
import com.RD.rd.ecn.service.EcnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * ECN 审批任务监听器
 *
 * <p>挂载在 BPMN 的 UserTask 节点上，处理：</p>
 * <ul>
 *   <li>{@code create} 事件：写入 ecn_approval 记录（PENDING），回填 task_id</li>
 *   <li>{@code complete} 事件：更新 ecn_approval 为 APPROVED，签名时间</li>
 *   <li>{@code delete} 事件：标记 SKIPPED（驳回导致跳过）</li>
 * </ul>
 *
 * <p>通过 {@code eventName} 区分事件类型（BPMN XML 中 {@code <flowable:taskListener event="create"/>}）。</p>
 *
 * <p>部门负责人动态分配：</p>
 * <ol>
 *   <li>从 process variables 读 requesterUserid</li>
 *   <li>查 sys_user 拿 requester 的 departmentId</li>
 *   <li>查 sys_department 拿 leaderUserId</li>
 *   <li>设置 task.assignee = leaderUserId（仅当 BPMN 没显式指定 assignee 时）</li>
 * </ol>
 *
 * <p>降级：部门未配置 leader 时回退到发起人 + 写 warn 日志。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EcnApprovalTaskListener implements TaskListener {

    private final EcnApprovalMapper approvalMapper;
    private final EcnChangeMapper ecnChangeMapper;
    private final SysUserMapper userMapper;
    private final SysDepartmentMapper departmentMapper;
    private final EcnService ecnService;

    @Override
    public void notify(DelegateTask task) {
        String eventName = task.getEventName();
        log.debug("[ECN TaskListener] 事件: {} taskId={} taskName={}",
                eventName, task.getId(), task.getName());

        try {
            switch (eventName) {
                case EVENTNAME_CREATE:
                    handleCreate(task);
                    break;
                case EVENTNAME_COMPLETE:
                    handleComplete(task);
                    break;
                case EVENTNAME_DELETE:
                    handleDelete(task);
                    break;
                default:
                    log.debug("[ECN TaskListener] 未处理事件: {}", eventName);
            }
        } catch (Exception e) {
            log.error("[ECN TaskListener] 处理 {} 事件失败: taskId={}", eventName, task.getId(), e);
            // 不抛异常避免影响 Flowable 主流程
        }
    }

    /**
     * 任务创建：动态分配 assignee + 写 ecn_approval
     */
    private void handleCreate(DelegateTask task) {
        // 1) 部门负责人动态分配（仅当 BPMN 未显式设置 assignee 时）
        if (task.getAssignee() == null || task.getAssignee().isBlank()) {
            assignDepartmentLeader(task);
        }

        // 2) 写 ecn_approval 记录
        String assignee = task.getAssignee();
        if (assignee == null || assignee.isBlank()) {
            log.warn("[ECN TaskListener] 任务 {} 仍无 assignee，跳过 approval 写入", task.getId());
            return;
        }
        EcnApproval approval = new EcnApproval();
        // 业务 key = ecnNumber, 通过它查 ecn_id
        String businessKey = task.getExecution().getProcessInstanceBusinessKey();
        if (businessKey == null) {
            log.warn("[ECN TaskListener] 流程实例无 businessKey，跳过 approval 写入");
            return;
        }
        approval.setEcnId(lookupEcnIdByNumber(businessKey));
        approval.setApproverUserid(assignee);
        SysUser approver = userMapper.selectByUserId(assignee);
        if (approver != null) {
            approval.setDepartment(lookupDeptName(approver.getDepartmentId()));
        }
        approval.setRole(task.getTaskDefinitionKey()); // e.g. "deptLeaderReview"
        approval.setStepOrder(currentStepOrder(approval.getEcnId()) + 1);
        approval.setStatus("PENDING");
        approval.setTaskId(task.getId());
        approval.setCreatedAt(LocalDateTime.now());
        approval.setUpdatedAt(LocalDateTime.now());
        approvalMapper.insert(approval);
        log.info("[ECN TaskListener] 创建 approval: ecnId={} step={} approver={} role={}",
                approval.getEcnId(), approval.getStepOrder(),
                approval.getApproverUserid(), approval.getRole());
    }

    /**
     * 任务完成：更新 approval 状态
     */
    private void handleComplete(DelegateTask task) {
        EcnApproval approval = approvalMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EcnApproval>()
                        .eq(EcnApproval::getTaskId, task.getId()));
        if (approval == null) {
            log.warn("[ECN TaskListener] 任务 {} 完成但无 approval 记录（可能被删除）", task.getId());
            return;
        }
        // 从 task 变量读 approved (前端 complete 时传)
        Object approvedObj = task.getVariable("approved");
        boolean approved = approvedObj instanceof Boolean b && b;
        approval.setStatus(approved ? "APPROVED" : "REJECTED");
        approval.setSignedAt(LocalDateTime.now());
        Object commentObj = task.getVariable("comment");
        if (commentObj instanceof String s && !s.isBlank()) {
            approval.setComment(s);
        }
        approval.setUpdatedAt(LocalDateTime.now());
        approvalMapper.updateById(approval);
        log.info("[ECN TaskListener] 完成 approval: id={} status={}",
                approval.getId(), approval.getStatus());

        // 同步 ECN 主表状态（任一驳回 = REJECTED，全部通过 = APPROVED 走下一阶段）
        Long ecnId = approval.getEcnId();
        if (ecnId != null) {
            // 简化: 当前规则 — 驳回即 REJECTED, 最后一步通过即 APPROVED
            if (!approved) {
                ecnService.onApprovalCompleted(ecnId, false);
            } else {
                // 检查是否还有 PENDING 的 approval, 如果没有就 APPROVED
                long pendingCount = approvalMapper.selectCount(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EcnApproval>()
                                .eq(EcnApproval::getEcnId, ecnId)
                                .eq(EcnApproval::getStatus, "PENDING"));
                if (pendingCount == 0) {
                    ecnService.onApprovalCompleted(ecnId, true);
                }
            }
        }
    }

    /**
     * 任务删除（驳回导致跳过的步骤）：标记 SKIPPED
     */
    private void handleDelete(DelegateTask task) {
        EcnApproval approval = approvalMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EcnApproval>()
                        .eq(EcnApproval::getTaskId, task.getId()));
        if (approval == null) return;
        if (!"PENDING".equals(approval.getStatus())) return; // 已结束的不要覆盖
        approval.setStatus("SKIPPED");
        approval.setUpdatedAt(LocalDateTime.now());
        approvalMapper.updateById(approval);
    }

    // ============================================
    // 内部
    // ============================================

    /**
     * 部门负责人动态分配：根据发起人查部门 → 查部门 leader
     */
    private void assignDepartmentLeader(DelegateTask task) {
        Object requesterObj = task.getVariable("requesterUserid");
        if (!(requesterObj instanceof String requesterUserid) || requesterUserid.isBlank()) {
            log.warn("[ECN TaskListener] 流程变量无 requesterUserid，无法动态分配");
            return;
        }
        SysUser requester = userMapper.selectByUserId(requesterUserid);
        if (requester == null || requester.getDepartmentId() == null) {
            log.warn("[ECN TaskListener] 发起人 {} 无部门，assignee 留空", requesterUserid);
            return;
        }
        SysDepartment dept = departmentMapper.selectById(requester.getDepartmentId());
        if (dept == null || dept.getLeaderUserId() == null || dept.getLeaderUserId().isBlank()) {
            log.warn("[ECN TaskListener] 部门 {} 无配置 leader，assignee 留空（走 admin 兜底）",
                    requester.getDepartmentId());
            return;
        }
        task.setAssignee(dept.getLeaderUserId());
        log.info("[ECN TaskListener] 动态分配: task={} assignee={} (部门 {} 负责人)",
                task.getId(), dept.getLeaderUserId(), dept.getName());
    }

    private String lookupDeptName(Long departmentId) {
        if (departmentId == null) return null;
        SysDepartment dept = departmentMapper.selectById(departmentId);
        return dept != null ? dept.getName() : null;
    }

    /**
     * 通过 ecn_number 查 ecn_id (BPMN business key = ecn_number)
     */
    private Long lookupEcnIdByNumber(String ecnNumber) {
        EcnChange ecn = ecnChangeMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EcnChange>()
                        .eq(EcnChange::getEcnNumber, ecnNumber)
                        .last("LIMIT 1"));
        return ecn != null ? ecn.getId() : null;
    }

    /**
     * 当前 ECN 的 step_order = max + 1
     */
    private int currentStepOrder(Long ecnId) {
        if (ecnId == null) return 1;
        Long max = approvalMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EcnApproval>()
                        .eq(EcnApproval::getEcnId, ecnId)
                        .orderByDesc(EcnApproval::getStepOrder)
                        .last("LIMIT 1"))
                .stream().findFirst().map(EcnApproval::getStepOrder).orElse(0);
        return (int) (max == null ? 0 : max);
    }
}
