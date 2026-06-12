import request from '@/utils/request'

/**
 * 任务 API
 *
 * 后端接口：
 *   GET  /api/tasks         — 分页查询（按角色自动过滤）
 *   GET  /api/tasks/{id}    — 任务详情
 *   POST /api/tasks         — 创建任务
 *   PUT  /api/tasks/{id}    — 更新任务（仅 PENDING_ACCEPT + 仅创建者）
 *
 * 状态流转（accept / submit / complete / reject / cancel）Phase 2.5 接入，
 * 本期不暴露。
 */

/**
 * 任务状态枚举 —— 对应后端 {@code task.status} 字段
 */
export const TASK_STATUSES = [
  'PENDING_ACCEPT',
  'IN_PROGRESS',
  'PENDING_VERIFY',
  'COMPLETED',
  'REJECTED',
  'WITHDRAWN'
] as const
export type TaskStatus = typeof TASK_STATUSES[number]

/**
 * 任务优先级：1-最高 / 2-高 / 3-中 / 4-低
 */
export const TASK_PRIORITIES = [1, 2, 3, 4] as const
export type TaskPriority = typeof TASK_PRIORITIES[number]

/**
 * 任务展示 VO —— 对应后端 {@code TaskVO}
 */
export interface TaskVO {
  id: number
  taskNo: string
  title: string
  description?: string
  sourceRemark?: string
  creatorId: string
  creatorName?: string
  assigneeId: string
  assigneeName?: string
  priority: TaskPriority
  status: TaskStatus
  selfAssigned?: boolean
  estimatedDuration?: number
  actualStartTime?: string
  actualDeadline?: string
  actualEndTime?: string
  rejectReason?: string
  version: number
  createdAt: string
  updatedAt: string
}

/**
 * 任务查询参数 —— 对应后端 {@code TaskQuery}
 */
export interface TaskQuery {
  status?: TaskStatus
  priority?: TaskPriority
  creatorId?: string
  assigneeId?: string
  /** 关键字（匹配 task_no / title） */
  keyword?: string
  pageNum?: number
  pageSize?: number
}

/**
 * 创建/更新任务请求 —— 对应后端 {@code CreateTaskRequest}
 */
export interface CreateTaskRequest {
  title: string
  description?: string
  sourceRemark?: string
  assigneeId: string
  priority: TaskPriority
  estimatedDuration?: number
}

/**
 * 分页响应（MyBatis-Plus Page 字段）
 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * 分页查询任务列表（按角色自动过滤）
 */
export function listTasks(params: TaskQuery) {
  return request<PageResult<TaskVO>>({ url: '/tasks', method: 'get', params })
}

/**
 * 任务详情
 */
export function getTask(id: number) {
  return request<TaskVO>({ url: `/tasks/${id}`, method: 'get' })
}

/**
 * 创建任务
 */
export function createTask(data: CreateTaskRequest) {
  return request<TaskVO>({ url: '/tasks', method: 'post', data })
}

/**
 * 更新任务
 */
export function updateTask(id: number, data: CreateTaskRequest) {
  return request<TaskVO>({ url: `/tasks/${id}`, method: 'put', data })
}

// ============================================
// 状态流转（Phase 2.5 接入）
// ============================================

/**
 * 接收方确认接收任务（PENDING_ACCEPT → IN_PROGRESS）
 */
export function acceptTask(id: number) {
  return request<void>({ url: `/tasks/${id}/accept`, method: 'post' })
}

/**
 * 执行方提交完成（IN_PROGRESS → PENDING_VERIFY）
 */
export function submitTask(id: number, remark?: string) {
  return request<void>({ url: `/tasks/${id}/submit`, method: 'post', data: { remark } })
}

/**
 * 发起方验收（PENDING_VERIFY → COMPLETED）
 */
export function completeTask(id: number) {
  return request<void>({ url: `/tasks/${id}/complete`, method: 'post' })
}

/**
 * 发起方驳回（PENDING_VERIFY → IN_PROGRESS）
 *
 * @param reason 驳回原因（必填）
 */
export function rejectTask(id: number, reason: string) {
  return request<void>({ url: `/tasks/${id}/reject`, method: 'post', data: { reason } })
}

/**
 * 发起方撤回（PENDING_ACCEPT/IN_PROGRESS → WITHDRAWN）
 */
export function cancelTask(id: number, reason?: string) {
  return request<void>({ url: `/tasks/${id}/cancel`, method: 'post', data: { reason } })
}
