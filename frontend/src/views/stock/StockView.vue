<template>
  <div class="stock-container">
    <!-- 操作栏 -->
    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <span class="page-title">库存管理</span>
        <el-button type="primary" @click="openInboundDialog">
          <el-icon><Plus /></el-icon>
          入库
        </el-button>
      </div>
    </el-card>

    <!-- 库存表格 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="stockList"
        v-loading="loading"
        stripe
        style="width: 100%"
        row-key="skuId"
        @row-click="handleRowClick"
        highlight-current-row
      >
        <el-table-column prop="skuId" label="SKU ID" width="80" align="center" />

        <el-table-column prop="productName" label="商品名称" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.productName">{{ row.productName }}</span>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>

        <el-table-column label="规格" width="140" align="center">
          <template #default="{ row }">
            <span v-if="row.color || row.size">
              {{ row.color || '-' }} / {{ row.size || '-' }}
            </span>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>

        <el-table-column prop="barcode" label="条码" width="160" align="center">
          <template #default="{ row }">
            <span v-if="row.barcode">{{ row.barcode }}</span>
            <span v-else style="color: #c0c4cc">无条码</span>
          </template>
        </el-table-column>

        <el-table-column label="库存数" width="120" align="center">
          <template #default="{ row }">
            <el-tag
              :type="(row.stockQty ?? 0) <= 0 ? 'danger' : (row.stockQty ?? 0) <= 10 ? 'warning' : 'success'"
            >
              {{ row.stockQty ?? 0 }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 入库弹窗 -->
    <el-dialog
      v-model="inboundDialogVisible"
      title="商品入库"
      width="700px"
      :close-on-click-modal="false"
      destroy-on-close
      @opened="focusBarcodeInput"
    >
      <!-- 扫码输入区 -->
      <div class="inbound-scan">
        <el-input
          ref="inboundBarcodeInputRef"
          v-model="inboundBarcode"
          placeholder="扫描或输入商品条码，按回车添加..."
          size="default"
          clearable
          @keyup.enter="handleInboundScan"
          @clear="focusBarcodeInput"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <!-- 入库明细列表 -->
      <el-table
        :data="inboundItems"
        stripe
        style="width: 100%; margin-top: 16px"
        max-height="320"
        row-key="_key"
      >
        <el-table-column label="序号" width="60" align="center">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>

        <el-table-column prop="productName" label="商品名称" min-width="140" show-overflow-tooltip />

        <el-table-column prop="skuSpec" label="规格" width="120" align="center">
          <template #default="{ row }">
            <span v-if="row.skuSpec">{{ row.skuSpec }}</span>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>

        <el-table-column prop="barcode" label="条码" width="150" align="center">
          <template #default="{ row }">
            <span v-if="row.barcode">{{ row.barcode }}</span>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>

        <el-table-column label="入库数量" width="140" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="row.qty"
              :min="1"
              :max="9999"
              size="small"
              controls-position="right"
              style="width: 110px"
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="80" align="center" fixed="right">
          <template #default="{ $index }">
            <el-button
              type="danger"
              link
              size="small"
              @click="removeInboundItem($index)"
            >
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="inboundItems.length === 0" class="empty-inbound">
        <el-empty description="请扫描条码添加入库商品" :image-size="80" />
      </div>

      <template #footer>
        <el-button @click="inboundDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="inboundSubmitting"
          :disabled="inboundItems.length === 0"
          @click="handleInboundSubmit"
        >
          确认入库
        </el-button>
      </template>
    </el-dialog>

    <!-- 库存流水弹窗 -->
    <el-dialog
      v-model="flowDialogVisible"
      :title="flowSkuName ? '库存流水 - ' + flowSkuName : '库存流水'"
      width="750px"
      destroy-on-close
    >
      <el-table
        :data="flowRecords"
        v-loading="flowLoading"
        stripe
        style="width: 100%"
        max-height="400"
        row-key="id"
      >
        <el-table-column label="时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="变动类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.type === 'INBOUND' ? 'success' : 'danger'" size="small">
              {{ row.type === 'INBOUND' ? '入库' : '出库' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="变动数量" width="100" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.type === 'INBOUND' ? '#67c23a' : '#f56c6c', fontWeight: 500 }">
              {{ row.type === 'INBOUND' ? '+' : '-' }}{{ row.qty }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="变动前" width="90" align="center">
          <template #default="{ row }">{{ row.beforeQty }}</template>
        </el-table-column>

        <el-table-column label="变动后" width="90" align="center">
          <template #default="{ row }">{{ row.afterQty }}</template>
        </el-table-column>

        <el-table-column prop="productName" label="商品名称" min-width="140" show-overflow-tooltip />
      </el-table>

      <div v-if="!flowLoading && flowRecords.length === 0" style="padding: 20px 0">
        <el-empty description="暂无库存流水" :image-size="80" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, Delete } from '@element-plus/icons-vue'
import {
  listStock,
  getStockFlow,
  inbound,
  type StockItemDTO,
  type StockRecordDTO
} from '@/api/stock'
import { queryByBarcode, type BarcodeResult } from '@/api/product'

// ---- 表格数据 ----
const stockList = ref<StockItemDTO[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

// ---- 加载库存列表 ----
async function loadStock() {
  loading.value = true
  try {
    const result = await listStock({
      page: currentPage.value - 1,
      size: pageSize.value
    })
    stockList.value = result.content
    total.value = result.totalElements
  } catch {
    ElMessage.error('加载库存列表失败')
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadStock()
}

function handleSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  loadStock()
}

// ---- 入库弹窗 ----
const inboundDialogVisible = ref(false)
const inboundBarcode = ref('')
const inboundBarcodeInputRef = ref<InstanceType<typeof import('element-plus').ElInput>>()
const inboundSubmitting = ref(false)

interface InboundItemForm {
  _key: string
  skuId: number
  productName: string
  skuSpec: string
  barcode: string
  qty: number
}

const inboundItems = ref<InboundItemForm[]>([])

function focusBarcodeInput() {
  nextTick(() => {
    inboundBarcodeInputRef.value?.focus()
  })
}

function openInboundDialog() {
  inboundItems.value = []
  inboundBarcode.value = ''
  inboundDialogVisible.value = true
}

/** 扫码添加入库明细 */
async function handleInboundScan() {
  const code = inboundBarcode.value.trim()
  if (!code) return

  // 检查是否已在入库列表中
  const existingIndex = inboundItems.value.findIndex((item) => item.barcode === code)
  if (existingIndex >= 0) {
    inboundItems.value[existingIndex].qty++
    inboundBarcode.value = ''
    focusBarcodeInput()
    ElMessage.success(`${inboundItems.value[existingIndex].productName} 入库数量 +1`)
    return
  }

  try {
    const result: BarcodeResult = await queryByBarcode(code)
    if (!result.found) {
      ElMessage.warning('未找到该条码对应的商品')
      inboundBarcode.value = ''
      focusBarcodeInput()
      return
    }

    const spec = result.skuSpec || ((result.color || '') + (result.size ? ' / ' + result.size : '')) || '-'

    inboundItems.value.push({
      _key: Date.now().toString() + Math.random(),
      skuId: result.skuId!,
      productName: result.productName || '未知商品',
      skuSpec: spec,
      barcode: code,
      qty: 1
    })

    ElMessage.success(`已添加：${result.productName || '未知商品'}`)
  } catch {
    ElMessage.error('查询条码失败，请重试')
  } finally {
    inboundBarcode.value = ''
    focusBarcodeInput()
  }
}

function removeInboundItem(index: number) {
  inboundItems.value.splice(index, 1)
}

/** 提交入库 */
async function handleInboundSubmit() {
  if (inboundItems.value.length === 0) {
    ElMessage.warning('请先添加入库商品')
    return
  }

  inboundSubmitting.value = true
  try {
    await inbound({
      items: inboundItems.value.map((item) => ({
        skuId: item.skuId,
        qty: item.qty
      }))
    })
    ElMessage.success('入库成功')
    inboundDialogVisible.value = false
    loadStock()
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || '入库失败'
    ElMessage.error(msg)
  } finally {
    inboundSubmitting.value = false
  }
}

// ---- 库存流水弹窗 ----
const flowDialogVisible = ref(false)
const flowSkuName = ref('')
const flowRecords = ref<StockRecordDTO[]>([])
const flowLoading = ref(false)

function handleRowClick(row: StockItemDTO) {
  if (!row.skuId) return
  const spec = (row.color || '') + (row.size ? ' / ' + row.size : '')
  flowSkuName.value = (row.productName || '未知商品') + (spec ? ' (' + spec + ')' : '')
  flowDialogVisible.value = true
  loadFlowRecords(row.skuId)
}

async function loadFlowRecords(skuId: number) {
  flowLoading.value = true
  flowRecords.value = []
  try {
    flowRecords.value = await getStockFlow(skuId)
  } catch {
    ElMessage.error('加载库存流水失败')
  } finally {
    flowLoading.value = false
  }
}

function formatTime(time?: string) {
  if (!time) return '-'
  return time.replace('T', ' ')
}

// ---- 初始化 ----
onMounted(() => {
  loadStock()
})
</script>

<style scoped>
.stock-container {
  padding: 20px;
}

.toolbar-card {
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
}

.table-card {
  min-height: 400px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* 入库弹窗 */
.inbound-scan {
  margin-bottom: 4px;
}

.inbound-scan .el-input {
  max-width: 500px;
}

.empty-inbound {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px 0;
}
</style>
