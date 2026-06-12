package com.RD.user;

import com.RD.common.Result;
import com.RD.user.dto.DepartmentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 部门 Controller
 *
 * <p>接口列表：</p>
 * <ul>
 *   <li>GET /api/departments — 部门列表（平铺，parentId 字段已含）</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Tag(name = "部门", description = "部门列表（前端按 parentId 自构树）")
public class DepartmentController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "部门列表（公开给所有登录用户）")
    public Result<List<DepartmentVO>> list() {
        return Result.success(userService.listDepartments());
    }
}
