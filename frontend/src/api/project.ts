import request from '@/utils/request'

// ============== 枚举常量从 constants 统一导入 ==============
export {
  PROJECT_TYPES,
  PROJECT_PHASES,
  PROJECT_STATUSES
} from './constants'

/**
 * 项目管理 API
 *
 * 后端接口：
 *   GET    /api/projects                        分页查询
 *   GET    /api/projects/{id}                   项目详情
 *   POST   /api/projects                        创建项目（ADMIN/MANAGER）
 *   PUT    /api/projects/{id}                   更新项目
 *   DELETE /api/projects/{id}                   删除项目
 *   GET    /api/projects/{id}/milestones        里程碑列表
 *   POST   /api/projects/{id}/milestones        创建里程碑
 *   DELETE /api/projects/{id}/milestones/{mid}  删除里程碑
 */

export interface ProjectVO {
  id: number
  code: string
  name: string
  type: 'HARDWARE' | 'FIRMWARE' | 'SOFTWARE' | 'MIXED'
  phase?: 'EVT' | 'DVT' | 'PVT' | 'MP'
  managerUserid: string
  managerName?: string
  startDate?: string
  endDate?: string
  actualStartDate?: string
  actualEndDate?: string
  status: 'PLANNING' | 'IN_PROGRESS' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED'
  progress: number
  description?: string
  tags?: string
  createdBy: string
  createdByName?: string
  createdAt: string
  updatedAt: string
}

export interface MilestoneVO {
  id: number
  projectId: number
  name: string
  phase: 'EVT' | 'DVT' | 'PVT' | 'MP'
  plannedStart: string
  plannedEnd: string
  actualStart?: string
  actualEnd?: string
  progress: number
  dependencies?: string
  ownerUserid?: string
  ownerName?: string
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'DELAYED'
  description?: string
  createdAt: string
  updatedAt: string
}

export interface ProjectQuery {
  keyword?: string
  type?: string
  phase?: string
  status?: string
  managerUserid?: string
  pageNum?: number
  pageSize?: number
}

export function listProjects(params: ProjectQuery) {
  return request({ url: '/projects', method: 'get', params })
}

export function getProject(id: number) {
  return request({ url: `/projects/${id}`, method: 'get' })
}

export function createProject(data: Partial<ProjectVO>) {
  return request({ url: '/projects', method: 'post', data })
}

export function updateProject(id: number, data: Partial<ProjectVO>) {
  return request({ url: `/projects/${id}`, method: 'put', data })
}

export function deleteProject(id: number) {
  return request({ url: `/projects/${id}`, method: 'delete' })
}

export function listMilestones(projectId: number) {
  return request({ url: `/projects/${projectId}/milestones`, method: 'get' })
}

export function createMilestone(projectId: number, data: Partial<MilestoneVO>) {
  return request({ url: `/projects/${projectId}/milestones`, method: 'post', data })
}

export function deleteMilestone(projectId: number, milestoneId: number) {
  return request({ url: `/projects/${projectId}/milestones/${milestoneId}`, method: 'delete' })
}
