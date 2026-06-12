import request from '@/utils/request'

/**
 * 缺陷跟踪 API
 *
 * 后端接口：
 *   GET    /api/defects              列表（多条件）
 *   GET    /api/defects/{id}         详情
 *   POST   /api/defects              创建
 *   PUT    /api/defects/{id}         更新
 *   DELETE /api/defects/{id}         软删除
 *   POST   /api/defects/{id}/transition  状态机转移
 *   GET    /api/defects/stats        统计
 *   GET    /api/defects/enums        枚举
 */

// ============== 枚举 ==============

export const DEFECT_SEVERITIES = ['CRITICAL', 'MAJOR', 'MINOR', 'TRIVIAL'] as const
export type DefectSeverity = typeof DEFECT_SEVERITIES[number]

export const DEFECT_PRIORITIES = ['HIGH', 'MEDIUM', 'LOW'] as const
export type DefectPriority = typeof DEFECT_PRIORITIES[number]

export const DEFECT_STATUSES = [
  'NEW',
  'ANALYZING',
  'FIX_IN_PROGRESS',
  'FIXED',
  'VERIFIED',
  'CLOSED',
  'REOPENED'
] as const
export type DefectStatus = typeof DEFECT_STATUSES[number]

export const DEFECT_PHASES = ['EVT', 'DVT', 'PVT', 'MP'] as const
export type DefectPhase = typeof DEFECT_PHASES[number]

/**
 * 状态机允许的转移 (from -> allowed to)
 * 与后端 DefectService.ALLOWED_TRANSITIONS 对齐
 */
export const ALLOWED_DEFECT_TRANSITIONS: Record<DefectStatus, DefectStatus[]> = {
  NEW:             ['ANALYZING', 'FIX_IN_PROGRESS', 'CLOSED'],
  ANALYZING:       ['FIX_IN_PROGRESS', 'CLOSED'],
  FIX_IN_PROGRESS: ['FIXED', 'ANALYZING', 'CLOSED'],
  FIXED:           ['VERIFIED', 'REOPENED'],
  VERIFIED:        ['CLOSED', 'REOPENED'],
  CLOSED:          ['REOPENED'],
  REOPENED:        ['ANALYZING', 'FIX_IN_PROGRESS', 'FIXED', 'CLOSED']
}

// ============== VO / Request ==============

export interface DefectAttachment {
  name: string
  url: string
  size?: number
}

export interface DefectVO {
  id: number
  defectNumber: string
  title: string
  severity: DefectSeverity
  priority: DefectPriority
  status: DefectStatus
  phaseFound?: DefectPhase
  rootCause?: string
  correctiveAction?: string
  preventiveAction?: string
  reporterUserid: string
  reporterName?: string
  assigneeUserid?: string
  assigneeName?: string
  verifierUserid?: string
  verifierName?: string
  projectId: number
  projectName?: string
  sprintTaskId?: number
  sprintTaskTitle?: string
  foundDate: string
  resolvedDate?: string
  verifiedDate?: string
  closedDate?: string
  attachments?: DefectAttachment[]
  createdAt: string
  updatedAt: string
}

export interface CreateDefectRequest {
  title: string
  severity: DefectSeverity
  priority?: DefectPriority
  status?: DefectStatus
  phaseFound?: DefectPhase
  rootCause?: string
  correctiveAction?: string
  preventiveAction?: string
  assigneeUserid?: string
  verifierUserid?: string
  projectId: number
  sprintTaskId?: number
  foundDate: string
  attachments?: DefectAttachment[]
}

export interface UpdateDefectRequest {
  title?: string
  severity?: DefectSeverity
  priority?: DefectPriority
  status?: DefectStatus
  phaseFound?: DefectPhase
  rootCause?: string
  correctiveAction?: string
  preventiveAction?: string
  assigneeUserid?: string
  verifierUserid?: string
  projectId?: number
  sprintTaskId?: number
  foundDate?: string
  resolvedDate?: string
  verifiedDate?: string
  closedDate?: string
  attachments?: DefectAttachment[]
}

export interface DefectStatsVO {
  total: number
  open: number
  criticalCount: number
  majorCount: number
  minorCount: number
  trivialCount: number
  newCount: number
  analyzingCount: number
  fixInProgressCount: number
  fixedCount: number
  verifiedCount: number
  closedCount: number
  reopenedCount: number
  openedThisWeek: number
  closedThisWeek: number
}

export interface DefectEnums {
  severities: DefectSeverity[]
  priorities: DefectPriority[]
  statuses: DefectStatus[]
  phases: DefectPhase[]
}

// ============== 接口 ==============

export interface ListDefectsParams {
  projectId?: number
  status?: DefectStatus
  severity?: DefectSeverity
  assigneeUserid?: string
  reporterUserid?: string
  keyword?: string
}

export function listDefects(params?: ListDefectsParams) {
  return request<DefectVO[]>({ url: '/defects', method: 'get', params })
}

export function getDefect(id: number) {
  return request<DefectVO>({ url: `/defects/${id}`, method: 'get' })
}

export function createDefect(data: CreateDefectRequest) {
  return request<DefectVO>({ url: '/defects', method: 'post', data })
}

export function updateDefect(id: number, data: UpdateDefectRequest) {
  return request<DefectVO>({ url: `/defects/${id}`, method: 'put', data })
}

export function deleteDefect(id: number) {
  return request<void>({ url: `/defects/${id}`, method: 'delete' })
}

export function transitionDefect(id: number, targetStatus: DefectStatus) {
  return request<DefectVO>({
    url: `/defects/${id}/transition`,
    method: 'post',
    data: { targetStatus }
  })
}

export function getDefectStats(projectId?: number) {
  return request<DefectStatsVO>({
    url: '/defects/stats',
    method: 'get',
    params: projectId ? { projectId } : undefined
  })
}

export function getDefectEnums() {
  return request<DefectEnums>({ url: '/defects/enums', method: 'get' })
}

// ============== 工具 ==============

/**
 * 严重度对应 el-tag type
 */
export const SEVERITY_TAG_TYPE: Record<DefectSeverity, '' | 'success' | 'warning' | 'danger' | 'info'> = {
  CRITICAL: 'danger',
  MAJOR:    'warning',
  MINOR:    'info',
  TRIVIAL:  'success'
}

/**
 * 状态对应 el-tag type
 */
export const STATUS_TAG_TYPE: Record<DefectStatus, '' | 'success' | 'warning' | 'danger' | 'info'> = {
  NEW:             'danger',
  ANALYZING:       'warning',
  FIX_IN_PROGRESS: 'warning',
  FIXED:           'info',
  VERIFIED:        'info',
  CLOSED:          'success',
  REOPENED:        'danger'
}

/**
 * 状态中文显示
 */
export const STATUS_LABELS: Record<DefectStatus, string> = {
  NEW:             '新建',
  ANALYZING:       '分析中',
  FIX_IN_PROGRESS: '修复中',
  FIXED:           '已修复',
  VERIFIED:        '已验证',
  CLOSED:          '已关闭',
  REOPENED:        '重开'
}

/**
 * 严重度中文
 */
export const SEVERITY_LABELS: Record<DefectSeverity, string> = {
  CRITICAL: '致命',
  MAJOR:    '严重',
  MINOR:    '一般',
  TRIVIAL:  '轻微'
}

/**
 * 优先级中文
 */
export const PRIORITY_LABELS: Record<DefectPriority, string> = {
  HIGH:   '高',
  MEDIUM: '中',
  LOW:    '低'
}

/**
 * 阶段中文
 */
export const PHASE_LABELS: Record<DefectPhase, string> = {
  EVT: 'EVT 工程验证',
  DVT: 'DVT 设计验证',
  PVT: 'PVT 生产验证',
  MP:  'MP 量产'
}
