<template>
  <div class="page">
    <!-- 页头 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon"><i class="fa fa-shield"></i></div>
        <div>
          <h1>攻击监控</h1>
          <p>实时指标 · 可疑 IP 溯源 · 6小时攻击概率预测</p>
        </div>
      </div>
      <div class="ip-selector">
        <label>监控目标</label>
        <select v-model="selectedIp" @change="onIpChange" class="ip-select">
          <option value="">请选择监控目标</option>
          <option v-for="t in targets" :key="t.ipAddress" :value="t.ipAddress">{{ t.ipAddress }}</option>
        </select>
      </div>
    </div>

    <!-- 无目标提示 -->
    <div v-if="!selectedIp" class="empty-page">
      <div class="empty-icon"><i class="fa fa-shield"></i></div>
      <p>请先在「监控控制台」启动对目标 IP 的监控，或从上方下拉框选择已有目标</p>
    </div>

    <template v-else>
      <!-- 4个指标卡片 -->
      <div class="metrics-row">
        <div class="metric-card" v-for="m in metricCards" :key="m.key">
          <div class="mc-header">
            <span class="mc-label">{{ m.label }}</span>
            <div class="mc-icon" :style="{ background: m.bg, color: m.color }">
              <i :class="m.icon"></i>
            </div>
          </div>
          <p class="mc-val">{{ snapshot[m.key] ?? '—' }}<span v-if="snapshot[m.key] != null" class="mc-unit">{{ m.unit }}</span></p>
          <div class="mc-bar-wrap">
            <div class="mc-bar" :style="{ width: barPct(snapshot[m.key], m.max) + '%', background: m.color }"></div>
          </div>
          <p class="mc-trend" :class="m.trend(snapshot[m.key])">
            <i :class="m.trend(snapshot[m.key]) === 'trend-up' ? 'fa fa-arrow-up' : 'fa fa-check'"></i>
            {{ m.trendLabel(snapshot[m.key]) }}
          </p>
        </div>
      </div>

      <!-- 预测图表 + 告警时间线 -->
      <div class="main-grid">
        <!-- 预测折线图 -->
        <div class="card">
          <div class="card-head" style="justify-content:space-between">
            <span><i class="fa fa-line-chart" style="color:#6366f1"></i> 6小时攻击概率预测</span>
            <button class="icon-btn" :disabled="predLoading" @click="fetchPrediction">
              <i :class="predLoading ? 'fa fa-spinner fa-spin' : 'fa fa-refresh'"></i>
            </button>
          </div>
          <div class="card-body chart-body">
            <div v-if="predLoading" class="chart-placeholder">
              <div class="spinner"></div>
              <p>DeepSeek 正在预测...</p>
            </div>
            <div v-else-if="predError" class="chart-placeholder">
              <div class="empty-icon"><i class="fa fa-exclamation-triangle"></i></div>
              <p>{{ predError }}</p>
            </div>
            <canvas v-else ref="chartCanvas" class="chart-canvas"></canvas>
          </div>
        </div>

        <!-- 告警时间线 -->
        <div class="card">
          <div class="card-head" style="justify-content:space-between">
            <span><i class="fa fa-bell" style="color:#ef4444"></i> 近期告警时间线</span>
            <button v-if="admin && recentAlerts.length" class="icon-btn icon-btn--danger" title="清空时间线告警" @click="clearTimeline">
              <i class="fa fa-trash"></i>
            </button>
          </div>
          <div class="card-body" style="padding:0; max-height:320px; overflow-y:auto">
            <div v-if="recentAlerts.length === 0" class="empty-state-sm">
              <i class="fa fa-check-circle"></i> 暂无告警记录
            </div>
            <div v-for="a in recentAlerts" :key="a.id" class="timeline-item">
              <div class="tl-dot" :class="'tl-dot--' + levelClass(a.level)"></div>
              <div class="tl-content">
                <div class="tl-header">
                  <span class="tl-badge" :class="'badge-' + levelClass(a.level)">{{ levelLabel(a.level) }}</span>
                  <span class="tl-time">{{ formatTime(a.createTime) }}</span>
                  <button v-if="admin" class="tl-del" title="删除该告警" @click="deleteTimelineAlert(a)">
                    <i class="fa fa-trash"></i>
                  </button>
                </div>
                <p class="tl-msg">{{ a.message }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 可疑 IP 溯源表 -->
      <div class="card" style="margin-top:1.25rem">
        <div class="card-head" style="justify-content:space-between">
          <span><i class="fa fa-map-marker" style="color:#ef4444"></i> 可疑 IP 溯源</span>
          <button class="icon-btn" @click="fetchSuspiciousIps"><i class="fa fa-refresh"></i></button>
        </div>
        <div class="card-body" style="padding:0">
          <table class="data-table">
            <thead>
              <tr>
                <th>IP 地址</th>
                <th>国家/地区</th>
                <th>城市</th>
                <th>ISP / 运营商</th>
                <th>首次发现</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="suspIpsLoading">
                <td colspan="5" class="empty-cell"><i class="fa fa-spinner fa-spin"></i></td>
              </tr>
              <tr v-else-if="suspiciousIps.length === 0">
                <td colspan="5" class="empty-cell">暂无可疑 IP 记录</td>
              </tr>
              <tr v-for="ip in suspiciousIps" :key="ip.ipAddress">
                <td class="mono">{{ ip.ipAddress }}</td>
                <td>{{ ip.countryFlag }} {{ ip.country || '未知' }}</td>
                <td>{{ ip.city || '—' }}</td>
                <td>{{ ip.isp || '—' }}</td>
                <td class="text-muted">{{ formatTime(ip.firstSeen) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import api from '../services/api.js'
import { isAdmin } from '../services/auth.js'
import { useToast } from '../composables/useToast.js'

const { success, error } = useToast()
const admin = isAdmin()

const selectedIp = ref('')
const targets = ref([])
const snapshot = ref({})
const recentAlerts = ref([])
const suspiciousIps = ref([])
const suspIpsLoading = ref(false)
const predLoading = ref(false)
const predError = ref('')
const chartCanvas = ref(null)
let chartInstance = null
let pollTimer = null

const metricCards = [
  {
    key: 'halfOpenConns', label: '半开连接数', unit: '', icon: 'fa fa-plug',
    bg: '#fff7ed', color: '#f97316', max: 200,
    trend: v => v > 50 ? 'trend-up' : 'trend-ok',
    trendLabel: v => v > 50 ? '超过阈值(50)' : '正常范围'
  },
  {
    key: 'requestRate', label: '请求速率', unit: '/min', icon: 'fa fa-tachometer',
    bg: '#eff6ff', color: '#3b82f6', max: 1000,
    // Slowloris 多线程并发建连时汇总频率会很高，高频不代表正常，仅作参考展示
    trend: () => 'trend-ok',
    trendLabel: v => v != null ? `${v} 次/分钟（参考值）` : '—'
  },
  {
    key: 'avgPacketSize', label: '平均包大小', unit: 'B', icon: 'fa fa-database',
    bg: '#f5f3ff', color: '#8b5cf6', max: 2048,
    trend: v => v < 1024 ? 'trend-up' : 'trend-ok',
    trendLabel: v => v < 1024 ? '异常偏小' : '正常范围'
  },
  {
    key: 'uniqueSourceIps', label: '唯一来源 IP', unit: '', icon: 'fa fa-users',
    bg: '#f0fdf4', color: '#22c55e', max: 500,
    trend: () => 'trend-ok',
    trendLabel: () => '已统计'
  },
]

function barPct(val, max) {
  if (val == null) return 0
  return Math.min((val / max) * 100, 100)
}

function levelClass(lvl) {
  return { 4: 'critical', 3: 'high', 2: 'medium', 1: 'low' }[lvl] || 'low'
}

function levelLabel(lvl) {
  return { 4: '严重', 3: '高危', 2: '中危', 1: '低危' }[lvl] || '低危'
}

function formatTime(t) {
  if (!t) return '—'
  try { return new Date(t).toLocaleString('zh-CN') } catch { return t }
}

function countryFlag(code) {
  if (!code || code.length !== 2) return ''
  return String.fromCodePoint(...[...code.toUpperCase()].map(c => 0x1F1E0 + c.charCodeAt(0) - 65))
}

async function fetchTargets() {
  try {
    const res = await api.get('/api/monitor/targets')
    targets.value = res.data || []
    if (targets.value.length && !selectedIp.value) {
      selectedIp.value = targets.value[0].ipAddress
      onIpChange()
    }
  } catch { /* silent */ }
}

async function fetchStatus() {
  if (!selectedIp.value) return
  try {
    const res = await api.get(`/api/monitor/status/${selectedIp.value}`)
    if (res.data?.latestSnapshot) {
      snapshot.value = res.data.latestSnapshot
      extractSuspiciousIps(res.data.latestSnapshot)
    }
  } catch { /* silent */ }
}

function extractSuspiciousIps(snp) {
  if (!snp?.aiVerdict) return
  try {
    const r = JSON.parse(snp.aiVerdict)
    if (r.suspiciousIps?.length) {
      suspiciousIps.value = r.suspiciousIps.map(ip => ({
        ...ip,
        countryFlag: countryFlag(ip.countryCode),
        firstSeen: snp.createTime
      }))
    }
  } catch { /* silent */ }
}

async function fetchAlerts() {
  try {
    const res = await api.get('/api/alerts?pageSize=20')
    recentAlerts.value = (res.data?.records || res.data || []).slice(0, 20)
  } catch { recentAlerts.value = [] }
}

async function deleteTimelineAlert(a) {
  if (!window.confirm('确认删除这条告警记录？')) return
  try {
    await api.delete(`/api/alerts/${a.id}`)
    recentAlerts.value = recentAlerts.value.filter(x => x.id !== a.id)
    success('告警已删除')
  } catch (e) {
    error(e?.response?.data?.message || '删除失败')
  }
}

async function clearTimeline() {
  if (!recentAlerts.value.length) return
  if (!window.confirm(`确认清空时间线上的 ${recentAlerts.value.length} 条告警？此操作不可恢复。`)) return
  const ids = recentAlerts.value.map(a => a.id)
  try {
    await api.delete('/api/alerts', { ids })
    recentAlerts.value = []
    success('告警时间线已清空')
  } catch (e) {
    error(e?.response?.data?.message || '清空失败')
  }
}

async function fetchSuspiciousIps() {
  suspIpsLoading.value = true
  await fetchStatus()
  suspIpsLoading.value = false
}

async function fetchPrediction() {
  if (!selectedIp.value) return
  predLoading.value = true
  predError.value = ''
  try {
    const res = await api.get(`/api/monitor/predict/${selectedIp.value}`)
    const points = res.data || []
    if (!points.length) { predError.value = '暂无历史数据，无法生成预测'; return }
    await nextTick()
    renderChart(points)
  } catch (e) {
    predError.value = e?.response?.data?.message || '获取预测数据失败'
  } finally {
    predLoading.value = false
  }
}

async function renderChart(points) {
  const { Chart } = await import('chart.js')
  if (chartInstance) { chartInstance.destroy(); chartInstance = null }
  await nextTick()
  if (!chartCanvas.value) return
  const labels = points.map(p => p.time)
  const data   = points.map(p => (p.probability * 100).toFixed(1))
  chartInstance = new Chart(chartCanvas.value, {
    type: 'line',
    data: {
      labels,
      datasets: [{
        label: '攻击概率 (%)',
        data,
        borderColor: '#6366f1',
        backgroundColor: 'rgba(99,102,241,0.08)',
        borderWidth: 2,
        pointRadius: 3,
        pointBackgroundColor: '#6366f1',
        fill: true,
        tension: 0.4
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: ctx => ` 攻击概率: ${ctx.parsed.y}%`
          }
        }
      },
      scales: {
        y: {
          min: 0, max: 100,
          ticks: { callback: v => v + '%', color: '#94a3b8', font: { size: 11 } },
          grid: { color: '#f1f5f9' }
        },
        x: {
          ticks: { color: '#94a3b8', font: { size: 11 } },
          grid: { display: false }
        }
      }
    }
  })
}

function onIpChange() {
  snapshot.value = {}
  recentAlerts.value = []
  suspiciousIps.value = []
  if (chartInstance) { chartInstance.destroy(); chartInstance = null }
  if (!selectedIp.value) return
  startPolling()
  fetchAlerts()
  fetchPrediction()
}

function startPolling() {
  stopPolling()
  fetchStatus()
  pollTimer = setInterval(() => { fetchStatus(); fetchAlerts() }, 30000)
}

function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

onMounted(() => fetchTargets())
onUnmounted(() => {
  stopPolling()
  if (chartInstance) chartInstance.destroy()
})
</script>

<style scoped>
.page { padding: 0; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 1rem; }
.header-left { display: flex; align-items: center; gap: 0.875rem; }
.header-icon {
  width: 2.75rem; height: 2.75rem;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-radius: 0.75rem;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 1.125rem;
  box-shadow: 0 4px 12px rgba(99,102,241,0.3);
}
.page-header h1 { font-size: 1.375rem; font-weight: 700; color: #1e293b; margin: 0; }
.page-header p  { font-size: 0.8125rem; color: #64748b; margin: 0.125rem 0 0; }
.ip-selector { display: flex; align-items: center; gap: 0.625rem; }
.ip-selector label { font-size: 0.8125rem; font-weight: 600; color: #374151; }
.ip-select {
  padding: 0.4375rem 0.75rem; border: 1px solid #d1d5db; border-radius: 0.5rem;
  font-size: 0.875rem; color: #1e293b; background: #fff; outline: none;
  min-width: 10rem; cursor: pointer;
}
.ip-select:focus { border-color: #6366f1; box-shadow: 0 0 0 3px rgba(99,102,241,0.1); }

.empty-page {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 5rem 1rem; gap: 1rem; color: #94a3b8; text-align: center;
}
.empty-page .empty-icon { width: 5rem; height: 5rem; background: #f1f5f9; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 2rem; }
.empty-page p { font-size: 0.9375rem; max-width: 22rem; line-height: 1.6; }

/* Metric cards */
.metrics-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; margin-bottom: 1.25rem; }
.metric-card {
  background: #fff; border: 1px solid #e2e8f0; border-radius: 0.875rem;
  padding: 1.125rem; box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.mc-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.625rem; }
.mc-label { font-size: 0.8125rem; font-weight: 500; color: #64748b; }
.mc-icon { width: 2rem; height: 2rem; border-radius: 0.5rem; display: flex; align-items: center; justify-content: center; font-size: 0.875rem; }
.mc-val { font-size: 1.625rem; font-weight: 700; color: #1e293b; margin: 0 0 0.25rem; line-height: 1.2; }
.mc-unit { font-size: 0.75rem; font-weight: 500; color: #94a3b8; margin-left: 0.2rem; }
.mc-bar-wrap { height: 4px; background: #f1f5f9; border-radius: 2px; overflow: hidden; margin-bottom: 0.5rem; }
.mc-bar { height: 100%; border-radius: 2px; transition: width 0.4s ease; }
.mc-trend { font-size: 0.75rem; display: flex; align-items: center; gap: 0.25rem; margin: 0; }
.trend-up { color: #ef4444; }
.trend-ok { color: #22c55e; }

/* Main grid */
.main-grid { display: grid; grid-template-columns: 3fr 2fr; gap: 1.25rem; }
.card { background: #fff; border: 1px solid #e2e8f0; border-radius: 0.875rem; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.card-head {
  display: flex; align-items: center; gap: 0.5rem;
  padding: 1rem 1.25rem; border-bottom: 1px solid #f1f5f9;
  font-size: 0.9375rem; font-weight: 600; color: #1e293b;
}
.card-body { padding: 1.25rem; }
.chart-body { height: 280px; display: flex; align-items: center; justify-content: center; }
.chart-canvas { width: 100% !important; height: 100% !important; }

.chart-placeholder { display: flex; flex-direction: column; align-items: center; gap: 0.75rem; color: #94a3b8; }
.spinner { width: 2rem; height: 2rem; border: 3px solid #e0e7ff; border-top-color: #6366f1; border-radius: 50%; animation: spin 0.8s linear infinite; }
.empty-icon { width: 3rem; height: 3rem; background: #f1f5f9; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 1.25rem; }

/* Timeline */
.empty-state-sm { padding: 2rem; text-align: center; color: #94a3b8; font-size: 0.875rem; display: flex; align-items: center; justify-content: center; gap: 0.4rem; }
.timeline-item { display: flex; gap: 0.75rem; padding: 0.875rem 1.25rem; border-bottom: 1px solid #f1f5f9; }
.timeline-item:last-child { border-bottom: none; }
.tl-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; margin-top: 0.375rem; }
.tl-dot--critical { background: #dc2626; }
.tl-dot--high     { background: #ea580c; }
.tl-dot--medium   { background: #ca8a04; }
.tl-dot--low      { background: #22c55e; }
.tl-content { flex: 1; min-width: 0; }
.tl-header { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.25rem; }
.tl-badge { font-size: 0.6875rem; font-weight: 700; padding: 0.15rem 0.5rem; border-radius: 9999px; }
.badge-critical { background: #fee2e2; color: #dc2626; }
.badge-high     { background: #ffedd5; color: #ea580c; }
.badge-medium   { background: #fefce8; color: #ca8a04; }
.badge-low      { background: #dcfce7; color: #16a34a; }
.tl-time { font-size: 0.75rem; color: #94a3b8; margin-left: auto; }
.tl-del {
  background: none; border: none; cursor: pointer; color: #cbd5e1;
  font-size: 0.75rem; padding: 0.1rem 0.25rem; margin-left: 0.25rem; transition: color 0.15s;
}
.tl-del:hover { color: #ef4444; }
.tl-msg { font-size: 0.8125rem; color: #475569; margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* Table */
.data-table { width: 100%; border-collapse: collapse; font-size: 0.875rem; }
.data-table th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.8125rem; font-weight: 600; color: #64748b; background: #f8fafc; border-bottom: 1px solid #e2e8f0; }
.data-table td { padding: 0.75rem 1.25rem; border-bottom: 1px solid #f1f5f9; color: #1e293b; }
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: #fafafa; }
.mono { font-family: 'Courier New', monospace; }
.text-muted { color: #94a3b8; }
.empty-cell { text-align: center; padding: 2.5rem; color: #94a3b8; }
.icon-btn { background: none; border: 1px solid #e2e8f0; border-radius: 0.375rem; padding: 0.25rem 0.5rem; cursor: pointer; color: #64748b; transition: all 0.15s; }
.icon-btn:hover:not(:disabled) { border-color: #6366f1; color: #6366f1; }
.icon-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.icon-btn--danger:hover:not(:disabled) { border-color: #ef4444; color: #ef4444; }

@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 1200px) { .metrics-row { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 900px)  { .main-grid { grid-template-columns: 1fr; } }
@media (max-width: 600px)  { .metrics-row { grid-template-columns: 1fr 1fr; } }
</style>
