<template>
  <div class="pos-container">
    <!-- 扫码输入区 -->
    <div class="scan-area">
      <el-input
        ref="barcodeInputRef"
        v-model="barcodeInput"
        placeholder="扫描或输入商品条码，按回车添加..."
        size="large"
        clearable
        class="scan-input"
        @keyup.enter="handleScan"
        @clear="focusBarcodeInput"
      >
        <template #prefix>
          <el-icon :size="20"><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- 交易明细表格 -->
    <el-card shadow="never" class="cart-card">
      <template #header>
        <div class="cart-header">
          <span class="cart-title">交易明细</span>
          <el-tag type="info" v-if="cartItems.length > 0">
            {{ cartItems.length }} 种商品 / {{ totalQuantity }} 件
          </el-tag>
        </div>
      </template>

      <el-table
        :data="cartItems"
        stripe
        style="width: 100%"
        row-key="_key"
        max-height="380"
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

        <el-table-column label="单价" width="110" align="right">
          <template #default="{ row }">
            <span class="price-cell">¥{{ row.unitPrice.toFixed(2) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="数量" width="130" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="row.quantity"
              :min="1"
              :max="999"
              size="small"
              controls-position="right"
              style="width: 100px"
              @change="() => recalcItem(row)"
            />
          </template>
        </el-table-column>

        <el-table-column label="单品折扣" width="130" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="row.discount"
              :min="0"
              :max="row.unitPrice * row.quantity"
              :precision="2"
              size="small"
              controls-position="right"
              style="width: 100px"
              @change="() => recalcItem(row)"
            />
          </template>
        </el-table-column>

        <el-table-column label="小计" width="110" align="right">
          <template #default="{ row }">
            <span class="subtotal-cell">¥{{ row.subtotal.toFixed(2) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="80" align="center" fixed="right">
          <template #default="{ row, $index }">
            <el-button
              type="danger"
              link
              size="small"
              @click="removeItem($index)"
            >
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="cartItems.length === 0" class="empty-cart">
        <el-empty description="暂无商品，请扫描条码添加" :image-size="100" />
      </div>
    </el-card>

    <!-- 底部结算栏 -->
    <div class="checkout-bar" v-if="cartItems.length > 0">
      <div class="checkout-summary">
        <div class="summary-item">
          <span class="summary-label">合计：</span>
          <span class="summary-value total">¥{{ totalAmount.toFixed(2) }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">整单折扣：</span>
          <el-input-number
            v-model="orderDiscount"
            :min="0"
            :max="totalAmount"
            :precision="2"
            size="small"
            controls-position="right"
            style="width: 130px"
          />
        </div>
        <div class="summary-item">
          <span class="summary-label">应收：</span>
          <span class="summary-value receivable">¥{{ receivable.toFixed(2) }}</span>
        </div>
      </div>

      <el-button
        type="success"
        size="large"
        class="checkout-btn"
        @click="openCheckoutDialog"
      >
        <el-icon><Sell /></el-icon>
        结账 F12
      </el-button>
    </div>

    <!-- 结账弹窗 -->
    <el-dialog
      v-model="checkoutDialogVisible"
      title="收款结算"
      width="480px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      destroy-on-close
      @opened="focusReceivedInput"
    >
      <el-form label-width="90px" class="checkout-form">
        <el-form-item label="应收金额">
          <el-input
            :model-value="'¥' + receivable.toFixed(2)"
            disabled
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="实收金额">
          <el-input
            ref="receivedInputRef"
            v-model="receivedAmount"
            placeholder="请输入实收金额"
            style="width: 100%"
            @input="calcChange"
          >
            <template #prefix>¥</template>
          </el-input>
        </el-form-item>

        <el-form-item label="找零">
          <el-input
            :model-value="'¥' + changeAmount.toFixed(2)"
            disabled
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="支付方式">
          <el-select v-model="paymentMethod" style="width: 100%">
            <el-option label="现金" value="CASH" />
            <el-option label="微信支付" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
          </el-select>
        </el-form-item>

        <el-form-item label="打印小票">
          <el-checkbox v-model="printReceipt" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="checkoutDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleCheckout"
        >
          确认收款
        </el-button>
      </template>
    </el-dialog>

    <!-- 成功弹窗 -->
    <el-dialog
      v-model="successDialogVisible"
      title="收款成功"
      width="420px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <div class="success-content">
        <el-result icon="success" title="交易完成">
          <template #sub-title>
            <div class="success-detail">
              <p><strong>订单号：</strong>{{ successOrderNo }}</p>
              <p><strong>交易时间：</strong>{{ successTime }}</p>
              <p><strong>实收金额：</strong>¥{{ successAmount.toFixed(2) }}</p>
              <p v-if="changeAmount > 0">
                <strong>找零：</strong>¥{{ successChange.toFixed(2) }}
              </p>
            </div>
          </template>
        </el-result>
      </div>

      <template #footer>
        <el-button type="primary" size="large" @click="startNewOrder" class="new-order-btn">
          开始下一单
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Delete, Sell } from '@element-plus/icons-vue'
import { queryByBarcode, type BarcodeResult } from '@/api/product'
import request from '@/api/request'

// ---- 交易明细项类型 ----
interface CartItem {
  _key: string
  barcode: string
  productName: string
  skuSpec: string
  unitPrice: number
  quantity: number
  discount: number
  subtotal: number
  skuId?: number
  productId?: number
}

// ---- 扫码输入 ----
const barcodeInput = ref('')
const barcodeInputRef = ref<InstanceType<typeof import('element-plus').ElInput>>()

function focusBarcodeInput() {
  nextTick(() => {
    barcodeInputRef.value?.focus()
  })
}

// ---- 交易明细 ----
const cartItems = ref<CartItem[]>([])

const totalQuantity = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + item.quantity, 0)
})

/** 单品小计 = 单价 x 数量 - 单品折扣 */
function recalcItem(item: CartItem) {
  item.subtotal = Math.max(0, item.unitPrice * item.quantity - item.discount)
}

/** 合计 = 所有单品小计之和 */
const totalAmount = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + item.subtotal, 0)
})

// ---- 整单折扣 ----
const orderDiscount = ref(0)

/** 应收 = 合计 - 整单折扣 */
const receivable = computed(() => {
  return Math.max(0, totalAmount.value - orderDiscount.value)
})

// ---- 扫码处理 ----
async function handleScan() {
  const code = barcodeInput.value.trim()
  if (!code) return

  // 检查是否已在购物车中
  const existingIndex = cartItems.value.findIndex((item) => item.barcode === code)
  if (existingIndex >= 0) {
    cartItems.value[existingIndex].quantity++
    recalcItem(cartItems.value[existingIndex])
    barcodeInput.value = ''
    focusBarcodeInput()
    ElMessage.success(`${cartItems.value[existingIndex].productName} 数量 +1`)
    return
  }

  // 查询条码
  try {
    const result: BarcodeResult = await queryByBarcode(code)
    if (!result.found) {
      ElMessage.warning('未找到该条码对应的商品')
      barcodeInput.value = ''
      focusBarcodeInput()
      return
    }

    const newItem: CartItem = {
      _key: Date.now().toString() + Math.random(),
      barcode: code,
      productName: result.productName || '未知商品',
      skuSpec: result.skuSpec || '',
      unitPrice: result.sellPrice || 0,
      quantity: 1,
      discount: 0,
      subtotal: result.sellPrice || 0,
      skuId: result.skuId,
      productId: result.productId
    }

    cartItems.value.push(newItem)
    ElMessage.success(`已添加：${newItem.productName}`)
  } catch {
    ElMessage.error('查询条码失败，请重试')
  } finally {
    barcodeInput.value = ''
    focusBarcodeInput()
  }
}

/** 删除明细项 */
function removeItem(index: number) {
  cartItems.value.splice(index, 1)
}

// ---- 结账弹窗 ----
const checkoutDialogVisible = ref(false)
const receivedAmount = ref('')
const changeAmount = ref(0)
const paymentMethod = ref('CASH')
const printReceipt = ref(true)
const submitting = ref(false)
const receivedInputRef = ref<InstanceType<typeof import('element-plus').ElInput>>()

function openCheckoutDialog() {
  if (cartItems.value.length === 0) {
    ElMessage.warning('请先添加商品')
    return
  }
  receivedAmount.value = receivable.value.toFixed(2)
  changeAmount.value = 0
  checkoutDialogVisible.value = true
}

function focusReceivedInput() {
  nextTick(() => {
    receivedInputRef.value?.focus()
  })
}

function calcChange() {
  const received = parseFloat(receivedAmount.value) || 0
  const change = received - receivable.value
  changeAmount.value = Math.max(0, change)
}

/** 确认收款 → POST /api/order */
async function handleCheckout() {
  const received = parseFloat(receivedAmount.value) || 0
  if (received < receivable.value) {
    ElMessage.warning('实收金额不足，请重新输入')
    return
  }

  if (cartItems.value.length === 0) {
    ElMessage.warning('请先添加商品')
    return
  }

  const orderData = {
    items: cartItems.value.map((item) => ({
      skuId: item.skuId,
      productId: item.productId,
      productName: item.productName,
      barcode: item.barcode,
      skuSpec: item.skuSpec,
      quantity: item.quantity,
      unitPrice: item.unitPrice,
      discount: item.discount,
      subtotal: item.subtotal
    })),
    totalAmount: totalAmount.value,
    discountAmount: orderDiscount.value,
    receivableAmount: receivable.value,
    receivedAmount: received,
    changeAmount: changeAmount.value,
    paymentMethod: paymentMethod.value,
    printReceipt: printReceipt.value
  }

  submitting.value = true
  try {
    const response = await request.post('/order', orderData)
    const data = response.data

    // 关闭结账弹窗，打开成功弹窗
    checkoutDialogVisible.value = false

    successOrderNo.value = data.orderNo || data.orderNumber || '-'
    successTime.value = data.createTime || data.orderTime || new Date().toLocaleString()
    successAmount.value = received
    successChange.value = changeAmount.value
    successDialogVisible.value = true
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || '提交订单失败'
    ElMessage.error(msg)
  } finally {
    submitting.value = false
  }
}

// ---- 成功弹窗 ----
const successDialogVisible = ref(false)
const successOrderNo = ref('')
const successTime = ref('')
const successAmount = ref(0)
const successChange = ref(0)

function startNewOrder() {
  successDialogVisible.value = false
  cartItems.value = []
  orderDiscount.value = 0
  receivedAmount.value = ''
  changeAmount.value = 0
  paymentMethod.value = 'CASH'
  printReceipt.value = true
  barcodeInput.value = ''
  focusBarcodeInput()
}

// ---- F12 快捷键绑定 ----
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F12' || e.code === 'F12') {
    e.preventDefault()
    openCheckoutDialog()
  }
}

onMounted(() => {
  focusBarcodeInput()
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.pos-container {
  padding: 20px;
  display: flex;
  flex-direction: column;
  height: 100%;
  box-sizing: border-box;
}

/* 扫码输入区 */
.scan-area {
  margin-bottom: 16px;
}

.scan-input {
  max-width: 600px;
}

/* 交易明细 */
.cart-card {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.cart-card :deep(.el-card__body) {
  flex: 1;
  overflow: auto;
  padding: 0;
}

.cart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cart-title {
  font-size: 16px;
  font-weight: 600;
}

.price-cell {
  color: #333;
  font-weight: 500;
}

.subtotal-cell {
  color: #e6a23c;
  font-weight: 600;
  font-size: 15px;
}

.empty-cart {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

/* 底部结算栏 */
.checkout-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-top: 2px solid #e6e6e6;
  padding: 16px 20px;
  margin-top: 16px;
  border-radius: 4px;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
}

.checkout-summary {
  display: flex;
  align-items: center;
  gap: 32px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

.summary-value {
  font-size: 20px;
  font-weight: 700;
}

.summary-value.total {
  color: #333;
}

.summary-value.receivable {
  color: #f56c6c;
}

.checkout-btn {
  min-width: 160px;
  font-size: 16px;
  font-weight: 600;
}

/* 结账弹窗 */
.checkout-form {
  padding-top: 8px;
}

/* 成功弹窗 */
.success-content {
  text-align: center;
}

.success-detail {
  text-align: left;
  display: inline-block;
  margin-top: 8px;
}

.success-detail p {
  margin: 8px 0;
  font-size: 14px;
  color: #606266;
}

.new-order-btn {
  min-width: 140px;
}
</style>
