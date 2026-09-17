<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <div class="logo">矫</div>
        <h1>矫务通</h1>
        <p>清河区社区矫正管理平台</p>
      </div>

      <form @submit.prevent="doLogin">
        <label class="field">
          <span>账号</span>
          <input v-model="username" placeholder="监管员 / 干警 / 对象账号" autocomplete="username" />
        </label>
        <label class="field">
          <span>密码</span>
          <input v-model="password" type="password" placeholder="请输入密码"
                 autocomplete="current-password" @keyup.enter="doLogin" />
        </label>

        <div v-if="error" class="error-msg">{{ error }}</div>

        <button class="login-btn" type="submit" :disabled="loading">
          {{ loading ? '登录中…' : '登 录' }}
        </button>
      </form>

      <div class="quick">
        <div class="quick-title">开箱体验账号（密码均为 123456）</div>
        <div class="quick-grid">
          <button type="button" @click="fill('jiandu')">监管员<br/><b>jiandu</b></button>
          <button type="button" @click="fill('jing1')">城东干警<br/><b>jing1</b></button>
          <button type="button" @click="fill('obj0001')">矫正对象<br/><b>obj0001</b></button>
          <button type="button" @click="fill('obj0005')">越界对象<br/><b>obj0005</b></button>
        </div>
      </div>
    </div>
    <div class="login-foot">内部业务系统 · 操作全程留痕 · 仅供授权人员使用</div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, ApiError } from '../api.js'
import { saveLogin } from '../store.js'

const router = useRouter()
const route = useRoute()
const username = ref('jiandu')
const password = ref('123456')
const loading = ref(false)
const error = ref('')

function fill(name) {
  username.value = name
  password.value = '123456'
  error.value = ''
}

async function doLogin() {
  if (!username.value.trim() || !password.value) {
    error.value = '请输入账号和密码'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const resp = await api('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username: username.value.trim(), password: password.value })
    })
    saveLogin(resp)
    const redirect = route.query.redirect
    if (redirect && typeof redirect === 'string' && !redirect.startsWith('//')) {
      router.push(redirect)
    } else {
      router.push(resp.role === 'OBJECT' ? '/mobile' : '/dashboard')
    }
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background:
      radial-gradient(1200px 500px at 80% -10%, rgba(255,255,255,.18), transparent),
      linear-gradient(135deg, #143a66 0%, #1d4e89 55%, #2b6cb0 100%);
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 24px;
}
.login-card {
  width: 400px; max-width: 100%;
  background: #fff;
  border-radius: 16px;
  padding: 34px 34px 26px;
  box-shadow: 0 30px 80px rgba(8, 25, 48, .35);
}
.login-brand { text-align: center; margin-bottom: 22px; }
.login-brand .logo {
  width: 58px; height: 58px; border-radius: 14px;
  background: var(--brand);
  color: #fff; font-size: 30px; font-weight: 700;
  display: inline-flex; align-items: center; justify-content: center;
  margin-bottom: 10px;
}
.login-brand h1 { font-size: 24px; color: var(--brand-dark); letter-spacing: 3px; }
.login-brand p { color: var(--ink-2); font-size: 13px; margin-top: 4px; }

.field { display: block; margin-bottom: 14px; }
.field span { display: block; font-size: 13px; color: var(--ink-2); margin-bottom: 6px; }
.login-btn {
  width: 100%; padding: 11px;
  background: var(--brand);
  font-size: 15px; letter-spacing: 6px;
  border-radius: 8px;
  margin-top: 4px;
}
.error-msg {
  background: #fdeceb; color: var(--accent);
  border: 1px solid #f5c6c4;
  padding: 8px 12px; border-radius: 6px;
  font-size: 13px; margin-bottom: 12px;
}
.quick { margin-top: 22px; border-top: 1px dashed var(--line); padding-top: 14px; }
.quick-title { font-size: 12px; color: var(--ink-2); margin-bottom: 8px; }
.quick-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.quick-grid button {
  background: #f3f6fa; color: var(--ink);
  font-size: 11px; line-height: 1.5;
  padding: 8px 4px; border-radius: 8px;
}
.quick-grid button b { color: var(--brand); font-weight: 600; }
.quick-grid button:hover { background: var(--brand-light); }
.login-foot { margin-top: 18px; color: rgba(255,255,255,.65); font-size: 12px; }

@media (max-width: 420px) {
  .login-card { padding: 26px 20px 20px; }
}
</style>
