<template>
  <div class="product-container">
    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索商品名称..."
          clearable
          style="width: 240px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-model="categoryId"
          placeholder="全部分类"
          clearable
          style="width: 180px; margin-left: 12px"
          @change="handleSearch"
        >
          <el-option
            v-for="cat in categories"
            :key="cat.id"
            :label="cat.name"
            :value="cat.id"
          />
        </el-select>

        <el-button type="primary" style="margin-left: 12px" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>

        <el-button style="margin-left: 12px" @click="handleReset">重置</el-button>

        <el-button type="success" style="margin-left: auto" @click="handleAdd">
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
        <el-table-column prop="id" label="ID" width="70" align="center" />

        <el-table-column label="图片" width="90" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.imageUrl"
              :src="row.imageUrl"
              style="width: 50px; height: 50px; border-radius: 4px"
              fit="cover"
              :preview-src-list="[row.imageUrl]"
              :initial-index="0"
            />
            <div v-else class="no-image">无图</div>
          </template>
        </el-table-column>

        <el-table-column prop="name" label="商品名称" min-width="160" show-overflow-tooltip />

        <el-table-column label="分类" width="110">
          <template #default="{ row }">
            <span v-if="row.categoryName">{{ row.categoryName }}</span>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>

        <el-table-column label="进价" width="100" align="right">
          <template #default="{ row }">
            <span style="color: #909399">{{ row.costPrice != null ? '¥' + row.costPrice : '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="售价" width="100" align="right">
          <template #default="{ row }">
            <span style="color: #e6a23c; font-weight: 500">
              {{ row.sellPrice != null ? '¥' + row.sellPrice : '-' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="规格标签" min-width="160">
          <template #default="{ row }">
            <div v-if="row.skus && row.skus.length > 0" class="sku-tags">
              <el-tag
                v-for="sku in row.skus"
                :key="sku.id"
                size="small"
                type="info"
                style="margin: 2px 4px 2px 0"
              >
                {{ sku.color || '?' }} / {{ sku.size || '?' }}
              </el-tag>
            </div>
            <span v-else style="color: #c0c4cc">无规格</span>
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

      <!-- 分页 -->
      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="page"
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑商品' : '新增商品'"
      width="780px"
      :close-on-click-modal="false"
      destroy-on-close
      @opened="handleDialogOpened"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="90px"
        class="product-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入商品名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryId">
              <el-select
                v-model="form.categoryId"
                placeholder="选择分类"
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="cat in categories"
                  :key="cat.id"
                  :label="cat.name"
                  :value="cat.id"
                />
              </el-select>
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

        <!-- SKU 规格管理 -->
        <el-divider content-position="left">
          <span style="font-weight: 500">规格管理 (SKU)</span>
        </el-divider>

        <div class="sku-section">
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
            <el-input
              v-model="sku.size"
              placeholder="尺码"
              style="width: 120px; margin-left: 8px"
              size="default"
            />
            <el-input
              v-model="sku.barcode"
              placeholder="条码"
              style="width: 150px; margin-left: 8px"
              size="default"
              :disabled="sku._generatingBarcode"
            >
              <template #append v-if="isEdit && sku.id">
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
              placeholder="初始库存"
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
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, Edit, Delete, Refresh } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import {
  listProducts,
  getProduct,
  createProduct,
  updateProduct,
  deleteProduct as deleteProductApi,
  addSku as addSkuApi,
  updateSku as updateSkuApi,
  deleteSku as deleteSkuApi,
  generateBarcode,
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
const saving = ref(false)
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
  name: string
  categoryId: number | null
  costPrice?: number
  sellPrice?: number
  imageUrl: string
  skus: SkuFormItem[]
}

const form = reactive<ProductForm>({
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

function handleImageError() {
  ElMessage.error('图片上传失败，请重试')
}

function handleImageRemove() {
  form.imageUrl = ''
  lastUploadedUrl = ''
}

// ---- 规格行操作 ----
function addSkuRow() {
  form.skus.push(createEmptySku())
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
  try {
    const detail = await getProduct(id)
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
  } catch {
    ElMessage.error('加载商品详情失败')
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
  } catch {
    ElMessage.error('删除失败')
  }
}

// ---- 生成条码 ----
async function handleGenerateBarcode(sku: SkuFormItem) {
  if (!editId.value || !sku.id) return
  sku._generatingBarcode = true
  try {
    const result = await generateBarcode(editId.value, sku.id)
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
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  // 过滤掉空的 SKU 行
  const validSkus: SkuFormItem[] = form.skus.filter(
    (s) => s.color || s.size || s.barcode
  )

  if (validSkus.length === 0) {
    ElMessage.warning('请至少填写一个规格')
    return
  }

  saving.value = true
  try {
    if (isEdit.value && editId.value != null) {
      // ---- 编辑模式 ----
      // 1. 更新商品基本信息
      await updateProduct(editId.value, {
        name: form.name,
        categoryId: form.categoryId ?? null,
        costPrice: form.costPrice,
        sellPrice: form.sellPrice,
        imageUrl: form.imageUrl || undefined
      })

      // 2. 处理 SKU
      for (const sku of validSkus) {
        if (sku._isNew) {
          // 新增 SKU
          await addSkuApi(editId.value, {
            color: sku.color || undefined,
            size: sku.size || undefined,
            barcode: sku.barcode || undefined,
            stockQty: sku.stockQty ?? 0
          })
        } else if (sku.id) {
          // 更新已有 SKU
          await updateSkuApi(editId.value, sku.id, {
            color: sku.color || undefined,
            size: sku.size || undefined,
            barcode: sku.barcode || undefined,
            stockQty: sku.stockQty ?? 0
          })
        }
      }

      // 3. 删除被移除的已有 SKU
      const keptSkus = validSkus.filter((s) => !s._isNew)
      const keptIds = new Set(keptSkus.map((s) => s.id).filter(Boolean))

      // 从后端加载的原始 skus 与当前对比
      if (editId.value) {
        try {
          const freshDetail = await getProduct(editId.value)
          if (freshDetail.skus) {
            for (const originalSku of freshDetail.skus) {
              if (originalSku.id && !keptIds.has(originalSku.id) && keptSkus.length > 0) {
                // 这个原始 SKU 被删除了
                try {
                  await deleteSkuApi(editId.value, originalSku.id)
                } catch {
                  // 删除失败不阻塞
                }
              }
            }
          }
        } catch {
          // 重新加载失败不阻塞
        }
      }

      ElMessage.success('商品更新成功')
    } else {
      // ---- 新增模式 ----
      await createProduct({
        name: form.name,
        categoryId: form.categoryId ?? null,
        costPrice: form.costPrice,
        sellPrice: form.sellPrice,
        imageUrl: form.imageUrl || undefined,
        skus: validSkus.map((sku) => ({
          color: sku.color || undefined,
          size: sku.size || undefined,
          barcode: sku.barcode || undefined,
          stockQty: sku.stockQty ?? 0
        }))
      })
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

// ---- 初始化 ----
onMounted(() => {
  loadCategories()
  loadProducts()
})
</script>

<style scoped>
.product-container {
  padding: 20px;
}

.search-card {
  margin-bottom: 16px;
}

.search-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

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
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 8px;
}

.sku-section {
  padding: 0 0 8px 0;
}

.sku-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.upload-tip {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}
</style>
