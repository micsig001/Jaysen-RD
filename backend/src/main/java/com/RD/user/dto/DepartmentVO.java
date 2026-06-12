package com.RD.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门 VO（含成员数）
 *
 * <p>用于 {@code GET /api/departments} 树形列表。</p>
 */
@Data
public class DepartmentVO {

    private Long id;
    private String deptId;
    private String name;
    private Long parentId;
    private Integer orderNum;
    private String leaderUserId;
    private String leaderName;
    private Integer memberCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
