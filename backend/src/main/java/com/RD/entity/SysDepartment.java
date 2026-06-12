package com.RD.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门实体
 *
 * <p>对应表 {@code sys_department}（V1 SQL 1.3 节）</p>
 */
@Data
@TableName("sys_department")
public class SysDepartment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 企业微信部门 ID */
    private String deptId;

    /** 部门名称 */
    private String name;

    /** 父部门 ID（0=根） */
    private Long parentId;

    /** 排序号 */
    private Integer orderNum;

    /** 部门负责人 UserID（企微） */
    private String leaderUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
