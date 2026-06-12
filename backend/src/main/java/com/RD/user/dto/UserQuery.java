package com.RD.user.dto;

import lombok.Data;

/**
 * 用户查询条件
 *
 * <p>用于 {@code GET /api/users} 分页参数。</p>
 */
@Data
public class UserQuery {

    /** 关键字（匹配 userId / name / mobile / email） */
    private String keyword;

    /** 部门 ID（精确） */
    private Long departmentId;

    /** 角色过滤（EMPLOYEE / MANAGER / ADMIN） */
    private String role;

    /** 状态过滤（1-启用，0-禁用） */
    private Integer status;

    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
