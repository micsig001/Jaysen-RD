package com.RD.rd.equipment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.RD.common.BusinessException;
import com.RD.entity.EquipmentReservation;
import com.RD.entity.LabEquipment;
import com.RD.entity.SysUser;
import com.RD.mapper.EquipmentReservationMapper;
import com.RD.mapper.LabEquipmentMapper;
import com.RD.mapper.SysUserMapper;
import com.RD.rd.common.RdConstants;
import com.RD.rd.equipment.dto.EquipmentReservationVO;
import com.RD.rd.equipment.dto.LabEquipmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 实验室设备服务（设备 + 预约）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LabEquipmentService {

    private final LabEquipmentMapper equipmentMapper;
    private final EquipmentReservationMapper reservationMapper;
    private final SysUserMapper userMapper;

    // ============================================
    // LabEquipment CRUD
    // ============================================

    /**
     * 分页查询设备
     */
    public Page<LabEquipmentVO> listEquipments(Integer pageNum, Integer pageSize,
                                                String keyword, String category, String status) {
        LambdaQueryWrapper<LabEquipment> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(LabEquipment::getAssetCode, keyword)
                    .or().like(LabEquipment::getName, keyword)
                    .or().like(LabEquipment::getModel, keyword));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(LabEquipment::getCategory, category);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(LabEquipment::getStatus, status);
        }
        wrapper.orderByAsc(LabEquipment::getAssetCode);

        Page<LabEquipment> page = new Page<>(
                pageNum != null && pageNum > 0 ? pageNum : 1,
                pageSize != null && pageSize > 0 ? pageSize : 10);
        Page<LabEquipment> result = equipmentMapper.selectPage(page, wrapper);

        Page<LabEquipmentVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        if (result.getRecords().isEmpty()) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }
        voPage.setRecords(result.getRecords().stream()
                .map(this::toEquipmentVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 设备详情
     */
    public LabEquipmentVO getEquipmentById(Long id) {
        if (id == null) {
            throw BusinessException.badRequest("设备 ID 不能为空");
        }
        LabEquipment equipment = equipmentMapper.selectById(id);
        if (equipment == null) {
            throw BusinessException.notFound("设备不存在: " + id);
        }
        return toEquipmentVO(equipment);
    }

    /**
     * 创建设备（仅 ADMIN）
     */
    @Transactional(rollbackFor = Exception.class)
    public LabEquipmentVO createEquipment(LabEquipment equipment, SysUser currentUser) {
        if (!"ADMIN".equals(currentUser.getRole())) {
            throw BusinessException.forbidden("仅管理员可创建设备");
        }
        validateEquipment(equipment);
        if (equipmentMapper.selectByAssetCode(equipment.getAssetCode()) != null) {
            throw BusinessException.badRequest("资产编号已存在: " + equipment.getAssetCode());
        }
        equipment.setStatus(RdConstants.EquipmentStatus.AVAILABLE);
        equipment.setCreatedBy(currentUser.getUserId());
        equipment.setCreatedAt(LocalDateTime.now());
        equipment.setUpdatedAt(LocalDateTime.now());
        equipmentMapper.insert(equipment);
        log.info("[设备] 创建: id={}, assetCode={}, name={}",
                equipment.getId(), equipment.getAssetCode(), equipment.getName());
        return toEquipmentVO(equipment);
    }

    /**
     * 更新设备（仅 ADMIN）
     */
    @Transactional(rollbackFor = Exception.class)
    public LabEquipmentVO updateEquipment(Long id, LabEquipment equipment, SysUser currentUser) {
        if (!"ADMIN".equals(currentUser.getRole())) {
            throw BusinessException.forbidden("仅管理员可修改设备");
        }
        LabEquipment existing = equipmentMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("设备不存在: " + id);
        }
        // 资产编号不可改
        if (StringUtils.hasText(equipment.getAssetCode())
                && !equipment.getAssetCode().equals(existing.getAssetCode())) {
            throw BusinessException.badRequest("资产编号不可修改");
        }
        equipment.setAssetCode(null);
        equipment.setId(id);
        equipment.setUpdatedAt(LocalDateTime.now());
        equipmentMapper.updateById(equipment);
        log.info("[设备] 更新: id={}, operator={}", id, currentUser.getUserId());
        return toEquipmentVO(equipmentMapper.selectById(id));
    }

    /**
     * 校准状态校准（每日定时调度也可调用）
     * 根据 calibration_due_date 自动更新 status
     */
    public int calibrateStatuses() {
        List<LabEquipment> all = equipmentMapper.selectList(
                new LambdaQueryWrapper<LabEquipment>().isNotNull(LabEquipment::getCalibrationDueDate));
        LocalDateTime now = LocalDateTime.now();
        int updated = 0;
        for (LabEquipment e : all) {
            if (e.getCalibrationDueDate() != null
                    && e.getCalibrationDueDate().isBefore(now.toLocalDate())
                    && !RdConstants.EquipmentStatus.CALIBRATION_OVERDUE.equals(e.getStatus())
                    && !RdConstants.EquipmentStatus.MAINTENANCE.equals(e.getStatus())
                    && !RdConstants.EquipmentStatus.SCRAPPED.equals(e.getStatus())) {
                e.setStatus(RdConstants.EquipmentStatus.CALIBRATION_OVERDUE);
                e.setUpdatedAt(now);
                equipmentMapper.updateById(e);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("[设备校准] 自动更新为 CALIBRATION_OVERDUE: count={}", updated);
        }
        return updated;
    }

    // ============================================
    // EquipmentReservation CRUD
    // ============================================

    /**
     * 创建预约
     *
     * <p>关键：必须检测时间冲突，同一台设备不允许 PENDING/CONFIRMED/IN_USE 状态的时间段重叠</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public EquipmentReservationVO createReservation(EquipmentReservation reservation, SysUser currentUser) {
        validateReservation(reservation);

        // 设备存在性 + 状态校验
        LabEquipment equipment = equipmentMapper.selectById(reservation.getEquipmentId());
        if (equipment == null) {
            throw BusinessException.notFound("设备不存在: " + reservation.getEquipmentId());
        }
        if (RdConstants.EquipmentStatus.MAINTENANCE.equals(equipment.getStatus())
                || RdConstants.EquipmentStatus.SCRAPPED.equals(equipment.getStatus())) {
            throw BusinessException.badRequest("设备当前不可预约（维修/报废）");
        }
        if (RdConstants.EquipmentStatus.CALIBRATION_OVERDUE.equals(equipment.getStatus())) {
            throw BusinessException.badRequest("设备校准过期，禁止预约（请先联系管理员校准）");
        }

        // 冲突检测
        List<EquipmentReservation> overlaps = reservationMapper.selectOverlapping(
                reservation.getEquipmentId(),
                reservation.getStartTime(),
                reservation.getEndTime());
        if (!overlaps.isEmpty()) {
            throw BusinessException.badRequest("设备在该时段已被预约");
        }

        reservation.setUserId(currentUser.getUserId());
        reservation.setStatus(RdConstants.ReservationStatus.PENDING);
        reservation.setCreatedAt(LocalDateTime.now());
        reservation.setUpdatedAt(LocalDateTime.now());
        reservationMapper.insert(reservation);
        log.info("[预约] 创建: id={}, equipmentId={}, user={}, {} - {}",
                reservation.getId(), reservation.getEquipmentId(), currentUser.getUserId(),
                reservation.getStartTime(), reservation.getEndTime());
        return toReservationVO(reservation, equipment, null, currentUser, null);
    }

    /**
     * 查询某设备在时间段的预约（用于日历展示）
     */
    public List<EquipmentReservationVO> listReservations(Long equipmentId, LocalDateTime from, LocalDateTime to) {
        if (equipmentId == null) {
            throw BusinessException.badRequest("设备 ID 不能为空");
        }
        List<EquipmentReservation> reservations = reservationMapper.selectByEquipmentInRange(
                equipmentId, from, to);
        if (reservations.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查关联数据
        LabEquipment equipment = equipmentMapper.selectById(equipmentId);
        java.util.Set<String> userIds = reservations.stream()
                .map(EquipmentReservation::getUserId)
                .collect(Collectors.toSet());
        java.util.Set<String> approverIds = reservations.stream()
                .map(EquipmentReservation::getApprovedBy)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        userIds.addAll(approverIds);
        Map<String, SysUser> userMap = batchLookupUsers(userIds);

        return reservations.stream()
                .map(r -> toReservationVO(r, equipment, userMap.get(r.getUserId()),
                        null, userMap.get(r.getApprovedBy())))
                .collect(Collectors.toList());
    }

    /**
     * 取消预约（仅预约人本人或 ADMIN）
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelReservation(Long id, SysUser currentUser) {
        EquipmentReservation existing = reservationMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("预约不存在: " + id);
        }
        boolean isOwner = existing.getUserId() != null
                && existing.getUserId().equals(currentUser.getUserId());
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());
        if (!isOwner && !isAdmin) {
            throw BusinessException.forbidden("仅预约本人或管理员可取消");
        }
        if (RdConstants.ReservationStatus.COMPLETED.equals(existing.getStatus())
                || RdConstants.ReservationStatus.IN_USE.equals(existing.getStatus())) {
            throw BusinessException.badRequest("预约已开始或已完成，不能取消");
        }
        existing.setStatus(RdConstants.ReservationStatus.CANCELLED);
        existing.setUpdatedAt(LocalDateTime.now());
        reservationMapper.updateById(existing);
        log.info("[预约] 取消: id={}, operator={}", id, currentUser.getUserId());
    }

    // ============================================
    // 内部方法
    // ============================================

    private void validateEquipment(LabEquipment e) {
        if (e == null) {
            throw BusinessException.badRequest("设备数据不能为空");
        }
        if (!StringUtils.hasText(e.getAssetCode())) {
            throw BusinessException.badRequest("资产编号不能为空");
        }
        if (!StringUtils.hasText(e.getName())) {
            throw BusinessException.badRequest("设备名称不能为空");
        }
        if (!StringUtils.hasText(e.getCategory())
                || !java.util.Set.of("SPECTRUM_ANALYZER", "OSCILLOSCOPE", "SIGNAL_GENERATOR",
                "NETWORK_ANALYZER", "POWER_METER", "OTHER").contains(e.getCategory())) {
            throw BusinessException.badRequest("设备类别非法");
        }
    }

    private void validateReservation(EquipmentReservation r) {
        if (r == null) {
            throw BusinessException.badRequest("预约数据不能为空");
        }
        if (r.getEquipmentId() == null) {
            throw BusinessException.badRequest("设备 ID 不能为空");
        }
        if (r.getStartTime() == null || r.getEndTime() == null) {
            throw BusinessException.badRequest("预约开始/结束时间不能为空");
        }
        if (!r.getEndTime().isAfter(r.getStartTime())) {
            throw BusinessException.badRequest("结束时间必须晚于开始时间");
        }
        if (r.getStartTime().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw BusinessException.badRequest("不能预约过去时间");
        }
    }

    private Map<String, SysUser> batchLookupUsers(java.util.Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUser> users = userMapper.selectByUserIds(new java.util.ArrayList<>(userIds));
        return users.stream().collect(Collectors.toMap(SysUser::getUserId, u -> u));
    }

    private LabEquipmentVO toEquipmentVO(LabEquipment e) {
        LabEquipmentVO vo = new LabEquipmentVO();
        vo.setId(e.getId());
        vo.setAssetCode(e.getAssetCode());
        vo.setName(e.getName());
        vo.setModel(e.getModel());
        vo.setManufacturer(e.getManufacturer());
        vo.setCategory(e.getCategory());
        vo.setLocation(e.getLocation());
        vo.setPurchaseDate(e.getPurchaseDate());
        vo.setWarrantyExpiry(e.getWarrantyExpiry());
        vo.setCalibrationDueDate(e.getCalibrationDueDate());
        vo.setCalibrationIntervalMonths(e.getCalibrationIntervalMonths());
        vo.setStatus(e.getStatus());
        vo.setSpecifications(e.getSpecifications());
        vo.setManualUrl(e.getManualUrl());
        vo.setQrCode(e.getQrCode());
        vo.setNotes(e.getNotes());
        vo.setCreatedBy(e.getCreatedBy());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setUpdatedAt(e.getUpdatedAt());
        return vo;
    }

    private EquipmentReservationVO toReservationVO(EquipmentReservation r,
                                                  LabEquipment equipment,
                                                  SysUser user,
                                                  SysUser currentUser,
                                                  SysUser approver) {
        EquipmentReservationVO vo = new EquipmentReservationVO();
        vo.setId(r.getId());
        vo.setEquipmentId(r.getEquipmentId());
        if (equipment != null) {
            vo.setEquipmentName(equipment.getName());
            vo.setEquipmentAssetCode(equipment.getAssetCode());
        }
        vo.setUserId(r.getUserId());
        vo.setUserName(user != null ? user.getName() : null);
        vo.setProjectId(r.getProjectId());
        vo.setPurpose(r.getPurpose());
        vo.setStartTime(r.getStartTime());
        vo.setEndTime(r.getEndTime());
        vo.setStatus(r.getStatus());
        vo.setActualStartTime(r.getActualStartTime());
        vo.setActualEndTime(r.getActualEndTime());
        vo.setNotes(r.getNotes());
        vo.setApprovedBy(r.getApprovedBy());
        vo.setApprovedByName(approver != null ? approver.getName() : null);
        vo.setApprovedAt(r.getApprovedAt());
        vo.setCreatedAt(r.getCreatedAt());
        vo.setUpdatedAt(r.getUpdatedAt());
        return vo;
    }
}
