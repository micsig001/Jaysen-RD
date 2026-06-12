import request from '@/utils/request'

/**
 * 用户 API
 *
 * 后端接口：
 *   GET /api/users/me          — 当前登录用户
 *   GET /api/users             — 用户分页查询（ADMIN 全查 / MANAGER 仅本部门）
 *   GET /api/users/{userId}    — 按 userId 查询单个用户（@ 提及 / 项目成员选择）
 */

/**
 * 当前登录用户 VO —— 对应后端 {@code CurrentUserVO}
 */
export interface CurrentUserVO {
  id: number
  userId: string
  name: string
  avatarUrl?: string
  mobile?: string
  email?: string
  departmentId?: number
  departmentName?: string
  position?: string
  /** 系统角色：EMPLOYEE / MANAGER / ADMIN */
  role: 'EMPLOYEE' | 'MANAGER' | 'ADMIN'
  /** 1-启用，0-禁用 */
  status: number
  lastLoginAt?: string
  lastSyncTime?: string
}

/**
 * 用户列表 VO —— 对应后端 {@code UserVO}
 */
export interface UserVO {
  id: number
  userId: string
  name: string
  avatarUrl?: string
  mobile?: string
  email?: string
  departmentId?: number
  departmentName?: string
  position?: string
  role: 'EMPLOYEE' | 'MANAGER' | 'ADMIN'
  status: number
  lastLoginAt?: string
  lastSyncTime?: string
}

/**
 * 用户分页查询参数 —— 对应后端 {@code UserQuery}
 */
export interface UserQuery {
  /** 关键字（匹配 userId / name / mobile / email） */
  keyword?: string
  /** 部门 ID（精确） */
  departmentId?: number
  /** 角色过滤（EMPLOYEE / MANAGER / ADMIN） */
  role?: 'EMPLOYEE' | 'MANAGER' | 'ADMIN'
  /** 状态过滤（1-启用，0-禁用） */
  status?: 0 | 1
  pageNum?: number
  pageSize?: number
}

/**
 * MyBatis-Plus 分页响应
 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * 当前登录用户信息
 */
export function getCurrentUser() {
  return request<CurrentUserVO>({ url: '/users/me', method: 'get' })
}

/**
 * 用户分页查询（ADMIN/MANAGER）
 */
export function listUsers(params: UserQuery) {
  return request<PageResult<UserVO>>({ url: '/users', method: 'get', params })
}

/**
 * 按 userId 查询单个用户（@ 提及 / 项目成员选择）
 */
export function getUserByUserId(userId: string) {
  return request<UserVO>({ url: `/users/${userId}`, method: 'get' })
}
