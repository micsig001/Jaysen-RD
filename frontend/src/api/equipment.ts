import request from '@/utils/request'

// ============== 枚举常量从 constants 统一导入 ==============
export {
  EQUIPMENT_CATEGORIES,
  EQUIPMENT_STATUSES
} from './constants'

/**
 * 实验室设备 API
 *
 * 后端接口：
 *   GET    /api/equipment                              分页查询
 *   GET    /api/equipment/{id}                         设备详情
 *   POST   /api/equipment                              创建设备（仅 ADMIN）
 *   PUT    /api/equipment/{id}                         更新设备
 *   GET    /api/equipment/{id}/reservations            设备预约列表
 *   POST   /api/equipment/{id}/reservations            创建设备预约
 *   DELETE /api/equipment/reservations/{id}            取消预约
 */

export interface LabEquipmentVO {
  id: number
  assetCode: string
  name: string
  model?: string
  manufacturer?: string
  category:
    | 'SPECTRUM_ANALYZER'
    | 'OSCILLOSCOPE'
    | 'SIGNAL_GENERATOR'
    | 'NETWORK_ANALYZER'
    | 'POWER_METER'
    | 'OTHER'
  location?: string
  purchaseDate?: string
  warrantyExpiry?: string
  calibrationDueDate?: string
  calibrationIntervalMonths?: number
  status:
    | 'AVAILABLE'
    | 'IN_USE'
    | 'MAINTENANCE'
    | 'CALIBRATION_OVERDUE'
    | 'SCRAPPED'
  specifications?: string
  manualUrl?: string
  qrCode?: string
  notes?: string
  createdBy: string
  createdAt: string
  updatedAt: string
}

export interface EquipmentReservationVO {
  id: number
  equipmentId: number
  equipmentName?: string
  equipmentAssetCode?: string
  userId: string
  userName?: string
  projectId?: number
  projectName?: string
  purpose?: string
  startTime: string
  endTime: string
  status:
    | 'PENDING'
    | 'CONFIRMED'
    | 'IN_USE'
    | 'COMPLETED'
    | 'CANCELLED'
    | 'NO_SHOW'
  actualStartTime?: string
  actualEndTime?: string
  notes?: string
  approvedBy?: string
  approvedByName?: string
  approvedAt?: string
  createdAt: string
  updatedAt: string
}

export function listEquipments(params: {
  pageNum?: number
  pageSize?: number
  keyword?: string
  category?: string
  status?: string
}) {
  return request({ url: '/equipment', method: 'get', params })
}

export function getEquipment(id: number) {
  return request({ url: `/equipment/${id}`, method: 'get' })
}

export function createEquipment(data: Partial<LabEquipmentVO>) {
  return request({ url: '/equipment', method: 'post', data })
}

export function updateEquipment(id: number, data: Partial<LabEquipmentVO>) {
  return request({ url: `/equipment/${id}`, method: 'put', data })
}

export function listReservations(
  equipmentId: number,
  from: string,
  to: string
) {
  return request({
    url: `/equipment/${equipmentId}/reservations`,
    method: 'get',
    params: { from, to }
  })
}

export function createReservation(
  equipmentId: number,
  data: {
    startTime: string
    endTime: string
    purpose?: string
    projectId?: number
    notes?: string
  }
) {
  return request({ url: `/equipment/${equipmentId}/reservations`, method: 'post', data })
}

export function cancelReservation(id: number) {
  return request({ url: `/equipment/reservations/${id}`, method: 'delete' })
}
