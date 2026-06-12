package com.RD.user;

import com.RD.common.BusinessException;
import com.RD.entity.SysDepartment;
import com.RD.entity.SysUser;
import com.RD.mapper.SysDepartmentMapper;
import com.RD.mapper.SysUserMapper;
import com.RD.user.dto.CurrentUserVO;
import com.RD.user.dto.DepartmentVO;
import com.RD.user.dto.UserQuery;
import com.RD.user.dto.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户服务（用户 + 部门）
 *
 * <p>范围：</p>
 * <ul>
 *   <li>当前用户信息（{@code /api/users/me}）—— mobile/email 走 {@code @SensitiveData} 切面脱敏</li>
 *   <li>用户分页查询（{@code /api/users}）—— 仅 ADMIN/MANAGER 可访问</li>
 *   <li>部门列表（{@code /api/departments}）—— 公开给所有登录用户</li>
 * </ul>
 *
 * <p>脱敏在 VO 字段上声明，{@code SensitiveDataAspect} 在 Controller 返回时统一处理，
 * Service 层不感知脱敏逻辑。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SysDepartmentMapper departmentMapper;

    // ============================================
    // 当前用户
    // ============================================

    /**
     * 获取当前登录用户信息
     */
    public CurrentUserVO getCurrentUser(SysUser currentUser) {
        if (currentUser == null) {
            throw BusinessException.unauthorized("未登录");
        }
        CurrentUserVO vo = new CurrentUserVO();
        vo.setId(currentUser.getId());
        vo.setUserId(currentUser.getUserId());
        vo.setName(currentUser.getName());
        vo.setAvatarUrl(currentUser.getAvatarUrl());
        vo.setMobile(currentUser.getMobile());
        vo.setEmail(currentUser.getEmail());
        vo.setDepartmentId(currentUser.getDepartmentId());
        vo.setPosition(currentUser.getPosition());
        vo.setRole(currentUser.getRole());
        vo.setStatus(currentUser.getStatus());
        vo.setLastLoginAt(currentUser.getLastLoginAt());
        vo.setLastSyncTime(currentUser.getLastSyncTime());
        // 部门名
        if (currentUser.getDepartmentId() != null) {
            SysDepartment dept = departmentMapper.selectById(currentUser.getDepartmentId());
            vo.setDepartmentName(dept != null ? dept.getName() : null);
        }
        return vo;
    }

    // ============================================
    // 用户列表
    // ============================================

    /**
     * 分页查询用户
     */
    public Page<UserVO> listUsers(UserQuery query, SysUser currentUser) {
        if (!"ADMIN".equals(currentUser.getRole()) && !"MANAGER".equals(currentUser.getRole())) {
            throw BusinessException.forbidden("仅管理员/部门经理可查询用户列表");
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysUser::getUserId, kw)
                    .or().like(SysUser::getName, kw)
                    .or().like(SysUser::getMobile, kw)
                    .or().like(SysUser::getEmail, kw));
        }
        if (query.getDepartmentId() != null) {
            wrapper.eq(SysUser::getDepartmentId, query.getDepartmentId());
        }
        if (StringUtils.hasText(query.getRole())) {
            wrapper.eq(SysUser::getRole, query.getRole());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        // MANAGER 仅看本部门（简化版；递归子部门留给 P3）
        if ("MANAGER".equals(currentUser.getRole())) {
            wrapper.eq(SysUser::getDepartmentId, currentUser.getDepartmentId());
        }
        wrapper.orderByAsc(SysUser::getName);

        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1
                ? 20
                : Math.min(query.getPageSize(), 200);
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        Page<SysUser> result = userMapper.selectPage(page, wrapper);

        Page<UserVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        if (result.getRecords().isEmpty()) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        // 批量查部门名
        Set<Long> deptIds = result.getRecords().stream()
                .map(SysUser::getDepartmentId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysDepartment> deptMap = deptIds.isEmpty()
                ? Collections.emptyMap()
                : departmentMapper.selectBatchIds(deptIds).stream()
                        .collect(Collectors.toMap(SysDepartment::getId, d -> d));

        voPage.setRecords(result.getRecords().stream()
                .map(u -> toUserVO(u, deptMap.get(u.getDepartmentId())))
                .collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 按 UserID 查询单个用户（公开给所有登录用户，用于 @ 提及 / 项目成员选择等）
     */
    public UserVO getUserByUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw BusinessException.badRequest("userId 不能为空");
        }
        SysUser user = userMapper.selectByUserId(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在: " + userId);
        }
        SysDepartment dept = user.getDepartmentId() != null
                ? departmentMapper.selectById(user.getDepartmentId())
                : null;
        return toUserVO(user, dept);
    }

    // ============================================
    // 部门
    // ============================================

    /**
     * 部门列表（平铺，parentId 字段已包含；前端可按 parentId 自构树）
     */
    public List<DepartmentVO> listDepartments() {
        List<SysDepartment> all = departmentMapper.selectList(
                new LambdaQueryWrapper<SysDepartment>().orderByAsc(SysDepartment::getOrderNum));
        if (all.isEmpty()) {
            return Collections.emptyList();
        }

        // 部门负责人姓名（批量）
        Set<String> leaderIds = all.stream()
                .map(SysDepartment::getLeaderUserId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Map<String, String> leaderNameMap = leaderIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectByUserIds(new ArrayList<>(leaderIds)).stream()
                        .collect(Collectors.toMap(SysUser::getUserId, SysUser::getName));

        return all.stream().map(d -> {
            DepartmentVO vo = new DepartmentVO();
            vo.setId(d.getId());
            vo.setDeptId(d.getDeptId());
            vo.setName(d.getName());
            vo.setParentId(d.getParentId());
            vo.setOrderNum(d.getOrderNum());
            vo.setLeaderUserId(d.getLeaderUserId());
            vo.setLeaderName(d.getLeaderUserId() != null
                    ? leaderNameMap.get(d.getLeaderUserId())
                    : null);
            vo.setMemberCount(userMapper.selectUserIdsByDeptId(d.getId()).size());
            vo.setCreatedAt(d.getCreatedAt());
            vo.setUpdatedAt(d.getUpdatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    // ============================================
    // 内部方法
    // ============================================

    private UserVO toUserVO(SysUser u, SysDepartment dept) {
        UserVO vo = new UserVO();
        vo.setId(u.getId());
        vo.setUserId(u.getUserId());
        vo.setName(u.getName());
        vo.setAvatarUrl(u.getAvatarUrl());
        vo.setMobile(u.getMobile());
        vo.setEmail(u.getEmail());
        vo.setDepartmentId(u.getDepartmentId());
        vo.setDepartmentName(dept != null ? dept.getName() : null);
        vo.setPosition(u.getPosition());
        vo.setRole(u.getRole());
        vo.setStatus(u.getStatus());
        vo.setLastLoginAt(u.getLastLoginAt());
        vo.setLastSyncTime(u.getLastSyncTime());
        return vo;
    }
}
