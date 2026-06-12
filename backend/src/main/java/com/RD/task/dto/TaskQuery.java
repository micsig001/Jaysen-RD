package com.RD.task.dto;

import lombok.Data;

/**
 * 任务查询参数
 *
 * <p>对应 {@code GET /api/tasks} 分页参数。数据范围由角色自动过滤：</p>
 * <ul>
 *   <li>ADMIN：全部</li>
 *   <li>MANAGER：本部门成员作为创建者/接收者的任务</li>
 *   <li>EMPLOYEE：仅自己作为创建者/接收者的任务</li>
 * </ul>
 */
@Data
public class TaskQuery {

    /** 状态过滤 */
    private String status;

    /** 优先级过滤（1-4） */
    private Integer priority;

    /** 创建人 UserID 精确过滤 */
    private String creatorId;

    /** 接收人 UserID 精确过滤 */
    private String assigneeId;

    /** 关键字（匹配 task_no / title） */
    private String keyword;

    /**
     * 仅超期未完成任务
     *
     * <p>true 时强制 status IN (PENDING_ACCEPT, IN_PROGRESS, PENDING_VERIFY)
     * 且 actual_deadline &lt; now；与 {@code status} 互斥（会覆盖）。</p>
     */
    private Boolean overdueOnly;

    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
