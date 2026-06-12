package com.RD.rd.bom.service;

import com.RD.common.BusinessException;
import com.RD.entity.BomHeader;
import com.RD.entity.BomItem;
import com.RD.entity.Project;
import com.RD.entity.SysUser;
import com.RD.mapper.BomHeaderMapper;
import com.RD.mapper.BomItemMapper;
import com.RD.mapper.ProjectMapper;
import com.RD.mapper.SysUserMapper;
import com.RD.rd.bom.dto.BomHeaderVO;
import com.RD.rd.bom.dto.BomItemVO;
import com.RD.rd.bom.dto.BomTreeNode;
import com.RD.rd.bom.dto.CreateBomHeaderRequest;
import com.RD.rd.bom.dto.CreateBomItemRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * BOM 多阶树服务
 *
 * <p>核心方法 {@link #getTree(Long)}: 从根 BOM 递归展开子 BOM,
 * 返回嵌套 {@link BomTreeNode}, 前端 el-tree 直接渲染。</p>
 *
 * <p>多阶实现: bom_item.sub_bom_id 指向子 bom_header.id,
 * 递归查询每个有 sub_bom_id 的物料的子 BOM, 直到最底层(无子组件)。</p>
 *
 * <p>循环引用防护: 维护一个 visited set, 防止 A.sub_bom_id=B, B.sub_bom_id=A 的死循环。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BomService {

    private final BomHeaderMapper bomHeaderMapper;
    private final BomItemMapper bomItemMapper;
    private final ProjectMapper projectMapper;
    private final SysUserMapper userMapper;

    // ============================================
    // BOM Header CRUD
    // ============================================

    /**
     * 查询顶层 BOM 列表（parent_bom_id = 0）
     *
     * <p>多阶树从根开始, 前端先看顶层列表, 点进去再递归拉子树</p>
     */
    public List<BomHeaderVO> listTopLevelBoms(Long projectId, String status) {
        LambdaQueryWrapper<BomHeader> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BomHeader::getParentBomId, 0L);
        if (projectId != null) {
            wrapper.eq(BomHeader::getProjectId, projectId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(BomHeader::getStatus, status);
        }
        wrapper.orderByDesc(BomHeader::getUpdatedAt);
        List<BomHeader> list = bomHeaderMapper.selectList(wrapper);
        return list.stream().map(h -> toHeaderVO(h, null)).collect(Collectors.toList());
    }

    /**
     * 查某个 BOM 的子 BOM（parent_bom_id = bomId）
     */
    public List<BomHeaderVO> listChildBoms(Long parentId) {
        List<BomHeader> list = bomHeaderMapper.selectList(
                new LambdaQueryWrapper<BomHeader>()
                        .eq(BomHeader::getParentBomId, parentId)
                        .orderByDesc(BomHeader::getUpdatedAt));
        return list.stream().map(h -> toHeaderVO(h, null)).collect(Collectors.toList());
    }

    /**
     * BOM 详情
     */
    public BomHeaderVO getBomById(Long id) {
        BomHeader h = bomHeaderMapper.selectById(id);
        if (h == null) {
            throw BusinessException.notFound("BOM 不存在: " + id);
        }
        Integer count = Math.toIntExact(bomItemMapper.selectCount(
                new LambdaQueryWrapper<BomItem>().eq(BomItem::getBomId, id)));
        return toHeaderVO(h, count);
    }

    /**
     * 创建 BOM 表头
     */
    @Transactional(rollbackFor = Exception.class)
    public BomHeaderVO createBom(CreateBomHeaderRequest req, SysUser currentUser) {
        if (!StringUtils.hasText(req.getBomCode())) {
            throw BusinessException.badRequest("BOM 编号不能为空");
        }
        // 唯一性
        if (bomHeaderMapper.selectCount(
                new LambdaQueryWrapper<BomHeader>().eq(BomHeader::getBomCode, req.getBomCode())) > 0) {
            throw BusinessException.badRequest("BOM 编号已存在: " + req.getBomCode());
        }
        BomHeader h = new BomHeader();
        h.setBomCode(req.getBomCode());
        h.setProductName(req.getProductName());
        h.setProductModel(req.getProductModel());
        h.setVersion(StringUtils.hasText(req.getVersion()) ? req.getVersion() : "1.0");
        h.setParentBomId(req.getParentBomId() == null ? 0L : req.getParentBomId());
        h.setProjectId(req.getProjectId());
        h.setStatus("DRAFT");
        h.setEffectiveDate(req.getEffectiveDate());
        h.setExpiryDate(req.getExpiryDate());
        h.setCreatedBy(currentUser.getUserId());
        h.setCreatedAt(LocalDateTime.now());
        h.setUpdatedAt(LocalDateTime.now());
        bomHeaderMapper.insert(h);
        log.info("[BOM] 创建: id={}, code={}, parent={}", h.getId(), h.getBomCode(), h.getParentBomId());
        return getBomById(h.getId());
    }

    /**
     * 删除 BOM（仅 DRAFT 状态可删, 有子 BOM 也禁删）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteBom(Long id, SysUser currentUser) {
        BomHeader h = bomHeaderMapper.selectById(id);
        if (h == null) {
            throw BusinessException.notFound("BOM 不存在: " + id);
        }
        if (!"DRAFT".equals(h.getStatus())) {
            throw BusinessException.badRequest("只有 DRAFT 状态可删除");
        }
        // 检查是否有子 BOM 引用 (bom_item.sub_bom_id == id)
        Long subRefCount = bomItemMapper.selectCount(
                new LambdaQueryWrapper<BomItem>().eq(BomItem::getSubBomId, id));
        if (subRefCount > 0) {
            throw BusinessException.badRequest("该 BOM 被其他 BOM 引用为子组件, 无法删除");
        }
        // 检查是否有子 BOM (parent_bom_id == id)
        Long childCount = bomHeaderMapper.selectCount(
                new LambdaQueryWrapper<BomHeader>().eq(BomHeader::getParentBomId, id));
        if (childCount > 0) {
            throw BusinessException.badRequest("该 BOM 有子 BOM, 请先删除子 BOM");
        }
        bomHeaderMapper.deleteById(id);
        log.info("[BOM] 删除: id={}, operator={}", id, currentUser.getUserId());
    }

    // ============================================
    // BOM Item CRUD
    // ============================================

    /**
     * 查某个 BOM 的所有物料行
     */
    public List<BomItemVO> listItems(Long bomId) {
        List<BomItem> items = bomItemMapper.selectList(
                new LambdaQueryWrapper<BomItem>()
                        .eq(BomItem::getBomId, bomId)
                        .orderByAsc(BomItem::getLineNo));
        return items.stream().map(i -> toItemVO(i, null)).collect(Collectors.toList());
    }

    /**
     * 新增物料行
     */
    @Transactional(rollbackFor = Exception.class)
    public BomItemVO createItem(Long bomId, CreateBomItemRequest req, SysUser currentUser) {
        validateItem(bomId, req);
        BomItem i = new BomItem();
        i.setBomId(bomId);
        i.setLineNo(req.getLineNo());
        i.setItemCode(req.getItemCode());
        i.setItemName(req.getItemName());
        i.setSpecification(req.getSpecification());
        i.setQuantity(req.getQuantity());
        i.setUnit(req.getUnit());
        i.setSupplier(req.getSupplier());
        i.setUnitPrice(req.getUnitPrice());
        i.setTotalPrice(req.getUnitPrice() != null && req.getQuantity() != null
                ? req.getUnitPrice().multiply(req.getQuantity()) : null);
        i.setRemark(req.getRemark());
        i.setSubBomId(req.getSubBomId());
        i.setCreatedAt(LocalDateTime.now());
        i.setUpdatedAt(LocalDateTime.now());
        bomItemMapper.insert(i);
        log.info("[BOM Item] 创建: id={}, bomId={}, item={}", i.getId(), bomId, i.getItemCode());
        return toItemVO(i, null);
    }

    /**
     * 删除物料行
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long itemId, SysUser currentUser) {
        BomItem i = bomItemMapper.selectById(itemId);
        if (i == null) {
            throw BusinessException.notFound("物料行不存在: " + itemId);
        }
        bomItemMapper.deleteById(itemId);
        log.info("[BOM Item] 删除: id={}, operator={}", itemId, currentUser.getUserId());
    }

    // ============================================
    // 多阶树
    // ============================================

    /**
     * 从根 BOM 递归展开多阶树
     *
     * @param rootBomId 根 BOM ID（必填）
     * @return 含 header + items(items 内 subBomId 非空的 item 递归嵌入 subBom)
     */
    public BomTreeNode getTree(Long rootBomId) {
        if (rootBomId == null) {
            throw BusinessException.badRequest("rootBomId 不能为空");
        }
        // visited set 防止循环引用
        return expandNode(rootBomId, new java.util.HashSet<>());
    }

    /**
     * 递归展开一个 BOM 节点
     *
     * @param bomId   当前要展开的 BOM
     * @param visited 已访问的 bomId set, 防止循环引用 (A -> B -> A)
     */
    private BomTreeNode expandNode(Long bomId, java.util.Set<Long> visited) {
        if (!visited.add(bomId)) {
            log.warn("[BOM] 检测到循环引用, bomId={}, visited={}", bomId, visited);
            return null;  // 截断循环
        }
        BomHeader h = bomHeaderMapper.selectById(bomId);
        if (h == null) {
            return null;
        }
        Integer count = Math.toIntExact(bomItemMapper.selectCount(
                new LambdaQueryWrapper<BomItem>().eq(BomItem::getBomId, bomId)));
        BomHeaderVO headerVO = toHeaderVO(h, count);

        List<BomItem> items = bomItemMapper.selectList(
                new LambdaQueryWrapper<BomItem>()
                        .eq(BomItem::getBomId, bomId)
                        .orderByAsc(BomItem::getLineNo));
        List<BomItemVO> itemVOs = items.stream().map(i -> {
            BomItemVO vo = toItemVO(i, null);
            // 递归: 该物料若有子 BOM, 嵌入 subBom
            if (i.getSubBomId() != null) {
                BomTreeNode sub = expandNode(i.getSubBomId(), visited);
                vo.setSubBom(sub);
            }
            return vo;
        }).collect(Collectors.toList());

        BomTreeNode node = new BomTreeNode();
        node.setHeader(headerVO);
        node.setItems(itemVOs);
        return node;
    }

    // ============================================
    // 内部
    // ============================================

    private void validateItem(Long bomId, CreateBomItemRequest req) {
        if (bomId == null) {
            throw BusinessException.badRequest("bomId 不能为空");
        }
        if (bomHeaderMapper.selectById(bomId) == null) {
            throw BusinessException.badRequest("BOM 不存在: " + bomId);
        }
        if (!StringUtils.hasText(req.getItemCode())) {
            throw BusinessException.badRequest("物料编码不能为空");
        }
        if (!StringUtils.hasText(req.getItemName())) {
            throw BusinessException.badRequest("物料名称不能为空");
        }
        if (req.getQuantity() == null) {
            throw BusinessException.badRequest("数量不能为空");
        }
        // 行号唯一
        if (bomItemMapper.selectCount(
                new LambdaQueryWrapper<BomItem>()
                        .eq(BomItem::getBomId, bomId)
                        .eq(BomItem::getLineNo, req.getLineNo())) > 0) {
            throw BusinessException.badRequest("行号已存在: " + req.getLineNo());
        }
        // 子 BOM 存在性
        if (req.getSubBomId() != null && bomHeaderMapper.selectById(req.getSubBomId()) == null) {
            throw BusinessException.badRequest("子 BOM 不存在: " + req.getSubBomId());
        }
    }

    private String lookupUserName(String userId) {
        if (!StringUtils.hasText(userId)) return null;
        SysUser u = userMapper.selectByUserId(userId);
        return u != null ? u.getName() : null;
    }

    private String lookupProjectName(Long projectId) {
        if (projectId == null) return null;
        Project p = projectMapper.selectById(projectId);
        return p != null ? p.getName() : null;
    }

    private BomHeaderVO toHeaderVO(BomHeader h, Integer itemCount) {
        BomHeaderVO vo = new BomHeaderVO();
        vo.setId(h.getId());
        vo.setBomCode(h.getBomCode());
        vo.setProductName(h.getProductName());
        vo.setProductModel(h.getProductModel());
        vo.setVersion(h.getVersion());
        vo.setParentBomId(h.getParentBomId());
        vo.setProjectId(h.getProjectId());
        vo.setProjectName(lookupProjectName(h.getProjectId()));
        vo.setStatus(h.getStatus());
        vo.setEffectiveDate(h.getEffectiveDate());
        vo.setExpiryDate(h.getExpiryDate());
        vo.setCreatedBy(h.getCreatedBy());
        vo.setCreatedByName(lookupUserName(h.getCreatedBy()));
        vo.setApprovedBy(h.getApprovedBy());
        vo.setApprovedByName(lookupUserName(h.getApprovedBy()));
        vo.setApprovedAt(h.getApprovedAt());
        vo.setCreatedAt(h.getCreatedAt());
        vo.setUpdatedAt(h.getUpdatedAt());
        vo.setItemCount(itemCount);
        return vo;
    }

    private BomItemVO toItemVO(BomItem i, BomTreeNode subBom) {
        BomItemVO vo = new BomItemVO();
        vo.setId(i.getId());
        vo.setBomId(i.getBomId());
        vo.setLineNo(i.getLineNo());
        vo.setItemCode(i.getItemCode());
        vo.setItemName(i.getItemName());
        vo.setSpecification(i.getSpecification());
        vo.setQuantity(i.getQuantity());
        vo.setUnit(i.getUnit());
        vo.setSupplier(i.getSupplier());
        vo.setUnitPrice(i.getUnitPrice());
        vo.setTotalPrice(i.getTotalPrice());
        vo.setRemark(i.getRemark());
        vo.setSubBomId(i.getSubBomId());
        vo.setSubBom(subBom);
        vo.setCreatedAt(i.getCreatedAt());
        vo.setUpdatedAt(i.getUpdatedAt());
        return vo;
    }
}
