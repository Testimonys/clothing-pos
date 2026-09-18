<template>
  <div class="product-container">
    <!-- luohuai codeX  modify: support store article numbers and preview actions; gap spacing keeps wrapped filters separate. -->
    <el-card class="search-card" shadow="never">
      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索名称、货号或条码"
          clearable
          style="width: 240px; max-width: 100%"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-tree-select
          v-model="categoryId"
          :data="categoryTree"
          :props="treeProps"
          placeholder="全部分类"
          clearable
          check-strictly
          style="width: 180px; max-width: 100%"
          @change="handleSearch"
        />

        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>

        <el-button @click="handleReset">重置</el-button>

        <el-button @click="importVisible = true">导入预览</el-button>
        <el-button type="success" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增商品
        </el-button>
      </div>
    </el-card>

    <!-- 商品表格 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="products"
        v-loading="loading"
        stripe
        style="width: 100%"
        row-key="id"
      >
        <!-- luohuai codeX  modify: show the store identifier without replacing database IDs. -->
        <el-table-column label="货号" width="115"><template #default="{ row }">{{ row.productCode || '未设置' }}</template></el-table-column>

        <el-table-column label="图片" width="90" align="center">
          <template #default="{ row }">
            <!-- luohuai codeX  modify: teleport the viewer outside table stacking contexts so rows and fixed columns cannot cover it. -->
            <el-image
              v-if="row.imageUrl"
              :src="row.imageUrl"
              style="width: 50px; height: 50px; border-radius: 4px"
              fit="cover"
              :preview-src-list="[row.imageUrl]"
              :initial-index="0"
              preview-teleported
            />
            <div v-else class="no-image">无图</div>
          </template>
        </el-table-column>

        <el-table-column prop="name" label="商品名称" min-width="160" show-overflow-tooltip />

        <!-- luohuai codeX generate: show the selling unit carried by real catalog records. -->
        <el-table-column prop="unit" label="单位" width="65" />

        <el-table-column label="分类" width="110">
          <template #default="{ row }">
            <span v-if="row.categoryName">{{ row.categoryName }}</span>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>

        <!-- luohuai codeX  modify: use consistent money formatting and distinguish catalog margin from sales profit. -->
        <el-table-column label="成本价" width="100" align="right">
          <template #default="{ row }">
            <span style="color: #909399">{{ money(row.costPrice) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="售价" width="100" align="right">
          <template #default="{ row }">
            <span style="color: #e6a23c; font-weight: 500">
              {{ money(row.sellPrice) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="标价毛利率" width="110" align="right">
          <template #default="{ row }">{{ marginRate(row.costPrice, row.sellPrice) }}</template>
        </el-table-column>
        <!-- luohuai codeX  modify: make each color/size, barcode and stock quantity visible. -->
        <el-table-column label="颜色 / 尺码 · 条码 · 库存" min-width="300">
          <template #default="{ row }">
            <div v-for="sku in row.skus || []" :key="sku.id" class="sku-summary">
              <strong>{{ sku.color || '待补颜色' }} / {{ sku.size || '待补尺码' }}</strong>
              <span>{{ sku.barcode || '未设置条码' }}</span>
              <el-tag size="small" type="info">{{ sku.stockQty ?? 0 }} {{ row.unit || '件' }}</el-tag>
            </div>
            <span v-if="!row.skus?.length">暂无规格</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-popconfirm
              title="确定删除该商品？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button type="danger" link size="small" style="margin-left: 4px">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- luohuai codeX  modify: convert zero-based API paging to the component's one-based page number. -->
      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          :current-page="page + 1"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- luohuai codeX  modify: allow room for explicit color/size combinations and catalog fields. -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑商品' : '新增商品'"
      width="min(1050px, 96vw)"
      :close-on-click-modal="false"
      destroy-on-close
      @opened="handleDialogOpened"
    >
      <el-form
        ref="formRef"
        v-loading="detailLoading"
        :model="form"
        :rules="formRules"
        label-width="90px"
        class="product-form"
      >
        <!-- luohuai codeX generate: article number remains optional for existing products. -->
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="货号" prop="productCode"><el-input v-model="form.productCode" maxlength="50" placeholder="门店货号，如 10001" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="单位" prop="unit"><el-input v-model="form.unit" maxlength="20" placeholder="件" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入商品名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryId">
              <el-tree-select
                v-model="form.categoryId"
                :data="categoryTree"
                :props="treeProps"
                placeholder="选择分类"
                clearable
                check-strictly
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="进价" prop="costPrice">
              <el-input-number
                v-model="form.costPrice"
                :min="0"
                :precision="2"
                style="width: 100%"
                placeholder="进价"
                controls-position="right"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="售价" prop="sellPrice">
              <el-input-number
                v-model="form.sellPrice"
                :min="0"
                :precision="2"
                style="width: 100%"
                placeholder="售价"
                controls-position="right"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 图片上传 -->
        <el-form-item label="商品图片">
          <el-upload
            :action="uploadAction"
            :headers="uploadHeaders"
            :on-success="handleImageSuccess"
            :on-error="handleImageError"
            :on-remove="handleImageRemove"
            :before-upload="checkImageType"
            :file-list="imageList"
            list-type="picture-card"
            :limit="1"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <span class="upload-tip">仅支持 jpg/png/gif，建议尺寸 300x300</span>
        </el-form-item>

        <!-- luohuai codeX  modify: explicitly generate color/size combinations without inferring absent source values. -->
        <el-divider content-position="left">
          <span style="font-weight: 500">规格管理 (SKU)</span>
        </el-divider>

        <el-alert title="同一商品按颜色和尺码分别管理。已有规格的库存只读，库存变化请到库存管理操作。" type="info" :closable="false" show-icon />
        <div class="spec-generator">
          <el-select v-model="batchColors" multiple filterable allow-create default-first-option placeholder="输入颜色后回车，可多选" style="width: 260px"><el-option v-for="color in batchColors" :key="color" :label="color" :value="color" /></el-select>
          <el-select v-model="batchSizes" multiple filterable allow-create default-first-option placeholder="选择或输入尺码" style="width: 260px"><el-option v-for="option in sizeOptions" :key="option.id" :label="option.name" :value="option.name" /></el-select>
          <el-button @click="generateCombinations">生成颜色 × 尺码</el-button>
          <el-button :loading="generatingBarcodes" @click="fillMissingBarcodes">补全空条码</el-button>
        </div>
        <div class="sku-section">
          <div class="sku-column-labels">颜色 / 尺码 / 独立条码 / 库存</div>
          <div
            v-for="(sku, index) in form.skus"
            :key="sku._key"
            class="sku-row"
          >
            <el-input
              v-model="sku.color"
              placeholder="颜色"
              style="width: 120px"
              size="default"
            />
            <el-select
              v-model="sku.size"
              placeholder="尺码"
              style="width: 130px; margin-left: 8px"
              size="default"
              filterable
              allow-create
              default-first-option
              clearable
            >
              <el-option v-for="s in sizeOptions" :key="s.id" :label="s.name" :value="s.name" />
            </el-select>
            <el-input
              v-model="sku.barcode"
              placeholder="条码"
              style="width: 240px; margin-left: 8px"
              size="default"
              :disabled="sku._generatingBarcode"
            >
              <template #append>
                <el-button
                  :loading="sku._generatingBarcode"
                  @click="handleGenerateBarcode(sku)"
                  style="width: 28px; padding: 0"
                >
                  <el-icon><Refresh /></el-icon>
                </el-button>
              </template>
            </el-input>
            <el-input-number
              v-model="sku.stockQty"
              :min="0"
              :disabled="!sku._isNew"
              :title="sku._isNew ? '初始库存' : '现有库存只读，请在库存管理操作'"
              placeholder="库存"
              style="width: 120px; margin-left: 8px"
              size="default"
              controls-position="right"
            />
            <el-button
              type="danger"
              :icon="Delete"
              circle
              size="small"
              style="margin-left: 8px; flex-shrink: 0"
              @click="removeSkuRow(index)"
            />
          </div>

          <el-button type="primary" link @click="addSkuRow">
            <el-icon><Plus /></el-icon>
            添加规格
          </el-button>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="detailLoading || generatingBarcodes || form.skus.some(s => s._generatingBarcode)" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
    <!-- luohuai codeX generate: separate preview flow never writes product or stock data. -->
    <ProductImportPreview v-model="importVisible" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, Edit, Delete, Refresh } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
// luohuai codeX  modify: connect the preview dialog and atomic catalog API.
import ProductImportPreview from '@/components/ProductImportPreview.vue'
import { listSizes, type SizeConfigDTO } from '@/api/size'
import {
  listProducts,
  getProduct,
  createProduct,
  updateProductCatalog,
  deleteProduct as deleteProductApi,
  generateNextBarcode,
  listCategories,
  type ProductDTO,
  type ProductSkuDTO,
  type CategoryDTO
} from '@/api/product'
import type { FormInstance, UploadProps, UploadFile, UploadUserFile } from 'element-plus'

// ---- auth ----
const authStore = useAuthStore()

// ---- 搜索 ----
const keyword = ref('')
const categoryId = ref<number | null>(null)
const categories = ref<CategoryDTO[]>([])

// 树形下拉配置（与分类管理页一致）
const treeProps = {
  children: 'children',
  label: 'name',
  value: 'id'
}

interface TreeNode {
  id: number
  name: string
  parentId: number | null
  sortOrder?: number
  children?: TreeNode[]
}

/** 将扁平分类列表转为树形结构（与分类管理页一致） */
function buildCategoryTree(flatList: CategoryDTO[]): TreeNode[] {
  const map = new Map<number, TreeNode>()
  const roots: TreeNode[] = []

  for (const cat of flatList) {
    if (cat.id == null) continue
    map.set(cat.id, {
      id: cat.id,
      name: cat.name ?? '',
      parentId: cat.parentId ?? null,
      sortOrder: cat.sortOrder ?? 0,
      children: []
    })
  }

  for (const cat of flatList) {
    if (cat.id == null) continue
    const node = map.get(cat.id)!
    if (cat.parentId != null && map.has(cat.parentId)) {
      const parent = map.get(cat.parentId)!
      parent.children!.push(node)
    } else {
      roots.push(node)
    }
  }

  function sortTree(nodes: TreeNode[]) {
    nodes.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
    for (const n of nodes) {
      if (n.children && n.children.length > 0) {
        sortTree(n.children)
      }
    }
  }
  sortTree(roots)

  return roots
}

/** 树形分类（计算属性） */
const categoryTree = computed<TreeNode[]>(() => buildCategoryTree(categories.value))

// ---- 分页 ----
const page = ref(0)
const size = ref(20)
const total = ref(0)

// ---- 表格数据 ----
const products = ref<ProductDTO[]>([])
const loading = ref(false)

// ---- 弹窗 ----
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
// luohuai codeX  modify: the backend derives deleted SKUs within one catalog transaction.
const importVisible = ref(false)
const batchColors = ref<string[]>([])
const batchSizes = ref<string[]>([])
const generatingBarcodes = ref(false)
const saving = ref(false)
// luohuai codeX generate: a failed detail request must never leave an old product form saveable under a new ID.
const detailLoading = ref(false)
const formRef = ref<FormInstance>()

// SKU 扩展类型（前端用）
interface SkuFormItem extends ProductSkuDTO {
  _key: string
  _isNew: boolean
  _generatingBarcode: boolean
}

function createEmptySku(): SkuFormItem {
  return {
    color: '',
    size: '',
    barcode: '',
    stockQty: 0,
    _key: Date.now().toString() + Math.random(),
    _isNew: true,
    _generatingBarcode: false
  }
}

interface ProductForm {
  // luohuai codeX  modify: retain textual article numbers and the selling unit in the complete form lifecycle.
  productCode: string
  unit: string
  name: string
  categoryId: number | null
  costPrice?: number
  sellPrice?: number
  imageUrl: string
  skus: SkuFormItem[]
}

const form = reactive<ProductForm>({
  // luohuai codeX  modify: compatible defaults for legacy products without article numbers.
  productCode: '',
  unit: '件',
  name: '',
  categoryId: null,
  costPrice: undefined,
  sellPrice: undefined,
  imageUrl: '',
  skus: [createEmptySku()]
})

const formRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  sellPrice: [{ required: true, message: '请输入售价', trigger: 'blur' }]
}

// ---- 图片上传 ----
const imageList = ref<UploadUserFile[]>([])
const uploadAction = '/api/upload/image'

const uploadHeaders = computed(() => ({
  Authorization: 'Bearer ' + authStore.token
}))

let lastUploadedUrl = ''

function checkImageType(file: UploadProps['beforeUpload'] extends (...args: any[]) => any
  ? Parameters<UploadProps['beforeUpload']>[0]
  : File
): boolean {
  const validTypes = ['image/jpeg', 'image/png', 'image/gif']
  if (!validTypes.includes((file as File).type)) {
    ElMessage.error('仅支持 jpg/png/gif 格式的图片')
    return false
  }
  const maxSize = 5 * 1024 * 1024 // 5MB
  if ((file as File).size > maxSize) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

function handleImageSuccess(response: { url?: string }) {
  if (response && response.url) {
    lastUploadedUrl = response.url
    form.imageUrl = response.url
  }
}

// luohuai codeX  modify: show controlled COS configuration/upload errors instead of a generic retry message.
function handleImageError(error: Error) {
  let message = '图片上传失败，请重试'
  try {
    const response = JSON.parse(error.message)
    if (typeof response.message === 'string') message = response.message
  } catch { /* Non-JSON transport failures use the fallback message. */ }
  ElMessage.error(message)
}

function handleImageRemove() {
  form.imageUrl = ''
  lastUploadedUrl = ''
}

// ---- 规格行操作 ----
async function addSkuRow() {
  // luohuai codeX  modify: bind the returned barcode to its own row, even when multiple rows are added quickly.
  const added = createEmptySku()
  form.skus.push(added)
  // 自动生成条码（失败则留空，可手动输入或点生成按钮）
  try {
    const result = await generateNextBarcode()
    const current = form.skus.find(s => s._key === added._key)
    if (current && !current.barcode) {
      current.barcode = result.barcode
    }
  } catch {
    // 忽略，用户可手动输入
  }
}

function removeSkuRow(index: number) {
  if (form.skus.length <= 1) {
    ElMessage.warning('至少保留一个规格')
    return
  }
  form.skus.splice(index, 1)
}

// ---- 加载数据 ----
async function loadCategories() {
  try {
    categories.value = await listCategories()
  } catch {
    // 分类加载失败不影响主流程
  }
}

async function loadProducts() {
  loading.value = true
  try {
    const params: {
      keyword?: string
      categoryId?: number
      page: number
      size: number
    } = {
      page: page.value,
      size: size.value
    }
    if (keyword.value) {
      params.keyword = keyword.value
    }
    if (categoryId.value) {
      params.categoryId = categoryId.value
    }

    const result = await listProducts(params)
    products.value = result.content
    total.value = result.totalElements
  } catch {
    ElMessage.error('加载商品列表失败')
  } finally {
    loading.value = false
  }
}

async function loadProductDetail(id: number) {
  // luohuai codeX  modify: guard asynchronous edits so stale detail responses cannot populate another product.
  detailLoading.value = true
  try {
    const detail = await getProduct(id)
    if (editId.value !== id || !dialogVisible.value) return
    // luohuai codeX  modify: populate newly supported business identifiers without changing existing SKU IDs.
    form.productCode = detail.productCode || ''
    form.unit = detail.unit || '件'
    form.name = detail.name || ''
    form.categoryId = detail.categoryId ?? null
    form.costPrice = detail.costPrice
    form.sellPrice = detail.sellPrice
    form.imageUrl = detail.imageUrl || ''

    // 初始化图片列表
    if (detail.imageUrl) {
      lastUploadedUrl = detail.imageUrl
      imageList.value = [{ name: 'image', url: detail.imageUrl } as UploadUserFile]
    } else {
      imageList.value = []
      lastUploadedUrl = ''
    }

    // 初始化 SKU 列表
    if (detail.skus && detail.skus.length > 0) {
      form.skus = detail.skus.map((sku) => ({
        ...sku,
        stockQty: sku.stockQty ?? 0,
        _key: 'existing_' + (sku.id || Date.now()),
        _isNew: false,
        _generatingBarcode: false
      }))
    } else {
      form.skus = [createEmptySku()]
    }

    // luohuai codeX  modify: removed specifications are determined server-side from the submitted complete list.
  } catch {
    ElMessage.error('加载商品详情失败')
    if (editId.value === id) dialogVisible.value = false
  } finally {
    if (editId.value === id) detailLoading.value = false
  }
}

// ---- 搜索/重置 ----
function handleSearch() {
  page.value = 0
  loadProducts()
}

function handleReset() {
  keyword.value = ''
  categoryId.value = null as any
  page.value = 0
  loadProducts()
}

function handlePageChange(p: number) {
  page.value = p - 1 // element-plus 页码从 1 开始，后端从 0 开始
  loadProducts()
}

function handleSizeChange(s: number) {
  size.value = s
  page.value = 0
  loadProducts()
}

// ---- 新增 / 编辑 ----
function handleAdd() {
  // luohuai codeX  modify: clear catalog metadata and batch selectors before starting another product.
  form.productCode = ''
  form.unit = '件'
  batchColors.value = []
  batchSizes.value = []
  detailLoading.value = false
  isEdit.value = false
  editId.value = null
  form.name = ''
  form.categoryId = null
  form.costPrice = undefined
  form.sellPrice = undefined
  form.imageUrl = ''
  form.skus = [createEmptySku()]
  imageList.value = []
  lastUploadedUrl = ''
  dialogVisible.value = true
}

function handleEdit(row: ProductDTO) {
  // luohuai codeX  modify: do not carry one product's batch selections into another.
  batchColors.value = []
  batchSizes.value = []
  detailLoading.value = true
  isEdit.value = true
  editId.value = row.id!
  dialogVisible.value = true
  // 数据在 dialog @opened 时加载
}

function handleDialogOpened() {
  if (isEdit.value && editId.value != null) {
    loadProductDetail(editId.value)
  }
  // 重置表单校验
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

async function handleDelete(row: ProductDTO) {
  try {
    await deleteProductApi(row.id!)
    ElMessage.success('删除成功')
    loadProducts()
  } catch (error: any) {
    // luohuai codeX  modify: explain why a product with stock/history cannot be deleted.
    ElMessage.error(error?.response?.data?.message || '删除失败')
  }
}

// ---- 生成条码 ----
async function handleGenerateBarcode(sku: SkuFormItem) {
  sku._generatingBarcode = true
  try {
    // luohuai codeX  modify: stage barcode changes so cancelling the form does not change a stored barcode.
    const result = await generateNextBarcode()
    sku.barcode = result.barcode
    ElMessage.success('条码生成成功')
  } catch {
    ElMessage.error('条码生成失败')
  } finally {
    sku._generatingBarcode = false
  }
}

// ---- 保存 ----
async function handleSave() {
  // luohuai codeX  modify: prevent overlapping writes or saving before details and generated barcodes are ready.
  if (saving.value || detailLoading.value || generatingBarcodes.value || form.skus.some(s => s._generatingBarcode)) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  // luohuai codeX  modify: validate variants then submit one transaction; never send back existing inventory.
  const validSkus: SkuFormItem[] = form.skus.filter(
    (s) => !s._isNew || s.color?.trim() || s.size?.trim() || s.barcode?.trim()
  )

  if (validSkus.length === 0) {
    ElMessage.warning('请至少填写一个规格')
    return
  }

  if (validSkus.some(s => s._isNew && (!s.color?.trim() || !s.size?.trim() || s.color === '0无' || s.size === '0均码'))) {
    ElMessage.warning('新规格必须填写真实颜色和尺码')
    return
  }
  // luohuai codeX  modify: ensure new variants have independent barcodes before submitting.
  if (validSkus.some(s => s._isNew && !s.barcode?.trim())) { ElMessage.warning('新规格请填写条码，或点击补全空条码'); return }
  const specKeys = validSkus.map(s => `${s.color?.trim().toLowerCase() || ''}\u0000${s.size?.trim().toLowerCase() || ''}`)
  if (new Set(specKeys).size !== specKeys.length) { ElMessage.warning('颜色和尺码组合重复'); return }
  const barcodes = validSkus.map(s => s.barcode?.trim().toLowerCase()).filter(Boolean)
  if (new Set(barcodes).size !== barcodes.length) { ElMessage.warning('不同规格不能共用条码'); return }
  const payload: ProductDTO = {
    productCode: form.productCode.trim() || null,
    unit: form.unit.trim() || '件',
    name: form.name.trim(), categoryId: form.categoryId ?? null,
    costPrice: form.costPrice, sellPrice: form.sellPrice, imageUrl: form.imageUrl || undefined,
    skus: validSkus.map(s => ({
      id: s._isNew ? undefined : s.id,
      color: s.color?.trim() || '', size: s.size?.trim() || '', barcode: s.barcode?.trim() || '',
      ...(s._isNew ? { stockQty: s.stockQty ?? 0 } : {})
    }))
  }

  saving.value = true
  try {
    if (isEdit.value && editId.value != null) {
      await updateProductCatalog(editId.value, payload)
      ElMessage.success('商品及规格已保存')
    } else {
      await createProduct(payload)
      ElMessage.success('商品创建成功')
    }

    dialogVisible.value = false
    loadProducts()
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || '保存失败'
    ElMessage.error(msg)
  } finally {
    saving.value = false
  }
}

// luohuai codeX generate: derive catalog margin without storing duplicate financial fields.
const money = (value?: number) => value == null ? '—' : `¥${value.toFixed(2)}`
function marginRate(cost?: number, sell?: number) {
  return cost == null || sell == null || sell <= 0 ? '—' : `${((sell - cost) / sell * 100).toFixed(2)}%`
}

// luohuai codeX generate: add distinct combinations and retain existing IDs, barcodes and stock.
function generateCombinations() {
  const colors = [...new Set(batchColors.value.map(s => s.trim()).filter(Boolean))]
  const sizes = [...new Set(batchSizes.value.map(s => s.trim()).filter(Boolean))]
  if (!colors.length || !sizes.length) { ElMessage.warning('请先输入颜色并选择尺码'); return }
  if (colors.length * sizes.length + form.skus.length > 500) { ElMessage.warning('单个商品最多500个规格'); return }
  const existing = new Set(form.skus.map(s => `${s.color?.trim().toLowerCase()}\u0000${s.size?.trim().toLowerCase()}`))
  const additions: SkuFormItem[] = []
  for (const color of colors) for (const size of sizes) {
    const key = `${color.toLowerCase()}\u0000${size.toLowerCase()}`
    if (!existing.has(key)) {
      additions.push({ ...createEmptySku(), color, size })
      existing.add(key)
    }
  }
  form.skus = form.skus.filter(s => !s._isNew || s.color || s.size || s.barcode).concat(additions)
  ElMessage.success(`已增加 ${additions.length} 个规格，请填写原条码或点击补全空条码`)
}

// luohuai codeX generate: reserve barcodes only for empty rows; existing store barcodes remain untouched.
async function fillMissingBarcodes() {
  if (generatingBarcodes.value) return
  generatingBarcodes.value = true
  try {
    for (const sku of form.skus.filter(s => !s.barcode?.trim())) {
      const { barcode } = await generateNextBarcode()
      if (!sku.barcode?.trim()) sku.barcode = barcode
    }
    ElMessage.success('空条码已补全，保存商品后生效')
  } catch { ElMessage.error('部分条码未能生成，请重试补全') }
  finally { generatingBarcodes.value = false }
}

// ---- 初始化 ----
// ---- 尺码标签（下拉选择，来源系统设置→标签管理） ----
const sizeOptions = ref<SizeConfigDTO[]>([])

async function loadSizeOptions() {
  try {
    sizeOptions.value = await listSizes()
  } catch {
    // 加载失败不阻塞，尺码仍可手动输入
  }
}

onMounted(() => {
  loadSizeOptions()
  loadCategories()
  loadProducts()
})
</script>

<style scoped>
/* luohuai codeX generate: keep batch selectors and specification details readable. */
.spec-generator { display: flex; flex-wrap: wrap; gap: 10px; margin: 16px 0; }
.sku-summary { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin: 5px 0; }
.sku-summary span { font-size: 12px; color: #606266; }
.sku-column-labels { color: #909399; margin-bottom: 8px; }
.product-container {
  padding: 20px;
}

.search-card {
  margin-bottom: 16px;
}

.search-bar {
  /* luohuai codeX  modify: avoid touching rows when search fields and action buttons wrap. */
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

/* luohuai codeX generate: flex gap replaces Element Plus sibling margins after a line break. */
.search-bar .el-button { margin-left: 0; }

.search-bar .el-input,
.search-bar .el-select {
  margin-bottom: 0;
}

.table-card {
  min-height: 400px;
}

.no-image {
  width: 50px;
  height: 50px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 12px;
  margin: 0 auto;
}

.sku-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.product-form {
  /* luohuai codeX  modify: absorb form-row gutters so the inner form does not gain an unnecessary horizontal scrollbar. */
  max-height: 60vh;
  overflow-y: auto;
  padding: 0 10px;
}

.sku-section {
  padding: 0 0 8px 0;
}

.sku-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 0;
  margin-bottom: 12px;
}

.upload-tip {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}
</style>
