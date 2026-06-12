import request from '@/utils/request'

/**
 * ECN 工程变更 API
 *
 * 后端接口：
 *   GET    /api/ecn                          — 分页查询
 *   GET    /api/ecn/{id}                     — 详情
 *   GET    /api/ecn/{id}/approvals           — 审批记录
 *   POST   /api/ecn                          — 创建
 *   PUT    /api/ecn/{id}                     — 更新
 *   POST   /api/ecn/{id}/submit              — 提交审批（启动 Flowable）
 *   POST   /api/ecn/{id}/cancel              — 撤回
 *   GET    /api/ecn/tasks/my-pending         — 当前用户待办
 *   GET    /api/ecn/tasks/my-candidate       — 候选
 *   POST   /api/ecn/tasks/{taskId}/claim     — claim
 *   POST   /api/ecn/tasks/{taskId}/complete  — 完成审批
 */

export const ECN_STATUSES = [
  'DRAFT',
  'UNDER_REVIEW',
  'APPROVED',
  'REJECTED',
  'IMPLEMENTED',
  'CANCELLED'
] as const
export type EcnStatus = typeof ECN_STATUSES[number]

export const ECN_CHANGE_TYPES = ['DESIGN', 'MATERIAL', 'PROCESS', 'DOCUMENT'] as const
export type EcnChangeType = typeof ECN_CHANGE_TYPES[number]

export const ECN_URGENCIES = ['NORMAL', 'URGENT', 'CRITICAL'] as const
export type EcnUrgency = typeof ECN_URGENCIES[number]

/**
 * ECN 详情 VO
 */
export interface EcnChangeVO {
  id: number
  ecnNumber: string
  title: string
  changeType: EcnChangeType
  urgency: EcnUrgency
  reason: string
  description: string
  impactAnalysis?: string
  affectedBomIds?: string
  requesterUserid: string
  requesterName?: string
  projectId?: number
  status: EcnStatus
  priority?: number
  targetDate?: string
  completedAt?: string
  processInstanceId?: string
  createdAt: string
  updatedAt: string
}

export interface EcnApprovalVO {
  id: number
  ecnId: number
  approverUserid: string
  approverName?: string
  department?: string
  role?: string
  stepOrder: number
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'SKIPPED'
  comment?: string
  signatureUrl?: string
  signedAt?: string
  taskId?: string
  createdAt: string
}

export interface CreateEcnRequest {
  title: string
  changeType: EcnChangeType
  urgency?: EcnUrgency
  reason: string
  description: string
  impactAnalysis?: string
  affectedBomIds?: string
  projectId?: number
  priority?: number
  targetDate?: string
}

export interface EcnQuery {
  status?: EcnStatus
  changeType?: EcnChangeType
  urgency?: EcnUrgency
  requesterUserid?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// ========== CRUD ==========

export function listEcn(params: EcnQuery) {
  return request<PageResult<EcnChangeVO>>({ url: '/ecn', method: 'get', params })
}

export function getEcn(id: number) {
  return request<EcnChangeVO>({ url: `/ecn/${id}`, method: 'get' })
}

export function listEcnApprovals(id: number) {
  return request<EcnApprovalVO[]>({ url: `/ecn/${id}/approvals`, method: 'get' })
}

export function createEcn(data: CreateEcnRequest) {
  return request<EcnChangeVO>({ url: '/ecn', method: 'post', data })
}

export function updateEcn(id: number, data: CreateEcnRequest) {
  return request<EcnChangeVO>({ url: `/ecn/${id}`, method: 'put', data })
}

export function submitEcn(id: number) {
  return request<EcnChangeVO>({ url: `/ecn/${id}/submit`, method: 'post' })
}

export function cancelEcn(id: number) {
  return request<EcnChangeVO>({ url: `/ecn/${id}/cancel`, method: 'post' })
}

// ========== Flowable Task 操作 ==========

/**
 * 当前用户待办（assignee 命中）
 */
export function listMyPendingEcnTasks() {
  return request<EcnChangeVO[]>({ url: '/ecn/tasks/my-pending', method: 'get' })
}

/**
 * claim 任务
 */
export function claimEcnTask(taskId: string) {
  return request<void>({ url: `/ecn/tasks/${taskId}/claim`, method: 'post' })
}

/**
 * 完成审批
 */
export function completeEcnTask(taskId: string, approved: boolean, comment?: string) {
  return request<void>({
    url: `/ecn/tasks/${taskId}/complete`,
    method: 'post',
    data: { approved, comment }
  })
}
