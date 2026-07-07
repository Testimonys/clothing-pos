<template>
  <div class="order-container">
    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <div class="search-bar">
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 380px"
          clearable
        />

        <el-select
          v-model="payMethod"
          placeholder="支付方式"
          clearable
          style="width: 150px; margin-left: 12px"
        >
          <el-option label="现金" value="CASH" />
          <el-option label="微信支付" value="WECHAT" />
          <el-option label="支付宝" value="ALIPAY" />
        </el-select>

        <el-button type="primary" style="margin-left: 12px" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>

        <el-button style="margin-left: 12px" @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <!-- 订单表格 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="orderList"
        v-loading="loading"
        stripe
        style="width: 100%"
        row-key="id"
        @row-click="handleRowClick"
        highlight-current-row
      >
        <el-table-column prop="orderNo" label="订单号" width="180" align="center" />

        <el-table-column label="交易时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="订单金额" width="130" align="right">
          <template #default="{ row }">
            <span style="color: #e6a23c; font-weight: 600">
              ¥{{ (row.totalAmount ?? 0).toFixed(2) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="实收金额" width="130" align="right">
          <template #default="{ row }">
            <span style="font-weight: 500">
              ¥{{ (row.receiveAmount ?? 0).toFixed(2) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="支付方式" width="110" align="center">
          <template #default="{ row }">
            <el-tag
              :type="payMethodTag(row.payMethod)"
              size="small"
            >
              {{ payMethodLabel(row.payMethod) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="cashierName" label="收银员" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.cashierName">{{ row.cashierName }}</span>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click.stop="showDetail(row)">
              <el-icon><View /></el-icon>
              详情
            </el-button>
            <el-button type="success" link size="small" style="margin-left: 4px" @click.stop="handleReprint(row)">
              <el-icon><Printer /></el-icon>
              补打小票
            </el-button>
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

    <!-- 订单详情弹窗 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="订单详情"
      width="750px"
      destroy-on-close
    >
      <div v-loading="detailLoading">
        <!-- 订单基本信息 -->
        <el-descriptions :column="2" border size="small" v-if="orderDetail">
          <el-descriptions-item label="订单号">
            {{ orderDetail.orderNo }}
          </el-descriptions-item>
          <el-descriptions-item label="交易时间">
            {{ formatTime(orderDetail.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="支付方式">
            <el-tag :type="payMethodTag(orderDetail.payMethod)" size="small">
              {{ payMethodLabel(orderDetail.payMethod) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="收银员">
            {{ orderDetail.cashierName || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 商品明细 -->
        <div class="detail-section" v-if="orderDetail?.items && orderDetail.items.length > 0">
          <h4 class="section-title">商品明细</h4>
          <el-table
            :data="orderDetail.items"
            stripe
            size="small"
            style="width: 100%"
          >
            <el-table-column label="序号" width="55" align="center">
              <template #default="{ $index }">{{ $index + 1 }}</template>
            </el-table-column>
            <el-table-column prop="productName" label="商品名称" min-width="150" show-overflow-tooltip />
            <el-table-column prop="skuSpec" label="规格" width="120" align="center">
              <template #default="{ row }">
                <span v-if="row.skuSpec">{{ row.skuSpec }}</span>
                <span v-else style="color: #c0c4cc">-</span>
              </template>
            </el-table-column>
            <el-table-column label="单价" width="90" align="right">
              <template #default="{ row }">
                ¥{{ (row.unitPrice ?? 0).toFixed(2) }}
              </template>
            </el-table-column>
            <el-table-column prop="qty" label="数量" width="65" align="center" />
            <el-table-column label="折扣" width="80" align="right">
              <template #default="{ row }">
                ¥{{ (row.discount ?? 0).toFixed(2) }}
              </template>
            </el-table-column>
            <el-table-column label="小计" width="100" align="right">
              <template #default="{ row }">
                <span style="color: #e6a23c; font-weight: 500">
                  ¥{{ (row.subTotal ?? 0).toFixed(2) }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 金额汇总 -->
        <div class="amount-summary" v-if="orderDetail">
          <h4 class="section-title">金额汇总</h4>
          <div class="summary-grid">
            <div class="summary-row">
              <span class="summary-label">合计金额：</span>
              <span class="summary-value">¥{{ (orderDetail.totalAmount ?? 0).toFixed(2) }}</span>
            </div>
            <div class="summary-row">
              <span class="summary-label">整单折扣：</span>
              <span class="summary-value">-¥{{ (orderDetail.discount ?? 0).toFixed(2) }}</span>
            </div>
            <div class="summary-row">
              <span class="summary-label">应付金额：</span>
              <span class="summary-value pay-amount">¥{{ (orderDetail.payAmount ?? 0).toFixed(2) }}</span>
            </div>
            <div class="summary-row">
              <span class="summary-label">实收金额：</span>
              <span class="summary-value receive-amount">¥{{ (orderDetail.receiveAmount ?? 0).toFixed(2) }}</span>
            </div>
            <div class="summary-row" v-if="(orderDetail.changeAmount ?? 0) > 0">
              <span class="summary-label">找零：</span>
              <span class="summary-value change-amount">¥{{ (orderDetail.changeAmount ?? 0).toFixed(2) }}</span>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          v-if="orderDetail"
          type="success"
          :loading="reprintLoading"
          @click="handleReprint(orderDetail)"
        >
          <el-icon><Printer /></el-icon>
          补打小票
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, View, Printer } from '@element-plus/icons-vue'
import {
  listOrders,
  getOrderDetail,
  reprintOrder,
  type OrderSummaryDTO,
  type OrderDetailDTO
} from '@/api/order'

// ---- 筛选条件 ----
const dateRange = ref<string[]>([])
const payMethod = ref('')

// ---- 分页 ----
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

// ---- 表格数据 ----
const orderList = ref<OrderSummaryDTO[]>([])
const loading = ref(false)

// ---- 支付方式显示 ----
const payMethodMap: Record<string, string> = {
  CASH: '现金',
  WECHAT: '微信支付',
  ALIPAY: '支付宝'
}

function payMethodLabel(method?: string) {
  if (!method) return '-'
  return payMethodMap[method] || method
}

function payMethodTag(method?: string) {
  if (method === 'CASH') return 'info'
  if (method === 'WECHAT') return 'success'
  if (method === 'ALIPAY') return ''
  return 'info'
}

/** 格式化时间 */
function formatTime(time?: string) {
  if (!time) return '-'
  return time.replace('T', ' ')
}

// ---- 加载订单列表 ----
async function loadOrders() {
  loading.value = true
  try {
    const params: {
      page: number
      size: number
      startTime?: string
      endTime?: string
      payMethod?: string
    } = {
      page: currentPage.value - 1,
      size: pageSize.value
    }

    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0]
      params.endTime = dateRange.value[1]
    }

    if (payMethod.value) {
      params.payMethod = payMethod.value
    }

    const result = await listOrders(params)
    orderList.value = result.content
    total.value = result.totalElements
  } catch {
    ElMessage.error('加载订单列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadOrders()
}

function handleReset() {
  dateRange.value = []
  payMethod.value = ''
  currentPage.value = 1
  loadOrders()
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadOrders()
}

function handleSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  loadOrders()
}

// ---- 订单详情弹窗 ----
const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const orderDetail = ref<OrderDetailDTO | null>(null)

async function showDetail(row: OrderSummaryDTO) {
  if (!row.id) return
  detailDialogVisible.value = true
  detailLoading.value = true
  orderDetail.value = null
  try {
    orderDetail.value = await getOrderDetail(row.id)
  } catch {
    ElMessage.error('加载订单详情失败')
  } finally {
    detailLoading.value = false
  }
}

function handleRowClick(row: OrderSummaryDTO) {
  showDetail(row)
}

// ---- 补打小票 ----
const reprintLoading = ref(false)

async function handleReprint(row: OrderSummaryDTO) {
  if (!row.id) return
  reprintLoading.value = true
  try {
    const result = await reprintOrder(row.id)
    ElMessage.success(result.message || '补打指令已发送')
  } catch {
    ElMessage.error('补打小票失败')
  } finally {
    reprintLoading.value = false
  }
}

// ---- 初始化 ----
onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.order-container {
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

.table-card {
  min-height: 400px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* 详情弹窗 */
.detail-section {
  margin-top: 20px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.amount-summary {
  margin-top: 20px;
}

.summary-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.summary-row {
  display: flex;
  align-items: center;
  min-width: 200px;
}

.summary-label {
  font-size: 14px;
  color: #606266;
}

.summary-value {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-left: 8px;
}

.summary-value.pay-amount {
  color: #f56c6c;
}

.summary-value.receive-amount {
  color: #409eff;
}

.summary-value.change-amount {
  color: #909399;
}
</style>
