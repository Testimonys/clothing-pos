<template>
  <div class="setting-container">
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <!-- ========== Tab 1: 用户管理 ========== -->
        <el-tab-pane label="用户管理" name="users">
          <div class="tab-header">
            <span class="tab-title">用户列表</span>
            <el-button type="primary" size="default" @click="handleAddUser">
              <el-icon><Plus /></el-icon>
              新增用户
            </el-button>
          </div>

          <el-table :data="users" v-loading="usersLoading" stripe style="width: 100%" row-key="id">
            <el-table-column prop="id" label="ID" width="70" align="center" />
            <el-table-column prop="username" label="用户名" min-width="140" />
            <el-table-column prop="displayName" label="显示名称" min-width="140" />
            <el-table-column label="角色" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.role === 'BOSS' ? 'danger' : 'info'" size="small">
                  {{ row.role === 'BOSS' ? '老板' : '店员' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="启用" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                  {{ row.enabled ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="170" align="center">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleEditUser(row)">
                  <el-icon><Edit /></el-icon>
                  编辑
                </el-button>
                <el-popconfirm
                  title="确定删除该用户？"
                  confirm-button-text="确定"
                  cancel-button-text="取消"
                  @confirm="handleDeleteUser(row)"
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
        </el-tab-pane>

        <!-- ========== Tab 2: 分类管理 ========== -->
        <el-tab-pane label="分类管理" name="categories">
          <div class="tab-header">
            <span class="tab-title">分类树</span>
            <el-button type="primary" size="default" @click="handleAddCategory">
              <el-icon><Plus /></el-icon>
              新增分类
            </el-button>
          </div>

          <div class="category-tree-wrap" v-loading="categoriesLoading">
            <el-tree
              ref="categoryTreeRef"
              :data="categoryTree"
              :props="treeProps"
              node-key="id"
              highlight-current
              :expand-on-click-node="false"
              default-expand-all
            >
              <template #default="{ node, data }">
                <span class="category-node">
                  <span class="category-node-label">{{ data.name }}</span>
                  <span class="category-node-actions">
                    <el-button type="primary" link size="small" @click.stop="handleEditCategory(data)">
                      <el-icon><Edit /></el-icon>
                    </el-button>
                  </span>
                </span>
              </template>
            </el-tree>

            <div v-if="!categoriesLoading && categoryTree.length === 0" class="empty-state">
              <el-empty description="暂无分类，请新增" :image-size="80" />
            </div>
          </div>
        </el-tab-pane>

        <!-- ========== Tab 3: 备份管理 ========== -->
        <el-tab-pane label="备份管理" name="backups">
          <div class="tab-header">
            <span class="tab-title">数据备份</span>
            <el-button type="primary" size="default" :loading="backingUp" @click="handleBackup">
              <el-icon><Refresh /></el-icon>
              一键备份
            </el-button>
          </div>

          <el-table :data="backups" v-loading="backupsLoading" stripe style="width: 100%" row-key="fileName">
            <el-table-column label="文件名" min-width="240">
              <template #default="{ row }">
                <el-icon><Document /></el-icon>
                {{ row.fileName }}
              </template>
            </el-table-column>
            <el-table-column label="文件大小" width="120" align="center">
              <template #default="{ row }">
                {{ formatSize(row.size) }}
              </template>
            </el-table-column>
            <el-table-column label="备份时间" width="170" align="center">
              <template #default="{ row }">
                {{ formatBackupTime(row.time) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleDownload(row.fileName)">
                  <el-icon><Download /></el-icon>
                  下载
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="!backupsLoading && backups.length === 0" class="empty-state" style="margin-top: 24px">
            <el-empty description="暂无备份记录，请点击「一键备份」" :image-size="80" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- ==================== 用户管理弹窗 ==================== -->
    <el-dialog
      v-model="userDialogVisible"
      :title="isEditUser ? '编辑用户' : '新增用户'"
      width="500px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="userFormRef" :model="userForm" :rules="userFormRules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="userForm.password"
            type="password"
            show-password
            :placeholder="isEditUser ? '留空则不修改密码' : '请输入密码'"
          />
        </el-form-item>
        <el-form-item label="显示名称" prop="displayName">
          <el-input v-model="userForm.displayName" placeholder="请输入显示名称" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" placeholder="选择角色" style="width: 100%">
            <el-option label="老板" value="BOSS" />
            <el-option label="店员" value="CLERK" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="userSaving" @click="handleSaveUser">保存</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 分类管理弹窗 ==================== -->
    <el-dialog
      v-model="categoryDialogVisible"
      :title="isEditCategory ? '编辑分类' : '新增分类'"
      width="480px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="categoryFormRef" :model="categoryForm" :rules="categoryFormRules" label-width="100px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="上级分类" prop="parentId">
          <el-tree-select
            v-model="categoryForm.parentId"
            :data="categoryTree"
            :props="treeProps"
            placeholder="选择上级分类（留空为顶级）"
            clearable
            check-strictly
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="categoryForm.sortOrder" :min="0" :max="9999" style="width: 100%" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="categorySaving" @click="handleSaveCategory">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, Edit, Delete, Refresh, Download, Document } from '@element-plus/icons-vue'
import request from '@/api/request'
import { useAuthStore } from '@/stores/auth'
import type { FormInstance, ElTree } from 'element-plus'

const authStore = useAuthStore()

// ---- 当前激活的 Tab ----
const activeTab = ref('users')

// ===========================
//  类型定义
// ===========================

interface UserDTO {
  id: number
  username: string
  displayName: string
  role: string
  enabled: boolean
  createTime: string
}

interface CreateUserRequest {
  username: string
  password?: string
  displayName: string
  role: string
}

interface CategoryDTO {
  id: number
  name: string
  parentId: number | null
  sortOrder?: number
  createTime?: string
}

interface BackupDTO {
  fileName: string
  size: number
  time: string
}

interface TreeNode {
  id: number
  name: string
  parentId: number | null
  sortOrder?: number
  children?: TreeNode[]
}

// ===========================
//  通用工具函数
// ===========================

function formatTime(time?: string): string {
  if (!time) return '-'
  return time.replace('T', ' ')
}

function formatBackupTime(time?: string): string {
  if (!time) return '-'
  // backup time format is already "yyyyMMdd_HHmmss"
  if (/^\d{8}_\d{6}$/.test(time || '')) {
    const m = time!.match(/^(\d{4})(\d{2})(\d{2})_(\d{2})(\d{2})(\d{2})$/)
    if (m) {
      return `${m[1]}-${m[2]}-${m[3]} ${m[4]}:${m[5]}:${m[6]}`
    }
  }
  return time.replace('T', ' ')
}

function formatSize(bytes: number): string {
  if (bytes == null) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

// ===========================
//  用户管理
// ===========================

const users = ref<UserDTO[]>([])
const usersLoading = ref(false)
const userDialogVisible = ref(false)
const isEditUser = ref(false)
const editUserId = ref<number | null>(null)
const userSaving = ref(false)
const userFormRef = ref<FormInstance>()

const userForm = reactive<CreateUserRequest>({
  username: '',
  password: '',
  displayName: '',
  role: ''
})

const userFormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度 2-50 个字符', trigger: 'blur' }
  ],
  password: [
    {
      validator: (_rule: any, value: string, callback: Function) => {
        if (!isEditUser.value && (!value || value.length === 0)) {
          callback(new Error('请输入密码'))
        } else if (value && value.length > 0 && value.length < 4) {
          callback(new Error('密码至少 4 个字符'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  displayName: [
    { required: true, message: '请输入显示名称', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

async function loadUsers() {
  usersLoading.value = true
  try {
    const res = await request.get('/setting/users')
    users.value = res.data as UserDTO[]
  } catch {
    ElMessage.error('加载用户列表失败')
  } finally {
    usersLoading.value = false
  }
}

function handleAddUser() {
  isEditUser.value = false
  editUserId.value = null
  userForm.username = ''
  userForm.password = ''
  userForm.displayName = ''
  userForm.role = ''
  userDialogVisible.value = true
  resetUserFormValidate()
}

function handleEditUser(row: UserDTO) {
  isEditUser.value = true
  editUserId.value = row.id
  userForm.username = row.username
  userForm.password = ''
  userForm.displayName = row.displayName
  userForm.role = row.role
  userDialogVisible.value = true
  resetUserFormValidate()
}

function resetUserFormValidate() {
  setTimeout(() => {
    userFormRef.value?.clearValidate()
  }, 50)
}

async function handleSaveUser() {
  const valid = await userFormRef.value?.validate().catch(() => false)
  if (!valid) return

  userSaving.value = true
  try {
    const body: CreateUserRequest = {
      username: userForm.username,
      displayName: userForm.displayName,
      role: userForm.role
    }

    if (isEditUser.value && editUserId.value != null) {
      // 编辑模式：密码为空时不传
      if (userForm.password) {
        body.password = userForm.password
      }
      await request.put(`/setting/users/${editUserId.value}`, body)
      ElMessage.success('用户更新成功')
    } else {
      // 新增模式：密码必传
      body.password = userForm.password
      await request.post('/setting/users', body)
      ElMessage.success('用户创建成功')
    }
    userDialogVisible.value = false
    loadUsers()
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || '保存失败'
    ElMessage.error(msg)
  } finally {
    userSaving.value = false
  }
}

async function handleDeleteUser(row: UserDTO) {
  try {
    await request.delete(`/setting/users/${row.id}`)
    ElMessage.success('删除成功')
    loadUsers()
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || '删除失败'
    ElMessage.error(msg)
  }
}

// ===========================
//  分类管理
// ===========================

const categoriesFlat = ref<CategoryDTO[]>([])
const categoriesLoading = ref(false)
const categoryTree = ref<TreeNode[]>([])
const categoryTreeRef = ref<InstanceType<typeof ElTree>>()
const categoryDialogVisible = ref(false)
const isEditCategory = ref(false)
const editCategoryId = ref<number | null>(null)
const categorySaving = ref(false)
const categoryFormRef = ref<FormInstance>()

const treeProps = {
  children: 'children',
  label: 'name',
  value: 'id'
}

interface CategoryForm {
  name: string
  parentId: number | null
  sortOrder: number
}

const categoryForm = reactive<CategoryForm>({
  name: '',
  parentId: null,
  sortOrder: 0
})

const categoryFormRules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { max: 50, message: '分类名称不超过 50 个字符', trigger: 'blur' }
  ],
  parentId: [
    {
      validator: (_rule: any, value: number | null, callback: Function) => {
        // 编辑时不允许选择自己或自己的子分类作为父分类
        if (isEditCategory.value && editCategoryId.value != null && value === editCategoryId.value) {
          callback(new Error('不能将自身设为上级分类'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

/** 将扁平分类列表转为树形结构 */
function buildCategoryTree(flatList: CategoryDTO[]): TreeNode[] {
  const map = new Map<number, TreeNode>()
  const roots: TreeNode[] = []

  // 先创建所有节点
  for (const cat of flatList) {
    map.set(cat.id, {
      id: cat.id,
      name: cat.name,
      parentId: cat.parentId ?? null,
      sortOrder: cat.sortOrder ?? 0,
      children: []
    })
  }

  // 建立父子关系
  for (const cat of flatList) {
    const node = map.get(cat.id)!
    if (cat.parentId != null && map.has(cat.parentId)) {
      const parent = map.get(cat.parentId)!
      parent.children!.push(node)
    } else {
      roots.push(node)
    }
  }

  // 排序：按 sortOrder 升序
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

/** 获取当前分类能被选为上级分类的树（排除自身及子分类） */
function getAvailableParentTree(): TreeNode[] {
  if (!isEditCategory.value || editCategoryId.value == null) {
    return categoryTree.value
  }
  // 深度克隆树
  const clone = JSON.parse(JSON.stringify(categoryTree.value)) as TreeNode[]

  const idsToRemove = new Set<number>()
  function collectDescendants(node: TreeNode) {
    idsToRemove.add(node.id)
    if (node.children) {
      for (const child of node.children) {
        collectDescendants(child)
      }
    }
  }

  // 找到要编辑的节点并收集它的所有子孙
  function findNodeById(nodes: TreeNode[], id: number): TreeNode | null {
    for (const n of nodes) {
      if (n.id === id) return n
      if (n.children) {
        const found = findNodeById(n.children, id)
        if (found) return found
      }
    }
    return null
  }

  const targetNode = findNodeById(clone, editCategoryId.value)
  if (targetNode) {
    collectDescendants(targetNode)
  }

  // 过滤掉要移除的节点
  function filterTree(nodes: TreeNode[]): TreeNode[] {
    return nodes
      .filter(n => !idsToRemove.has(n.id))
      .map(n => ({
        ...n,
        children: n.children ? filterTree(n.children) : []
      }))
  }

  return filterTree(clone)
}

async function loadCategories() {
  categoriesLoading.value = true
  try {
    const res = await request.get('/setting/categories')
    categoriesFlat.value = res.data as CategoryDTO[]
    categoryTree.value = buildCategoryTree(categoriesFlat.value)
  } catch {
    ElMessage.error('加载分类列表失败')
  } finally {
    categoriesLoading.value = false
  }
}

function handleAddCategory() {
  isEditCategory.value = false
  editCategoryId.value = null
  categoryForm.name = ''
  categoryForm.parentId = null
  categoryForm.sortOrder = 0
  categoryDialogVisible.value = true
  resetCategoryFormValidate()
}

function handleEditCategory(data: TreeNode) {
  isEditCategory.value = true
  editCategoryId.value = data.id
  categoryForm.name = data.name
  categoryForm.parentId = data.parentId ?? null
  categoryForm.sortOrder = data.sortOrder ?? 0
  categoryDialogVisible.value = true
  resetCategoryFormValidate()
}

function resetCategoryFormValidate() {
  setTimeout(() => {
    categoryFormRef.value?.clearValidate()
  }, 50)
}

async function handleSaveCategory() {
  const valid = await categoryFormRef.value?.validate().catch(() => false)
  if (!valid) return

  categorySaving.value = true
  try {
    const body = {
      name: categoryForm.name,
      parentId: categoryForm.parentId || null,
      sortOrder: categoryForm.sortOrder
    }

    if (isEditCategory.value && editCategoryId.value != null) {
      await request.put(`/setting/categories/${editCategoryId.value}`, body)
      ElMessage.success('分类更新成功')
    } else {
      await request.post('/setting/categories', body)
      ElMessage.success('分类创建成功')
    }
    categoryDialogVisible.value = false
    loadCategories()
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || '保存失败'
    ElMessage.error(msg)
  } finally {
    categorySaving.value = false
  }
}

// ===========================
//  备份管理
// ===========================

const backups = ref<BackupDTO[]>([])
const backupsLoading = ref(false)
const backingUp = ref(false)

async function loadBackups() {
  backupsLoading.value = true
  try {
    const res = await request.get('/setting/backup/list')
    backups.value = res.data as BackupDTO[]
  } catch {
    ElMessage.error('加载备份列表失败')
  } finally {
    backupsLoading.value = false
  }
}

async function handleBackup() {
  backingUp.value = true
  try {
    const res = await request.post('/setting/backup')
    const data = res.data as BackupDTO
    ElMessage.success(`备份成功: ${data.fileName}`)
    // 刷新备份列表
    loadBackups()
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || '备份失败'
    ElMessage.error(msg)
  } finally {
    backingUp.value = false
  }
}

function handleDownload(fileName: string) {
  const token = authStore.token
  // 直接用 window.location 下载
  const url = `/api/setting/backup/download/${encodeURIComponent(fileName)}`
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  // 如果 token 存在，通过 cookie 或 header 方式不好处理，用 fetch + blob
  // 更稳妥：用 fetch 带 Authorization header
  fetch(url, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
    .then((response) => {
      if (!response.ok) throw new Error('下载失败')
      return response.blob()
    })
    .then((blob) => {
      const blobUrl = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = blobUrl
      a.download = fileName
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(blobUrl)
    })
    .catch(() => {
      ElMessage.error('下载备份文件失败')
    })
}

// ---- 初始化 ----
onMounted(() => {
  if (authStore.isBoss) {
    // 只有老板才能看到设置页面，但按需加载数据
    activeTab.value = 'users'
  }
  loadUsers()
  loadCategories()
  loadBackups()
})
</script>

<style scoped>
.setting-container {
  padding: 20px;
}

.tab-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.tab-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.category-tree-wrap {
  min-height: 200px;
  padding: 8px 0;
}

.category-node {
  display: flex;
  align-items: center;
  width: 100%;
}

.category-node-label {
  flex: 1;
  font-size: 14px;
}

.category-node-actions {
  display: none;
  margin-left: 8px;
}

.category-node:hover .category-node-actions {
  display: inline-flex;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px 0;
}
</style>
