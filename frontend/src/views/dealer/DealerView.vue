<template>
  <!-- luohuai codeX generate: dedicated first-level dealer management keeps purchasing provenance visible. -->
  <div class="dealer-container">
    <el-card shadow="never">
      <div class="page-header">
        <div><h2>经销商管理</h2><p>编码由系统从 100 起生成，创建后不可修改或回收。</p></div>
        <el-button type="primary" @click="openCreate"><el-icon><Plus /></el-icon>新增经销商</el-button>
      </div>
      <el-table :data="dealers" v-loading="loading" stripe row-key="id">
        <el-table-column prop="code" label="编码" width="90" />
        <el-table-column prop="name" label="经销商名称" min-width="150" />
        <el-table-column prop="contactName" label="联系人" width="120" />
        <el-table-column prop="phone" label="联系电话" width="150" />
        <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="editingId ? '编辑经销商' : '新增经销商'" width="560px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item v-if="editingId" label="经销商编码"><el-input v-model="form.code" disabled /></el-form-item>
        <el-form-item label="经销商名称" prop="name"><el-input v-model="form.name" maxlength="100" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contactName" maxlength="50" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.phone" maxlength="50" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" maxlength="255" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" maxlength="500" show-word-limit /></el-form-item>
        <el-form-item v-if="editingId" label="启用状态"><el-switch v-model="form.enabled" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
// luohuai codeX generate: administrator CRUD never accepts edits to immutable dealer codes.
import { onMounted, reactive, ref, nextTick } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { createDealer, listDealersForSetting, updateDealer, type DealerDTO } from '@/api/dealer'

const dealers = ref<DealerDTO[]>([])
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const editingId = ref<number>()
const formRef = ref<FormInstance>()
const form = reactive<DealerDTO>({ code: '', name: '', contactName: '', phone: '', address: '', remark: '', enabled: true })
const rules = { name: [{ required: true, message: '请输入经销商名称', trigger: 'blur' }] }

async function load() {
  loading.value = true
  try { dealers.value = await listDealersForSetting() }
  catch { ElMessage.error('加载经销商失败') }
  finally { loading.value = false }
}

function reset(row?: DealerDTO) {
  editingId.value = row?.id
  Object.assign(form, { code: row?.code || '', name: row?.name || '', contactName: row?.contactName || '', phone: row?.phone || '', address: row?.address || '', remark: row?.remark || '', enabled: row?.enabled ?? true })
  visible.value = true
  nextTick(() => formRef.value?.clearValidate())
}
function openCreate() { reset() }
function openEdit(row: DealerDTO) { reset(row) }

async function save() {
  if (!await formRef.value?.validate().catch(() => false)) return
  saving.value = true
  try {
    if (editingId.value) await updateDealer(editingId.value, form)
    else await createDealer(form)
    ElMessage.success('经销商已保存')
    visible.value = false
    await load()
  } catch (error: any) { ElMessage.error(error?.response?.data?.message || '保存失败') }
  finally { saving.value = false }
}

onMounted(load)
</script>

<style scoped>
/* luohuai codeX generate: match existing card spacing while allowing actions to wrap on narrow screens. */
.dealer-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; flex-wrap: wrap; margin-bottom: 18px; }
.page-header h2 { margin: 0 0 6px; font-size: 20px; }
.page-header p { margin: 0; color: #909399; }
</style>
