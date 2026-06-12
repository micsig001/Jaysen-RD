package com.RD.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 *
 * <p>对应表 {@code sys_user}（V1 SQL 1.2 节）</p>
 *
 * <p>字段命名与 Task 项目的 {@code users} 表保持一致，
 * 方便 WeWorkAuthService / WeWorkSyncService 包搬入时零修改。</p>
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 企业微信 UserID */
    private String userId;

    /** 姓名（显示名称） */
    private String name;

    /** 头像 URL */
    private String avatarUrl;

    /** 手机号（加密存储） */
    private String mobile;

    /** 邮箱 */
    private String email;

    /** 所属部门 ID */
    private Long departmentId;

    /** 职位 */
    private String position;

    /** 系统角色：EMPLOYEE / MANAGER / ADMIN */
    private String role;

    /** 状态：1-启用，0-禁用 */
    private Integer status;

    /** 是否手动分配角色（企微同步不覆盖） */
    @TableField("is_manual_role")
    private Boolean manualRole;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;

    /** 最后同步时间 */
    private LocalDateTime lastSyncTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
