<template>
  <div v-if="forbidden" class="embedded-error">
    <ErrorView kind="forbidden" :detail="forbidden" />
  </div>

  <div v-else-if="detail">
    <div class="back"><a @click="$router.back()">← 返回</a></div>

    <div class="card head-card">
      <div>
        <div class="name-line">
          <h2>{{ detail.maskedName }}</h2>
          <span class="badge" :class="'s-' + detail.status">{{ detail.statusLabel }}</span>
          <span v-if="detail.openViolationCount > 0" class="red-pill">{{ detail.openViolationCount }} 起未处理违规</span>
        </div>
        <div class="code-line muted small">
          {{ detail.code }} · {{ detail.officeName }} · {{ detail.crimeType }}
        </div>
      </div>
      <div class="head-actions">
        <button v-if="canReveal && !detail.nameRevealed" class="ghost" @click="askReveal">
          🔓 查看全名（需填理由）
        </button>
        <span v-else-if="detail.nameRevealed" class="full-name">
          全名：<b>{{ detail.fullName }}</b>
        </span>
      </div>
    </div>

    <div v-if="staff" class="card state-card">
      <h3>状态流转</h3>
      <div class="state-actions">
        <button v-for="a in detail.nextActions" :key="a"
                :class="a === 'TERMINATED' || a === 'REVOKED' ? 'danger' : 'ghost'"
                @click="askTransition(a)">
          转为「{{ statusLabel(a) }}」
        </button>
        <span v-if="detail.nextActions.length === 0" class="muted small">
          「解除」为终态，矫正关系已终止，无后续流转
        </span>
      </div>
      <div class="advanced">
        <span class="muted small">状态机校验演示：</span>
        <select v-model="manualTarget">
          <option value="">尝试非法流转（会被拦截并说明原因）…</option>
          <option v-for="s in ALL_STATUSES" :key="s.value" :value="s.value">{{ s.label }}</option>
        </select>
        <button class="subtle" :disabled="!manualTarget" @click="manualTarget && askTransition(manualTarget)">提交</button>
      </div>
    </div>

    <div class="detail-grid">
      <div>
        <div class="card">
          <h3>基本信息</h3>
          <div class="info-grid">
            <div><label>矫正起止</label><span>{{ detail.sentenceStart }} 至 {{ detail.sentenceEnd }}</span></div>
            <div><label>联系电话</label><span>{{ maskPhone(detail.phone) }}</span></div>
            <div class="span2"><label>居住地址</label><span>{{ detail.address }}</span></div>
            <div><label>保证人/家属</label><span>{{ detail.guardian }}</span></div>
            <div><label>电子围栏</label><span>半径 {{ detail.fenceRadiusMeters }} 米</span></div>
          </div>
        </div>

        <div class="card" style="margin-top:14px">
          <h3>近期轨迹（最近 200 点）<span class="muted small">红点=越界，灰点=断网补报</span></h3>
          <div ref="trackEl" class="track"></div>
          <div class="legend small muted">
            <span><i class="lg online"></i>实时上报</span>
            <span><i class="lg offline"></i>断网恢复补报</span>
            <span><i class="lg home"></i>居住点/围栏圆心</span>
          </div>
        </div>
      </div>

      <div>
        <div class="card">
          <h3>矫正动态时间线</h3>
          <div v-if="timeline.length === 0" class="empty">暂无动态</div>
          <ul v-else class="timeline">
            <li v-for="(e, i) in timeline" :key="i" :class="'ev-' + e.type">
              <div class="ev-dot"></div>
              <div class="ev-body">
                <div class="ev-title">
                  <span class="ev-tag" :class="'t-' + e.type">{{ typeTag(e.type) }}</span>
                  {{ e.title }}
                </div>
                <div v-if="e.detail" class="ev-detail muted small">{{ e.detail }}</div>
                <div class="ev-meta small muted">
                  {{ fmt(e.at) }}<span v-if="e.operator"> · {{ e.operator }}</span>
                </div>
              </div>
            </li>
          </ul>
        </div>

        <div class="card" style="margin-top:14px">
          <h3>请假外出</h3>
          <div v-for="l in leaves" :key="l.id" class="leave-item">
            <div class="row">
              <b>{{ l.destination || '—' }}</b>
              <span class="spacer"></span>
              <span class="leave-st" :class="'ls-' + l.status">{{ leaveText(l.status) }}</span>
            </div>
            <div class="muted small">{{ l.startDate }} 至 {{ l.endDate }} · {{ l.reason }}</div>
            <div v-if="l.status === 'PENDING' && staff" class="row" style="margin-top:8px">
              <button @click="decide(l.id, true)">批准并转请假外出</button>
              <button class="subtle" @click="decide(l.id, false)">驳回</button>
            </div>
          </div>
          <div v-if="leaves.length === 0" class="empty">暂无请假记录</div>
        </div>

        <div v-if="staff" class="card" style="margin-top:14px">
          <h3>全名查阅留痕</h3>
          <div v-if="nameLogs.length === 0" class="empty">暂无查阅记录</div>
          <ul v-else class="view-logs">
            <li v-for="l in nameLogs" :key="l.id">
              <b>{{ l.viewerName }}</b>（{{ roleText(l.viewerRole) }}）
              <span class="muted small">{{ fmt(l.createdAt) }}</span>
              <div class="small">理由：{{ l.reason }}</div>
            </li>
          </ul>
        </div>
      </div>
    </div>

    <!-- 查看全名二次确认 -->
    <div v-if="revealing" class="modal-mask" @click.self="revealing = false">
      <div class="modal">
        <h3>⚠️ 敏感操作二次确认</h3>
        <p class="muted small" style="line-height:1.8">
          您即将查看对象 <b>{{ detail.maskedName }}（{{ detail.code }}）</b> 的真实姓名。<br/>
          按最小必要原则，请填写查阅理由；提交后系统将<b>永久留痕</b>（操作人、时间、理由），可被审计追溯。
        </p>
        <textarea v-model="revealReason" rows="3" placeholder="例如：办理跨所协查，需核对本人身份信息（不少于4个字）"></textarea>
        <div class="actions">
          <button class="subtle" @click="revealing = false">取消</button>
          <button class="danger" @click="confirmReveal" :disabled="submitting">确认查看并留痕</button>
        </div>
      </div>
    </div>

    <!-- 状态流转 -->
    <div v-if="transitionTarget" class="modal-mask" @click.self="transitionTarget = ''">
      <div class="modal">
        <h3>状态流转：{{ detail.statusLabel }} → {{ statusLabel(transitionTarget) }}</h3>
        <p v-if="['WARNED','REVOKED','TERMINATED'].includes(transitionTarget)" class="warn-tip">
          该流转影响对象重大权益，请如实填写事由并存档。
        </p>
        <textarea v-model="transitionReason" rows="3" placeholder="请填写流转事由（不少于2个字）"></textarea>
        <div class="actions">
          <button class="subtle" @click="transitionTarget = ''">取消</button>
          <button @click="confirmTransition" :disabled="submitting">确认流转</button>
        </div>
      </div>
    </div>
  </div>

  <div v-else class="empty">加载中…</div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { api, ApiError } from '../api.js'
import { session, toast } from '../store.js'
import ErrorView from './ErrorView.vue'

const props = defineProps({ id: { type: String, required: true } })

const detail = ref(null)
const timeline = ref([])
const leaves = ref([])
const nameLogs = ref([])
const track = ref([])
const forbidden = ref('')
const trackEl = ref(null)
let chart = null

const staff = computed(() => {
  const r = session.user?.role
  return r === 'SUPERVISOR' || r === 'OFFICER'
})
const canReveal = staff

const ALL_STATUSES = [
  { value: 'INTAKE', label: '入矫登记' },
  { value: 'ACTIVE', label: '在矫' },
  { value: 'LEAVE', label: '请假外出' },
  { value: 'WARNED', label: '训诫' },
  { value: 'REVOKED', label: '收监' },
  { value: 'TERMINATED', label: '解除' }
]
function statusLabel(s) { return ALL_STATUSES.find(x => x.value === s)?.label || s }
function leaveText(s) { return { PENDING: '待审批', APPROVED: '已批准', REJECTED: '已驳回', RETURNED: '已销假' }[s] || s }
function roleText(r) { return { SUPERVISOR: '监管员', OFFICER: '司法所干警', OBJECT: '矫正对象' }[r] || r }
function typeTag(t) { return { STATUS: '状态', VIOLATION: '违规', LEAVE: '请假' }[t] || t }
function maskPhone(p) { return p || '—' }
function fmt(at) {
  if (!at) return ''
  return new Date(at).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

async function load() {
  forbidden.value = ''
  try {
    detail.value = await api(`/api/objects/${props.id}`)
    const [tl, lv, nv, tr] = await Promise.all([
      api(`/api/objects/${props.id}/timeline`),
      api(`/api/objects/${props.id}/leaves`),
      staff.value ? api(`/api/objects/${props.id}/name-view-logs`) : Promise.resolve([]),
      api(`/api/locations/objects/${props.id}/track`)
    ])
    timeline.value = tl
    leaves.value = lv
    nameLogs.value = nv
    track.value = tr
    await nextTick()
    renderTrack()
  } catch (e) {
    if (e instanceof ApiError && (e.status === 403 || e.status === 404)) {
      // 越权/不存在：呈现明确错误态，而不是空白页
      forbidden.value = e.message
    } else {
      toast(e.message || '档案加载失败', 'error')
    }
  }
}

function renderTrack() {
  if (!trackEl.value || track.value.length === 0) {
    if (trackEl.value) trackEl.value.innerHTML = '<div class="empty">暂无轨迹数据</div>'
    return
  }
  if (chart) chart.dispose()
  chart = echarts.init(trackEl.value)
  const points = [...track.value].reverse()
  const d = detail.value
  const online = points.filter(p => p.source !== 'OFFLINE_SYNC')
    .map(p => [p.lng, p.lat])
  const offline = points.filter(p => p.source === 'OFFLINE_SYNC')
    .map(p => [p.lng, p.lat])

  const series = [
    {
      type: 'lines', coordinateSystem: 'cartesian2d',
      polyline: true,
      data: [{ coords: points.map(p => [p.lng, p.lat]) }],
      lineStyle: { color: '#1d4e89', width: 2, opacity: .55 }
    },
    {
      name: '实时', type: 'scatter', data: online, symbolSize: 7,
      itemStyle: { color: '#1d4e89' }
    },
    {
      name: '断网补报', type: 'scatter', data: offline, symbolSize: 9,
      itemStyle: { color: '#c9302c', borderColor: '#fff', borderWidth: 1 }
    }
  ]
  if (d.homeLng != null && d.homeLat != null) {
    series.push({
      name: '居住点', type: 'scatter', data: [[d.homeLng, d.homeLat]], symbolSize: 14,
      itemStyle: { color: '#2e8b57' }, z: 5
    })
    const rLng = (d.fenceRadiusMeters || 500) / (111320 * Math.cos(d.homeLat * Math.PI / 180))
    const rLat = (d.fenceRadiusMeters || 500) / 111320
    const ring = []
    for (let i = 0; i <= 64; i++) {
      const a = i / 64 * Math.PI * 2
      ring.push([d.homeLng + rLng * Math.cos(a), d.homeLat + rLat * Math.sin(a)])
    }
    series.push({
      type: 'lines', coordinateSystem: 'cartesian2d', polyline: true,
      data: [{ coords: ring }],
      lineStyle: { color: '#2e8b57', width: 1.5, type: 'dashed', opacity: .7 },
      silent: true, z: 1
    })
  }
  chart.setOption({
    grid: { left: 56, right: 16, top: 20, bottom: 34 },
    tooltip: {
      trigger: 'item',
      formatter: (p) => {
        if (p.seriesType === 'scatter') {
          return `${p.seriesName}<br/>${p.value[1].toFixed(5)}, ${p.value[0].toFixed(5)}`
        }
        return ''
      }
    },
    xAxis: { type: 'value', name: '经度', nameLocation: 'middle', nameGap: 22,
      axisLabel: { formatter: v => Number(v).toFixed(3), fontSize: 10 } },
    yAxis: { type: 'value', name: '纬度',
      axisLabel: { formatter: v => Number(v).toFixed(3), fontSize: 10 } },
    series
  })
  window.addEventListener('resize', resizeChart)
}
function resizeChart() { chart && chart.resize() }

// ---- 全名二次确认 ----
const revealing = ref(false)
const revealReason = ref('')
const submitting = ref(false)

function askReveal() {
  revealing.value = true
  revealReason.value = ''
}
async function confirmReveal() {
  if (revealReason.value.trim().length < 4) {
    toast('查阅理由不少于4个字', 'warn')
    return
  }
  submitting.value = true
  try {
    detail.value = await api(`/api/objects/${props.id}/reveal-name`, {
      method: 'POST',
      body: JSON.stringify({ reason: revealReason.value.trim() })
    })
    toast('已记录查阅留痕', 'success')
    revealing.value = false
    nameLogs.value = await api(`/api/objects/${props.id}/name-view-logs`)
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    submitting.value = false
  }
}

// ---- 状态流转 ----
const transitionTarget = ref('')
const transitionReason = ref('')
const manualTarget = ref('')

function askTransition(target) {
  transitionTarget.value = target
  transitionReason.value = ''
}
async function confirmTransition() {
  if (transitionReason.value.trim().length < 2) {
    toast('流转事由不少于2个字', 'warn')
    return
  }
  submitting.value = true
  try {
    detail.value = await api(`/api/objects/${props.id}/transitions`, {
      method: 'POST',
      body: JSON.stringify({ toStatus: transitionTarget.value, reason: transitionReason.value.trim() })
    })
    toast(`已流转为「${statusLabel(transitionTarget.value)}」`, 'success')
    transitionTarget.value = ''
    manualTarget.value = ''
    timeline.value = await api(`/api/objects/${props.id}/timeline`)
  } catch (e) {
    // 非法回退被拦下：把后端的原因完整提示出来
    toast(e.message, 'error', 6000)
  } finally {
    submitting.value = false
  }
}

async function decide(leaveId, approve) {
  try {
    await api(`/api/leaves/${leaveId}/decision?approve=${approve}`, { method: 'POST', body: '{}' })
    toast(approve ? '已批准，对象状态转为请假外出' : '已驳回', 'success')
    await load()
  } catch (e) {
    toast(e.message, 'error')
  }
}

onMounted(load)
</script>

<style scoped>
.back { margin-bottom: 10px; }
.back a { cursor: pointer; color: var(--ink-2); }

.head-card { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.name-line { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.name-line h2 { color: var(--brand-dark); font-size: 22px; }
.code-line { margin-top: 6px; }
.red-pill {
  background: var(--accent); color: #fff; font-size: 12px;
  padding: 3px 10px; border-radius: 999px;
}
.full-name b { color: var(--accent); font-size: 16px; letter-spacing: 1px; }

.state-card { margin-top: 14px; }
.state-actions { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 12px; }
.advanced { display: flex; gap: 10px; align-items: center; border-top: 1px dashed var(--line); padding-top: 12px; }
.advanced select { width: auto; max-width: 300px; }
.warn-tip {
  background: #fdeede; color: #b55c16; border-radius: 6px;
  padding: 8px 12px; font-size: 13px; margin-bottom: 10px;
}

.detail-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-top: 14px;
}
@media (max-width: 1024px) { .detail-grid { grid-template-columns: 1fr; } }

.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px 20px; }
.info-grid .span2 { grid-column: span 2; }
.info-grid label { display: block; color: var(--ink-2); font-size: 12px; margin-bottom: 2px; }

.track { height: 300px; }
.legend { display: flex; gap: 16px; margin-top: 6px; }
.lg { display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-right: 4px; }
.lg.online { background: #1d4e89; }
.lg.offline { background: #c9302c; }
.lg.home { background: #2e8b57; }

.timeline { list-style: none; }
.timeline li { position: relative; padding: 0 0 16px 20px; border-left: 2px solid var(--line); }
.timeline li:last-child { padding-bottom: 0; }
.ev-dot {
  position: absolute; left: -6px; top: 4px;
  width: 10px; height: 10px; border-radius: 50%;
  background: var(--brand); border: 2px solid #fff;
}
.ev-VIOLATION .ev-dot { background: var(--accent); }
.ev-LEAVE .ev-dot { background: var(--warn); }
.ev-tag {
  font-size: 11px; padding: 1px 7px; border-radius: 4px;
  background: var(--brand-light); color: var(--brand);
  margin-right: 6px;
}
.ev-tag.t-VIOLATION { background: #fdeceb; color: var(--accent); }
.ev-tag.t-LEAVE { background: #fff3df; color: #b06b00; }
.ev-title { font-size: 13px; }
.ev-detail { margin: 3px 0; line-height: 1.6; }

.leave-item { padding: 10px 0; border-bottom: 1px dashed var(--line); }
.leave-item:last-child { border-bottom: none; }
.leave-st { font-size: 12px; padding: 2px 9px; border-radius: 999px; }
.ls-PENDING { background: #fff3df; color: #b06b00; }
.ls-APPROVED { background: #e3f4ea; color: #237a49; }
.ls-REJECTED { background: #fdeceb; color: var(--accent); }
.ls-RETURNED { background: #eceff3; color: #6b7480; }

.view-logs { list-style: none; display: flex; flex-direction: column; gap: 10px; }
.view-logs li { background: #f7f9fc; border-radius: 8px; padding: 8px 12px; }

.embedded-error :deep(.err-page) { min-height: auto; }

@media (max-width: 560px) {
  .head-card { flex-direction: column; align-items: flex-start; }
  .info-grid { grid-template-columns: 1fr; }
  .info-grid .span2 { grid-column: span 1; }
}
</style>
