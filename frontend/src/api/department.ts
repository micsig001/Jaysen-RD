import request from '@/utils/request'

/**
 * 部门 API
 *
 * 后端接口：
 *   GET /api/departments — 部门列表（平铺，parentId 字段已含；前端按 parentId 自构树）
 */

/**
 * 部门 VO —— 对应后端 {@code DepartmentVO}
 */
export interface DepartmentVO {
  id: number
  /** 企微部门 ID */
  deptId: string
  name: string
  /** 父部门 ID（0 = 根） */
  parentId: number
  orderNum: number
  leaderUserId?: string
  leaderName?: string
  /** 部门成员数（仅启用 status=1 的用户） */
  memberCount: number
  createdAt?: string
  updatedAt?: string
}

/**
 * 部门列表（平铺）
 */
export function listDepartments() {
  return request<DepartmentVO[]>({ url: '/departments', method: 'get' })
}
