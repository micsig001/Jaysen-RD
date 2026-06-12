package com.RD.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.RD.entity.EquipmentReservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备预约 Mapper
 */
@Mapper
public interface EquipmentReservationMapper extends BaseMapper<EquipmentReservation> {

    /**
     * 按设备 ID + 时间区间查询（冲突检测用）
     *
     * <p>逻辑：start_time &lt; end AND end_time &gt; start，
     * 同时状态是 PENDING / CONFIRMED / IN_USE 时算冲突</p>
     */
    @Select("SELECT * FROM equipment_reservation " +
            "WHERE equipment_id = #{equipmentId} " +
            "  AND status IN ('PENDING', 'CONFIRMED', 'IN_USE') " +
            "  AND start_time < #{end} " +
            "  AND end_time > #{start}")
    List<EquipmentReservation> selectOverlapping(@Param("equipmentId") Long equipmentId,
                                               @Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    /**
     * 按设备 ID 查询某时间段的预约（用于日历展示）
     */
    @Select("SELECT * FROM equipment_reservation " +
            "WHERE equipment_id = #{equipmentId} " +
            "  AND start_time >= #{from} " +
            "  AND end_time <= #{to} " +
            "ORDER BY start_time ASC")
    List<EquipmentReservation> selectByEquipmentInRange(@Param("equipmentId") Long equipmentId,
                                                       @Param("from") LocalDateTime from,
                                                       @Param("to") LocalDateTime to);
}
