package com.RD.user;

import com.RD.common.Result;
import com.RD.entity.SysUser;
import com.RD.mapper.SysUserMapper;
import com.RD.user.dto.CurrentUserVO;
import com.RD.user.dto.UserQuery;
import com.RD.user.dto.UserVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户 Controller
 *
 * <p>接口列表：</p>
 * <ul>
 *   <li>GET /api/users/me          — 当前登录用户（本人）</li>
 *   <li>GET /api/users             — 用户分页查询（ADMIN/MANAGER）</li>
 *   <li>GET /api/users/{userId}    — 按 UserID 查询单个用户（@ 提及 / 成员选择）</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户", description = "当前用户 / 用户列表 / 按 userId 查询")
public class UserController {

    private final UserService userService;
    private final SysUserMapper userMapper;

    /**
     * 当前登录用户信息
     */
    @GetMapping("/me")
    @Operation(summary = "当前登录用户信息（mobile/email 自动脱敏，仅本人/ADMIN 见明文）")
    public Result<CurrentUserVO> me() {
        return Result.success(userService.getCurrentUser(currentUser()));
    }

    /**
     * 用户分页查询（ADMIN 全查 / MANAGER 仅本部门）
     */
    @GetMapping
    @Operation(summary = "用户分页查询")
    public Result<Page<UserVO>> list(UserQuery query) {
        return Result.success(userService.listUsers(query, currentUser()));
    }

    /**
     * 按 UserID 查询单个用户（公开给所有登录用户）
     */
    @GetMapping("/{userId}")
    @Operation(summary = "按 userId 查询单个用户（@ 提及 / 成员选择）")
    public Result<UserVO> getByUserId(@PathVariable String userId) {
        return Result.success(userService.getUserByUserId(userId));
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
