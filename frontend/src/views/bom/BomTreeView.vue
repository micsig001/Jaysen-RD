<template>
  <div class="bom-container">
    <el-card shadow="never" class="toolbar">
      <div class="toolbar-row">
        <el-select
          v-model="filterProjectId"
          placeholder="选择项目 (可空)"
          clearable
          style="width: 240px"
          @change="loadTopLevelBoms"
        >
          <el-option
            v-for="p in projectOptions"
            :key="p.id"
            :label="`${p.code} - ${p.name}`"
            :value="p.id"
          />
        </el-select>
        <el-button type="primary" :icon="Plus" @click="handleCreateBom">新建 BOM</el-button>
        <el-button :icon="Refresh" @click="loadTopLevelBoms">刷新</el-button>
        <div class="spacer" />
        <span class="muted">{{ topLevelBoms.length }} 个顶层 BOM</span>
      </div>
    </el-card>

    <el-card shadow="never" v-loading="loading" class="content">
      <el-empty v-if="!loading && topLevelBoms.length === 0" description="暂无顶层 BOM" />
      <el-tree
        v-else
        :data="topLevelBoms"
        :props="defaultProps"
        node-key="id"
        default-expand-all
        :expand-on-click-node="false"
        class="bom-tree"
      >
        <template #default="{ node, data }">
          <div class="tree-node-content">
            <span class="tree-label">
              <el-icon><Box /></el-icon>
              <strong>{{ data.bomCode }}</strong>
              <span>{{ data.productName }}</span>
              <el-tag size="small" :type="bomStatusTagType(data.status)">{{ bomStatusLabel(data.status) }}</el-tag>
              <span class="muted">v{{ data.version }}</span>
            </span>
            <span class="tree-actions">
              <el-button link type="primary" size="small" @click="handleViewTree(data)">
                查看多阶
              </el-button>
              <el-button link type="success" size="small" @click="handleCreateItem(data)">
                加物料
              </el-button>
              <el-button link type="warning" size="small" @click="handleCreateChild(data)">
                加子 BOM
              </el-button>
              <el-button
                v-if="data.status === 'DRAFT'"
                link
                type="danger"
                size="small"
                @click="handleDelete(data)"
              >
                删除
              </el-button>
            </span>
          </div>
        </template>
      </el-tree>
    </el-card>

    <!-- ========== BOM 详情 / 多阶树 侧拉抽屉 ========== -->
    <el-drawer
      v-model="treeDrawerVisible"
      :title="treeDrawerTitle"
      direction="rtl"
      size="60%"
      :close-on-click-modal="false"
    >
      <div v-if="bomTreeData" v-loading="treeLoading" class="tree-drawer-body">
        <el-card shadow="never" class="tree-header-card">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="BOM 编号">{{ bomTreeData.header.bomCode }}</el-descriptions-item>
            <el-descriptions-item label="产品名称">{{ bomTreeData.header.productName }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="bomStatusTagType(bomTreeData.header.status)" size="small">
                {{ bomStatusLabel(bomTreeData.header.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="版本">v{{ bomTreeData.header.version }}</el-descriptions-item>
            <el-descriptions-item label="物料行数">{{ bomTreeData.header.itemCount }}</el-descriptions-item>
            <el-descriptions-item label="项目">{{ bomTreeData.header.projectName || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never" class="tree-content-card">
          <template #header>
            <div class="tree-card-header">
              <span>多阶物料树</span>
              <span class="muted">递归展开子 BOM, 循环引用自动截断</span>
            </div>
          </template>
          <el-tree
            :data="[bomTreeData]"
            :props="treeInnerProps"
            node-key="id"
            default-expand-all
            :render-after-expand="false"
            class="inner-tree"
          >
            <template #default="{ data }">
              <!-- 物料行叶子节点 -->
              <div v-if="data.lineNo" class="inner-item">
                <el-tag size="small" effect="plain">第 {{ data.lineNo }} 行</el-tag>
                <strong>{{ data.itemCode }}</strong>
                <span>{{ data.itemName }}</span>
                <span class="muted">{{ data.specification || '' }}</span>
                <span class="quantity">{{ data.quantity }} {{ data.unit }}</span>
                <span v-if="data.unitPrice" class="muted">
                  单价 ¥{{ data.unitPrice }} / 合计 ¥{{ data.totalPrice }}
                </span>
                <span v-if="data.subBom" class="has-sub">
                  <el-icon><Right /></el-icon> 含子 BOM
                </span>
              </div>
              <!-- BOM 头节点 (顶层 root) -->
              <div v-else class="inner-header">
                <el-icon><Box /></el-icon>
                <strong>{{ data.bomCode }}</strong>
                <span>{{ data.productName }}</span>
                <el-tag size="small" :type="bomStatusTagType(data.status)">
                  {{ bomStatusLabel(data.status) }}
                </el-tag>
              </div>
            </template>
          </el-tree>
        </el-card>
      </div>
    </el-drawer>

    <!-- ========== 创建 BOM 对话框 ========== -->
    <el-dialog v-model="createDialogVisible" title="新建 BOM" width="520px">
      <el-form ref="bomFormRef" :model="bomForm" :rules="bomRules" label-width="100px">
        <el-form-item label="BOM 编号" prop="bomCode">
          <el-input v-model="bomForm.bomCode" placeholder="如 BOM-EVT-001" maxlength="64" />
        </el-form-item>
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="bomForm.productName" placeholder="如 频谱分析仪主板" maxlength="256" />
        </el-form-item>
        <el-form-item label="产品型号">
          <el-input v-model="bomForm.productModel" placeholder="如 SA-2000" maxlength="128" />
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="bomForm.version" placeholder="默认 1.0" maxlength="32" />
        </el-form-item>
        <el-form-item label="父 BOM" v-if="bomForm.parentBomId">
          <el-tag>#{{ bomForm.parentBomId }}</el-tag>
        </el-form-item>
        <el-form-item label="项目">
          <el-select v-model="bomForm.projectId" placeholder="可选" clearable style="width: 100%">
            <el-option v-for="p in projectOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="生效日期">
          <el-date-picker v-model="bomForm.effectiveDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreateBomSubmit">创建</el-button>
      </template>
    </el-dialog>

    <!-- ========== 加物料行对话框 ========== -->
    <el-dialog v-model="itemDialogVisible" :title="`BOM #${itemForm.bomId} 新增物料行`" width="560px">
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="100px">
        <el-form-item label="行号" prop="lineNo">
          <el-input-number v-model="itemForm.lineNo" :min="1" :max="9999" />
        </el-form-item>
        <el-form-item label="物料编码" prop="itemCode">
          <el-input v-model="itemForm.itemCode" maxlength="64" />
        </el-form-item>
        <el-form-item label="物料名称" prop="itemName">
          <el-input v-model="itemForm.itemName" maxlength="256" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="itemForm.specification" maxlength="512" />
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="itemForm.quantity" :min="0" :precision="4" :step="1" />
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="itemForm.unit" placeholder="PCS / SET / m" maxlength="32" />
        </el-form-item>
        <el-form-item label="供应商">
          <el-input v-model="itemForm.supplier" maxlength="256" />
        </el-form-item>
        <el-form-item label="单价">
          <el-input-number v-model="itemForm.unitPrice" :min="0" :precision="4" :step="1" />
        </el-form-item>
        <el-form-item label="子 BOM">
          <el-select
            v-model="itemForm.subBomId"
            placeholder="可选: 选一个子 BOM 形成多阶"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="b in topLevelBoms"
              :key="b.id"
              :label="`${b.bomCode} - ${b.productName}`"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="itemCreating" @click="handleCreateItemSubmit">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, Box, Right } from '@element-plus/icons-vue'
import {
  listTopLevelBoms,
  listBomChildren,
  getBomTree,
  createBom,
  deleteBom,
  createBomItem,
  BOM_STATUSES,
  type BomHeaderVO,
  type BomTreeNode,
  type CreateBomHeaderRequest,
  type CreateBomItemRequest
} from '@/api/bom'
import { listProjects, type ProjectVO } from '@/api/project'

type ElTagType = 'primary' | 'success' | 'warning' | 'info' | 'danger' | ''

// ========== 数据 ==========

const loading = ref(false)
const topLevelBoms = ref<BomHeaderVO[]>([])
const projectOptions = ref<ProjectVO[]>([])

// 树相关
const defaultProps = { children: 'children', label: 'productName' }

// 抽屉 + 树数据
const treeDrawerVisible = ref(false)
const treeDrawerTitle = ref('BOM 多阶树')
const treeLoading = ref(false)
const bomTreeData = ref<BomTreeNode | null>(null)
const treeInnerProps = { children: 'items', label: 'labelText' }

// ========== 加载 ==========

async function loadProjects() {
  try {
    const res = await listProjects({ pageSize: 200 })
    projectOptions.value = res.data.records
  } catch (e) {
    projectOptions.value = []
  }
}

async function loadTopLevelBoms() {
  loading.value = true
  try {
    const res = await listTopLevelBoms(
      filterProjectId.value ? { projectId: filterProjectId.value } : undefined
    )
    topLevelBoms.value = res.data
    // 顶层拉完还要拉每个的子节点 (parent_bom_id = id)
    await enrichWithChildren(topLevelBoms.value)
  } catch (e) {
    topLevelBoms.value = []
  } finally {
    loading.value = false
  }
}

async function enrichWithChildren(headers: BomHeaderVO[]) {
  await Promise.all(headers.map(async (h) => {
    try {
      const res = await listBomChildren(h.id)
      ;(h as any).children = res.data
      // 递归 (二层就够了, 避免前端拉爆)
      if ((h as any).children?.length) {
        await enrichWithChildren((h as any).children)
      }
    } catch (e) {
      ;(h as any).children = []
    }
  }))
}

const filterProjectId = ref<number | undefined>()

// ========== 创建 BOM ==========

const createDialogVisible = ref(false)
const createLoading = ref(false)
const bomFormRef = ref<FormInstance>()
const bomForm = reactive<CreateBomHeaderRequest & { parentBomId?: number }>({
  bomCode: '',
  productName: '',
  productModel: '',
  version: '',
  parentBomId: undefined,
  projectId: undefined,
  effectiveDate: undefined
})
const bomRules: FormRules = {
  bomCode: [{ required: true, message: '请输入 BOM 编号', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }]
}

function handleCreateBom() {
  bomForm.bomCode = ''
  bomForm.productName = ''
  bomForm.productModel = ''
  bomForm.version = ''
  bomForm.parentBomId = undefined
  bomForm.projectId = filterProjectId.value
  bomForm.effectiveDate = undefined
  createDialogVisible.value = true
}

function handleCreateChild(parent: BomHeaderVO) {
  bomForm.bomCode = ''
  bomForm.productName = ''
  bomForm.productModel = ''
  bomForm.version = ''
  bomForm.parentBomId = parent.id
  bomForm.projectId = filterProjectId.value
  bomForm.effectiveDate = undefined
  createDialogVisible.value = true
}

async function handleCreateBomSubmit() {
  if (!bomFormRef.value) return
  const valid = await bomFormRef.value.validate().catch(() => false)
  if (!valid) return
  createLoading.value = true
  try {
    const res = await createBom({
      bomCode: bomForm.bomCode,
      productName: bomForm.productName,
      productModel: bomForm.productModel || undefined,
      version: bomForm.version || undefined,
      parentBomId: bomForm.parentBomId,
      projectId: bomForm.projectId,
      effectiveDate: bomForm.effectiveDate
    })
    ElMessage.success(`BOM「${res.data.bomCode}」已创建`)
    createDialogVisible.value = false
    await loadTopLevelBoms()
  } catch (e) { /* ignore */ } finally { createLoading.value = false }
}

async function handleDelete(b: BomHeaderVO) {
  try {
    await ElMessageBox.confirm(
      `确认删除 BOM「${b.bomCode}」? 仅 DRAFT 状态 + 无子 BOM/物料引用时可删。`,
      '删除 BOM',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch { return }
  try {
    await deleteBom(b.id)
    ElMessage.success('已删除')
    await loadTopLevelBoms()
  } catch (e) { /* ignore */ }
}

// ========== 创建物料行 ==========

const itemDialogVisible = ref(false)
const itemCreating = ref(false)
const itemFormRef = ref<FormInstance>()
const itemForm = reactive<{ bomId: number } & CreateBomItemRequest>({
  bomId: 0,
  lineNo: 1,
  itemCode: '',
  itemName: '',
  specification: '',
  quantity: 1,
  unit: 'PCS',
  supplier: '',
  unitPrice: undefined,
  subBomId: undefined
})
const itemRules: FormRules = {
  lineNo: [{ required: true, message: '请输入行号', trigger: 'blur' }],
  itemCode: [{ required: true, message: '请输入物料编码', trigger: 'blur' }],
  itemName: [{ required: true, message: '请输入物料名称', trigger: 'blur' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }]
}

function handleCreateItem(b: BomHeaderVO) {
  itemForm.bomId = b.id
  itemForm.lineNo = 1
  itemForm.itemCode = ''
  itemForm.itemName = ''
  itemForm.specification = ''
  itemForm.quantity = 1
  itemForm.unit = 'PCS'
  itemForm.supplier = ''
  itemForm.unitPrice = undefined
  itemForm.subBomId = undefined
  itemDialogVisible.value = true
}

async function handleCreateItemSubmit() {
  if (!itemFormRef.value) return
  const valid = await itemFormRef.value.validate().catch(() => false)
  if (!valid) return
  itemCreating.value = true
  try {
    await createBomItem(itemForm.bomId, {
      lineNo: itemForm.lineNo,
      itemCode: itemForm.itemCode,
      itemName: itemForm.itemName,
      specification: itemForm.specification || undefined,
      quantity: itemForm.quantity,
      unit: itemForm.unit,
      supplier: itemForm.supplier || undefined,
      unitPrice: itemForm.unitPrice,
      subBomId: itemForm.subBomId
    })
    ElMessage.success('物料行已创建')
    itemDialogVisible.value = false
  } catch (e) { /* ignore */ } finally { itemCreating.value = false }
}

// ========== 多阶树 抽屉 ==========

async function handleViewTree(b: BomHeaderVO) {
  treeDrawerTitle.value = `BOM 多阶树 - ${b.bomCode} (${b.productName})`
  bomTreeData.value = null
  treeDrawerVisible.value = true
  treeLoading.value = true
  try {
    bomTreeData.value = (await getBomTree(b.id)).data
  } catch (e) {
    bomTreeData.value = null
  } finally {
    treeLoading.value = false
  }
}

// ========== 标签映射 ==========

const BOM_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿', UNDER_REVIEW: '审批中', APPROVED: '已批准', OBSOLETE: '已废弃'
}
const BOM_STATUS_TAG_TYPES: Record<string, ElTagType> = {
  DRAFT: 'info', UNDER_REVIEW: 'warning', APPROVED: 'success', OBSOLETE: ''
}
function bomStatusLabel(s: string) { return BOM_STATUS_LABELS[s] ?? s }
function bomStatusTagType(s: string) { return BOM_STATUS_TAG_TYPES[s] ?? '' }

onMounted(() => {
  loadProjects()
  loadTopLevelBoms()
})
</script>

<style scoped>
.bom-container {
  padding: 0;
}
.toolbar {
  border-radius: 6px;
  margin-bottom: 12px;
}
.toolbar-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.spacer { flex: 1; }
.muted { color: #909399; }
.content {
  border-radius: 6px;
}
.bom-tree {
  background: transparent;
}
.tree-node-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 4px 0;
}
.tree-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}
.tree-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}
.tree-node-content:hover .tree-actions {
  opacity: 1;
}
.tree-drawer-body {
  padding: 0 4px;
}
.tree-header-card,
.tree-content-card {
  border-radius: 6px;
  margin-bottom: 12px;
}
.tree-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.inner-tree {
  background: transparent;
}
.inner-item,
.inner-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  width: 100%;
}
.inner-item {
  padding: 4px 0;
}
.inner-header {
  padding: 4px 0;
  font-weight: 500;
}
.quantity {
  color: #409eff;
  font-weight: 600;
}
.has-sub {
  color: #e6a23c;
  font-size: 12px;
}
</style>
