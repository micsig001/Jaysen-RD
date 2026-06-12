package com.RD.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户列表 VO
 *
 * <p>用于 {@code GET /api/users} 分页查询。</p>
 *
 * <p>注：脱敏注解 {@code @SensitiveData} 当前未启用（Phase 2 暂不需要），
 * 后续如需对 mobile/email 脱敏，在字段上重新加 {@code @SensitiveData(type = ...)} 即可。</p>
 */
@Data
public class UserVO {

    private Long id;

    /** 企微 UserID */
    private String userId;

    private String name;
    private String avatarUrl;

    /** 手机号 */
    private String mobile;

    /** 邮箱 */
    private String email;

    private Long departmentId;
    private String departmentName;
    private String position;
    private String role;
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSyncTime;
}
