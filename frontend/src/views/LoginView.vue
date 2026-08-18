<template>
  <div class="login-wrapper">
    <el-card class="login-card" shadow="always">
      <template #header>
        <div class="login-brand">
          <div class="login-logo"><el-icon :size="26"><Goods /></el-icon></div>
          <h2 class="login-title">华兴鞋服城</h2>
          <p class="login-sub">服装零售管理系统</p>
        </div>
      </template>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.loginAction(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/pos')
  } catch (err: any) {
    const msg = err?.response?.data?.message || '登录失败，请检查用户名和密码'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    linear-gradient(150deg, rgba(74, 36, 48, 0.62) 0%, rgba(58, 30, 35, 0.78) 55%, rgba(44, 22, 26, 0.85) 100%),
    url('/images/login-bg.jpg') center / cover no-repeat;
  background-color: #3a1e23;
  position: relative;
  overflow: hidden;
}

/* 背景光斑装饰（缓慢漂移动画） */
.login-wrapper::before,
.login-wrapper::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  pointer-events: none;
  will-change: transform;
}
.login-wrapper::before {
  width: 440px;
  height: 440px;
  background: radial-gradient(circle, #a8585f, transparent 70%);
  top: -130px;
  right: -90px;
  opacity: 0.6;
  animation: drift-a 24s ease-in-out infinite alternate;
}
.login-wrapper::after {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, #8c3b4a, transparent 70%);
  bottom: -150px;
  left: -100px;
  opacity: 0.55;
  animation: drift-b 30s ease-in-out infinite alternate;
}
@keyframes drift-a {
  to { transform: translate(50px, 36px) scale(1.15); }
}
@keyframes drift-b {
  to { transform: translate(-40px, -28px) scale(1.1); }
}

/* 毛玻璃卡片 */
.login-card {
  width: 420px;
  border-radius: 22px;
  padding: 10px;
  position: relative;
  z-index: 1;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.10);
  backdrop-filter: blur(24px) saturate(160%);
  -webkit-backdrop-filter: blur(24px) saturate(160%);
  border: 1px solid rgba(255, 255, 255, 0.20);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.28),
    inset 0 0 0 1px rgba(255, 255, 255, 0.04),
    0 20px 60px rgba(20, 8, 12, 0.45);
  animation: card-in 0.8s cubic-bezier(0.22, 1, 0.36, 1) both;
  transition: box-shadow 0.4s cubic-bezier(0.22, 1, 0.36, 1);
}
.login-card:hover {
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.32),
    inset 0 0 0 1px rgba(255, 255, 255, 0.06),
    0 30px 80px rgba(20, 8, 12, 0.6);
}
/* 卡片入场 */
@keyframes card-in {
  from { opacity: 0; transform: translateY(30px); filter: blur(10px); }
  to { opacity: 1; transform: none; filter: none; }
}
/* 扫光特效（hover 时从左到右滑过） */
.login-card::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 60%;
  height: 100%;
  background: linear-gradient(100deg, transparent, rgba(255, 255, 255, 0.12), transparent);
  transform: translateX(-160%) skewX(-18deg);
  transition: transform 0.7s cubic-bezier(0.4, 0, 0.2, 1);
  pointer-events: none;
}
.login-card:hover::after {
  transform: translateX(240%) skewX(-18deg);
}

.login-card :deep(.el-card__header) {
  border-bottom: none;
  padding: 26px 28px 6px;
}

.login-brand {
  text-align: center;
}

.login-logo {
  width: 56px;
  height: 56px;
  margin: 0 auto 14px;
  border-radius: 16px;
  background: linear-gradient(135deg, #d98b91, #a8585f 55%, #7a2f3e);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 26px rgba(122, 47, 62, 0.45);
}

.login-title {
  margin: 0;
  font-size: 28px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 3px;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.2);
}

.login-sub {
  margin: 8px 0 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.65);
  letter-spacing: 4px;
}

/* 玻璃化的表单 */
.login-card :deep(.el-form-item__label) {
  color: rgba(255, 255, 255, 0.9);
  font-size: 15px;
  font-weight: 500;
}
.login-card :deep(.el-form-item) {
  margin-bottom: 22px;
}
.login-card :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.12);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.18);
  border-radius: 10px;
  height: 46px;
  padding: 0 14px;
  transition: box-shadow 0.25s, background 0.25s;
}
.login-card :deep(.el-input__wrapper:hover) {
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.3);
}
.login-card :deep(.el-input__wrapper.is-focus) {
  background: rgba(255, 255, 255, 0.18);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.5),
    0 0 0 4px rgba(168, 88, 95, 0.3);
}
.login-card :deep(.el-input__inner) {
  color: #fff;
  font-size: 16px;
}
.login-card :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.5);
}
.login-card :deep(.el-input__prefix .el-icon) {
  color: rgba(255, 255, 255, 0.65);
  font-size: 18px;
}

/* 登录按钮 */
.login-card :deep(.el-button--primary) {
  height: 46px;
  font-size: 17px;
  letter-spacing: 6px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #b4636c, #8c3b4a);
  box-shadow: 0 10px 26px rgba(122, 47, 62, 0.4);
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.25s, filter 0.25s;
}
.login-card :deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 14px 32px rgba(122, 47, 62, 0.55);
  filter: brightness(1.05);
}
.login-card :deep(.el-button--primary:active) {
  transform: translateY(0) scale(0.97);
}
</style>
