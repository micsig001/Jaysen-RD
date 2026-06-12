import request from '@/utils/request'

/**
 * Sprint 看板 API
 *
 * 后端接口：
 *   Sprint：
 *     GET    /api/sprints                      列表
 *     GET    /api/sprints/{id}                 详情
 *     POST   /api/sprints                      创建
 *     POST   /api/sprints/{id}/activate         启动 (PLANNED → ACTIVE)
 *     POST   /api/sprints/{id}/complete         完成 (ACTIVE → COMPLETED)
 *   Sprint Task：
 *     GET    /api/sprint-tasks                 列表
 *     GET    /api/sprint-tasks/{id}            详情
 *     POST   /api/sprint-tasks                 创建
 *     PUT    /api/sprint-tasks/{id}            更新
 *     DELETE /api/sprint-tasks/{id}            删除
 *     POST   /api/sprint-tasks/{id}/move       拖拽移动 (status + orderNum + sprintId)
 */

export const SPRINT_STATUSES = ['PLANNED', 'ACTIVE', 'COMPLETED', 'CANCELLED'] as const
export type SprintStatus = typeof SPRINT_STATUSES[number]

export const SPRINT_TASK_STATUSES = [
  'BACKLOG',
  'TODO',
  'IN_PROGRESS',
  'REVIEW',
  'DONE'
] as const
export type SprintTaskStatus = typeof SPRINT_TASK_STATUSES[number]

export const SPRINT_TASK_TYPES = ['FEATURE', 'BUG', 'OPTIMIZATION', 'TEST'] as const
export type SprintTaskType = typeof SPRINT_TASK_TYPES[number]

export const SPRINT_TASK_PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as const
export type SprintTaskPriority = typeof SPRINT_TASK_PRIORITIES[number]

/**
 * Sprint 详情 VO
 */
export interface SprintVO {
  id: number
  projectId: number
  projectName?: string
  name: string
  startDate: string
  endDate: string
  goal?: string
  status: SprintStatus
  createdBy: string
  createdByName?: string
  taskCount: number
  doneCount: number
  createdAt: string
  updatedAt: string
}

/**
 * Sprint 任务 VO
 */
export interface SprintTaskVO {
  id: number
  projectId: number
  sprintId?: number
  title: string
  description?: string
  type: SprintTaskType
  priority: SprintTaskPriority
  status: SprintTaskStatus
  assigneeUserid?: string
  assigneeName?: string
  reporterUserid: string
  reporterName?: string
  estimatedHours?: number
  actualHours?: number
  storyPoints?: number
  tags?: string[]
  dueDate?: string
  completedAt?: string
  orderNum: number
  createdAt: string
  updatedAt: string
}

export interface CreateSprintRequest {
  projectId: number
  name: string
  startDate: string
  endDate: string
  goal?: string
}

export interface CreateSprintTaskRequest {
  projectId: number
  sprintId?: number
  title: string
  description?: string
  type?: SprintTaskType
  priority?: SprintTaskPriority
  status?: SprintTaskStatus
  assigneeUserid?: string
  estimatedHours?: number
  storyPoints?: number
  tags?: string
  dueDate?: string
  orderNum?: number
}

// ========== Sprint ==========

export function listSprints(params?: { projectId?: number; status?: SprintStatus }) {
  return request<SprintVO[]>({ url: '/sprints', method: 'get', params })
}

export function getSprint(id: number) {
  return request<SprintVO>({ url: `/sprints/${id}`, method: 'get' })
}

export function createSprint(data: CreateSprintRequest) {
  return request<SprintVO>({ url: '/sprints', method: 'post', data })
}

export function activateSprint(id: number) {
  return request<SprintVO>({ url: `/sprints/${id}/activate`, method: 'post' })
}

export function completeSprint(id: number) {
  return request<SprintVO>({ url: `/sprints/${id}/complete`, method: 'post' })
}

// ========== Sprint Task ==========

export function listSprintTasks(params?: { sprintId?: number; projectId?: number; status?: SprintTaskStatus }) {
  return request<SprintTaskVO[]>({ url: '/sprint-tasks', method: 'get', params })
}

export function getSprintTask(id: number) {
  return request<SprintTaskVO>({ url: `/sprint-tasks/${id}`, method: 'get' })
}

export function createSprintTask(data: CreateSprintTaskRequest) {
  return request<SprintTaskVO>({ url: '/sprint-tasks', method: 'post', data })
}

export function updateSprintTask(id: number, data: CreateSprintTaskRequest) {
  return request<SprintTaskVO>({ url: `/sprint-tasks/${id}`, method: 'put', data })
}

export function deleteSprintTask(id: number) {
  return request<void>({ url: `/sprint-tasks/${id}`, method: 'delete' })
}

export interface MoveTaskRequest {
  sprintId: number | null
  status: SprintTaskStatus
  orderNum: number
}

/**
 * 拖拽移动任务 (列内 / 跨列)
 */
export function moveSprintTask(id: number, data: MoveTaskRequest) {
  return request<SprintTaskVO>({ url: `/sprint-tasks/${id}/move`, method: 'post', data })
}
