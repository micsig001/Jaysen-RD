<template>
  <div class="defect-container">
    <!-- ========== 顶部统计卡片 ========== -->
    <el-row :gutter="12" class="stats-row">
      <el-col :xs="12" :sm="6" :md="3">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">总缺陷数</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="3">
        <el-card shadow="hover" class="stat-card open">
          <div class="stat-value">{{ stats.open }}</div>
          <div class="stat-label">未关闭</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="3">
        <el-card shadow="hover" class="stat-card critical">
          <div class="stat-value">{{ stats.criticalCount }}</div>
          <div class="stat-label">致命</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="3">
        <el-card shadow="hover" class="stat-card major">
          <div class="stat-value">{{ stats.majorCount }}</div>
          <div class="stat-label">严重</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="6">
        <el-card shadow="hover" class="stat-card trend">
          <div class="stat-trend">
            <div>
              <div class="stat-value-sm">{{ stats.openedThisWeek }}</div>
              <div class="stat-label">本周新增</div>
            </div>
            <div>
              <div class="stat-value-sm">{{ stats.closedThisWeek }}</div>
              <div class="stat-label">本周关闭</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ========== 过滤区 ========== -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="项目">
          <el-select
            v-model="query.projectId"
            placeholder="全部项目"
            clearable
            filterable
            style="width: 200px"
          >
            <el-option
              v-for="p in projectList"
              :key="p.id"
              :label="p.name"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            placeholder="全部"
            clearable
            style="width: 140px"
          >
            <el-option
              v-for="s in DEFECT_STATUSES"
              :key="s"
              :label="STATUS_LABELS[s]"
              :value="s"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="严重度">
          <el-select
            v-model="query.severity"
            placeholder="全部"
            clearable
            style="width: 120px"
          >
            <el-option
              v-for="s in DEFECT_SEVERITIES"
              :key="s"
              :label="SEVERITY_LABELS[s]"
              :value="s"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="缺陷编号 / 标题"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <el-button type="success" :icon="Plus" @click="handleCreate">新建缺陷</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ========== 表格 ========== -->
    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="loading"
        :data="defectList"
        stripe
        border
        :header-cell-style="{ background: '#f5f7fa', color: '#303133' }"
        empty-text="暂无缺陷"
      >
        <el-table-column prop="defectNumber" label="编号" width="170" />
        <el-table-column label="标题" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="handleView(row)">
              {{ row.title }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="严重度" width="100">
          <template #default="{ row }">
            <el-tag :type="SEVERITY_TAG_TYPE[row.severity]" size="small">
              {{ SEVERITY_LABELS[row.severity] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG_TYPE[row.status]" size="small">
              {{ STATUS_LABELS[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="阶段" width="90">
          <template #default="{ row }">
            <span v-if="row.phaseFound" class="phase-tag">{{ row.phaseFound }}</span>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="报告人" width="100">
          <template #default="{ row }">
            {{ row.reporterName || row.reporterUserid }}
          </template>
        </el-table-column>
        <el-table-column label="处理人" width="100">
          <template #default="{ row }">
            <span v-if="row.assigneeName">{{ row.assigneeName }}</span>
            <span v-else class="muted">未指派</span>
          </template>
        </el-table-column>
        <el-table-column label="项目" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.projectName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="发现日期" width="120">
          <template #default="{ row }">
            {{ row.foundDate }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)">详情</el-button>
            <el-dropdown @command="(cmd: string) => handleCommand(cmd, row)" size="small">
              <el-button link type="primary" size="small">
                操作<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="next in ALLOWED_DEFECT_TRANSITIONS[row.status] || []"
                    :key="next"
                    :command="`transition:${next}`"
                  >
                    → {{ STATUS_LABELS[next] }}
                  </el-dropdown-item>
                  <el-dropdown-item command="edit" divided>编辑</el-dropdown-item>
                  <el-dropdown-item command="delete">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- ========== 创建/编辑缺陷对话框 ========== -->
    <el-dialog
      v-model="editDialogVisible"
      :title="editingId ? '编辑缺陷' : '新建缺陷'"
      width="640px"
      :close-on-click-modal="false"
      @close="resetEditForm"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="100px"
        label-position="right"
      >
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="editForm.title"
            placeholder="简述缺陷现象"
            maxlength="512"
            show-word-limit
          />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="严重度" prop="severity">
              <el-select v-model="editForm.severity" placeholder="请选择" style="width: 100%">
                <el-option
                  v-for="s in DEFECT_SEVERITIES"
                  :key="s"
                  :label="SEVERITY_LABELS[s]"
                  :value="s"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-select v-model="editForm.priority" placeholder="请选择" style="width: 100%">
                <el-option
                  v-for="p in DEFECT_PRIORITIES"
                  :key="p"
                  :label="PRIORITY_LABELS[p]"
                  :value="p"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="项目" prop="projectId">
              <el-select
                v-model="editForm.projectId"
                placeholder="选择项目"
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="p in projectList"
                  :key="p.id"
                  :label="p.name"
                  :value="p.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="阶段" prop="phaseFound">
              <el-select v-model="editForm.phaseFound" placeholder="发现阶段" clearable style="width: 100%">
                <el-option
                  v-for="ph in DEFECT_PHASES"
                  :key="ph"
                  :label="PHASE_LABELS[ph]"
                  :value="ph"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="处理人" prop="assigneeUserid">
              <el-input
                v-model="editForm.assigneeUserid"
                placeholder="企微 UserID（可空）"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="验证人" prop="verifierUserid">
              <el-input
                v-model="editForm.verifierUserid"
                placeholder="企微 UserID（可空）"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="发现日期" prop="foundDate">
          <el-date-picker
            v-model="editForm.foundDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="根本原因" prop="rootCause">
          <el-input
            v-model="editForm.rootCause"
            type="textarea"
            :rows="2"
            placeholder="可选, 分析后的根本原因"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="纠正措施" prop="correctiveAction">
          <el-input
            v-model="editForm.correctiveAction"
            type="textarea"
            :rows="2"
            placeholder="可选, 已采取的纠正措施"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="预防措施" prop="preventiveAction">
          <el-input
            v-model="editForm.preventiveAction"
            type="textarea"
            :rows="2"
            placeholder="可选, 防止复发的预防措施"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="handleEditSubmit">
          {{ editingId ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ========== 详情抽屉 ========== -->
    <el-drawer
      v-model="detailDrawerVisible"
      :title="detailDefect ? `${detailDefect.defectNumber} - ${detailDefect.title}` : '缺陷详情'"
      direction="rtl"
      size="600px"
    >
      <div v-if="detailDefect" class="detail-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="缺陷编号">
            {{ detailDefect.defectNumber }}
          </el-descriptions-item>
          <el-descriptions-item label="严重度">
            <el-tag :type="SEVERITY_TAG_TYPE[detailDefect.severity]" size="small">
              {{ SEVERITY_LABELS[detailDefect.severity] }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="优先级">
            {{ PRIORITY_LABELS[detailDefect.priority] }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="STATUS_TAG_TYPE[detailDefect.status]" size="small">
              {{ STATUS_LABELS[detailDefect.status] }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="detailDefect.phaseFound" label="发现阶段">
            {{ PHASE_LABELS[detailDefect.phaseFound] }}
          </el-descriptions-item>
          <el-descriptions-item label="项目">
            {{ detailDefect.projectName }} (#{{ detailDefect.projectId }})
          </el-descriptions-item>
          <el-descriptions-item v-if="detailDefect.sprintTaskTitle" label="看板任务">
            #{{ detailDefect.sprintTaskId }} - {{ detailDefect.sprintTaskTitle }}
          </el-descriptions-item>
          <el-descriptions-item label="报告人">
            {{ detailDefect.reporterName || detailDefect.reporterUserid }}
          </el-descriptions-item>
          <el-descriptions-item label="处理人">
            {{ detailDefect.assigneeName || detailDefect.assigneeUserid || '未指派' }}
          </el-descriptions-item>
          <el-descriptions-item label="验证人">
            {{ detailDefect.verifierName || detailDefect.verifierUserid || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="发现日期">
            {{ detailDefect.foundDate }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detailDefect.resolvedDate" label="修复日期">
            {{ detailDefect.resolvedDate }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detailDefect.verifiedDate" label="验证日期">
            {{ detailDefect.verifiedDate }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detailDefect.closedDate" label="关闭日期">
            {{ detailDefect.closedDate }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detailDefect.rootCause" label="根本原因">
            <pre class="text-pre">{{ detailDefect.rootCause }}</pre>
          </el-descriptions-item>
          <el-descriptions-item v-if="detailDefect.correctiveAction" label="纠正措施">
            <pre class="text-pre">{{ detailDefect.correctiveAction }}</pre>
          </el-descriptions-item>
          <el-descriptions-item v-if="detailDefect.preventiveAction" label="预防措施">
            <pre class="text-pre">{{ detailDefect.preventiveAction }}</pre>
          </el-descriptions-item>
        </el-descriptions>

        <el-alert
          v-if="(ALLOWED_DEFECT_TRANSITIONS[detailDefect.status] || []).length"
          class="transition-hint"
          type="info"
          :closable="false"
        >
          <template #title>
            可流转至:
            <el-tag
              v-for="next in ALLOWED_DEFECT_TRANSITIONS[detailDefect.status]"
              :key="next"
              :type="STATUS_TAG_TYPE[next]"
              size="small"
              class="transition-tag"
              @click="handleTransitionFromDrawer(next)"
            >
              {{ STATUS_LABELS[next] }}
            </el-tag>
          </template>
        </el-alert>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, ArrowDown } from '@element-plus/icons-vue'
import {
  listDefects,
  getDefect,
  createDefect,
  updateDefect,
  deleteDefect,
  transitionDefect,
  getDefectStats,
  DEFECT_SEVERITIES,
  DEFECT_PRIORITIES,
  DEFECT_STATUSES,
  DEFECT_PHASES,
  ALLOWED_DEFECT_TRANSITIONS,
  SEVERITY_TAG_TYPE,
  STATUS_TAG_TYPE,
  STATUS_LABELS,
  SEVERITY_LABELS,
  PRIORITY_LABELS,
  PHASE_LABELS,
  type DefectVO,
  type DefectStatsVO,
  type DefectStatus,
  type DefectSeverity,
  type DefectPriority,
  type DefectPhase,
  type CreateDefectRequest,
  type UpdateDefectRequest
} from '@/api/defect'
import { listProjects, type ProjectVO } from '@/api/project'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// ============== 状态 ==============

const loading = ref(false)
const defectList = ref<DefectVO[]>([])
const projectList = ref<ProjectVO[]>([])
const stats = ref<DefectStatsVO>({
  total: 0, open: 0,
  criticalCount: 0, majorCount: 0, minorCount: 0, trivialCount: 0,
  newCount: 0, analyzingCount: 0, fixInProgressCount: 0,
  fixedCount: 0, verifiedCount: 0, closedCount: 0, reopenedCount: 0,
  openedThisWeek: 0, closedThisWeek: 0
})

const query = reactive({
  projectId: undefined as number | undefined,
  status: undefined as DefectStatus | undefined,
  severity: undefined as DefectSeverity | undefined,
  keyword: ''
})

// ============== 编辑对话框 ==============

const editDialogVisible = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

interface EditFormShape {
  title: string
  severity: DefectSeverity | ''
  priority: DefectPriority
  phaseFound: DefectPhase | undefined
  projectId: number | undefined
  foundDate: string
  assigneeUserid: string
  verifierUserid: string
  rootCause: string
  correctiveAction: string
  preventiveAction: string
}

const editForm = reactive<EditFormShape>({
  title: '',
  severity: 'MAJOR',
  priority: 'MEDIUM',
  phaseFound: undefined,
  projectId: undefined,
  foundDate: new Date().toISOString().slice(0, 10),
  assigneeUserid: '',
  verifierUserid: '',
  rootCause: '',
  correctiveAction: '',
  preventiveAction: ''
})

const editRules: FormRules = {
  title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
  severity: [{ required: true, message: '请选择严重度', trigger: 'change' }],
  projectId: [{ required: true, message: '请选择项目', trigger: 'change' }],
  foundDate: [{ required: true, message: '请选择发现日期', trigger: 'change' }]
}

// ============== 详情抽屉 ==============

const detailDrawerVisible = ref(false)
const detailDefect = ref<DefectVO | null>(null)

const currentUserId = computed(() => userStore.userInfo?.userId || '')

// ============== 数据加载 ==============

async function loadData() {
  loading.value = true
  try {
    const res = await listDefects({
      projectId: query.projectId,
      status: query.status,
      severity: query.severity,
      keyword: query.keyword || undefined
    })
    defectList.value = res.data
  } catch (e: any) {
    ElMessage.error(e?.message || '加载缺陷列表失败')
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const res = await getDefectStats(query.projectId)
    stats.value = res.data
  } catch (e) {
    // 静默
  }
}

async function loadProjects() {
  try {
    const res = await listProjects({})
    projectList.value = res.data
  } catch (e) {
    // 静默
  }
}

// ============== 过滤操作 ==============

function handleSearch() {
  loadData()
  loadStats()
}

function handleReset() {
  query.projectId = undefined
  query.status = undefined
  query.severity = undefined
  query.keyword = ''
  handleSearch()
}

// ============== 列表操作 ==============

function handleCreate() {
  editingId.value = null
  resetEditForm()
  editDialogVisible.value = true
}

function handleView(row: DefectVO) {
  detailDefect.value = row
  detailDrawerVisible.value = true
}

async function handleCommand(cmd: string, row: DefectVO) {
  if (cmd === 'edit') {
    handleEdit(row)
  } else if (cmd === 'delete') {
    await handleDelete(row)
  } else if (cmd.startsWith('transition:')) {
    const target = cmd.split(':')[1] as DefectStatus
    await handleTransition(row, target)
  }
}

function handleEdit(row: DefectVO) {
  editingId.value = row.id
  editForm.title = row.title
  editForm.severity = row.severity
  editForm.priority = row.priority
  editForm.phaseFound = row.phaseFound
  editForm.projectId = row.projectId
  editForm.foundDate = row.foundDate
  editForm.assigneeUserid = row.assigneeUserid || ''
  editForm.verifierUserid = row.verifierUserid || ''
  editForm.rootCause = row.rootCause || ''
  editForm.correctiveAction = row.correctiveAction || ''
  editForm.preventiveAction = row.preventiveAction || ''
  editDialogVisible.value = true
}

async function handleDelete(row: DefectVO) {
  try {
    await ElMessageBox.confirm(
      `确认删除缺陷 ${row.defectNumber} 吗？该操作不可恢复。`,
      '删除确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deleteDefect(row.id)
    ElMessage.success('已删除')
    loadData()
    loadStats()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

async function handleTransition(row: DefectVO, target: DefectStatus) {
  try {
    await ElMessageBox.confirm(
      `将缺陷 ${row.defectNumber} 从 ${STATUS_LABELS[row.status]} 流转到 ${STATUS_LABELS[target]}？`,
      '状态流转',
      { type: 'info' }
    )
  } catch {
    return
  }
  try {
    await transitionDefect(row.id, target)
    ElMessage.success(`已流转到 ${STATUS_LABELS[target]}`)
    loadData()
    loadStats()
    if (detailDefect.value?.id === row.id) {
      const res = await getDefect(row.id)
      detailDefect.value = res.data
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '流转失败')
  }
}

async function handleTransitionFromDrawer(target: DefectStatus) {
  if (!detailDefect.value) return
  await handleTransition(detailDefect.value, target)
}

// ============== 表单提交 ==============

async function handleEditSubmit() {
  if (!editFormRef.value) return
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return

  editSubmitting.value = true
  try {
    if (editingId.value) {
      const payload: UpdateDefectRequest = {
        title: editForm.title,
        severity: editForm.severity as DefectSeverity,
        priority: editForm.priority,
        phaseFound: editForm.phaseFound,
        projectId: editForm.projectId,
        foundDate: editForm.foundDate,
        assigneeUserid: editForm.assigneeUserid || undefined,
        verifierUserid: editForm.verifierUserid || undefined,
        rootCause: editForm.rootCause || undefined,
        correctiveAction: editForm.correctiveAction || undefined,
        preventiveAction: editForm.preventiveAction || undefined
      }
      await updateDefect(editingId.value, payload)
      ElMessage.success('已保存')
    } else {
      const payload: CreateDefectRequest = {
        title: editForm.title,
        severity: editForm.severity as DefectSeverity,
        priority: editForm.priority,
        phaseFound: editForm.phaseFound,
        projectId: editForm.projectId!,
        foundDate: editForm.foundDate!,
        assigneeUserid: editForm.assigneeUserid || undefined,
        verifierUserid: editForm.verifierUserid || undefined,
        rootCause: editForm.rootCause || undefined,
        correctiveAction: editForm.correctiveAction || undefined,
        preventiveAction: editForm.preventiveAction || undefined
      }
      await createDefect(payload)
      ElMessage.success('已创建')
    }
    editDialogVisible.value = false
    loadData()
    loadStats()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    editSubmitting.value = false
  }
}

function resetEditForm() {
  editForm.title = ''
  editForm.severity = 'MAJOR'
  editForm.priority = 'MEDIUM'
  editForm.phaseFound = undefined
  editForm.projectId = undefined
  editForm.foundDate = new Date().toISOString().slice(0, 10)
  editForm.assigneeUserid = ''
  editForm.verifierUserid = ''
  editForm.rootCause = ''
  editForm.correctiveAction = ''
  editForm.preventiveAction = ''
  editFormRef.value?.clearValidate()
}

// ============== 初始化 ==============

onMounted(() => {
  loadProjects()
  loadData()
  loadStats()
})
</script>

<style scoped>
.defect-container {
  padding: 16px;
}

.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
  cursor: default;
}

.stat-card .stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

.stat-card .stat-value-sm {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.stat-card .stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.stat-card.open .stat-value { color: #e6a23c; }
.stat-card.critical .stat-value { color: #f56c6c; }
.stat-card.major .stat-value { color: #e6a23c; }

.stat-card.trend .stat-trend {
  display: flex;
  justify-content: space-around;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-form {
  margin-bottom: -18px;
}

.table-card {
  margin-bottom: 16px;
}

.phase-tag {
  display: inline-block;
  padding: 0 6px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 3px;
  font-size: 12px;
}

.muted {
  color: #c0c4cc;
}

.text-pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 13px;
  color: #606266;
}

.detail-content {
  padding: 0 16px;
}

.transition-hint {
  margin-top: 16px;
}

.transition-tag {
  margin-right: 6px;
  cursor: pointer;
}
</style>
