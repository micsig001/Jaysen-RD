package com.RD.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当前登录用户信息 VO
 *
 * <p>用于 {@code GET /api/users/me}，由 JWT 解析的 userId 查出完整用户信息后返回。</p>
 *
 * <p>注：脱敏注解 {@code @SensitiveData} 当前未启用（Phase 2 暂不需要），
 * 后续如需对 mobile/email 脱敏，在字段上重新加 {@code @SensitiveData(type = ...)} 即可。</p>
 */
@Data
public class CurrentUserVO {

    private Long id;

    /** 企微 UserID */
    private String userId;

    /** 姓名 */
    private String name;

    /** 头像 URL */
    private String avatarUrl;

    /** 手机号 */
    private String mobile;

    /** 邮箱 */
    private String email;

    /** 部门 ID */
    private Long departmentId;

    /** 部门名称（关联 sys_department 冗余） */
    private String departmentName;

    /** 职位 */
    private String position;

    /** 角色：EMPLOYEE / MANAGER / ADMIN */
    private String role;

    /** 状态：1-启用，0-禁用 */
    private Integer status;

    /** 最后登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;

    /** 最后同步时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSyncTime;
}
