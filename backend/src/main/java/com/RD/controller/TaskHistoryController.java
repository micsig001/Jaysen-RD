package com.RD.controller;

import com.RD.common.Result;
import com.RD.entity.SysUser;
import com.RD.entity.TaskStatusHistory;
import com.RD.mapper.SysUserMapper;
import com.RD.mapper.TaskStatusHistoryMapper;
import com.RD.task.dto.TaskStatusHistoryVO;
import com.RD.task.service.TaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务状态历史 Controller
 *
 * <p>接口列表：</p>
 * <ul>
 *   <li>GET /api/tasks/{id}/history — 任务状态流转历史（按时间正序，最早在上）</li>
 * </ul>
 *
 * <p>权限：复用 {@code TaskService.getTaskById} 的数据权限校验 —— 无权查看的任务
 * 同样无权查看其历史，不暴露存在性。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks/{id}/history")
@RequiredArgsConstructor
@Tag(name = "任务状态历史", description = "任务状态流转时间线")
public class TaskHistoryController {

    private final TaskService taskService;
    private final TaskStatusHistoryMapper historyMapper;
    private final SysUserMapper userMapper;

    /**
     * 任务状态流转历史
     */
    @GetMapping
    @Operation(summary = "任务状态流转历史（按时间正序）")
    public Result<List<TaskStatusHistoryVO>> list(@PathVariable Long id) {
        // 1) 权限校验：复用 getTaskById 触发数据权限检查，VO 丢弃
        taskService.getTaskById(id, currentUser());

        // 2) 查历史
        List<TaskStatusHistory> histories = historyMapper.selectList(
                new LambdaQueryWrapper<TaskStatusHistory>()
                        .eq(TaskStatusHistory::getTaskId, id)
                        .orderByAsc(TaskStatusHistory::getCreatedAt));
        if (histories.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        // 3) VO 转换（operatorName 已由 recordStatusHistory 写入；这里再补一次防 null）
        return Result.success(histories.stream().map(this::toVO).collect(Collectors.toList()));
    }

    private TaskStatusHistoryVO toVO(TaskStatusHistory h) {
        TaskStatusHistoryVO vo = new TaskStatusHistoryVO();
        vo.setId(h.getId());
        vo.setTaskId(h.getTaskId());
        vo.setFromStatus(h.getFromStatus());
        vo.setToStatus(h.getToStatus());
        vo.setOperatorId(h.getOperatorId());
        // recordStatusHistory 已写入 operatorName，这里 null 时再回查一次
        if (h.getOperatorName() == null && h.getOperatorId() != null) {
            SysUser u = userMapper.selectByUserId(h.getOperatorId());
            if (u != null) {
                vo.setOperatorName(u.getName());
            }
        } else {
            vo.setOperatorName(h.getOperatorName());
        }
        vo.setRemark(h.getRemark());
        vo.setCreatedAt(h.getCreatedAt());
        return vo;
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
