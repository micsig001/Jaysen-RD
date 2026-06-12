package com.RD.rd.equipment;

import com.RD.audit.AuditLog;
import com.RD.common.Result;
import com.RD.entity.EquipmentReservation;
import com.RD.entity.LabEquipment;
import com.RD.entity.SysUser;
import com.RD.mapper.SysUserMapper;
import com.RD.rd.equipment.dto.EquipmentReservationVO;
import com.RD.rd.equipment.dto.LabEquipmentVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 实验室设备 Controller
 *
 * <p>接口列表：</p>
 * <ul>
 *   <li>GET    /api/equipment                            分页查询设备</li>
 *   <li>GET    /api/equipment/{id}                       设备详情</li>
 *   <li>POST   /api/equipment                            创建设备（仅 ADMIN）</li>
 *   <li>PUT    /api/equipment/{id}                       更新设备（仅 ADMIN）</li>
 *   <li>GET    /api/equipment/{id}/reservations          设备预约列表（按时间段）</li>
 *   <li>POST   /api/equipment/{id}/reservations          创建预约</li>
 *   <li>DELETE /api/equipment/reservations/{id}          取消预约（本人/ADMIN）</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
@Tag(name = "实验室设备", description = "设备台账 + 预约日历")
public class LabEquipmentController {

    private final LabEquipmentService equipmentService;
    private final SysUserMapper userMapper;

    // ============================================
    // LabEquipment CRUD
    // ============================================

    @GetMapping
    @Operation(summary = "分页查询设备")
    public Result<Page<LabEquipmentVO>> listEquipments(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        return Result.success(equipmentService.listEquipments(pageNum, pageSize, keyword, category, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "设备详情")
    public Result<LabEquipmentVO> getEquipment(@PathVariable Long id) {
        return Result.success(equipmentService.getEquipmentById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(operationType = "CREATE", resourceType = "EQUIPMENT",
            resourceIdParam = "#equipment.id", description = "创建设备")
    @Operation(summary = "创建设备（仅 ADMIN）")
    public Result<LabEquipmentVO> createEquipment(@RequestBody LabEquipment equipment) {
        return Result.success(equipmentService.createEquipment(equipment, currentUser()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(operationType = "UPDATE", resourceType = "EQUIPMENT",
            resourceIdParam = "#id", description = "更新设备")
    @Operation(summary = "更新设备（仅 ADMIN）")
    public Result<LabEquipmentVO> updateEquipment(@PathVariable Long id, @RequestBody LabEquipment equipment) {
        return Result.success(equipmentService.updateEquipment(id, equipment, currentUser()));
    }

    // ============================================
    // Reservation CRUD
    // ============================================

    @GetMapping("/{id}/reservations")
    @Operation(summary = "设备预约列表（按时间段）")
    public Result<List<EquipmentReservationVO>> listReservations(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return Result.success(equipmentService.listReservations(id, from, to));
    }

    @PostMapping("/{id}/reservations")
    @AuditLog(operationType = "CREATE", resourceType = "EQUIPMENT_RESERVATION",
            resourceIdParam = "#reservation.id", description = "创建设备预约")
    @Operation(summary = "创建设备预约")
    public Result<EquipmentReservationVO> createReservation(
            @PathVariable Long id, @RequestBody EquipmentReservation reservation) {
        reservation.setEquipmentId(id); // 防止 body 里篡改设备 ID
        return Result.success(equipmentService.createReservation(reservation, currentUser()));
    }

    @DeleteMapping("/reservations/{id}")
    @AuditLog(operationType = "CANCEL", resourceType = "EQUIPMENT_RESERVATION",
            resourceIdParam = "#id", description = "取消设备预约")
    @Operation(summary = "取消设备预约（仅本人/ADMIN）")
    public Result<Void> cancelReservation(@PathVariable Long id) {
        equipmentService.cancelReservation(id, currentUser());
        return Result.success();
    }

    // ============================================
    // 工具方法
    // ============================================

    private SysUser currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal == null) {
            throw com.RD.common.BusinessException.unauthorized("未登录");
        }
        SysUser user = userMapper.selectByUserId(principal.toString());
        if (user == null) {
            throw com.RD.common.BusinessException.unauthorized("用户不存在: " + principal);
        }
        return user;
    }
}
