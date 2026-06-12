/**
 * 业务常量集中地（前端 + 后端对齐）
 *
 * <p>对应后端 {@code com.RD.rd.common.RdConstants}。
 * 数值字面量集中放这里，避免散在多个 API 包装 / 页面里。</p>
 *
 * @author Mavis
 */

// ============== Project 类型 ==============
export const PROJECT_TYPES: Array<{ value: string; label: string }> = [
  { value: 'HARDWARE', label: '硬件' },
  { value: 'FIRMWARE', label: '固件' },
  { value: 'SOFTWARE', label: '上位机软件' },
  { value: 'MIXED', label: '混合' }
]

// ============== Project 阶段 ==============
export const PROJECT_PHASES: Array<{ value: string; label: string }> = [
  { value: 'EVT', label: 'EVT（工程验证）' },
  { value: 'DVT', label: 'DVT（设计验证）' },
  { value: 'PVT', label: 'PVT（生产验证）' },
  { value: 'MP', label: 'MP（量产）' }
]

// ============== Project 状态 ==============
export const PROJECT_STATUSES: Array<{ value: string; label: string; type: string }> = [
  { value: 'PLANNING', label: '规划中', type: 'info' },
  { value: 'IN_PROGRESS', label: '进行中', type: 'success' },
  { value: 'ON_HOLD', label: '挂起', type: 'warning' },
  { value: 'COMPLETED', label: '已完成', type: 'success' },
  { value: 'CANCELLED', label: '已取消', type: 'info' }
]

// ============== Equipment 类别 ==============
export const EQUIPMENT_CATEGORIES: Array<{ value: string; label: string }> = [
  { value: 'SPECTRUM_ANALYZER', label: '频谱分析仪' },
  { value: 'OSCILLOSCOPE', label: '示波器' },
  { value: 'SIGNAL_GENERATOR', label: '信号发生器' },
  { value: 'NETWORK_ANALYZER', label: '网络分析仪' },
  { value: 'POWER_METER', label: '功率计' },
  { value: 'OTHER', label: '其他' }
]

// ============== Equipment 状态 ==============
export const EQUIPMENT_STATUSES: Array<{ value: string; label: string; type: string }> = [
  { value: 'AVAILABLE', label: '可用', type: 'success' },
  { value: 'IN_USE', label: '使用中', type: 'warning' },
  { value: 'MAINTENANCE', label: '维修中', type: 'info' },
  { value: 'CALIBRATION_OVERDUE', label: '校准过期', type: 'danger' },
  { value: 'SCRAPPED', label: '已报废', type: 'info' }
]

// ============== Reservation 状态 ==============
export const RESERVATION_STATUSES: Record<string, { label: string; type: string }> = {
  PENDING: { label: '待确认', type: 'info' },
  CONFIRMED: { label: '已确认', type: 'primary' },
  IN_USE: { label: '使用中', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' },
  NO_SHOW: { label: '未到场', type: 'danger' }
}

// ============== Milestone 状态 ==============
export const MILESTONE_STATUSES: Record<string, { label: string; type: string }> = {
  NOT_STARTED: { label: '未开始', type: 'info' },
  IN_PROGRESS: { label: '进行中', type: 'primary' },
  COMPLETED: { label: '已完成', type: 'success' },
  DELAYED: { label: '延期', type: 'danger' }
}
