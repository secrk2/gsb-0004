<template>
  <div class="err-page">
    <div class="err-card card">
      <div class="err-icon">{{ kind === 'forbidden' ? '⛔' : kind === 'offline' ? '📡' : '⚠️' }}</div>
      <h2>{{ title }}</h2>
      <p class="muted">{{ detail }}</p>
      <div class="actions">
        <button @click="goDashboard">返回作战台</button>
        <button class="ghost" @click="goBack">返回上一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({ kind: { type: String, default: 'forbidden' }, detail: { type: String, default: '' } })
const router = useRouter()

const title = computed(() => ({
  forbidden: '无权访问该档案',
  offline: '当前处于离线状态',
  error: '页面出错了'
}[props.kind] || '出错了'))

const fallbackDetail = computed(() => props.detail || {
  forbidden: '该对象属于其他司法所管辖，对象间已做数据隔离。本次越权访问已被系统拦截并记录，如有调档需要请走跨所协查流程。',
  offline: '乡村网络不稳定，定位已暂存本机；恢复联网后会自动补报，不会用旧位置冒充当前位置。',
  error: '服务暂时不可用，请稍后重试。'
}[props.kind])

function goDashboard() { router.push('/dashboard') }
function goBack() { router.back() }
</script>

<style scoped>
.err-page {
  min-height: 60vh;
  display: flex; align-items: center; justify-content: center;
  padding: 20px;
}
.err-card {
  max-width: 520px; width: 100%;
  text-align: center;
  padding: 40px 32px;
}
.err-icon { font-size: 52px; margin-bottom: 14px; }
.err-card h2 { color: var(--brand-dark); margin-bottom: 10px; }
.err-card p { line-height: 1.8; margin-bottom: 24px; }
.actions { display: flex; gap: 12px; justify-content: center; flex-wrap: wrap; }
</style>
