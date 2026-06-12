package com.RD.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.RD.entity.SysDepartment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 部门 Mapper
 */
@Mapper
public interface SysDepartmentMapper extends BaseMapper<SysDepartment> {

    /**
     * 根据企微部门 ID 查询
     */
    @Select("SELECT * FROM sys_department WHERE dept_id = #{deptId}")
    SysDepartment selectByDeptId(@Param("deptId") String deptId);

    /**
     * 批量根据 DeptID 查询
     */
    @Select("<script>" +
            "SELECT * FROM sys_department WHERE dept_id IN " +
            "<foreach collection='deptIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<SysDepartment> selectByDeptIds(@Param("deptIds") List<String> deptIds);
}
