package com.RD.rd.project.dto;

import lombok.Data;

/**
 * 项目列表查询条件
 */
@Data
public class ProjectQuery {

    /** 关键词（匹配 code / name） */
    private String keyword;

    /** 项目类型 */
    private String type;

    /** 项目阶段 */
    private String phase;

    /** 状态 */
    private String status;

    /** 项目负责人 UserID（精确匹配） */
    private String managerUserid;

    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
