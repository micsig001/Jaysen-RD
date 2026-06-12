package com.RD.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 敏捷看板任务
 *
 * <p>对应表 {@code sprint_task}（V1 SQL 3.4 节）</p>
 *
 * <p>{@code status} 5 个值映射看板 5 列：</p>
 * <ul>
 *   <li>{@code BACKLOG} — 待规划（不在当前冲刺）</li>
 *   <li>{@code TODO} — 待办（已规划到当前冲刺但未开始）</li>
 *   <li>{@code IN_PROGRESS} — 进行中</li>
 *   <li>{@code REVIEW} — 待验收</li>
 *   <li>{@code DONE} — 已完成</li>
 * </ul>
 */
@Data
@TableName("sprint_task")
public class SprintTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    /** 所属冲刺 ID（可空：BACKLOG 任务） */
    @TableField("sprint_id")
    private Long sprintId;

    private String title;
    private String description;

    /** FEATURE / BUG / OPTIMIZATION / TEST */
    private String type;

    /** LOW / MEDIUM / HIGH / CRITICAL */
    private String priority;

    /** BACKLOG / TODO / IN_PROGRESS / REVIEW / DONE */
    private String status;

    @TableField("assignee_userid")
    private String assigneeUserid;

    @TableField("reporter_userid")
    private String reporterUserid;

    @TableField("estimated_hours")
    private BigDecimal estimatedHours;

    @TableField("actual_hours")
    private BigDecimal actualHours;

    @TableField("story_points")
    private Integer storyPoints;

    /** JSON 字符串 */
    private String tags;

    /** JSON 字符串 */
    private String attachments;

    @TableField("due_date")
    private LocalDate dueDate;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    /** 看板排序（同列内） */
    @TableField("order_num")
    private Integer orderNum;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
