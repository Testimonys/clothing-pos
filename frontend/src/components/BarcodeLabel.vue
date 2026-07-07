<template>
  <div class="barcode-label">
    <div ref="labelContent" class="label-content">
      <div class="product-name">{{ productName }}</div>
      <div v-if="skuSpec" class="sku-spec">{{ skuSpec }}</div>
      <svg ref="barcodeSvg" class="barcode-svg"></svg>
      <div class="barcode-text">{{ barcode }}</div>
    </div>
    <el-button type="primary" size="large" class="print-btn" @click="handlePrint">
      <el-icon><Printer /></el-icon>
      打印标签
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import JsBarcode from 'jsbarcode'
import { Printer } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  barcode: string
  productName: string
  skuSpec?: string
}>(), {
  skuSpec: ''
})

const barcodeSvg = ref<SVGSVGElement | null>(null)

function generateBarcode(): void {
  if (!barcodeSvg.value || !props.barcode) return
  try {
    JsBarcode(barcodeSvg.value, props.barcode, {
      format: 'CODE128',
      width: 2,
      height: 60,
      displayValue: false,
      margin: 8,
      background: '#ffffff',
      lineColor: '#000000'
    })
  } catch {
    console.error('条码生成失败')
  }
}

function handlePrint(): void {
  window.print()
}

onMounted(() => {
  generateBarcode()
})

watch(() => props.barcode, () => {
  generateBarcode()
})
</script>

<style scoped>
.barcode-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.label-content {
  background: #fff;
  padding: 16px 24px;
  text-align: center;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
}

.product-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
  line-height: 1.4;
}

.sku-spec {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  line-height: 1.4;
}

.barcode-svg {
  display: block;
  margin: 0 auto;
}

.barcode-text {
  font-size: 13px;
  color: #333;
  margin-top: 6px;
  font-family: 'Courier New', Courier, monospace;
  letter-spacing: 1px;
}

.print-btn {
  width: 100%;
}

/* 打印样式：仅显示标签内容，隐藏按钮 */
@media print {
  .print-btn {
    display: none !important;
  }

  .barcode-label {
    position: absolute;
    top: 0;
    left: 0;
  }

  .label-content {
    border: none;
    padding: 12px;
  }
}
</style>
