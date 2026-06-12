package com.RD.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务实体
 *
 * <p>对应表 {@code task}（V1 SQL 2.1 节）</p>
 *
 * <p>字段命名与 V1 SQL 完全对齐；跟旧 Task 项目（{@code com.task.entity.Task}）的差异：
 * <ul>
 *   <li>表名：旧 {@code tasks} → 新 {@code task}</li>
 *   <li>自指派字段：旧 {@code is_self_assigned} → 新 {@code self_assigned}</li>
 *   <li>开始/截止/完成字段：旧 {@code completed_at} / {@code withdrawn_at} → 新 {@code actual_start_time} / {@code actual_deadline} / {@code actual_end_time}</li>
 *   <li>新增 {@code reject_reason}（驳回原因）</li>
 *   <li>预估时长单位：旧 {@code minute} → 新 {@code hour}</li>
 * </ul>
 *
 * <p>状态机：{@code PENDING_ACCEPT → IN_PROGRESS → PENDING_VERIFY → COMPLETED}，
 * 旁路：{@code → REJECTED} / {@code → WITHDRAWN}。
 * 状态流转由 {@code com.RD.task.service.TaskStateMachineService} 负责（Phase 2.5 接入）。</p>
 */
@Data
@TableName("task")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务编号（业务唯一，{@code TaskNoGenerator} 生成） */
    @TableField("task_no")
    private String taskNo;

    @NotBlank(message = "任务标题不能为空")
    @Size(max = 256, message = "任务标题不能超过 256 字符")
    private String title;

    @Size(max = 5000, message = "任务描述不能超过 5000 字符")
    private String description;

    /** 来源备注 */
    @TableField("source_remark")
    private String sourceRemark;

    /** 创建人 UserID（发起方） */
    @NotBlank(message = "创建人不能为空")
    @TableField("creator_id")
    private String creatorId;

    /** 执行人 UserID（接收方） */
    @NotBlank(message = "执行人不能为空")
    @TableField("assignee_id")
    private String assigneeId;

    /** 优先级 1-最高 / 2-高 / 3-中 / 4-低 */
    @Min(value = 1, message = "优先级最小为 1")
    @Max(value = 4, message = "优先级最大为 4")
    private Integer priority;

    /**
     * 状态：PENDING_ACCEPT / IN_PROGRESS / PENDING_VERIFY / COMPLETED / REJECTED / WITHDRAWN
     */
    private String status;

    /** 是否自己发给自己（true 才允许删除） */
    @TableField("self_assigned")
    private Boolean selfAssigned;

    /** 预估时长（小时） */
    @TableField("estimated_duration")
    private Integer estimatedDuration;

    /** 实际开始时间（接收方确认时记录） */
    @TableField("actual_start_time")
    private LocalDateTime actualStartTime;

    /** 截止时间（actual_start_time + estimated_duration） */
    @TableField("actual_deadline")
    private LocalDateTime actualDeadline;

    /** 实际完成时间 */
    @TableField("actual_end_time")
    private LocalDateTime actualEndTime;

    /** 驳回原因（最近一次） */
    @TableField("reject_reason")
    private String rejectReason;

    /** 乐观锁 */
    @Version
    private Integer version;

    /** 逻辑删除（0 未删 / 1 已删） */
    @TableLogic
    private Integer deleted;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
