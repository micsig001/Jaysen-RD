import request from '@/utils/request'

/**
 * 研发仪表盘 API
 *
 * 后端接口：
 *   GET /api/dashboard/overview   总览
 *   GET /api/dashboard/tasks      任务
 *   GET /api/dashboard/equipment  设备
 *   GET /api/dashboard/sprint     Sprint
 */

export interface DashboardOverviewVO {
  projectCount: number
  activeProjectCount: number
  taskCount: number
  openTaskCount: number
  defectCount: number
  openDefectCount: number
  activeSprintCount: number
  equipmentCount: number
  availableEquipmentCount: number
  recent7Days: string[]
  taskCreatedTrend: Record<string, number>
  taskCompletedTrend: Record<string, number>
  defectCreatedTrend: Record<string, number>
  defectClosedTrend: Record<string, number>
}

export interface OverdueTask {
  id: number
  taskNo: string
  title: string
  status: string
  priority: string
  assigneeUserid?: string
  assigneeName?: string
  actualDeadline: string
  overdueDays: number
}

export interface AssigneeLoad {
  assigneeUserid: string
  assigneeName?: string
  openCount: number
  overdueCount: number
}

export interface TaskDashboardVO {
  statusDistribution: Record<string, number>
  priorityDistribution: Record<string, number>
  typeDistribution?: Record<string, number>
  recent7Days: string[]
  createdTrend: Record<string, number>
  completedTrend: Record<string, number>
  overdueCount: number
  overdueTop: OverdueTask[]
  assigneeTop: AssigneeLoad[]
}

export interface EquipmentUsage {
  equipmentId: number
  equipmentName: string
  assetCode: string
  category?: string
  dailyCounts: number[]
}

export interface CalibrationDue {
  equipmentId: number
  equipmentName: string
  assetCode: string
  calibrationDueDate: string
  daysUntilDue: number
}

export interface EquipmentDashboardVO {
  statusDistribution: Record<string, number>
  categoryDistribution: Record<string, number>
  recent7Days: string[]
  usage: EquipmentUsage[]
  calibrationDue: CalibrationDue[]
  topUsage: EquipmentUsage[]
}

export interface SprintProgress {
  sprintId: number
  sprintName: string
  projectId: number
  projectName?: string
  totalTasks: number
  doneTasks: number
  totalStoryPoints: number
  doneStoryPoints: number
  startDate?: string
  endDate?: string
  progressPercent: number
}

export interface SprintDashboardVO {
  activeSprintCount: number
  activeSprints: SprintProgress[]
}

export function getOverview() {
  return request<DashboardOverviewVO>({ url: '/dashboard/overview', method: 'get' })
}

export function getTaskDashboard() {
  return request<TaskDashboardVO>({ url: '/dashboard/tasks', method: 'get' })
}

export function getEquipmentDashboard() {
  return request<EquipmentDashboardVO>({ url: '/dashboard/equipment', method: 'get' })
}

export function getSprintDashboard() {
  return request<SprintDashboardVO>({ url: '/dashboard/sprint', method: 'get' })
}

// ============== 枚举辅助（前端展示用） ==============

export const EQUIPMENT_STATUS_LABELS: Record<string, string> = {
  AVAILABLE: '可用',
  IN_USE: '使用中',
  MAINTENANCE: '维修中',
  CALIBRATION_OVERDUE: '校准过期',
  SCRAPPED: '已报废'
}

export const EQUIPMENT_STATUS_COLORS: Record<string, string> = {
  AVAILABLE: '#67c23a',
  IN_USE: '#409eff',
  MAINTENANCE: '#e6a23c',
  CALIBRATION_OVERDUE: '#f56c6c',
  SCRAPPED: '#909399'
}

export const EQUIPMENT_CATEGORY_LABELS: Record<string, string> = {
  SPECTRUM_ANALYZER: '频谱分析仪',
  OSCILLOSCOPE: '示波器',
  SIGNAL_GENERATOR: '信号源',
  NETWORK_ANALYZER: '网络分析仪',
  POWER_METER: '功率计',
  OTHER: '其他'
}

export const TASK_STATUS_LABELS: Record<string, string> = {
  PENDING_ACCEPT: '待接收',
  IN_PROGRESS: '进行中',
  PENDING_VERIFY: '待验收',
  COMPLETED: '已完成',
  REJECTED: '已拒绝',
  WITHDRAWN: '已撤回'
}

export const TASK_STATUS_COLORS: Record<string, string> = {
  PENDING_ACCEPT: '#909399',
  IN_PROGRESS: '#e6a23c',
  PENDING_VERIFY: '#409eff',
  COMPLETED: '#67c23a',
  REJECTED: '#f56c6c',
  WITHDRAWN: '#909399'
}

export const TASK_PRIORITY_LABELS: Record<string, string> = {
  '1': '高',
  '2': '中',
  '3': '低',
  '4': '闲'
}

export const TASK_PRIORITY_COLORS: Record<string, string> = {
  '1': '#f56c6c',
  '2': '#e6a23c',
  '3': '#909399',
  '4': '#c0c4cc'
}
