package com.RD.rd.bom.dto;

import lombok.Data;

import java.util.List;

/**
 * BOM 树节点（多阶递归）
 *
 * <p>结构：</p>
 * <pre>
 * BomTreeNode
 *   ├─ header: BomHeaderVO
 *   └─ items: List<BomItemVO>
 *                └─ BomItemVO.subBom: BomTreeNode (递归)
 * </pre>
 *
 * <p>前端 el-tree 用 children = {@code items} 渲染, 每个 BomItemVO 自己的 subBom.children
 * 又是 items —— 形成自然递归。</p>
 */
@Data
public class BomTreeNode {

    private BomHeaderVO header;
    private List<BomItemVO> items;
}
