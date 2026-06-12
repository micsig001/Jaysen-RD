<template>
  <div class="project-detail-container">
    <el-button :icon="ArrowLeft" @click="goBack" plain class="back-btn">返回列表</el-button>

    <el-card v-loading="loading" shadow="never" class="info-card" v-if="project">
      <template #header>
        <div class="info-header">
          <h2>
            <el-tag :type="getTypeTagType(project.type) as any" size="large" effect="dark" class="type-tag">
              {{ getTypeLabel(project.type) }}
            </el-tag>
            <span class="project-name">{{ project.name }}</span>
            <el-tag :type="getStatusTagType(project.status) as any" size="large" class="status-tag">
              {{ getStatusLabel(project.status) }}
            </el-tag>
          </h2>
          <p class="project-code">项目编号：{{ project.code }}</p>
        </div>
      </template>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="项目阶段">
          <el-tag v-if="project.phase" effect="plain">{{ project.phase }}</el-tag>
          <span v-else class="muted">未指定</span>
        </el-descriptions-item>
        <el-descriptions-item label="负责人">
          <span v-if="project.managerName">{{ project.managerName }}</span>
          <code v-else class="userid-code">{{ project.managerUserid }}</code>
        </el-descriptions-item>
        <el-descriptions-item label="创建人">
          <span v-if="project.createdByName">{{ project.createdByName }}</span>
          <code v-else class="userid-code">{{ project.createdBy }}</code>
        </el-descriptions-item>
        <el-descriptions-item label="计划开始">
          {{ project.startDate || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="计划结束">
          {{ project.endDate || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="实际起止">
          {{ project.actualStartDate || '?' }} ~ {{ project.actualEndDate || '?' }}
        </el-descriptions-item>
        <el-descriptions-item label="整体进度" :span="3">
          <el-progress
            :percentage="Number(project.progress || 0)"
            :status="project.progress >= 100 ? 'success' : ''"
            style="max-width: 480px"
          />
        </el-descriptions-item>
        <el-descriptions-item v-if="project.description" label="项目描述" :span="3">
          {{ project.description }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 里程碑 -->
    <el-card shadow="never" class="milestone-card">
      <template #header>
        <div class="card-header">
          <h3>里程碑 ({{ milestones.length }})</h3>
          <el-button type="primary" :icon="Plus" @click="openCreateMilestoneDialog">新建里程碑</el-button>
        </div>
      </template>

      <el-empty v-if="!loading && milestones.length === 0" description="还没有里程碑" />

      <el-timeline v-else>
        <el-timeline-item
          v-for="m in milestones"
          :key="m.id"
          :timestamp="`${m.plannedStart} ~ ${m.plannedEnd}`"
          :type="getMilestoneType(m.status)"
          :hollow="m.status === 'NOT_STARTED'"
        >
          <div class="milestone-row">
            <div class="milestone-info">
              <el-tag size="small" effect="plain" class="phase-tag">{{ m.phase }}</el-tag>
              <span class="milestone-name">{{ m.name }}</span>
              <el-tag size="small" :type="getMilestoneStatusType(m.status) as any">
                {{ getMilestoneStatusLabel(m.status) }}
              </el-tag>
              <el-progress
                :percentage="Number(m.progress || 0)"
                :show-text="false"
                style="width: 120px; margin-left: 12px; display: inline-block; vertical-align: middle"
              />
              <span class="progress-text">{{ Number(m.progress || 0).toFixed(0) }}%</span>
              <span v-if="m.ownerName" class="owner-text">
                <el-icon><User /></el-icon> {{ m.ownerName }}
              </span>
              <span v-else-if="m.ownerUserid" class="owner-text">
                <el-icon><User /></el-icon> <code class="userid-code">{{ m.ownerUserid }}</code>
              </span>
            </div>
            <el-button size="small" type="danger" link @click="handleDeleteMilestone(m)">
              删除
            </el-button>
          </div>
          <p v-if="m.description" class="milestone-desc">{{ m.description }}</p>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <!-- 新建里程碑对话框 -->
    <el-dialog v-model="milestoneDialogVisible" title="新建里程碑" width="520px" :close-on-click-modal="false">
      <el-form ref="milestoneFormRef" :model="milestoneForm" :rules="milestoneFormRules" label-width="100px">
        <el-form-item label="里程碑名称" prop="name">
          <el-input v-model="milestoneForm.name" placeholder="如 EVT-样机评审" />
        </el-form-item>
        <el-form-item label="阶段" prop="phase">
          <el-select v-model="milestoneForm.phase" placeholder="选择阶段" style="width: 100%">
            <el-option
              v-for="opt in PROJECT_PHASES"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="计划开始" prop="plannedStart">
          <el-date-picker v-model="milestoneForm.plannedStart" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计划结束" prop="plannedEnd">
          <el-date-picker v-model="milestoneForm.plannedEnd" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="责任人">
          <el-input v-model="milestoneForm.ownerUserid" placeholder="企微 UserID" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="milestoneForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="milestoneDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreateMilestone">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft, Plus, User } from '@element-plus/icons-vue'
import {
  getProject,
  listMilestones,
  createMilestone,
  deleteMilestone,
  PROJECT_PHASES,
  PROJECT_STATUSES,
  type ProjectVO,
  type MilestoneVO
} from '@/api/project'

const router = useRouter()
const route = useRoute()

const projectId = computed(() => Number(route.params.id))
const project = ref<ProjectVO | null>(null)
const milestones = ref<MilestoneVO[]>([])
const loading = ref(false)
const submitting = ref(false)

const milestoneDialogVisible = ref(false)
const milestoneFormRef = ref<FormInstance>()
const milestoneForm = reactive<Partial<MilestoneVO>>({
  name: '',
  phase: 'EVT',
  plannedStart: '',
  plannedEnd: '',
  ownerUserid: '',
  description: ''
})
const milestoneFormRules: FormRules = {
  name: [{ required: true, message: '请输入里程碑名称', trigger: 'blur' }],
  phase: [{ required: true, message: '请选择阶段', trigger: 'change' }],
  plannedStart: [{ required: true, message: '请选择计划开始日期', trigger: 'change' }],
  plannedEnd: [{ required: true, message: '请选择计划结束日期', trigger: 'change' }]
}

const TYPE_TAG_TYPE: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
  HARDWARE: '',
  FIRMWARE: 'success',
  SOFTWARE: 'warning',
  MIXED: 'info'
}
const STATUS_TAG_TYPE: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
  PLANNING: 'info',
  IN_PROGRESS: 'success',
  ON_HOLD: 'warning',
  COMPLETED: 'success',
  CANCELLED: 'info'
}
const MILESTONE_STATUS_TYPE: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
  NOT_STARTED: 'info',
  IN_PROGRESS: 'primary' as any,
  COMPLETED: 'success',
  DELAYED: 'danger'
}
const MILESTONE_TIMELINE_TYPE: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
  NOT_STARTED: 'info',
  IN_PROGRESS: 'primary',
  COMPLETED: 'success',
  DELAYED: 'danger'
}

function getTypeLabel(type?: string) {
  const opt = [
    { value: 'HARDWARE', label: '硬件' },
    { value: 'FIRMWARE', label: '固件' },
    { value: 'SOFTWARE', label: '上位机软件' },
    { value: 'MIXED', label: '混合' }
  ].find((t) => t.value === type)
  return opt?.label ?? type ?? '-'
}
function getTypeTagType(type?: string) {
  return (type && TYPE_TAG_TYPE[type]) ?? ''
}
function getStatusLabel(status?: string) {
  return PROJECT_STATUSES.find((s) => s.value === status)?.label ?? status ?? '-'
}
function getStatusTagType(status?: string) {
  return (status && STATUS_TAG_TYPE[status]) ?? ''
}
function getMilestoneStatusLabel(status?: string) {
  return (
    {
      NOT_STARTED: '未开始',
      IN_PROGRESS: '进行中',
      COMPLETED: '已完成',
      DELAYED: '延期'
    }[status ?? ''] ?? status ?? '-'
  )
}
function getMilestoneStatusType(status?: string) {
  return (status && MILESTONE_STATUS_TYPE[status]) ?? ''
}
function getMilestoneType(status?: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  const t = (status && MILESTONE_TIMELINE_TYPE[status]) as 'primary' | 'success' | 'warning' | 'danger' | 'info' | undefined
  return t ?? 'info'
}

const loadProject = async () => {
  loading.value = true
  try {
    project.value = (await getProject(projectId.value)) as any
  } catch (e: any) {
    ElMessage.error('加载项目详情失败: ' + (e?.message || ''))
  } finally {
    loading.value = false
  }
}

const loadMilestones = async () => {
  try {
    milestones.value = (await listMilestones(projectId.value)) as any
  } catch (e: any) {
    ElMessage.error('加载里程碑失败: ' + (e?.message || ''))
  }
}

const goBack = () => {
  router.push({ name: 'RdProjects' })
}

const openCreateMilestoneDialog = () => {
  Object.assign(milestoneForm, {
    name: '',
    phase: 'EVT',
    plannedStart: '',
    plannedEnd: '',
    ownerUserid: '',
    description: ''
  })
  milestoneDialogVisible.value = true
  milestoneFormRef.value?.clearValidate()
}

const handleCreateMilestone = async () => {
  await milestoneFormRef.value?.validate().catch(() => Promise.reject())
  submitting.value = true
  try {
    await createMilestone(projectId.value, milestoneForm)
    ElMessage.success('里程碑已创建')
    milestoneDialogVisible.value = false
    await loadMilestones()
  } catch (e: any) {
    ElMessage.error('创建失败: ' + (e?.message || ''))
  } finally {
    submitting.value = false
  }
}

const handleDeleteMilestone = async (m: MilestoneVO) => {
  try {
    await ElMessageBox.confirm(
      `确定删除里程碑「${m.name}」？`,
      '删除里程碑',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteMilestone(projectId.value, m.id)
    ElMessage.success('已删除')
    await loadMilestones()
  } catch (e: any) {
    ElMessage.error('删除失败: ' + (e?.message || ''))
  }
}

onMounted(async () => {
  await loadProject()
  await loadMilestones()
})
</script>

<style scoped>
.project-detail-container {
  max-width: 1280px;
  margin: 0 auto;
}

.back-btn {
  margin-bottom: 12px;
}

.info-card,
.milestone-card {
  border-radius: 8px;
  margin-bottom: 16px;
}

.info-header h2 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.type-tag {
  font-weight: 600;
}

.project-name {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.status-tag {
  margin-left: auto;
}

.project-code {
  margin: 8px 0 0 0;
  color: #909399;
  font-size: 13px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.muted {
  color: #c0c4cc;
}

.userid-code {
  background: #f5f7fa;
  padding: 1px 6px;
  border-radius: 3px;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
  color: #d63384;
}

.milestone-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.milestone-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.phase-tag {
  font-family: 'SFMono-Regular', Consolas, monospace;
}

.milestone-name {
  font-weight: 600;
  color: #303133;
}

.progress-text {
  font-size: 12px;
  color: #606266;
  min-width: 32px;
}

.owner-text {
  font-size: 12px;
  color: #909399;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: 4px;
}

.milestone-desc {
  margin: 6px 0 0 0;
  color: #606266;
  font-size: 13px;
}
</style>
