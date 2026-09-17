<template>
  <div>
    <div class="dash-head">
      <div>
        <h2>矫务作战台</h2>
        <div class="muted small">{{ today }} · 数据按司法所隔离，仅展示管辖范围</div>
      </div>
      <button class="ghost" @click="load" :disabled="loading">{{ loading ? '刷新中…' : '刷新数据' }}</button>
    </div>

    <!-- 全区 KPI -->
    <div class="kpi-row">
      <div class="kpi">
        <div class="kpi-num">{{ totals.active }}</div>
        <div class="kpi-label">在矫合计</div>
      </div>
      <div class="kpi">
        <div class="kpi-num warn">{{ totals.dueToday }}</div>
        <div class="kpi-label">今日应报到</div>
      </div>
      <div class="kpi">
        <div class="kpi-num danger">{{ totals.overdue }}</div>
        <div class="kpi-label">逾期未报</div>
      </div>
      <div class="kpi">
        <div class="kpi-num danger">{{ totals.openViolations }}</div>
        <div class="kpi-label">未处理违规红点</div>
      </div>
    </div>

    <div class="grid-dashboard">
      <!-- 左：各所在矫漏斗 -->
      <div>
        <div class="card funnel-card">
          <h3>各司法所在矫漏斗 <span class="muted small">入矫登记 → 在矫 → 请假/训诫 → 收监/解除</span></h3>
          <div class="office-grid">
            <div v-for="o in data.offices" :key="o.officeId" class="office-box"
                 :class="{ alert: o.openViolations > 0 }">
              <div class="ob-head">
                <span class="ob-name">{{ o.officeName }}</span>
                <span v-if="o.openViolations > 0" class="red-pill">{{ o.openViolations }} 起待处理</span>
              </div>
              <div class="funnel"></div>
              <div class="ob-foot">
                <span>在册 {{ o.total }} 人</span>
                <span :class="{ 'hot': o.dueToday > 0 }">今日报到 {{ o.dueToday }}</span>
                <span :class="{ 'hot': o.overdue > 0 }">逾期 {{ o.overdue }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 今日应报到 -->
        <div class="card">
          <h3>今日应报到（{{ data.dueToday.length }}）</h3>
          <div v-if="data.dueToday.length === 0" class="empty">今日暂无待报到人员</div>
          <table v-else class="tbl">
            <thead>
            <tr><th>对象</th><th class="hide-m">状态</th><th class="hide-m">罪名</th><th></th></tr>
            </thead>
            <tbody>
            <tr v-for="d in data.dueToday" :key="d.checkInId" @click="openObj(d.objectId)">
              <td>{{ d.maskedName }}</td>
              <td class="hide-m"><span class="badge" :class="'s-' + d.status">{{ d.statusLabel }}</span></td>
              <td class="hide-m muted">{{ d.crimeType }}</td>
              <td style="text-align:right"><button class="subtle">档案</button></td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 右：红点 + 逾期 -->
      <div>
        <div class="card red-card">
          <h3><span class="dot"></span> 越界与违规红点（{{ data.redDots.length }}）</h3>
          <div v-if="data.redDots.length === 0" class="empty ok-empty">✅ 暂无未处理的越界与违规</div>
          <div v-else class="red-list">
            <div v-for="r in data.redDots" :key="r.violationId" class="red-item"
                 :class="{ boundary: r.type === 'BOUNDARY' }">
              <div class="ri-top" @click="openObj(r.objectId)">
                <span class="ri-type" :class="'t-' + r.type">{{ r.typeLabel }}</span>
                <span class="ri-name">{{ r.maskedName }}</span>
                <span class="spacer"></span>
                <span class="muted small">{{ fmt(r.occurredAt) }}</span>
              </div>
              <div class="ri-detail">{{ r.detail }}</div>
              <div class="ri-actions">
                <button class="subtle" @click="openObj(r.objectId)">查看档案</button>
                <button class="ghost" @click="handle(r)">处理消点</button>
              </div>
            </div>
          </div>
        </div>

        <div class="card">
          <h3>逾期未报到（{{ data.overdue.length }}）</h3>
          <div v-if="data.overdue.length === 0" class="empty">无逾期</div>
          <table v-else class="tbl">
            <tbody>
            <tr v-for="d in data.overdue" :key="d.checkInId" @click="openObj(d.objectId)">
              <td>{{ d.maskedName }}</td>
              <td class="hide-m muted">{{ d.crimeType }}</td>
              <td style="text-align:right"><span class="ri-type t-CHECKIN_MISS">催报到</span></td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 处理红点弹窗 -->
    <div v-if="handling" class="modal-mask" @click.self="handling = null">
      <div class="modal">
        <h3>处理违规 · 消点</h3>
        <p class="muted small" style="margin-bottom:10px">
          {{ handling.typeLabel }}｜{{ handling.maskedName }}<br/>{{ handling.detail }}
        </p>
        <textarea v-model="handleNote" rows="3" placeholder="请填写处理意见（不少于2个字），将随违规记录留痕"></textarea>
        <div class="actions">
          <button class="subtle" @click="handling = null">取消</button>
          <button @click="submitHandle" :disabled="submitting">提交处理</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { api } from '../api.js'
import { toast } from '../store.js'

const router = useRouter()
const loading = ref(false)
const data = reactive({ offices: [], dueToday: [], overdue: [], redDots: [] })
let charts = []

const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })

const totals = computed(() => ({
  active: data.offices.reduce((s, o) => s + o.active, 0),
  dueToday: data.offices.reduce((s, o) => s + o.dueToday, 0),
  overdue: data.offices.reduce((s, o) => s + o.overdue, 0),
  openViolations: data.offices.reduce((s, o) => s + o.openViolations, 0)
}))

async function load() {
  loading.value = true
  try {
    const resp = await api('/api/dashboard')
    Object.assign(data, resp)
    await nextTick()
      renderFunnels()
  } catch (e) {
    toast(e.message || '作战台数据加载失败', 'error')
  } finally {
    loading.value = false
  }
}

function renderFunnels() {
  charts.forEach(c => c.dispose())
  charts = []
  const els = document.querySelectorAll('.funnel')
  data.offices.forEach((o, i) => {
    const el = els[i]
    if (!el) return
    const chart = echarts.init(el)
    chart.setOption({
      grid: { left: 70, right: 16, top: 6, bottom: 6 },
      xAxis: { type: 'value', show: false },
      yAxis: {
        type: 'category',
        inverse: true,
        data: ['入矫登记', '在矫', '请假外出', '训诫', '收监', '解除'],
        axisLine: { show: false }, axisTick: { show: false },
        axisLabel: { color: '#5a6675', fontSize: 11 }
      },
      series: [{
        type: 'bar',
        barWidth: 12,
        data: [
          { value: o.intake, itemStyle: { color: '#9bb8d8' } },
          { value: o.active, itemStyle: { color: '#2e8b57' } },
          { value: o.leave, itemStyle: { color: '#e0a53f' } },
          { value: o.warned, itemStyle: { color: '#e0782f' } },
          { value: o.revoked, itemStyle: { color: '#c9302c' } },
          { value: o.terminated, itemStyle: { color: '#a7b0bc' } }
        ],
        label: { show: true, position: 'right', fontSize: 11, color: '#1f2733' }
      }]
    })
    charts.push(chart)
  })
}

window.addEventListener('resize', () => charts.forEach(c => c.resize()))

function openObj(id) {
  router.push(`/objects/${id}`)
}

function fmt(at) {
  if (!at) return ''
  const d = new Date(at)
  const diff = (Date.now() - d.getTime()) / 1000
  if (diff < 3600) return Math.max(1, Math.round(diff / 60)) + ' 分钟前'
  if (diff < 86400) return Math.round(diff / 3600) + ' 小时前'
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const handling = ref(null)
const handleNote = ref('')
const submitting = ref(false)

function handle(r) {
  handling.value = r
  handleNote.value = ''
}

async function submitHandle() {
  if (handleNote.value.trim().length < 2) {
    toast('请填写不少于2个字的处理意见', 'warn')
    return
  }
  submitting.value = true
  try {
    await api(`/api/violations/${handling.value.violationId}/handle`, {
      method: 'POST',
      body: JSON.stringify({ note: handleNote.value.trim() })
    })
    toast('已处理，红点消除', 'success')
    handling.value = null
    await load()
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.dash-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.dash-head h2 { color: var(--brand-dark); font-size: 20px; margin-bottom: 4px; }

.kpi-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 16px; }
.kpi {
  background: #fff; border-radius: var(--radius); box-shadow: var(--shadow);
  padding: 16px 20px; border-left: 4px solid var(--brand);
}
.kpi-num { font-size: 30px; font-weight: 700; color: var(--brand-dark); }
.kpi-num.warn { color: var(--warn); }
.kpi-num.danger { color: var(--accent); }
.kpi-label { color: var(--ink-2); font-size: 13px; margin-top: 2px; }

.office-box {
  border: 1px solid var(--line); border-radius: 10px; padding: 12px;
  background: #fff;
}
.office-box.alert { border-color: #eeb4b2; box-shadow: 0 0 0 3px rgba(201,48,44,.07); }
.ob-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.ob-name { font-weight: 600; color: var(--brand-dark); font-size: 14px; }
.red-pill {
  background: var(--accent); color: #fff; font-size: 11px;
  padding: 2px 9px; border-radius: 999px;
  animation: pulse 1.6s infinite;
}
@keyframes pulse {
  0%,100% { box-shadow: 0 0 0 0 rgba(201,48,44,.4); }
  50% { box-shadow: 0 0 0 5px rgba(201,48,44,0); }
}
.funnel { height: 150px; }
.ob-foot { display: flex; gap: 12px; font-size: 12px; color: var(--ink-2); margin-top: 4px; }
.ob-foot .hot { color: var(--accent); font-weight: 600; }

.funnel-card { margin-bottom: 16px; }

.red-card { border-top: 3px solid var(--accent); margin-bottom: 16px; }
.red-list { display: flex; flex-direction: column; gap: 10px; max-height: 460px; overflow-y: auto; }
.red-item { border: 1px solid #f0d3d2; border-radius: 9px; padding: 10px 12px; background: #fff7f7; }
.red-item.boundary { border-color: #e8b7ae; background: #fdf4f1; }
.ri-top { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.ri-name { font-weight: 600; font-size: 13px; }
.ri-type {
  font-size: 11px; padding: 2px 8px; border-radius: 4px;
  background: #f3e0de; color: #a23a35;
}
.ri-type.t-BOUNDARY { background: #f8d7d5; color: #b12b27; }
.ri-type.t-CHECKIN_MISS { background: #fde9cf; color: #b06b00; }
.ri-type.t-WARNING { background: #fbe6d6; color: #b55c16; }
.ri-detail { font-size: 12px; color: var(--ink-2); margin: 6px 0 8px; line-height: 1.6; }
.ri-actions { display: flex; gap: 8px; justify-content: flex-end; }
.ri-actions button { padding: 5px 12px; font-size: 12px; }
.ok-empty { color: var(--ok); }

@media (max-width: 1024px) {
  .kpi-row { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 560px) {
  .kpi-row { grid-template-columns: repeat(2, 1fr); gap: 10px; }
  .kpi { padding: 12px 14px; }
  .kpi-num { font-size: 24px; }
}
</style>
