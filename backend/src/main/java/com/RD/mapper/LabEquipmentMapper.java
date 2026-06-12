package com.RD.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.RD.entity.LabEquipment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 实验室设备 Mapper
 */
@Mapper
public interface LabEquipmentMapper extends BaseMapper<LabEquipment> {

    /**
     * 按资产编号查询
     */
    @Select("SELECT * FROM lab_equipment WHERE asset_code = #{assetCode} AND deleted = 0 LIMIT 1")
    LabEquipment selectByAssetCode(@Param("assetCode") String assetCode);

    /**
     * 按类别 + 状态查询
     */
    @Select("SELECT * FROM lab_equipment " +
            "WHERE category = #{category} AND status = #{status} AND deleted = 0 " +
            "ORDER BY asset_code ASC")
    List<LabEquipment> selectByCategoryAndStatus(@Param("category") String category,
                                                @Param("status") String status);
}
