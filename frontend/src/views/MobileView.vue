<template>
  <div class="mobile-wrap">
    <!-- 离线横幅：断网必提示 -->
    <div class="offline-banner" :class="!state.online ? 'is-offline' : (state.queue.length ? 'is-pending' : 'is-online')">
      <template v-if="!state.online">
        <span class="ob-icon">📡</span>
        <div>
          <b>当前网络不可用（乡村弱网/断网）</b>
          <div class="small">定位不会丢失：已在本机暂存 <b>{{ state.queue.length }}</b> 条真实定位，
            恢复联网后自动按时间顺序补报；系统禁止用旧位置冒充当前位置。</div>
        </div>
      </template>
      <template v-else-if="state.queue.length > 0">
        <span class="ob-icon">🔄</span>
        <div>
          <b>网络已恢复，待补报 {{ state.queue.length }} 条</b>
          <div class="small">将按定位真实时间升序补报，服务端逐条幂等去重、同位置合并，不产生重复轨迹点。</div>
        </div>
        <button @click="syncQueue" :disabled="state.syncing">{{ state.syncing ? '补报中…' : '立即补报' }}</button>
      </template>
      <template v-else>
        <span class="ob-icon">✅</span>
        <div><b>网络正常</b><div class="small">定位实时上报，每条均带设备生成时间与唯一编号</div></div>
      </template>
    </div>

    <!-- 本人状态卡 -->
    <div v-if="detail" class="card profile-card">
      <div class="pc-top">
        <div>
          <div class="pc-name">{{ detail.fullName || detail.maskedName }}</div>
          <div class="muted small">{{ detail.code }} · {{ detail.officeName }}</div>
        </div>
        <span class="badge" :class="'s-' + detail.status">{{ detailStatusLabel }}</span>
      </div>
      <div class="pc-crime muted small">罪名：{{ detail.crimeType }} ｜ 矫正截止：{{ detail.sentenceEnd }}</div>

      <!-- 报到 -->
      <div class="checkin-row">
        <div>
          <div class="ci-title">今日报到</div>
          <div class="muted small">{{ todayCheckedIn ? '今日已完成报到' : '今日尚未报到，请在规定时间内完成' }}</div>
        </div>
        <button v-if="!todayCheckedIn" @click="doCheckIn">立即报到</button>
        <span v-else class="done-tag">✓ 已报到</span>
      </div>

      <!-- 销假 -->
      <div v-if="detail.status === 'LEAVE'" class="checkin-row">
        <div>
          <div class="ci-title">请假外出中</div>
          <div class="muted small">返回居住地后请及时销假</div>
        </div>
        <button @click="doReturn">我已返回，销假</button>
      </div>
    </div>

    <!-- 定位状态 -->
    <div class="card loc-card">
      <h3>我的定位</h3>
      <div class="loc-state">
        <span class="loc-dot" :class="gpsClass"></span>
        <span>{{ state.gps }}</span>
        <span class="spacer"></span>
        <label class="net-switch small">
          演示断网
          <input type="checkbox" :checked="!state.online" @change="toggleNet" />
        </label>
      </div>

      <div v-if="lastFix" class="last-fix small muted">
        最近定位：{{ lastFix.lat.toFixed(5) }}, {{ lastFix.lng.toFixed(5) }}
        ｜{{ formatTime(lastFix.recordedAt) }}
        ｜精度 ±{{ Math.round(lastFix.accuracy || 15) }}m
      </div>

      <div class="sim-box">
        <div class="small muted" style="margin-bottom:6px">定位采集（无 GPS 环境可用下方按钮模拟真实设备定位）：</div>
        <div class="sim-grid">
          <button class="ghost" @click="simulateFix(0, 0)">原地定位（应合并）</button>
          <button class="ghost" @click="simulateFix(0.00045, 0.0002)">移动约 50 米（新增轨迹）</button>
          <button class="danger" @click="simulateFix(0.0075, 0.004)">偏移约 900 米（越界）</button>
          <button class="subtle" @click="simulateStale()">上报 10 分钟前旧位置（应被拒）</button>
        </div>
        <div class="small muted" style="margin-top:6px">
          重复点击同一条定位（网络重试）也不会产生第二个轨迹点——每点带唯一编号，服务端幂等。
        </div>
        <button class="ghost retry-btn" @click="retryLast" :disabled="!lastFix || state.online === false">
          重传上一条定位（验证幂等）
        </button>
      </div>

      <!-- 受理结果 -->
      <div v-if="state.results.length" class="results">
        <div v-for="(r, i) in state.results.slice(0, 6)" :key="i" class="result-item" :class="'r-' + r.outcome">
          <span class="r-tag">{{ outcomeText(r.outcome) }}</span>
          <span class="r-msg">{{ r.message }}</span>
        </div>
      </div>
    </div>

    <!-- 离线队列 -->
    <div v-if="state.queue.length" class="card queue-card">
      <h3>本机待补报队列（{{ state.queue.length }}）</h3>
      <div class="small muted" style="margin-bottom:8px">已持久化存储，退出页面或关机后仍在；恢复网络自动补报。</div>
      <div class="queue-list">
        <div v-for="q in state.queue.slice().reverse().slice(0, 8)" :key="q.clientId" class="queue-item small">
          <span>{{ formatTime(q.recordedAt) }}</span>
          <span>{{ q.lat.toFixed(5) }},{{ q.lng.toFixed(5) }}</span>
          <span class="muted">{{ q.tag || '真实定位' }}</span>
        </div>
      </div>
    </div>

    <!-- 请假 -->
    <div v-if="detail" class="card">
      <h3>请假外出申请</h3>
      <div v-if="detail.status !== 'ACTIVE'" class="small muted">
        当前为「{{ detailStatusLabel }}」状态，仅在矫对象可提交请假申请
      </div>
      <template v-else>
        <div class="leave-form">
          <input v-model="leaveForm.reason" placeholder="请假事由（不少于2字）" />
          <input v-model="leaveForm.destination" placeholder="目的地" />
          <div class="row">
            <input v-model="leaveForm.startDate" type="date" />
            <input v-model="leaveForm.endDate" type="date" />
          </div>
          <button @click="applyLeave">提交申请</button>
        </div>
      </template>
      <div v-for="l in leaves" :key="l.id" class="my-leave small">
        <span class="leave-st" :class="'ls-' + l.status">{{ leaveText(l.status) }}</span>
        {{ l.destination }} · {{ l.startDate }}~{{ l.endDate }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { api, ApiError } from '../api.js'
import { session, toast } from '../store.js'

const QUEUE_KEY = 'jwt_location_queue_v1'

// 非安全上下文（http://局域网IP）下 crypto.randomUUID 不可用，做降级
function uuid() {
  if (window.crypto?.randomUUID) return window.crypto.randomUUID()
  return 'id-' + Date.now().toString(36) + '-' +
    Array.from({ length: 4 }, () => Math.floor(Math.random() * 0x10000).toString(16).padStart(4, '0')).join('')
}

const detail = ref(null)
const leaves = ref([])
const checkIns = ref([])
const lastFix = ref(null)
let watchId = null
let simAnchor = null

const state = reactive({
  online: navigator.onLine,
  gps: '等待定位…',
  queue: loadQueue(),
  results: [],
  syncing: false
})

const detailStatusLabel = computed(() => detail.value?.statusLabel || '')
const todayStr = new Date().toISOString().slice(0, 10)
const todayCheckedIn = computed(() =>
  checkIns.value.some(c => c.dueDate === todayStr && c.status === 'DONE'))

const gpsClass = computed(() => {
  if (!state.online) return 'off'
  if (state.gps.startsWith('定位过期') || state.gps.includes('旧位置')) return 'bad'
  if (state.gps.startsWith('已定位')) return 'ok'
  return ''
})

// ---------- 队列持久化 ----------
function loadQueue() {
  try { return JSON.parse(localStorage.getItem(QUEUE_KEY) || '[]') } catch { return [] }
}
function persistQueue() {
  localStorage.setItem(QUEUE_KEY, JSON.stringify(state.queue))
}

// ---------- 距离 ----------
function distanceMeters(a, b) {
  const R = 6371000
  const dLat = (b.lat - a.lat) * Math.PI / 180
  const dLng = (b.lng - a.lng) * Math.PI / 180
  const s = Math.sin(dLat / 2) ** 2
    + Math.cos(a.lat * Math.PI / 180) * Math.cos(b.lat * Math.PI / 180) * Math.sin(dLng / 2) ** 2
  return R * 2 * Math.atan2(Math.sqrt(s), Math.sqrt(1 - s))
}
const MERGE_M = 8

// ---------- 采集一条定位 ----------
async function ingestFix(fix, { isRetry = false } = {}) {
  lastFix.value = fix
  state.gps = state.online ? `已定位 ${formatTime(fix.recordedAt)}` : '离线定位已暂存'

  // 客户端侧同位置合并：锚点取服务端最新点或队列最后一点
  const anchor = currentAnchor()
  if (!isRetry && anchor && distanceMeters(anchor, fix) < MERGE_M) {
    pushResult('MERGED', `与上一位置相距不足 ${MERGE_M} 米，判定位置未变更，已合并（不新增轨迹点）`)
    return
  }

  if (state.online) {
    try {
      const resp = await api('/api/locations/report', {
        method: 'POST',
        body: JSON.stringify(fix)
      })
      const r = resp.results[0]
      if (r.outcome === 'STORED') simAnchor = { lat: fix.lat, lng: fix.lng }
      pushResult(r.outcome, r.message)
      if (r.outcome === 'STORED' || r.outcome === 'MERGED') refreshTrackQuietly()
    } catch (e) {
      if (e instanceof ApiError && e.status === 0) {
        enqueue(fix)
      } else {
        // 旧位置/非法时间：服务端拒绝，明确提示，不入队、不冒充
        pushResult('REJECTED', e.message)
        state.gps = '旧位置被拒绝'
      }
    }
  } else {
    enqueue(fix)
  }
}

function currentAnchor() {
  if (simAnchor) return simAnchor
  const q = state.queue[state.queue.length - 1]
  return q ? { lat: q.lat, lng: q.lng } : (lastFix.value ? { lat: lastFix.value.lat, lng: lastFix.value.lng } : null)
}

function enqueue(fix) {
  state.queue.push(fix)
  persistQueue()
  pushResult('QUEUED', `离线暂存定位（${formatTime(fix.recordedAt)}），恢复网络后补报`)
}

function pushResult(outcome, message) {
  state.results.unshift({ outcome, message })
  if (state.results.length > 10) state.results.pop()
}

function outcomeText(o) {
  return { STORED: '新增轨迹', MERGED: '合并', REJECTED: '已拒绝', QUEUED: '已暂存', IDEMPOTENT: '幂等' }[o] || o
}

// ---------- 断网恢复：批量幂等补报 ----------
async function syncQueue() {
  if (state.queue.length === 0 || state.syncing) return
  state.syncing = true
  try {
    const ordered = state.queue.slice().sort((a, b) =>
      new Date(a.recordedAt) - new Date(b.recordedAt))
    const resp = await api('/api/locations/sync', {
      method: 'POST',
      body: JSON.stringify({ points: ordered })
    })
    // 所有结果（新增/合并/拒绝）服务端都已登记幂等台账，本地清空，绝不重复补报
    state.queue = []
    persistQueue()
    const last = ordered[ordered.length - 1]
    if (last) simAnchor = { lat: last.lat, lng: last.lng }
    pushResult('STORED',
      `补报完成：共 ${resp.total} 条，新增轨迹 ${resp.stored}、位置未变更合并 ${resp.merged}、异常拒绝 ${resp.rejected}，无重复轨迹点`)
    toast(`补报完成：新增 ${resp.stored} / 合并 ${resp.merged} / 拒绝 ${resp.rejected}`, 'success', 5000)
    await loadBase()
  } catch (e) {
    if (e instanceof ApiError && e.status === 0) {
      toast('网络仍不可用，继续保留离线队列', 'warn')
    } else {
      toast(e.message, 'error')
    }
  } finally {
    state.syncing = false
  }
}

// ---------- 网络事件 ----------
function onOnline() {
  state.online = true
  state.gps = '网络已恢复'
  toast('网络已恢复，准备补报离线定位…', 'success')
  setTimeout(syncQueue, 400)
}
function onOffline() {
  state.online = false
  state.gps = '已断网，定位将暂存本机'
  toast('已进入离线模式，定位将暂存本机', 'warn', 5000)
}
function toggleNet(e) {
  // 演示用：手动切换联网状态
  const goOffline = e.target.checked
  if (goOffline) onOffline()
  else onOnline()
}

// ---------- GPS ----------
function startWatch() {
  if (!navigator.geolocation) {
    state.gps = '设备不支持定位，可用下方模拟按钮'
    return
  }
  watchId = navigator.geolocation.watchPosition(
    (pos) => {
      // 在线时如果设备给出的是陈旧定位，明确提示，不拿旧位置糊弄
      const ageSec = (Date.now() - pos.timestamp) / 1000
      if (state.online && ageSec > 300) {
        state.gps = `定位过期（${Math.round(ageSec)} 秒前），等待新定位，不发送旧位置`
        return
      }
      ingestFix({
        clientId: uuid(),
        lat: pos.coords.latitude,
        lng: pos.coords.longitude,
        accuracy: pos.coords.accuracy,
        recordedAt: new Date(pos.timestamp).toISOString()
      })
    },
    () => { state.gps = '定位权限未授予，可用下方模拟按钮演示' },
    { enableHighAccuracy: true, maximumAge: 0, timeout: 15000 }
  )
}

// ---------- 模拟（乡村真机之外的演示/测试通道） ----------
let simBase = null
async function simulateFix(dLat, dLng) {
  await ensureSimBase()
  const base = { lat: simBase.lat + dLat, lng: simBase.lng + dLng }
  if (dLat === 0 && dLng === 0) {
    // 原地：保持与锚点重合
    const a = currentAnchor() || simBase
    base.lat = a.lat
    base.lng = a.lng
  }
  const fix = {
    clientId: uuid(),
    lat: base.lat,
    lng: base.lng,
    accuracy: 12,
    recordedAt: new Date().toISOString()
  }
  await ingestFix(fix)
}

async function simulateStale() {
  await ensureSimBase()
  const a = currentAnchor() || simBase
  const fix = {
    clientId: uuid(),
    lat: a.lat + 0.0001,
    lng: a.lng + 0.0001,
    accuracy: 12,
    recordedAt: new Date(Date.now() - 10 * 60 * 1000).toISOString() // 10 分钟前
  }
  lastFix.value = fix
  await ingestFix(fix)
}

async function retryLast() {
  if (!lastFix.value) return
  // 重传必须沿用同一 clientId —— 服务端据此幂等去重
  await ingestFix(lastFix.value, { isRetry: true })
  toast('已用相同编号重传，服务端不会重复落点', 'info')
}

async function ensureSimBase() {
  if (simBase) return
  // 以本人围栏圆心为模拟基准
  if (detail.value?.homeLat) {
    simBase = { lat: detail.value.homeLat, lng: detail.value.homeLng }
  } else {
    simBase = { lat: 32.0312, lng: 118.7866 }
  }
}

async function refreshTrackQuietly() {
  try {
    const tr = await api(`/api/locations/objects/${session.user.objectId}/track`)
    if (tr.length && !simAnchor) {
      const latest = tr[0]
      simAnchor = { lat: latest.lat, lng: latest.lng }
    }
  } catch { /* ignore */ }
}

// ---------- 报到 / 销假 / 请假 ----------
async function doCheckIn() {
  try {
    await api('/api/me/check-in', { method: 'POST' })
    toast('报到成功', 'success')
    await loadBase()
  } catch (e) { toast(e.message, 'error') }
}

async function doReturn() {
  try {
    detail.value = await api('/api/me/return', { method: 'POST' })
    toast('销假成功，状态已恢复为在矫', 'success')
    await loadBase()
  } catch (e) { toast(e.message, 'error') }
}

const leaveForm = reactive({ reason: '', destination: '', startDate: todayStr, endDate: todayStr })
async function applyLeave() {
  if (leaveForm.reason.trim().length < 2) return toast('请填写请假事由', 'warn')
  try {
    await api(`/api/objects/${session.user.objectId}/leaves`, {
      method: 'POST',
      body: JSON.stringify({ ...leaveForm })
    })
    toast('请假申请已提交，等待司法所审批', 'success')
    leaveForm.reason = ''
    leaveForm.destination = ''
    await loadBase()
  } catch (e) { toast(e.message, 'error') }
}

function leaveText(s) { return { PENDING: '待审批', APPROVED: '已批准', REJECTED: '已驳回', RETURNED: '已销假' }[s] || s }
function formatTime(at) {
  return new Date(at).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

// ---------- 加载 ----------
async function loadBase() {
  const id = session.user.objectId
  detail.value = await api(`/api/objects/${id}`)
  const [lv, ci] = await Promise.all([
    api(`/api/objects/${id}/leaves`),
    api(`/api/objects/${id}/check-ins`)
  ])
  leaves.value = lv
  checkIns.value = ci
  await refreshTrackQuietly()
}

onMounted(async () => {
  window.addEventListener('online', onOnline)
  window.addEventListener('offline', onOffline)
  try {
    await loadBase()
    startWatch()
    if (state.queue.length > 0 && navigator.onLine) {
      toast(`检测到 ${state.queue.length} 条未上报定位，自动补报中…`, 'info', 4000)
      setTimeout(syncQueue, 800)
    }
  } catch (e) {
    toast(e.message, 'error')
  }
})

onUnmounted(() => {
  window.removeEventListener('online', onOnline)
  window.removeEventListener('offline', onOffline)
  if (watchId != null) navigator.geolocation?.clearWatch(watchId)
})
</script>

<style scoped>
.mobile-wrap { max-width: 480px; margin: 0 auto; display: flex; flex-direction: column; gap: 12px; }

.offline-banner {
  border-radius: 10px;
  padding: 12px 14px;
  display: flex; align-items: center; gap: 10px;
  line-height: 1.5;
}
.offline-banner.is-offline {
  background: #fdeceb; border: 1px solid #f5c6c4; color: #8f211e;
}
.offline-banner.is-online {
  background: #e9f6ee; border: 1px solid #bfe3cd; color: #1f6b41;
}
.offline-banner.is-pending { background: #fff7e6; border: 1px solid #f3d9a8; color: #8a5a12; }
.ob-icon { font-size: 22px; }
.offline-banner button { margin-left: auto; padding: 7px 14px; font-size: 13px; white-space: nowrap; }

.profile-card .pc-top { display: flex; align-items: center; justify-content: space-between; }
.pc-name { font-size: 18px; font-weight: 700; color: var(--brand-dark); }
.pc-crime { margin: 6px 0 12px; }
.checkin-row {
  display: flex; align-items: center; justify-content: space-between;
  border-top: 1px dashed var(--line); padding-top: 12px; margin-top: 12px;
}
.ci-title { font-weight: 600; font-size: 14px; }
.done-tag { color: var(--ok); font-weight: 600; font-size: 14px; }

.loc-dot {
  width: 10px; height: 10px; border-radius: 50%;
  background: #b7c0cc; display: inline-block; margin-right: 2px;
}
.loc-dot.ok { background: var(--ok); box-shadow: 0 0 0 3px rgba(46,139,87,.18); }
.loc-dot.bad { background: var(--accent); box-shadow: 0 0 0 3px rgba(201,48,44,.18); }
.loc-dot.off { background: var(--warn); }
.loc-state { display: flex; align-items: center; gap: 8px; font-size: 14px; margin-bottom: 8px; }
.net-switch { display: flex; align-items: center; gap: 6px; color: var(--ink-2); }
.net-switch input { width: auto; }
.last-fix { margin-bottom: 10px; }

.sim-box {
  background: #f7f9fc; border-radius: 8px; padding: 10px 12px; margin-top: 6px;
}
.sim-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.sim-grid button { font-size: 12px; padding: 8px 6px; line-height: 1.4; }
.retry-btn { width: 100%; margin-top: 8px; font-size: 12px; padding: 7px; }

.results { margin-top: 10px; display: flex; flex-direction: column; gap: 6px; }
.result-item {
  display: flex; gap: 8px; align-items: flex-start;
  font-size: 12px; line-height: 1.5;
  padding: 7px 10px; border-radius: 7px;
}
.r-tag {
  flex-shrink: 0; font-size: 11px; padding: 1px 7px; border-radius: 4px;
  background: #e5edf7; color: #35618f; white-space: nowrap;
}
.result-item.r-STORED { background: #eef8f2; }
.result-item.r-STORED .r-tag { background: #d4efe0; color: #1f6b41; }
.result-item.r-MERGED { background: #f3f6fa; color: var(--ink-2); }
.result-item.r-IDEMPOTENT { background: #f3f6fa; color: var(--ink-2); }
.result-item.r-REJECTED { background: #fdeceb; }
.result-item.r-REJECTED .r-tag { background: #f8d7d5; color: #b12b27; }
.result-item.r-QUEUED { background: #fff7e6; }
.result-item.r-QUEUED .r-tag { background: #fbe4bc; color: #8a5a12; }

.queue-card { border-left: 4px solid var(--warn); }
.queue-list { display: flex; flex-direction: column; gap: 4px; }
.queue-item {
  display: flex; gap: 10px;
  background: #fffaf0; border-radius: 6px; padding: 5px 10px;
}
.queue-item span:nth-child(2) { font-family: ui-monospace, Menlo, monospace; }

.leave-form { display: flex; flex-direction: column; gap: 8px; margin-bottom: 10px; }
.leave-form .row { gap: 8px; }
.my-leave { padding: 6px 0; border-top: 1px dashed var(--line); }
.leave-st {
  font-size: 11px; padding: 1px 8px; border-radius: 999px; margin-right: 6px;
  background: #eef2f7; color: var(--ink-2);
}
.ls-PENDING { background: #fff3df; color: #b06b00; }
.ls-APPROVED { background: #e3f4ea; color: #237a49; }
.ls-REJECTED { background: #fdeceb; color: var(--accent); }
.ls-RETURNED { background: #eceff3; color: #6b7480; }

@media (max-width: 390px) {
  .mobile-wrap { gap: 10px; }
  .card { padding: 13px; }
  .sim-grid { grid-template-columns: 1fr; }
}
</style>
