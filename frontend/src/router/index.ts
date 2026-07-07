import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import MainLayout from '@/views/layout/MainLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/pos',
    children: [
      {
        path: 'pos',
        name: 'Pos',
        component: () => import('@/views/pos/PosView.vue'),
        meta: { title: '收银台', icon: 'Sell', requiresAuth: true }
      },
      {
        path: 'product',
        name: 'Product',
        component: () => import('@/views/product/ProductView.vue'),
        meta: { title: '商品管理', icon: 'Goods', requiresAuth: true }
      },
      {
        path: 'stock',
        name: 'Stock',
        component: () => import('@/views/stock/StockView.vue'),
        meta: { title: '库存管理', icon: 'Box', requiresAuth: true }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/orders/OrdersView.vue'),
        meta: { title: '订单管理', icon: 'Document', requiresAuth: true }
      },
      {
        path: 'setting',
        name: 'Setting',
        component: () => import('@/views/setting/SettingView.vue'),
        meta: { title: '系统设置', icon: 'Setting', requiresAuth: true, bossOnly: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()

  // 已登录用户访问 /login → 重定向到 /pos
  if (to.path === '/login' && authStore.isLoggedIn) {
    next('/pos')
    return
  }

  // 需要认证但未登录 → 跳转 /login
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next('/login')
    return
  }

  // bossOnly 路由需要 boss 角色
  if (to.meta.bossOnly && !authStore.isBoss) {
    next('/pos')
    return
  }

  next()
})

export default router
