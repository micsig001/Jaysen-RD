package com.RD.user.dto;

import com.RD.privacy.SensitiveData;
import com.RD.privacy.SensitiveType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户列表 VO
 *
 * <p>用于 {@code GET /api/users} 分页查询。</p>
 *
 * <p>敏感字段由 {@code SensitiveDataAspect} 在 Controller 返回时自动脱敏：
 * 列表场景下 ADMIN 默认可见明文（maskForAdmin=false），其他用户看到脱敏值。</p>
 */
@Data
public class UserVO {

    private Long id;

    /** 企微 UserID —— 切面"本人"豁免靠字段名 == userId 严格匹配 */
    private String userId;

    private String name;
    private String avatarUrl;

    /** 手机号（脱敏：仅 ADMIN/本人见明文） */
    @SensitiveData(type = SensitiveType.MOBILE)
    private String mobile;

    /** 邮箱（脱敏：仅 ADMIN/本人见明文） */
    @SensitiveData(type = SensitiveType.EMAIL)
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
