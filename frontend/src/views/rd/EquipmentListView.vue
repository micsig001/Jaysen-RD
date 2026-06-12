<template>
  <div class="equipment-list-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <h3>实验室设备</h3>
          <el-button
            v-if="userStore.isAdmin"
            type="primary"
            :icon="Plus"
            @click="openCreateDialog"
          >
            新增设备
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="资产编号 / 名称 / 型号"
            clearable
            style="width: 220px"
            @keyup.enter="loadEquipments(1)"
            @clear="loadEquipments(1)"
          />
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="query.category" placeholder="全部" clearable style="width: 160px" @change="loadEquipments(1)">
            <el-option
              v-for="opt in EQUIPMENT_CATEGORIES"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px" @change="loadEquipments(1)">
            <el-option
              v-for="opt in EQUIPMENT_STATUSES"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadEquipments(1)">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading"
        :data="equipmentList"
        border
        stripe
        style="width: 100%"
        :empty-text="'暂无设备'"
      >
        <el-table-column prop="assetCode" label="资产编号" width="120" />
        <el-table-column prop="name" label="设备名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="model" label="型号" width="120" show-overflow-tooltip />
        <el-table-column label="类别" width="120">
          <template #default="{ row }">
            {{ getCategoryLabel(row.category) }}
          </template>
        </el-table-column>
        <el-table-column prop="location" label="位置" width="120" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="getStatusType(row.status) as any">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下次校准" width="120">
          <template #default="{ row }">
            <span
              v-if="row.calibrationDueDate"
              :class="{
                'cal-overdue': isCalibrationOverdue(row),
                'cal-fine': !isCalibrationOverdue(row)
              }"
            >
              {{ row.calibrationDueDate }}
            </span>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="goReserve(row)">
              预约
            </el-button>
            <el-button
              v-if="userStore.isAdmin"
              size="small"
              type="warning"
              link
              @click="openEditDialog(row)"
            >
              编辑
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
        @size-change="loadEquipments(1)"
        @current-change="loadEquipments"
      />
    </el-card>

    <!-- 新建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增设备' : '编辑设备'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="资产编号" prop="assetCode">
          <el-input v-model="form.assetCode" placeholder="如 EQ-001" :disabled="dialogMode === 'edit'" />
        </el-form-item>
        <el-form-item label="设备名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类别" prop="category">
          <el-select v-model="form.category" placeholder="选择类别" style="width: 100%">
            <el-option
              v-for="opt in EQUIPMENT_CATEGORIES"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="form.model" />
        </el-form-item>
        <el-form-item label="制造商">
          <el-input v-model="form.manufacturer" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item label="采购日期">
          <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="校准周期(月)">
          <el-input-number v-model="form.calibrationIntervalMonths" :min="0" :max="120" />
        </el-form-item>
        <el-form-item label="下次校准">
          <el-date-picker v-model="form.calibrationDueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.notes" type="textarea" :rows="2" />
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
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  listEquipments,
  createEquipment,
  updateEquipment,
  EQUIPMENT_CATEGORIES,
  EQUIPMENT_STATUSES,
  type LabEquipmentVO
} from '@/api/equipment'

const router = useRouter()
const userStore = useUserStore()

const query = reactive({
  keyword: '',
  category: '' as string,
  status: '' as string,
  pageNum: 1,
  pageSize: 10
})
const loading = ref(false)
const equipmentList = ref<LabEquipmentVO[]>([])
const total = ref(0)

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Partial<LabEquipmentVO> & { id?: number }>({
  id: undefined,
  assetCode: '',
  name: '',
  category: 'SPECTRUM_ANALYZER',
  model: '',
  manufacturer: '',
  location: '',
  purchaseDate: undefined,
  calibrationIntervalMonths: 12,
  calibrationDueDate: undefined,
  notes: ''
})

const formRules: FormRules = {
  assetCode: [{ required: true, message: '请输入资产编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择类别', trigger: 'change' }]
}

function getCategoryLabel(c?: string) {
  return EQUIPMENT_CATEGORIES.find((x) => x.value === c)?.label ?? c ?? '-'
}
function getStatusLabel(s?: string) {
  return EQUIPMENT_STATUSES.find((x) => x.value === s)?.label ?? s ?? '-'
}
function getStatusType(s?: string) {
  return EQUIPMENT_STATUSES.find((x) => x.value === s)?.type ?? ''
}
function isCalibrationOverdue(row: LabEquipmentVO) {
  if (!row.calibrationDueDate) return false
  return new Date(row.calibrationDueDate).getTime() < Date.now()
}

const loadEquipments = async (pageNum?: number) => {
  if (pageNum) query.pageNum = pageNum
  loading.value = true
  try {
    const res: any = await listEquipments(query)
    equipmentList.value = res.records || []
    total.value = res.total || 0
  } catch (e: any) {
    ElMessage.error('加载设备列表失败: ' + (e?.message || ''))
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.keyword = ''
  query.category = ''
  query.status = ''
  loadEquipments(1)
}

const goReserve = (row: LabEquipmentVO) => {
  router.push({ name: 'RdEquipmentCalendar', query: { equipmentId: String(row.id) } })
}

const openCreateDialog = () => {
  dialogMode.value = 'create'
  Object.assign(form, {
    id: undefined,
    assetCode: '',
    name: '',
    category: 'SPECTRUM_ANALYZER',
    model: '',
    manufacturer: '',
    location: '',
    purchaseDate: undefined,
    calibrationIntervalMonths: 12,
    calibrationDueDate: undefined,
    notes: ''
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

const openEditDialog = (row: LabEquipmentVO) => {
  dialogMode.value = 'edit'
  Object.assign(form, {
    id: row.id,
    assetCode: row.assetCode,
    name: row.name,
    category: row.category,
    model: row.model,
    manufacturer: row.manufacturer,
    location: row.location,
    purchaseDate: row.purchaseDate,
    calibrationIntervalMonths: row.calibrationIntervalMonths,
    calibrationDueDate: row.calibrationDueDate,
    notes: row.notes
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

const handleSubmit = async () => {
  await formRef.value?.validate().catch(() => Promise.reject())
  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createEquipment(form)
      ElMessage.success('设备已创建')
    } else {
      await updateEquipment(form.id!, form)
      ElMessage.success('设备已更新')
    }
    dialogVisible.value = false
    await loadEquipments()
  } catch (e: any) {
    ElMessage.error(dialogMode.value === 'create' ? '创建失败: ' : '更新失败: ' + (e?.message || ''))
  } finally {
    submitting.value = false
  }
}

onMounted(() => loadEquipments(1))
</script>

<style scoped>
.equipment-list-container {
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

.muted {
  color: #c0c4cc;
}

.cal-overdue {
  color: #f56c6c;
  font-weight: 500;
}

.cal-fine {
  color: #67c23a;
}
</style>
