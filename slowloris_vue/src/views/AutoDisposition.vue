<template>
  <div class="page">
    <!-- 页头 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon"><i class="fa fa-magic"></i></div>
        <div>
          <h1>自动处置中心</h1>
          <p>感知 → 分析 → 决策 → 处置 全自动闭环 · 实时事件流</p>
        </div>
      </div>
      <div class="stream-badge" :class="'stream--' + streamStatus">
        <span class="badge-dot"></span>
        {{ streamLabel }}
      </div>
    </div>

    <!-- 闭环流程条 -->
    <div class="pipeline">
      <div class="pipe-step" v-for="(s, i) in pipeline" :key="s.key">
        <div class="pipe-icon" :class="{ active: s.active }"><i :class="s.icon"></i></div>
        <span class="pipe-label">{{ s.label }}</span>
        <i v-if="i < pipeline.length - 1" class="fa fa-angle-right pipe-arrow"></i>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon stat-icon--purple"><i class="fa fa-robot"></i></div>
        <div class="stat-info">
          <p class="stat-val">{{ config.autoMode === 1 ? '已开启' : '已关闭' }}</p>
          <p class="stat-label">自动处置模式</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon--red"><i class="fa fa-ban"></i></div>
        <div class="stat-info">
          <p class="stat-val">{{ blockedIps.length }}</p>
          <p class="stat-label">封禁中 IP</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon--orange"><i class="fa fa-crosshairs"></i></div>
        <div class="stat-info">
          <p class="stat-val">{{ overview.activeTargets ?? 0 }}</p>
          <p class="stat-label">监控目标</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon--blue"><i class="fa fa-bolt"></i></div>
        <div class="stat-info">
          <p class="stat-val">{{ detections.length }}</p>
          <p class="stat-label">近期决策</p>
        </div>
      </div>
    </div>

    <div class="main-grid">
      <!-- 左：配置 + 封禁列表 -->
      <div class="col">
        <!-- 自动处置配置 -->
        <div class="card">
          <div class="card-head"><i class="fa fa-sliders" style="color:#8b5cf6"></i> 自动处置策略</div>
          <div class="card-body">
            <div class="switch-row">
              <div>
                <p class="switch-title">自动封禁模式</p>
                <p class="switch-sub">开启后，达到阈值的攻击来源 IP 将被 agent 自动软封禁</p>
              </div>
              <label class="switch">
                <input type="checkbox" :checked="config.autoMode === 1" @change="toggleAutoMode" :disabled="savingConfig" />
                <span class="slider"></span>
              </label>
            </div>

            <div class="form-grid">
              <div class="form-item">
                <label>风险评分阈值</label>
                <input type="number" min="0" max="100" v-model.number="config.blockRiskScore" />
              </div>
              <div class="form-item">
                <label>最低严重级别</label>
                <select v-model="config.blockSeverity">
                  <option value="low">低危</option>
                  <option value="medium">中危</option>
                  <option value="high">高危</option>
                  <option value="critical">严重</option>
                </select>
              </div>
              <div class="form-item">
                <label>最低置信度 (0-1)</label>
                <input type="number" min="0" max="1" step="0.05" v-model.number="config.blockMinConfidence" />
              </div>
              <div class="form-item">
                <label>自动解封 (分钟, 0=否)</label>
                <input type="number" min="0" v-model.number="config.autoUnblockMinutes" />
              </div>
            </div>
            <button class="btn btn-save" :disabled="savingConfig" @click="saveConfig">
              <i :class="savingConfig ? 'fa fa-spinner fa-spin' : 'fa fa-save'"></i>
              {{ savingConfig ? '保存中...' : '保存策略' }}
            </button>
          </div>
        </div>

        <!-- 封禁 IP 列表 -->
        <div class="card" style="margin-top:1.25rem">
          <div class="card-head" style="justify-content:space-between">
            <span><i class="fa fa-ban" style="color:#ef4444"></i> 封禁中 IP</span>
            <div class="manual-block">
              <input v-model="manualIp" placeholder="手动封禁 IP" class="mini-input" />
              <button class="icon-btn" @click="manualBlock"><i class="fa fa-plus"></i></button>
            </div>
          </div>
          <div class="card-body" style="padding:0">
            <table class="data-table">
              <thead>
                <tr><th>IP 地址</th><th>来源</th><th>原因</th><th>封禁时间</th><th>操作</th></tr>
              </thead>
              <tbody>
                <tr v-if="blockedIps.length === 0">
                  <td colspan="5" class="empty-cell">暂无封禁记录</td>
                </tr>
                <tr v-for="b in blockedIps" :key="b.id">
                  <td class="mono">{{ b.ipAddress }}</td>
                  <td>
                    <span class="tag" :class="b.auto === 1 ? 'tag-auto' : 'tag-manual'">
                      {{ b.auto === 1 ? '自动' : '人工' }}
                    </span>
                  </td>
                  <td class="reason-cell" :title="b.reason">{{ b.reason || '—' }}</td>
                  <td class="text-muted">{{ formatTime(b.blockTime) }}</td>
                  <td><button class="link-btn" @click="unblock(b.ipAddress)">解封</button></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- 右：实时事件流 + 决策记录 -->
      <div class="col">
        <div class="card">
          <div class="card-head" style="justify-content:space-between">
            <span><i class="fa fa-stream" style="color:#3b82f6"></i> 实时事件流</span>
            <button class="icon-btn" @click="liveEvents = []"><i class="fa fa-trash-o"></i></button>
          </div>
          <div class="card-body feed-body">
            <div v-if="liveEvents.length === 0" class="empty-state-sm">
              <i class="fa fa-circle-o-notch fa-spin"></i> 等待 agent 事件...
            </div>
            <div v-for="(e, i) in liveEvents" :key="i" class="feed-item" :class="'feed--' + e.kind">
              <div class="feed-dot"></div>
              <div class="feed-content">
                <div class="feed-head">
                  <span class="feed-tag" :class="'ftag-' + e.kind">{{ e.tagLabel }}</span>
                  <span class="feed-time">{{ e.time }}</span>
                </div>
                <p class="feed-msg">{{ e.message }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 决策记录 -->
        <div class="card" style="margin-top:1.25rem">
          <div class="card-head" style="justify-content:space-between">
            <span><i class="fa fa-gavel" style="color:#f59e0b"></i> 决策记录</span>
            <div class="head-actions">
              <button v-if="detections.length" class="icon-btn icon-btn--danger" title="清空全部决策记录" @click="clearDetections">
                <i class="fa fa-trash"></i>
              </button>
              <button class="icon-btn" @click="fetchDetections"><i class="fa fa-refresh"></i></button>
            </div>
          </div>
          <div class="card-body" style="padding:0; max-height:300px; overflow-y:auto">
            <table class="data-table">
              <thead>
                <tr><th>攻击来源 IP</th><th>级别</th><th>风险</th><th>状态</th><th>时间</th><th>操作</th></tr>
              </thead>
              <tbody>
                <tr v-if="detections.length === 0">
                  <td colspan="6" class="empty-cell">暂无决策记录</td>
                </tr>
                <tr v-for="d in detections" :key="d.id">
                  <td class="mono">{{ d.ipAddress }}</td>
                  <td><span class="sev-badge" :class="'sev-' + (d.severity || 'low')">{{ severityLabel(d.severity) }}</span></td>
                  <td>{{ d.riskScore != null ? Math.round(d.riskScore) : '—' }}</td>
                  <td>
                    <span class="status-badge" :class="'st-' + d.status">{{ statusLabel(d.status) }}</span>
                  </td>
                  <td class="text-muted">{{ formatTime(d.detectedAt) }}</td>
                  <td>
                    <button class="link-btn link-danger" @click="deleteDetection(d)">
                      <i class="fa fa-trash"></i> 删除
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import api from '../services/api.js'
import { createAgentStream } from '../services/agentStream.js'
import { useToast } from '../composables/useToast.js'

const { success, error, warning } = useToast()

const config = reactive({
  autoMode: 0, blockRiskScore: 80, blockSeverity: 'high',
  blockMinConfidence: 0.7, autoUnblockMinutes: 0,
})
const overview = ref({})
const blockedIps = ref([])
const detections = ref([])
const liveEvents = ref([])
const manualIp = ref('')
const savingConfig = ref(false)
const streamStatus = ref('connecting')
let stream = null
let refreshTimer = null

const streamLabel = computed(() => ({
  connecting: '连接中...', connected: '实时已连接', disconnected: '已断开 · 重连中',
  unauthorized: '未授权', closed: '已关闭',
}[streamStatus.value] || '—'))

const pipeline = computed(() => {
  const attacking = liveEvents.value[0]?.kind === 'attack' || liveEvents.value[0]?.kind === 'disposition'
  return [
    { key: 'perceive', icon: 'fa fa-feed', label: '感知', active: streamStatus.value === 'connected' },
    { key: 'analyze', icon: 'fa fa-brain', label: '分析', active: liveEvents.value.length > 0 },
    { key: 'decide', icon: 'fa fa-gavel', label: '决策', active: detections.value.length > 0 },
    { key: 'dispose', icon: 'fa fa-shield', label: '处置', active: config.autoMode === 1 && attacking },
  ]
})

function severityLabel(s) {
  return { critical: '严重', high: '高危', medium: '中危', low: '低危' }[s] || '低危'
}
function statusLabel(s) {
  return { blocked: '已封禁', active: '待处置', resolved: '已解除' }[s] || s
}
function formatTime(t) {
  if (!t) return '—'
  try { return new Date(t).toLocaleString('zh-CN') } catch { return t }
}

async function fetchConfig() {
  try {
    const res = await api.get('/api/monitor/config')
    if (res.data) Object.assign(config, res.data)
  } catch { /* silent */ }
}
async function fetchOverview() {
  try { overview.value = (await api.get('/api/monitor/agent/overview')).data || {} } catch { /* silent */ }
}
async function fetchBlocked() {
  try { blockedIps.value = (await api.get('/api/monitor/blocked')).data || [] } catch { blockedIps.value = [] }
}
async function fetchDetections() {
  try { detections.value = (await api.get('/api/monitor/detections', { limit: 50 })).data || [] } catch { detections.value = [] }
}

async function deleteDetection(d) {
  if (!window.confirm(`确认删除来源 ${d.ipAddress} 的这条决策记录？`)) return
  try {
    await api.delete(`/api/monitor/detections/${d.id}`)
    detections.value = detections.value.filter(x => x.id !== d.id)
    success('决策记录已删除')
  } catch (e) {
    error(e?.response?.data?.message || '删除失败')
  }
}

async function clearDetections() {
  if (!detections.value.length) return
  if (!window.confirm(`确认清空全部 ${detections.value.length} 条决策记录？此操作不可恢复。`)) return
  const ids = detections.value.map(d => d.id)
  try {
    await api.delete('/api/monitor/detections', { ids })
    detections.value = []
    success('决策记录已清空')
  } catch (e) {
    error(e?.response?.data?.message || '清空失败')
  }
}

async function saveConfig() {
  savingConfig.value = true
  try {
    await api.put('/api/monitor/config', {
      autoMode: config.autoMode,
      blockRiskScore: config.blockRiskScore,
      blockSeverity: config.blockSeverity,
      blockMinConfidence: config.blockMinConfidence,
      autoUnblockMinutes: config.autoUnblockMinutes,
    })
    success('策略已保存')
    fetchOverview()
  } catch (e) {
    error(e?.response?.data?.message || '保存失败')
  } finally { savingConfig.value = false }
}

function toggleAutoMode(e) {
  config.autoMode = e.target.checked ? 1 : 0
  saveConfig()
}

async function manualBlock() {
  const ip = manualIp.value.trim()
  if (!/^(\d{1,3}\.){3}\d{1,3}$/.test(ip)) { warning('请输入合法 IPv4 地址'); return }
  try {
    await api.post('/api/monitor/block', { ip, reason: '人工封禁' })
    success(`已封禁 ${ip}`)
    manualIp.value = ''
    fetchBlocked()
  } catch (e) {
    error(e?.response?.data?.message || '封禁失败')
  }
}

async function unblock(ip) {
  try {
    await api.post('/api/monitor/unblock', { ip })
    success(`已解封 ${ip}`)
    fetchBlocked()
  } catch (e) {
    error(e?.response?.data?.message || '解封失败')
  }
}

function pushEvent(kind, tagLabel, message) {
  liveEvents.value.unshift({ kind, tagLabel, message, time: new Date().toLocaleTimeString('zh-CN') })
  if (liveEvents.value.length > 50) liveEvents.value.pop()
}

function handleStreamEvent(event, payload) {
  switch (event) {
    case 'analysis':
      pushEvent(payload.isAttack ? 'attack' : 'analysis',
        payload.isAttack ? '检出攻击' : '分析',
        `${payload.ip} · ${payload.isAttack ? '判定攻击' : '流量正常'}` +
        (payload.riskScore != null ? ` · 风险 ${Math.round(payload.riskScore)}` : ''))
      break
    case 'disposition': {
      const actionMap = { auto_block: '自动封禁', advise: '生成建议', auto_no_target: '无可封禁来源' }
      pushEvent('disposition', '决策',
        `${payload.targetIp} · ${actionMap[payload.action] || payload.action}` +
        (payload.blockedIps?.length ? ` · 封禁 ${payload.blockedIps.length} 个IP` : ''))
      fetchDetections(); fetchBlocked()
      break
    }
    case 'block':
      pushEvent('block', '封禁', `${payload.ipAddress} 已封禁（${payload.auto === 1 ? '自动' : '人工'}）`)
      fetchBlocked()
      break
    case 'unblock':
      pushEvent('unblock', '解封', `${payload.ipAddress} 已解封`)
      fetchBlocked()
      break
    case 'config':
      Object.assign(config, payload)
      break
  }
}

onMounted(() => {
  fetchConfig(); fetchOverview(); fetchBlocked(); fetchDetections()
  stream = createAgentStream({
    onEvent: handleStreamEvent,
    onStatus: (s) => { streamStatus.value = s },
  })
  // 兜底刷新：SSE 不可用时仍保持数据更新
  refreshTimer = setInterval(() => { fetchOverview(); fetchBlocked() }, 30000)
})

onUnmounted(() => {
  if (stream) stream.close()
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
.page { padding: 0; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 1rem; }
.header-left { display: flex; align-items: center; gap: 0.875rem; }
.header-icon {
  width: 2.75rem; height: 2.75rem;
  background: linear-gradient(135deg, #8b5cf6, #6366f1);
  border-radius: 0.75rem; display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 1.125rem; box-shadow: 0 4px 12px rgba(139,92,246,0.3);
}
.page-header h1 { font-size: 1.375rem; font-weight: 700; color: #1e293b; margin: 0; }
.page-header p  { font-size: 0.8125rem; color: #64748b; margin: 0.125rem 0 0; }

.stream-badge { display: flex; align-items: center; gap: 0.4rem; padding: 0.375rem 0.875rem; border-radius: 9999px; font-size: 0.8125rem; font-weight: 500; border: 1px solid; }
.stream--connected { background: #f0fdf4; color: #16a34a; border-color: #bbf7d0; }
.stream--connecting, .stream--disconnected { background: #fffbeb; color: #d97706; border-color: #fde68a; }
.stream--unauthorized, .stream--closed { background: #f8fafc; color: #64748b; border-color: #e2e8f0; }
.badge-dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
.stream--connected .badge-dot { animation: pulse 2s infinite; }

/* Pipeline */
.pipeline { display: flex; align-items: center; gap: 0.5rem; background: #fff; border: 1px solid #e2e8f0; border-radius: 0.875rem; padding: 1rem 1.25rem; margin-bottom: 1.25rem; box-shadow: 0 1px 4px rgba(0,0,0,0.06); flex-wrap: wrap; }
.pipe-step { display: flex; align-items: center; gap: 0.5rem; }
.pipe-icon { width: 2.25rem; height: 2.25rem; border-radius: 0.625rem; display: flex; align-items: center; justify-content: center; background: #f1f5f9; color: #94a3b8; font-size: 0.875rem; transition: all 0.3s; }
.pipe-icon.active { background: #ede9fe; color: #7c3aed; box-shadow: 0 0 0 3px rgba(124,58,237,0.12); }
.pipe-label { font-size: 0.8125rem; font-weight: 600; color: #475569; }
.pipe-arrow { color: #cbd5e1; margin: 0 0.25rem; }

/* Stats */
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; margin-bottom: 1.25rem; }
.stat-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 0.875rem; padding: 1rem 1.25rem; display: flex; align-items: center; gap: 0.875rem; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.stat-icon { width: 2.5rem; height: 2.5rem; border-radius: 0.625rem; flex-shrink: 0; display: flex; align-items: center; justify-content: center; font-size: 1rem; }
.stat-icon--blue { background: #eff6ff; color: #3b82f6; }
.stat-icon--red { background: #fef2f2; color: #ef4444; }
.stat-icon--purple { background: #f5f3ff; color: #8b5cf6; }
.stat-icon--orange { background: #fff7ed; color: #f97316; }
.stat-val { font-size: 1.25rem; font-weight: 700; color: #1e293b; margin: 0; }
.stat-label { font-size: 0.75rem; color: #94a3b8; margin: 0.125rem 0 0; }

.main-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.25rem; align-items: start; }
.col { min-width: 0; }
.card { background: #fff; border: 1px solid #e2e8f0; border-radius: 0.875rem; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.card-head { display: flex; align-items: center; gap: 0.5rem; padding: 1rem 1.25rem; border-bottom: 1px solid #f1f5f9; font-size: 0.9375rem; font-weight: 600; color: #1e293b; }
.card-body { padding: 1.25rem; }

/* Switch */
.switch-row { display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding-bottom: 1rem; border-bottom: 1px solid #f1f5f9; margin-bottom: 1rem; }
.switch-title { font-size: 0.9375rem; font-weight: 600; color: #1e293b; margin: 0; }
.switch-sub { font-size: 0.75rem; color: #94a3b8; margin: 0.2rem 0 0; max-width: 22rem; }
.switch { position: relative; display: inline-block; width: 46px; height: 26px; flex-shrink: 0; }
.switch input { opacity: 0; width: 0; height: 0; }
.slider { position: absolute; cursor: pointer; inset: 0; background: #cbd5e1; border-radius: 9999px; transition: 0.3s; }
.slider:before { content: ''; position: absolute; height: 20px; width: 20px; left: 3px; bottom: 3px; background: #fff; border-radius: 50%; transition: 0.3s; }
.switch input:checked + .slider { background: #8b5cf6; }
.switch input:checked + .slider:before { transform: translateX(20px); }
.switch input:disabled + .slider { opacity: 0.6; cursor: not-allowed; }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.875rem; margin-bottom: 1rem; }
.form-item label { display: block; font-size: 0.75rem; font-weight: 600; color: #64748b; margin-bottom: 0.375rem; }
.form-item input, .form-item select { width: 100%; padding: 0.4375rem 0.625rem; border: 1px solid #d1d5db; border-radius: 0.5rem; font-size: 0.875rem; color: #1e293b; outline: none; }
.form-item input:focus, .form-item select:focus { border-color: #8b5cf6; box-shadow: 0 0 0 3px rgba(139,92,246,0.1); }
.btn { padding: 0.5625rem 1.5rem; border: none; border-radius: 0.5rem; font-size: 0.9375rem; font-weight: 600; cursor: pointer; display: inline-flex; align-items: center; gap: 0.5rem; }
.btn:disabled { opacity: 0.55; cursor: not-allowed; }
.btn-save { background: linear-gradient(135deg, #8b5cf6, #6366f1); color: #fff; box-shadow: 0 2px 8px rgba(139,92,246,0.3); }
.btn-save:hover:not(:disabled) { opacity: 0.9; }

.manual-block { display: flex; gap: 0.4rem; }
.mini-input { padding: 0.3rem 0.5rem; border: 1px solid #d1d5db; border-radius: 0.375rem; font-size: 0.8125rem; width: 9rem; outline: none; font-family: 'Courier New', monospace; }
.mini-input:focus { border-color: #8b5cf6; }

/* Tables */
.data-table { width: 100%; border-collapse: collapse; font-size: 0.875rem; }
.data-table th { padding: 0.625rem 1rem; text-align: left; font-size: 0.75rem; font-weight: 600; color: #64748b; background: #f8fafc; border-bottom: 1px solid #e2e8f0; }
.data-table td { padding: 0.625rem 1rem; border-bottom: 1px solid #f1f5f9; color: #1e293b; }
.data-table tr:last-child td { border-bottom: none; }
.mono { font-family: 'Courier New', monospace; }
.text-muted { color: #94a3b8; font-size: 0.8125rem; }
.empty-cell { text-align: center; padding: 2rem; color: #94a3b8; }
.reason-cell { max-width: 12rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.icon-btn { background: none; border: 1px solid #e2e8f0; border-radius: 0.375rem; padding: 0.25rem 0.5rem; cursor: pointer; color: #64748b; transition: all 0.15s; }
.icon-btn:hover { border-color: #8b5cf6; color: #8b5cf6; }
.icon-btn--danger:hover { border-color: #ef4444; color: #ef4444; }
.head-actions { display: flex; align-items: center; gap: 0.5rem; }
.link-btn { background: none; border: none; cursor: pointer; color: #ef4444; font-size: 0.8125rem; }
.link-btn:hover { text-decoration: underline; }
.link-danger { color: #ef4444; }

.tag { font-size: 0.6875rem; font-weight: 700; padding: 0.1rem 0.45rem; border-radius: 9999px; }
.tag-auto { background: #ede9fe; color: #7c3aed; }
.tag-manual { background: #f1f5f9; color: #64748b; }
.sev-badge, .status-badge { font-size: 0.6875rem; font-weight: 700; padding: 0.1rem 0.45rem; border-radius: 9999px; }
.sev-critical { background: #fee2e2; color: #dc2626; }
.sev-high { background: #ffedd5; color: #ea580c; }
.sev-medium { background: #fefce8; color: #ca8a04; }
.sev-low { background: #dcfce7; color: #16a34a; }
.st-blocked { background: #fee2e2; color: #dc2626; }
.st-active { background: #fef9c3; color: #a16207; }
.st-resolved { background: #dcfce7; color: #16a34a; }

/* Feed */
.feed-body { max-height: 320px; overflow-y: auto; }
.empty-state-sm { padding: 2rem; text-align: center; color: #94a3b8; font-size: 0.875rem; display: flex; align-items: center; justify-content: center; gap: 0.4rem; }
.feed-item { display: flex; gap: 0.75rem; padding: 0.625rem 0; border-bottom: 1px solid #f8fafc; }
.feed-item:last-child { border-bottom: none; }
.feed-dot { width: 9px; height: 9px; border-radius: 50%; flex-shrink: 0; margin-top: 0.35rem; background: #94a3b8; }
.feed--attack .feed-dot, .feed--disposition .feed-dot, .feed--block .feed-dot { background: #ef4444; }
.feed--unblock .feed-dot { background: #22c55e; }
.feed--analysis .feed-dot { background: #3b82f6; }
.feed-content { flex: 1; min-width: 0; }
.feed-head { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.15rem; }
.feed-tag { font-size: 0.6875rem; font-weight: 700; padding: 0.1rem 0.45rem; border-radius: 9999px; }
.ftag-analysis { background: #eff6ff; color: #2563eb; }
.ftag-attack, .ftag-disposition, .ftag-block { background: #fee2e2; color: #dc2626; }
.ftag-unblock { background: #dcfce7; color: #16a34a; }
.feed-time { font-size: 0.6875rem; color: #94a3b8; margin-left: auto; }
.feed-msg { font-size: 0.8125rem; color: #475569; margin: 0; }

@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.4} }
@media (max-width: 1024px) { .main-grid { grid-template-columns: 1fr; } .stats-row { grid-template-columns: repeat(2,1fr); } }
@media (max-width: 600px) { .form-grid { grid-template-columns: 1fr; } .stats-row { grid-template-columns: 1fr 1fr; } }
</style>
