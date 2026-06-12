package com.RD.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 缺陷跟踪
 *
 * <p>对应表 {@code defect_tracking}（V1 SQL 6.3 节）</p>
 *
 * <p>状态机：</p>
 * <pre>
 *   NEW → ANALYZING → FIX_IN_PROGRESS → FIXED → VERIFIED → CLOSED
 *                                            ↑        ↓
 *                                            └── REOPENED ←── (任意阶段都可重开)
 * </pre>
 *
 * <p>严重度：CRITICAL / MAJOR / MINOR / TRIVIAL</p>
 * <p>优先级：HIGH / MEDIUM / LOW</p>
 *
 * <p>{@code sprint_task_id} 关联 {@link SprintTask} —— 缺陷在哪个看板任务中发现</p>
 * <p>{@code attachments} JSON 字符串: [{name,url,size}, ...]</p>
 */
@Data
@TableName("defect_tracking")
public class Defect {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("defect_number")
    private String defectNumber;

    private String title;

    /** CRITICAL / MAJOR / MINOR / TRIVIAL */
    private String severity;

    /** HIGH / MEDIUM / LOW */
    private String priority;

    /** NEW / ANALYZING / FIX_IN_PROGRESS / FIXED / VERIFIED / CLOSED / REOPENED */
    private String status;

    /** EVT / DVT / PVT / MP (发现阶段) */
    @TableField("phase_found")
    private String phaseFound;

    @TableField("root_cause")
    private String rootCause;

    @TableField("corrective_action")
    private String correctiveAction;

    @TableField("preventive_action")
    private String preventiveAction;

    @TableField("reporter_userid")
    private String reporterUserid;

    @TableField("assignee_userid")
    private String assigneeUserid;

    @TableField("verifier_userid")
    private String verifierUserid;

    @TableField("project_id")
    private Long projectId;

    @TableField("sprint_task_id")
    private Long sprintTaskId;

    @TableField("found_date")
    private LocalDate foundDate;

    @TableField("resolved_date")
    private LocalDate resolvedDate;

    @TableField("verified_date")
    private LocalDate verifiedDate;

    @TableField("closed_date")
    private LocalDate closedDate;

    /** JSON 字符串：[{name,url,size}, ...] */
    private String attachments;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
