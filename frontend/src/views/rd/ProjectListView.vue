<template>
  <div class="project-list-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <h3>项目管理</h3>
          <el-button
            v-if="userStore.isAdmin || userStore.isManager"
            type="primary"
            :icon="Plus"
            @click="openCreateDialog"
          >
            新建项目
          </el-button>
        </div>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="项目编号 / 名称"
            clearable
            style="width: 200px"
            @keyup.enter="loadProjects(1)"
            @clear="loadProjects(1)"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 140px" @change="loadProjects(1)">
            <el-option
              v-for="opt in PROJECT_TYPES"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="阶段">
          <el-select v-model="query.phase" placeholder="全部" clearable style="width: 140px" @change="loadProjects(1)">
            <el-option
              v-for="opt in PROJECT_PHASES"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px" @change="loadProjects(1)">
            <el-option
              v-for="opt in PROJECT_STATUSES"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadProjects(1)">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table
        v-loading="loading"
        :data="projectList"
        border
        stripe
        style="width: 100%"
        :empty-text="'暂无项目'"
      >
        <el-table-column prop="code" label="项目编号" width="120" />
        <el-table-column prop="name" label="项目名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="getTypeTagType(row.type)">
              {{ getTypeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="阶段" width="80">
          <template #default="{ row }">
            <span v-if="row.phase" class="phase-text">{{ row.phase }}</span>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="managerName" label="负责人" width="100">
          <template #default="{ row }">
            <span v-if="row.managerName">{{ row.managerName }}</span>
            <span v-else class="muted">{{ row.managerUserid }}</span>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="120">
          <template #default="{ row }">
            <el-progress
              :percentage="Number(row.progress || 0)"
              :status="row.progress >= 100 ? 'success' : ''"
            />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="getStatusTagType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="计划" width="200">
          <template #default="{ row }">
            <span v-if="row.startDate && row.endDate" class="date-text">
              {{ row.startDate }} ~ {{ row.endDate }}
            </span>
            <span v-else class="muted">未排期</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="goDetail(row.id)">
              详情
            </el-button>
            <el-button
              v-if="userStore.isAdmin || userStore.isManager"
              size="small"
              type="warning"
              link
              @click="openEditDialog(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="userStore.isAdmin"
              size="small"
              type="danger"
              link
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        style="margin-top: 16px; text-align: right"
        @size-change="loadProjects(1)"
        @current-change="loadProjects"
      />
    </el-card>

    <!-- 新建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建项目' : '编辑项目'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="项目编号" prop="code">
          <el-input v-model="form.code" placeholder="如 P2024-001" :disabled="dialogMode === 'edit'" />
        </el-form-item>
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="form.name" placeholder="项目名称" />
        </el-form-item>
        <el-form-item label="项目类型" prop="type">
          <el-select v-model="form.type" placeholder="选择类型" style="width: 100%">
            <el-option
              v-for="opt in PROJECT_TYPES"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="硬件阶段" prop="phase">
          <el-select v-model="form.phase" placeholder="仅硬件类项目必填" clearable style="width: 100%">
            <el-option
              v-for="opt in PROJECT_PHASES"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人" prop="managerUserid">
          <el-input v-model="form.managerUserid" placeholder="企微 UserID" />
        </el-form-item>
        <el-form-item label="计划开始">
          <el-date-picker
            v-model="form.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="计划结束">
          <el-date-picker
            v-model="form.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  listProjects,
  createProject,
  updateProject,
  deleteProject,
  PROJECT_TYPES,
  PROJECT_PHASES,
  PROJECT_STATUSES,
  type ProjectVO
} from '@/api/project'

const router = useRouter()
const userStore = useUserStore()

const query = reactive({
  keyword: '',
  type: '' as string,
  phase: '' as string,
  status: '' as string,
  pageNum: 1,
  pageSize: 10
})

const loading = ref(false)
const projectList = ref<ProjectVO[]>([])
const total = ref(0)

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Partial<ProjectVO> & { id?: number }>({
  id: undefined,
  code: '',
  name: '',
  type: 'HARDWARE',
  phase: undefined,
  managerUserid: '',
  startDate: undefined,
  endDate: undefined,
  description: ''
})

const formRules: FormRules = {
  code: [{ required: true, message: '请输入项目编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择项目类型', trigger: 'change' }],
  managerUserid: [{ required: true, message: '请输入负责人 UserID', trigger: 'blur' }]
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

function getTypeLabel(type?: string) {
  return PROJECT_TYPES.find((t) => t.value === type)?.label ?? type ?? '-'
}
function getTypeTagType(type?: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  return (type && TYPE_TAG_TYPE[type]) ?? ''
}
function getStatusLabel(status?: string) {
  return PROJECT_STATUSES.find((s) => s.value === status)?.label ?? status ?? '-'
}
function getStatusTagType(status?: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  return (status && STATUS_TAG_TYPE[status]) ?? ''
}

const loadProjects = async (pageNum?: number) => {
  if (pageNum) query.pageNum = pageNum
  loading.value = true
  try {
    const res: any = await listProjects(query)
    projectList.value = res.records || []
    total.value = res.total || 0
  } catch (e: any) {
    ElMessage.error('加载项目列表失败: ' + (e?.message || ''))
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.keyword = ''
  query.type = ''
  query.phase = ''
  query.status = ''
  loadProjects(1)
}

const goDetail = (id: number) => {
  router.push({ name: 'RdProjectDetail', params: { id: String(id) } })
}

const openCreateDialog = () => {
  dialogMode.value = 'create'
  Object.assign(form, {
    id: undefined,
    code: '',
    name: '',
    type: 'HARDWARE',
    phase: undefined,
    managerUserid: '',
    startDate: undefined,
    endDate: undefined,
    description: ''
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

const openEditDialog = (row: ProjectVO) => {
  dialogMode.value = 'edit'
  Object.assign(form, {
    id: row.id,
    code: row.code,
    name: row.name,
    type: row.type,
    phase: row.phase,
    managerUserid: row.managerUserid,
    startDate: row.startDate,
    endDate: row.endDate,
    description: row.description
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

const handleSubmit = async () => {
  await formRef.value?.validate().catch(() => {
    return Promise.reject()
  })
  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createProject(form)
      ElMessage.success('项目已创建')
    } else {
      await updateProject(form.id!, form)
      ElMessage.success('项目已更新')
    }
    dialogVisible.value = false
    await loadProjects()
  } catch (e: any) {
    ElMessage.error(dialogMode.value === 'create' ? '创建失败: ' : '更新失败: ' + (e?.message || ''))
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row: ProjectVO) => {
  try {
    await ElMessageBox.confirm(
      `确定删除项目「${row.name}」？将同时删除其所有里程碑，且不可恢复。`,
      '删除项目',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteProject(row.id)
    ElMessage.success('已删除')
    await loadProjects()
  } catch (e: any) {
    ElMessage.error('删除失败: ' + (e?.message || ''))
  }
}

onMounted(() => {
  loadProjects(1)
})
</script>

<style scoped>
.project-list-container {
  max-width: 1400px;
  margin: 0 auto;
}

.page-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.filter-form {
  margin-bottom: 16px;
}

.phase-text {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  font-weight: 500;
  color: #409eff;
}

.date-text {
  font-size: 12px;
  color: #606266;
}

.muted {
  color: #c0c4cc;
  font-size: 12px;
}
</style>
