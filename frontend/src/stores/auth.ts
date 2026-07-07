import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getCurrentUser, type UserInfo } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  // ---- state ----
  const token = ref<string>(localStorage.getItem('token') || '')
  const user = ref<UserInfo | null>(loadUser())

  function loadUser(): UserInfo | null {
    try {
      const raw = localStorage.getItem('user')
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  }

  function saveUser(u: UserInfo) {
    localStorage.setItem('user', JSON.stringify(u))
  }

  function saveToken(t: string) {
    localStorage.setItem('token', t)
  }

  function clearAuth() {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  // ---- getters ----
  const isLoggedIn = computed(() => !!token.value)
  const isBoss = computed(() => user.value?.role === 'BOSS')

  // ---- actions ----
  async function loginAction(username: string, password: string) {
    const result = await loginApi({ username, password })
    token.value = result.token
    saveToken(result.token)

    const u: UserInfo = {
      username,
      displayName: result.displayName,
      role: result.role
    }
    user.value = u
    saveUser(u)
  }

  async function fetchUser() {
    try {
      const u = await getCurrentUser()
      user.value = u
      saveUser(u)
    } catch {
      // token 失效时清除本地状态
      clearAuth()
      token.value = ''
      user.value = null
    }
  }

  async function logoutAction() {
    try {
      await logoutApi()
    } catch {
      // 即使接口失败也清除本地状态
    } finally {
      clearAuth()
      token.value = ''
      user.value = null
    }
  }

  return {
    token,
    user,
    isLoggedIn,
    isBoss,
    loginAction,
    fetchUser,
    logoutAction
  }
})
