package com.RD.rd.bom;

import com.RD.audit.AuditLog;
import com.RD.common.Result;
import com.RD.entity.SysUser;
import com.RD.mapper.SysUserMapper;
import com.RD.rd.bom.dto.BomHeaderVO;
import com.RD.rd.bom.dto.BomItemVO;
import com.RD.rd.bom.dto.BomTreeNode;
import com.RD.rd.bom.dto.CreateBomHeaderRequest;
import com.RD.rd.bom.dto.CreateBomItemRequest;
import com.RD.rd.bom.service.BomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * BOM Controller
 *
 * <p>接口列表：</p>
 * <ul>
 *   <li>GET    /api/boms                          — 顶层 BOM 列表 (parent=0)</li>
 *   <li>GET    /api/boms/{id}                     — BOM 详情</li>
 *   <li>GET    /api/boms/{id}/children             — 子 BOM 列表 (parent=id)</li>
 *   <li>GET    /api/boms/{id}/items               — 物料行列表</li>
 *   <li>GET    /api/boms/{id}/tree                — 多阶树 (递归展开)</li>
 *   <li>POST   /api/boms                          — 创建 BOM 表头</li>
 *   <li>DELETE /api/boms/{id}                     — 删除 BOM (仅 DRAFT, 无子)</li>
 *   <li>POST   /api/boms/{id}/items               — 新增物料行</li>
 *   <li>DELETE /api/bom-items/{itemId}            — 删除物料行</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "BOM 多阶树", description = "BOM 表头 + 物料行 + 多阶递归树")
public class BomController {

    private final BomService bomService;
    private final SysUserMapper userMapper;

    // ========== Header ==========

    @GetMapping("/api/boms")
    @Operation(summary = "顶层 BOM 列表 (parent_bom_id=0)")
    public Result<List<BomHeaderVO>> listTopLevel(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status) {
        return Result.success(bomService.listTopLevelBoms(projectId, status));
    }

    @GetMapping("/api/boms/{id}")
    @Operation(summary = "BOM 详情")
    public Result<BomHeaderVO> getBom(@PathVariable Long id) {
        return Result.success(bomService.getBomById(id));
    }

    @GetMapping("/api/boms/{id}/children")
    @Operation(summary = "子 BOM 列表 (parent_bom_id=id)")
    public Result<List<BomHeaderVO>> listChildren(@PathVariable Long id) {
        return Result.success(bomService.listChildBoms(id));
    }

    @GetMapping("/api/boms/{id}/tree")
    @Operation(summary = "多阶树 (递归展开 items.sub_bom, 循环引用防护)")
    public Result<BomTreeNode> getTree(@PathVariable Long id) {
        return Result.success(bomService.getTree(id));
    }

    @PostMapping("/api/boms")
    @AuditLog(operationType = "CREATE", resourceType = "BOM",
            description = "创建 BOM")
    @Operation(summary = "创建 BOM 表头")
    public Result<BomHeaderVO> createBom(@Valid @RequestBody CreateBomHeaderRequest req) {
        return Result.success(bomService.createBom(req, currentUser()));
    }

    @DeleteMapping("/api/boms/{id}")
    @AuditLog(operationType = "DELETE", resourceType = "BOM",
            resourceIdParam = "#id", description = "删除 BOM")
    @Operation(summary = "删除 BOM (仅 DRAFT + 无子 BOM/物料行被引用)")
    public Result<Void> deleteBom(@PathVariable Long id) {
        bomService.deleteBom(id, currentUser());
        return Result.success();
    }

    // ========== Item ==========

    @GetMapping("/api/boms/{id}/items")
    @Operation(summary = "BOM 物料行列表（按行号升序）")
    public Result<List<BomItemVO>> listItems(@PathVariable Long id) {
        return Result.success(bomService.listItems(id));
    }

    @PostMapping("/api/boms/{id}/items")
    @AuditLog(operationType = "CREATE", resourceType = "BOM_ITEM",
            description = "新增 BOM 物料行")
    @Operation(summary = "新增物料行")
    public Result<BomItemVO> createItem(@PathVariable Long id,
                                       @Valid @RequestBody CreateBomItemRequest req) {
        return Result.success(bomService.createItem(id, req, currentUser()));
    }

    @DeleteMapping("/api/bom-items/{itemId}")
    @AuditLog(operationType = "DELETE", resourceType = "BOM_ITEM",
            resourceIdParam = "#itemId", description = "删除物料行")
    @Operation(summary = "删除物料行")
    public Result<Void> deleteItem(@PathVariable Long itemId) {
        bomService.deleteItem(itemId, currentUser());
        return Result.success();
    }

    // ========== 工具 ==========

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
