<template>
  <div>
    <div class="dash-head">
      <div>
        <h2>对象档案</h2>
        <div class="muted small">姓名默认脱敏显示为「拼音首字母·编号」，查看全名需二次确认并填理由</div>
      </div>
    </div>

    <div class="card filters">
      <input v-model="keyword" class="search" placeholder="搜索编号 / 罪名 / 脱敏姓名" />
      <select v-model="statusFilter">
        <option value="">全部状态</option>
        <option v-for="s in STATUS_OPTS" :key="s.value" :value="s.value">{{ s.label }}</option>
      </select>
      <span class="muted small">共 {{ filtered.length }} 人</span>
    </div>

    <div class="card" style="margin-top:14px">
      <table class="tbl">
        <thead>
        <tr>
          <th>编号</th>
          <th>脱敏姓名</th>
          <th>状态</th>
          <th class="hide-m">所属司法所</th>
          <th class="hide-m">罪名</th>
          <th class="hide-m">矫正截止</th>
          <th>违规</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="o in filtered" :key="o.id" @click="open(o.id)">
          <td class="mono">{{ o.code }}</td>
          <td class="masked">{{ o.maskedName }}</td>
          <td><span class="badge" :class="'s-' + o.status">{{ o.statusLabel }}</span></td>
          <td class="hide-m muted">{{ o.officeName }}</td>
          <td class="hide-m">{{ o.crimeType }}</td>
          <td class="hide-m muted">{{ o.sentenceEnd }}</td>
          <td>
            <span v-if="o.openViolations" class="dot" title="有未处理违规"></span>
            <span v-else class="muted small">—</span>
          </td>
        </tr>
        <tr v-if="filtered.length === 0">
          <td colspan="7"><div class="empty">没有符合条件的对象</div></td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api.js'
import { toast } from '../store.js'

const router = useRouter()
const list = ref([])
const keyword = ref('')
const statusFilter = ref('')

const STATUS_OPTS = [
  { value: 'INTAKE', label: '入矫登记' },
  { value: 'ACTIVE', label: '在矫' },
  { value: 'LEAVE', label: '请假外出' },
  { value: 'WARNED', label: '训诫' },
  { value: 'REVOKED', label: '收监' },
  { value: 'TERMINATED', label: '解除' }
]

const filtered = computed(() => list.value.filter(o => {
  if (statusFilter.value && o.status !== statusFilter.value) return false
  if (keyword.value.trim()) {
    const k = keyword.value.trim().toLowerCase()
    return (o.code + o.maskedName + (o.crimeType || '')).toLowerCase().includes(k)
  }
  return true
}))

function open(id) {
  router.push(`/objects/${id}`)
}

onMounted(async () => {
  try {
    list.value = await api('/api/objects')
  } catch (e) {
    toast(e.message, 'error')
  }
})
</script>

<style scoped>
.dash-head h2 { color: var(--brand-dark); font-size: 20px; margin-bottom: 4px; }
.filters { display: flex; gap: 12px; align-items: center; }
.filters .search { max-width: 320px; }
.filters select { width: 150px; }
.mono { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 13px; }
.masked { letter-spacing: .5px; font-weight: 600; }
@media (max-width: 560px) {
  .filters { flex-wrap: wrap; }
  .filters .search, .filters select { max-width: none; width: 100%; }
}
</style>
