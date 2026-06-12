package com.RD.user.dto;

import com.RD.privacy.SensitiveData;
import com.RD.privacy.SensitiveType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当前登录用户信息 VO
 *
 * <p>用于 {@code GET /api/users/me}，由 JWT 解析的 userId 查出完整用户信息后返回。</p>
 *
 * <p>敏感字段（mobile / email）由 {@code SensitiveDataAspect} 在 Controller 返回时自动脱敏：
 * 仅本人/ADMIN 可见明文，其他用户看到脱敏值。</p>
 */
@Data
public class CurrentUserVO {

    private Long id;

    /** 企微 UserID —— 切面"本人"豁免靠字段名 == userId 严格匹配 */
    private String userId;

    /** 姓名 */
    private String name;

    /** 头像 URL */
    private String avatarUrl;

    /** 手机号（脱敏：仅本人/ADMIN 见明文） */
    @SensitiveData(type = SensitiveType.MOBILE)
    private String mobile;

    /** 邮箱（脱敏：仅本人/ADMIN 见明文） */
    @SensitiveData(type = SensitiveType.EMAIL)
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
