package com.RD.task.service;

import com.RD.common.BusinessException;
import com.RD.entity.SysUser;
import com.RD.entity.Task;
import com.RD.entity.TaskStatusHistory;
import com.RD.mapper.SysUserMapper;
import com.RD.mapper.TaskMapper;
import com.RD.mapper.TaskStatusHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 任务状态机服务
 *
 * <p>实现双重确认状态机：</p>
 * <pre>
 *   PENDING_ACCEPT ─(accept, 接收方)──→ IN_PROGRESS
 *   IN_PROGRESS   ─(submit, 执行方)──→ PENDING_VERIFY
 *   PENDING_VERIFY ─(complete, 发起方)──→ COMPLETED
 *   PENDING_VERIFY ─(reject,  发起方)──→ IN_PROGRESS   (驳回重做)
 *   PENDING_ACCEPT ─(cancel,  发起方)──→ WITHDRAWN    (发起方撤回)
 *   IN_PROGRESS   ─(cancel,  发起方)──→ WITHDRAWN    (任意时刻撤回)
 * </pre>
 *
 * <p>每一步状态流转都：</p>
 * <ol>
 *   <li>校验操作人权限（接收方/发起方）</li>
 *   <li>校验源状态是否合法</li>
 *   <li>写状态历史（{@code task_status_history}）</li>
 *   <li>乐观锁并发控制（{@code updateById} 影响 0 → 409 Conflict）</li>
 * </ol>
 *
 * <p>未集成：{@code WeWorkMessageService}（企微通知），等 WeWork 模块一起做。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskStateMachineService {

    private final TaskMapper taskMapper;
    private final TaskStatusHistoryMapper statusHistoryMapper;
    private final SysUserMapper userMapper;

    // ============================================
    // PENDING_ACCEPT → IN_PROGRESS（接收方）
    // ============================================

    /**
     * 接收方确认接收任务
     *
     * <p>副作用：记录 {@code actual_start_time}，根据 {@code estimated_duration} 推算 {@code actual_deadline}。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void acceptTask(Long taskId, String operatorUserId) {
        Task task = selectOrThrow(taskId);
        ensureOperator(task, operatorUserId, RoleOf.ASSIGNEE, "接收");

        if (!"PENDING_ACCEPT".equals(task.getStatus())) {
            throw BusinessException.badRequest("任务状态不允许接收（当前: " + task.getStatus() + "）");
        }

        LocalDateTime now = LocalDateTime.now();
        String fromStatus = task.getStatus();
        task.setActualStartTime(now);
        if (task.getEstimatedDuration() != null && task.getEstimatedDuration() > 0) {
            // estimated_duration 单位：小时
            task.setActualDeadline(now.plusHours(task.getEstimatedDuration()));
        }
        task.setStatus("IN_PROGRESS");
        task.setUpdatedAt(now);
        updateWithOptimisticLock(task);
        recordStatusHistory(taskId, fromStatus, "IN_PROGRESS", operatorUserId, "确认接收任务");
        log.info("[任务] {} 已被 {} 接收", taskId, operatorUserId);
    }

    // ============================================
    // IN_PROGRESS → PENDING_VERIFY（执行方）
    // ============================================

    /**
     * 执行方提交完成（待发起方验收）
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitTask(Long taskId, String operatorUserId, String submitRemark) {
        Task task = selectOrThrow(taskId);
        ensureOperator(task, operatorUserId, RoleOf.ASSIGNEE, "提交");

        if (!"IN_PROGRESS".equals(task.getStatus())) {
            throw BusinessException.badRequest("任务状态不允许提交（当前: " + task.getStatus() + "）");
        }

        String fromStatus = task.getStatus();
        task.setStatus("PENDING_VERIFY");
        task.setUpdatedAt(LocalDateTime.now());
        updateWithOptimisticLock(task);
        recordStatusHistory(taskId, fromStatus, "PENDING_VERIFY", operatorUserId, submitRemark);
        log.info("[任务] {} 已提交待验收 (操作人: {})", taskId, operatorUserId);
    }

    // ============================================
    // PENDING_VERIFY → COMPLETED（发起方）
    // ============================================

    /**
     * 发起方验收通过
     */
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long taskId, String operatorUserId) {
        Task task = selectOrThrow(taskId);
        ensureOperator(task, operatorUserId, RoleOf.CREATOR, "验收");

        if (!"PENDING_VERIFY".equals(task.getStatus())) {
            throw BusinessException.badRequest("任务状态不允许验收（当前: " + task.getStatus() + "）");
        }

        String fromStatus = task.getStatus();
        LocalDateTime now = LocalDateTime.now();
        task.setStatus("COMPLETED");
        task.setActualEndTime(now);
        task.setUpdatedAt(now);
        updateWithOptimisticLock(task);
        recordStatusHistory(taskId, fromStatus, "COMPLETED", operatorUserId, "验收通过");
        log.info("[任务] {} 已验收完成 (操作人: {})", taskId, operatorUserId);
    }

    // ============================================
    // PENDING_VERIFY → IN_PROGRESS（发起方驳回）
    // ============================================

    /**
     * 发起方驳回（退回到 IN_PROGRESS 状态，让执行方重做）
     */
    @Transactional(rollbackFor = Exception.class)
    public void rejectTask(Long taskId, String operatorUserId, String rejectReason) {
        if (rejectReason == null || rejectReason.isBlank()) {
            throw BusinessException.badRequest("驳回原因不能为空");
        }
        Task task = selectOrThrow(taskId);
        ensureOperator(task, operatorUserId, RoleOf.CREATOR, "驳回");

        if (!"PENDING_VERIFY".equals(task.getStatus())) {
            throw BusinessException.badRequest("任务状态不允许驳回（当前: " + task.getStatus() + "）");
        }

        String fromStatus = task.getStatus();
        task.setStatus("IN_PROGRESS");
        task.setRejectReason(rejectReason);
        task.setUpdatedAt(LocalDateTime.now());
        updateWithOptimisticLock(task);
        recordStatusHistory(taskId, fromStatus, "IN_PROGRESS", operatorUserId, "驳回：" + rejectReason);
        log.info("[任务] {} 已被驳回，原因: {} (操作人: {})", taskId, rejectReason, operatorUserId);
    }

    // ============================================
    // PENDING_ACCEPT/IN_PROGRESS → WITHDRAWN（发起方撤回）
    // ============================================

    /**
     * 发起方撤回任务（仅 PENDING_ACCEPT 或 IN_PROGRESS 状态可撤回）
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(Long taskId, String operatorUserId, String cancelReason) {
        Task task = selectOrThrow(taskId);
        ensureOperator(task, operatorUserId, RoleOf.CREATOR, "撤回");

        String current = task.getStatus();
        if (!"PENDING_ACCEPT".equals(current) && !"IN_PROGRESS".equals(current)) {
            throw BusinessException.badRequest("任务已开始验收或已完成，无法撤回（当前: " + current + "）");
        }

        String fromStatus = current;
        task.setStatus("WITHDRAWN");
        task.setUpdatedAt(LocalDateTime.now());
        updateWithOptimisticLock(task);
        recordStatusHistory(taskId, fromStatus, "WITHDRAWN", operatorUserId,
                "撤回" + (cancelReason != null && !cancelReason.isBlank() ? "：" + cancelReason : ""));
        log.info("[任务] {} 已撤回 (操作人: {})", taskId, operatorUserId);
    }

    // ============================================
    // 内部方法
    // ============================================

    /**
     * 查询任务，不存在抛 404
     */
    private Task selectOrThrow(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw BusinessException.notFound("任务不存在: " + taskId);
        }
        return task;
    }

    /**
     * 校验操作人身份
     */
    private void ensureOperator(Task task, String operatorUserId, RoleOf role, String action) {
        boolean allowed = switch (role) {
            case CREATOR -> Objects.equals(task.getCreatorId(), operatorUserId);
            case ASSIGNEE -> Objects.equals(task.getAssigneeId(), operatorUserId);
        };
        if (!allowed) {
            log.warn("[任务] 用户 {} 无权{}任务 {}", operatorUserId, action, task.getId());
            throw BusinessException.forbidden("仅" + role.zhName() + "可" + action);
        }
    }

    /**
     * 乐观锁：MyBatis-Plus 自动 WHERE version=?
     *
     * <p>MP 3.5.5 OptimisticLockerInnerInterceptor 改 SQL 为 {@code UPDATE ... WHERE id=? AND version=? SET version=version+1}，
     * 影响行数=0 表示已被并发修改 → 抛 409 让前端提示刷新。</p>
     */
    private void updateWithOptimisticLock(Task task) {
        int affected = taskMapper.updateById(task);
        if (affected == 0) {
            log.warn("[Task {}] 乐观锁冲突：affected=0，version 已被其他请求修改", task.getId());
            throw new BusinessException(409, "任务已被其他请求修改，请刷新后重试");
        }
    }

    /**
     * 记录状态历史（含操作人姓名）
     */
    private void recordStatusHistory(Long taskId, String fromStatus, String toStatus,
                                     String operatorUserId, String remark) {
        String operatorName = null;
        if (operatorUserId != null) {
            SysUser u = userMapper.selectByUserId(operatorUserId);
            if (u != null) {
                operatorName = u.getName();
            }
        }
        TaskStatusHistory history = new TaskStatusHistory();
        history.setTaskId(taskId);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setOperatorId(operatorUserId);
        history.setOperatorName(operatorName);
        history.setRemark(remark);
        history.setCreatedAt(LocalDateTime.now());
        statusHistoryMapper.insert(history);
    }

    // ============================================
    // 操作人角色枚举（状态机内部用，不外暴）
    // ============================================

    private enum RoleOf {
        CREATOR("发起方"),
        ASSIGNEE("接收方");

        private final String zh;

        RoleOf(String zh) { this.zh = zh; }

        String zhName() { return zh; }
    }
}
