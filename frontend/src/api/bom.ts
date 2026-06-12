import request from '@/utils/request'

/**
 * BOM 多阶树 API
 *
 * 后端接口：
 *   Header：
 *     GET    /api/boms                          顶层 BOM 列表 (parent=0)
 *     GET    /api/boms/{id}                     BOM 详情
 *     GET    /api/boms/{id}/children             子 BOM 列表
 *     GET    /api/boms/{id}/items               物料行列表
 *     GET    /api/boms/{id}/tree                多阶树 (递归展开)
 *     POST   /api/boms                          创建
 *     DELETE /api/boms/{id}                     删除
 *   Item：
 *     POST   /api/boms/{id}/items               新增
 *     DELETE /api/bom-items/{itemId}            删除
 */

export const BOM_STATUSES = ['DRAFT', 'UNDER_REVIEW', 'APPROVED', 'OBSOLETE'] as const
export type BomStatus = typeof BOM_STATUSES[number]

/**
 * BOM 表头 VO
 */
export interface BomHeaderVO {
  id: number
  bomCode: string
  productName: string
  productModel?: string
  version: string
  parentBomId: number
  projectId?: number
  projectName?: string
  status: BomStatus
  effectiveDate?: string
  expiryDate?: string
  createdBy: string
  createdByName?: string
  approvedBy?: string
  approvedByName?: string
  approvedAt?: string
  createdAt: string
  updatedAt: string
  itemCount?: number
}

/**
 * BOM 物料行 VO
 */
export interface BomItemVO {
  id: number
  bomId: number
  lineNo: number
  itemCode: string
  itemName: string
  specification?: string
  quantity: number
  unit: string
  supplier?: string
  unitPrice?: number
  totalPrice?: number
  remark?: string
  subBomId?: number
  /** 递归: 物料有子 BOM 时嵌入, 前端 el-tree 展开 */
  subBom?: BomTreeNode
  createdAt: string
  updatedAt: string
}

/**
 * BOM 树节点 (header + items, items 内 subBomId 非空的物料递归嵌入 subBom)
 */
export interface BomTreeNode {
  header: BomHeaderVO
  items: BomItemVO[]
}

export interface CreateBomHeaderRequest {
  bomCode: string
  productName: string
  productModel?: string
  version?: string
  parentBomId?: number
  projectId?: number
  effectiveDate?: string
  expiryDate?: string
}

export interface CreateBomItemRequest {
  lineNo: number
  itemCode: string
  itemName: string
  specification?: string
  quantity: number
  unit: string
  supplier?: string
  unitPrice?: number
  remark?: string
  subBomId?: number
}

// ========== Header ==========

export function listTopLevelBoms(params?: { projectId?: number; status?: BomStatus }) {
  return request<BomHeaderVO[]>({ url: '/boms', method: 'get', params })
}

export function getBom(id: number) {
  return request<BomHeaderVO>({ url: `/boms/${id}`, method: 'get' })
}

export function listBomChildren(id: number) {
  return request<BomHeaderVO[]>({ url: `/boms/${id}/children`, method: 'get' })
}

export function listBomItems(id: number) {
  return request<BomItemVO[]>({ url: `/boms/${id}/items`, method: 'get' })
}

export function getBomTree(id: number) {
  return request<BomTreeNode>({ url: `/boms/${id}/tree`, method: 'get' })
}

export function createBom(data: CreateBomHeaderRequest) {
  return request<BomHeaderVO>({ url: '/boms', method: 'post', data })
}

export function deleteBom(id: number) {
  return request<void>({ url: `/boms/${id}`, method: 'delete' })
}

// ========== Item ==========

export function createBomItem(bomId: number, data: CreateBomItemRequest) {
  return request<BomItemVO>({ url: `/boms/${bomId}/items`, method: 'post', data })
}

export function deleteBomItem(itemId: number) {
  return request<void>({ url: `/bom-items/${itemId}`, method: 'delete' })
}
