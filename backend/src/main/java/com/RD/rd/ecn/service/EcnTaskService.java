package com.RD.rd.ecn.service;

import com.RD.common.BusinessException;
import com.RD.entity.SysUser;
import com.RD.mapper.SysUserMapper;
import com.RD.rd.ecn.dto.EcnChangeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ECN 流程任务服务
 *
 * <p>职责：查询当前用户待办、claim / unclaim、complete（带 approved 变量）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcnTaskService {

    private final ProcessEngine processEngine;
    private final EcnService ecnService;
    private final SysUserMapper userMapper;

    private TaskService taskService() {
        return processEngine.getTaskService();
    }

    /**
     * 查询当前用户待办（assignee = userId）
     */
    public List<EcnChangeVO> listMyPendingTasks(SysUser currentUser) {
        if (currentUser == null) {
            throw BusinessException.unauthorized("未登录");
        }
        String userId = currentUser.getUserId();
        List<Task> tasks = taskService().createTaskQuery()
                .taskAssignee(userId)
                .processDefinitionKey("ecn-default")
                .orderByTaskCreateTime().desc()
                .list();
        return tasksToEcnVOs(tasks);
    }

    /**
     * 查询当前用户候选任务（candidateGroups / candidateUsers）
     *
     * <p>Phase 2 暂未启用 candidateUser,先暴露接口。</p>
     */
    public List<EcnChangeVO> listMyCandidateTasks(SysUser currentUser) {
        if (currentUser == null) {
            throw BusinessException.unauthorized("未登录");
        }
        // 当前 BPMN 没用 candidateUsers, 简单返回空
        return Collections.emptyList();
    }

    /**
     * claim 任务（未分配 → 分配给当前用户）
     *
     * <p>Phase 2 简化: 任务已被 assignee 锁定, claim 主要用于多候选人场景。当前流程每个 UserTask 节点
     * 在 create 时已通过 TaskListener 动态分配 assignee, claim 通常无操作; 但保留 API 完整性。</p>
     */
    public void claimTask(String taskId, SysUser currentUser) {
        if (currentUser == null) {
            throw BusinessException.unauthorized("未登录");
        }
        try {
            taskService().claim(taskId, currentUser.getUserId());
            log.info("[ECN Task] 用户 {} claim 任务 {}", currentUser.getUserId(), taskId);
        } catch (Exception e) {
            log.warn("[ECN Task] claim 失败: taskId={}, userId={}, error={}",
                    taskId, currentUser.getUserId(), e.getMessage());
            throw BusinessException.badRequest("认领任务失败: " + e.getMessage());
        }
    }

    /**
     * 完成任务（审批通过/驳回）
     *
     * <p>设置流程变量 {@code approved} (Boolean) + {@code comment} (String),
     * 由 BPMN 排他网关判断下一分支。TaskListener.complete 同步 ecn_approval + ecn_change 状态。</p>
     */
    public void completeTask(String taskId, boolean approved, String comment, SysUser currentUser) {
        if (currentUser == null) {
            throw BusinessException.unauthorized("未登录");
        }
        Task task = taskService().createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw BusinessException.notFound("任务不存在: " + taskId);
        }
        // 校验操作人 = assignee (避免 A 审批 B 的任务)
        if (!currentUser.getUserId().equals(task.getAssignee())
                && !"ADMIN".equals(currentUser.getRole())) {
            throw BusinessException.forbidden("仅任务 assignee 可审批");
        }
        // 设置变量
        taskService().setVariable(taskId, "approved", approved);
        if (comment != null && !comment.isBlank()) {
            taskService().setVariable(taskId, "comment", comment);
        }
        // 完成
        taskService().complete(taskId);
        log.info("[ECN Task] 用户 {} 完成审批: taskId={} approved={} comment={}",
                currentUser.getUserId(), taskId, approved, comment);
    }

    /**
     * Task -> EcnChangeVO 转换
     */
    private List<EcnChangeVO> tasksToEcnVOs(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }
        // 通过 businessKey 查 ecn
        List<String> ecnNumbers = tasks.stream()
                .map(t -> t.getExecution() != null ? t.getExecution().getBusinessKey() : null)
                .filter(bk -> bk != null && !bk.isBlank())
                .distinct()
                .collect(Collectors.toList());
        if (ecnNumbers.isEmpty()) {
            return Collections.emptyList();
        }
        // 简化: 调 ecnService.listEcn 过滤, 或直接 EcnChangeMapper.selectList
        // 走 ecnService.listEcn 触发数据权限校验; 但 listEcn 不支持 ecnNumbers IN 过滤
        // 改: 多个单独查 (Phase 2 数量小, 可接受)
        List<EcnChangeVO> result = new ArrayList<>();
        for (String number : ecnNumbers) {
            try {
                EcnChangeVO vo = ecnService.getEcnByEcnNumber(number, currentUser());
                if (vo != null) result.add(vo);
            } catch (Exception e) {
                log.debug("[ECN Task] 查 ecn({}) 失败: {}", number, e.getMessage());
            }
        }
        return result;
    }

    // 临时方法: 让上面的 currentUser() 走 EcnService 已有的 currentUser() 工具方法
    private SysUser currentUser() {
        // 这里在 TaskService 调用栈里, security context 仍有 principal
        Object principal = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        if (principal == null) return null;
        SysUser user = userMapper.selectByUserId(principal.toString());
        return user;
    }
}
