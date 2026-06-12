package com.RD.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
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
 * 项目实体
 *
 * <p>对应表 {@code project}（V1 SQL 3.1 节）</p>
 *
 * <p>字段命名：snake_case → camelCase（{@code manager_userid} → {@code managerUserid}），
 * 跟 spec 文档中的字段名保持一致。</p>
 *
 * <p>枚举字段（type / phase / status）用 {@link String} 存储，
 * 由业务层校验取值范围（避免 MyBatis-Plus 的 {@code IEnum} 跟 ENUM 类型错位）。</p>
 *
 * @author Mavis
 */
@Data
@TableName("project")
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 项目编号（唯一） */
    private String code;

    /** 项目名称 */
    private String name;

    /** 项目类型：HARDWARE / FIRMWARE / SOFTWARE / MIXED */
    private String type;

    /** 硬件阶段（仅 HARDWARE/MIXED 使用）：EVT / DVT / PVT / MP */
    private String phase;

    /** 项目负责人 UserID（企微） */
    private String managerUserid;

    /** 计划开始日期 */
    private LocalDate startDate;

    /** 计划结束日期 */
    private LocalDate endDate;

    /** 实际开始日期 */
    private LocalDate actualStartDate;

    /** 实际结束日期 */
    private LocalDate actualEndDate;

    /** 状态：PLANNING / IN_PROGRESS / ON_HOLD / COMPLETED / CANCELLED */
    private String status;

    /** 完成百分比 0-100 */
    private BigDecimal progress;

    /** 描述 */
    private String description;

    /** 标签（JSON 字符串，由业务层序列化/反序列化） */
    private String tags;

    /** 创建人 UserID */
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
