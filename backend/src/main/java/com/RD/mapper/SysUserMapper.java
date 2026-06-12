package com.RD.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.RD.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据企微 UserID 查询
     */
    @Select("SELECT * FROM sys_user WHERE user_id = #{userId}")
    SysUser selectByUserId(@Param("userId") String userId);

    /**
     * 批量根据 UserID 查询
     */
    @Select("<script>" +
            "SELECT * FROM sys_user WHERE user_id IN " +
            "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<SysUser> selectByUserIds(@Param("userIds") List<String> userIds);

    /**
     * 根据部门 ID 查询本部门所有成员的 UserID
     */
    @Select("SELECT user_id FROM sys_user WHERE department_id = #{deptId} AND status = 1")
    List<String> selectUserIdsByDeptId(@Param("deptId") Long deptId);
}
