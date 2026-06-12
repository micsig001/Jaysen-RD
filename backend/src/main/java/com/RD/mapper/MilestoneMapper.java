package com.RD.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.RD.entity.Milestone;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 里程碑 Mapper
 */
@Mapper
public interface MilestoneMapper extends BaseMapper<Milestone> {

    /**
     * 按项目 ID 查询所有里程碑（按阶段 + 计划开始日期排序）
     */
    @Select("SELECT * FROM milestone WHERE project_id = #{projectId} " +
            "ORDER BY phase ASC, planned_start ASC")
    List<Milestone> selectByProjectId(@Param("projectId") Long projectId);

    /**
     * 按项目 ID + 阶段查询
     */
    @Select("SELECT * FROM milestone WHERE project_id = #{projectId} AND phase = #{phase} " +
            "ORDER BY planned_start ASC")
    List<Milestone> selectByProjectIdAndPhase(@Param("projectId") Long projectId,
                                              @Param("phase") String phase);
}
