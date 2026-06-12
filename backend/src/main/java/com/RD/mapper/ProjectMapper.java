package com.RD.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.RD.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 项目 Mapper
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

    /**
     * 按项目编号查询
     */
    @Select("SELECT * FROM project WHERE code = #{code} AND deleted = 0 LIMIT 1")
    Project selectByCode(@Param("code") String code);

    /**
     * 按负责人 UserID 查询（用于数据权限过滤后查自己负责的项目）
     */
    @Select("SELECT * FROM project WHERE manager_userid = #{managerUserid} AND deleted = 0")
    List<Project> selectByManagerUserid(@Param("managerUserid") String managerUserid);
}
