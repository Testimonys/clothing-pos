<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <el-icon class="logo-icon"><Goods /></el-icon>
        <div class="logo-text">
          <h2>华兴鞋服城</h2>
          <span class="logo-sub">服装零售管理系统</span>
        </div>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#3a1e23"
        text-color="#c9b4ba"
        active-text-color="#f6ecee"
      >
        <el-menu-item index="/pos">
          <el-icon><Sell /></el-icon>
          <span>收银台</span>
        </el-menu-item>
        <el-menu-item index="/product">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/stock">
          <el-icon><Box /></el-icon>
          <span>库存管理</span>
        </el-menu-item>
        <el-menu-item index="/orders">
          <el-icon><Document /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item v-if="authStore.isBoss" index="/report">
          <el-icon><TrendCharts /></el-icon>
          <span>销售报表</span>
        </el-menu-item>
        <el-menu-item v-if="authStore.isBoss" index="/setting">
          <el-icon><Setting /></el-icon>
          <span>系统设置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 右侧主体 -->
    <el-container>
      <!-- 顶部栏 -->
      <el-header class="header">
        <div class="header-left">
          <span class="welcome-text">
            欢迎，{{ authStore.user?.displayName || '用户' }}
            <el-tag v-if="authStore.isBoss" type="danger" size="small" style="margin-left: 8px">管理员</el-tag>
            <el-tag v-else type="info" size="small" style="margin-left: 8px">店员</el-tag>
          </span>
        </div>
        <div class="header-right">
          <el-button type="danger" size="small" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeMenu = computed(() => {
  const path = route.path
  // 匹配一级子路由路径，比如 /pos
  return path
})

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    // 用户取消
    return
  }
  await authStore.logoutAction()
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background-color: #3a1e23;
  overflow-y: auto;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #a8585f, #7a2f3e);
  color: #fff;
  font-size: 20px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-text {
  line-height: 1.1;
}

.logo-text h2 {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
  margin: 0;
  white-space: nowrap;
}

.logo-sub {
  display: block;
  color: #8f6a72;
  font-size: 10px;
  letter-spacing: 2px;
  margin-top: 3px;
  white-space: nowrap;
}

.sidebar .el-menu {
  border-right: none;
}

.sidebar :deep(.el-menu-item) {
  height: 48px;
  margin: 2px 8px;
  border-radius: 8px;
  transition: background-color 0.2s cubic-bezier(0.4, 0, 0.2, 1), color 0.2s;
}

.sidebar :deep(.el-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.06);
  color: #f6ecee;
}

.sidebar :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(168, 88, 95, 0.35), rgba(168, 88, 95, 0.12));
  color: #f6ecee;
}

.sidebar :deep(.el-menu-item.is-active .el-icon) {
  color: #e8b7a3;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid var(--border-color, #e6e6e6);
  padding: 0 20px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
}

.welcome-text {
  font-size: 14px;
  color: var(--text-primary, #333);
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
}

.el-main {
  background: #fbf7f7;
  padding: 0;
}
</style>
