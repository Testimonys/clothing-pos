<template>
  <!-- luohuai codeX generate: preview source rows and specification gaps without offering an unimplemented import action. -->
  <el-dialog v-model="visible" title="商品导入预览" width="94%" :close-on-click-modal="false" destroy-on-close>
    <el-alert type="info" :closable="false" show-icon title="每行对应一个颜色／尺码规格。同一货号可有多行，但每个规格须使用独立条码。预览不会写入商品或库存。" />
    <div class="upload-row">
      <el-upload accept=".xls,.xlsx" :auto-upload="false" :show-file-list="false" :on-change="selectFile" :disabled="loading">
        <el-button :disabled="loading">选择 XLS / XLSX</el-button>
      </el-upload>
      <span>{{ selectedFile?.name || '尚未选择文件（最大 5MB、5000 行）' }}</span>
      <el-button type="primary" :loading="loading" :disabled="!selectedFile" @click="preview">检查并预览</el-button>
    </div>
    <template v-if="result">
      <div class="summary-row">
        <strong>{{ result.productCount }} 个货号 / {{ result.totalRows }} 条规格记录</strong>
        <el-tag type="success">可新增 {{ result.readyRows }}</el-tag>
        <el-tag type="warning">待补规格 {{ result.needsSpecRows }}</el-tag>
        <el-tag type="danger">已有冲突 {{ result.conflictRows }}</el-tag>
        <el-tag type="danger">数据错误 {{ result.invalidRows }}</el-tag>
        <el-select v-model="statusFilter" style="width: 160px" @change="page = 1">
          <el-option label="全部记录" value="" />
          <el-option v-for="(label, status) in labels" :key="status" :label="label" :value="status" />
        </el-select>
      </div>
      <el-table :data="pagedRows" stripe max-height="480" style="width: 100%">
        <el-table-column label="来源" width="115"><template #default="{ row }">{{ row.sheet }}:{{ row.sourceRow }}</template></el-table-column>
        <el-table-column label="检查结果" width="110"><template #default="{ row }"><el-tag :type="statusType(row.status)">{{ labels[row.status as PreviewStatus] }}</el-tag></template></el-table-column>
        <el-table-column prop="productCode" label="货号" width="110" />
        <el-table-column prop="name" label="商品名称" min-width="150" />
        <el-table-column prop="barcode" label="规格条码" width="170" />
        <el-table-column label="颜色" width="90"><template #default="{ row }">{{ row.color || '待补充' }}</template></el-table-column>
        <el-table-column label="尺码" width="90"><template #default="{ row }">{{ row.size || '待补充' }}</template></el-table-column>
        <el-table-column prop="unit" label="单位" width="65" />
        <el-table-column label="成本价" width="95"><template #default="{ row }">{{ money(row.costPrice) }}</template></el-table-column>
        <el-table-column label="零售价" width="95"><template #default="{ row }">{{ money(row.sellPrice) }}</template></el-table-column>
        <el-table-column prop="categoryName" label="来源分类" width="100" />
        <el-table-column label="说明" min-width="280"><template #default="{ row }">{{ [...row.errors, ...row.conflicts, ...row.warnings].join('；') || '字段检查通过，尚未导入' }}</template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="50" :total="filteredRows.length" layout="total, prev, pager, next" class="preview-pages" />
      <p v-for="note in result.notes" :key="note" class="preview-note">{{ note }}</p>
    </template>
    <template #footer><el-button @click="visible = false">关闭预览</el-button></template>
  </el-dialog>
</template>

<script setup lang="ts">
// luohuai codeX generate: bind upload and local pagination to the read-only server preview response.
import { computed, ref } from 'vue'
import { ElMessage, type UploadFile } from 'element-plus'
import { previewProductImport, type ProductImportPreview, type PreviewStatus } from '@/api/product'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()
const visible = computed({ get: () => props.modelValue, set: value => emit('update:modelValue', value) })
const selectedFile = ref<File>()
const loading = ref(false)
const result = ref<ProductImportPreview>()
const statusFilter = ref('')
const page = ref(1)
const labels: Record<PreviewStatus, string> = { READY: '可新增', NEEDS_SPEC: '待补规格', CONFLICT: '已有冲突', INVALID: '数据错误' }
const filteredRows = computed(() => (result.value?.rows || []).filter(row => !statusFilter.value || row.status === statusFilter.value))
const pagedRows = computed(() => filteredRows.value.slice((page.value - 1) * 50, page.value * 50))
const money = (value?: number | null) => value == null ? '—' : `¥${value.toFixed(2)}`
const statusType = (status: PreviewStatus) => status === 'READY' ? 'success' : status === 'NEEDS_SPEC' ? 'warning' : 'danger'

function selectFile(upload: UploadFile) {
  result.value = undefined
  selectedFile.value = undefined
  if (!upload.raw || !/\.xlsx?$/i.test(upload.name) || upload.raw.size > 5 * 1024 * 1024) {
    ElMessage.warning('请选择不超过 5MB 的 XLS/XLSX 文件')
    return
  }
  selectedFile.value = upload.raw
}

async function preview() {
  if (!selectedFile.value || loading.value) return
  loading.value = true
  result.value = undefined
  try {
    result.value = await previewProductImport(selectedFile.value)
    statusFilter.value = ''
    page.value = 1
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '预览失败，请检查文件后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* luohuai codeX generate: keep upload, totals and long validation messages readable. */
.upload-row, .summary-row { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; margin: 18px 0; }
.preview-pages { margin: 16px 0; }
.preview-note { color: #606266; line-height: 1.6; margin: 8px 0; }
</style>
