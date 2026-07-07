<template>
  <div class="page">
    <!-- 页头 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon"><i class="fa fa-tachometer"></i></div>
        <div>
          <h1>监控控制台</h1>
          <p>输入目标 IP，DeepSeek 自动分析攻击态势</p>
        </div>
      </div>
      <div class="poll-badge" :class="isMonitoring ? 'badge--active' : 'badge--idle'">
        <span class="badge-dot"></span>
        {{ isMonitoring ? '监控中 · 每30秒刷新' : '未启动' }}
      </div>
    </div>

    <!-- IP 控制卡 -->
    <div class="control-card">
      <div class="control-inner">
        <div class="ip-field">
          <label>目标服务器公网 IP</label>
          <input
            v-model="targetIp"
            type="text"
            placeholder="例：1.2.3.4"
            class="ip-input"
            :disabled="isMonitoring"
          />
        </div>
        <div class="control-btns">
          <button v-if="!isMonitoring" class="btn btn-start" :disabled="startLoading" @click="startMonitor">
            <i :class="startLoading ? 'fa fa-spinner fa-spin' : 'fa fa-play'"></i>
            {{ startLoading ? '启动中...' : '开始监控' }}
          </button>
          <button v-else class="btn btn-stop" :disabled="stopLoading" @click="stopMonitor">
            <i :class="stopLoading ? 'fa fa-spinner fa-spin' : 'fa fa-stop'"></i>
            {{ stopLoading ? '停止中...' : '停止监控' }}
          </button>
          <button v-if="isMonitoring" class="btn btn-agent" @click="openInstall">
            <i class="fa fa-download"></i> 获取探针安装命令
          </button>
        </div>
      </div>
      <p v-if="controlError" class="control-err"><i class="fa fa-exclamation-circle"></i> {{ controlError }}</p>
      <p v-if="isMonitoring" class="agent-hint">
        <span class="agent-status" :class="agentOnline ? 'agent-on' : 'agent-off'">
          <span class="badge-dot"></span>{{ agentOnline ? '探针在线' : '探针未上报' }}
        </span>
        <span v-if="!agentOnline" class="agent-tip">
          想采集这台服务器的真实流量，请在该机上安装探针 —— 点「获取探针安装命令」。
        </span>
        <span v-else-if="agentLastSeen" class="agent-tip">最后心跳：{{ formatTime(agentLastSeen) }}</span>
      </p>
    </div>

    <!-- 统计卡片行 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon stat-icon--blue"><i class="fa fa-crosshairs"></i></div>
        <div class="stat-info">
          <p class="stat-val">{{ monitoredTargets.length }}</p>
          <p class="stat-label">监控目标数</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon--red"><i class="fa fa-bell"></i></div>
        <div class="stat-info">
          <p class="stat-val">{{ activeAlertCount }}</p>
          <p class="stat-label">活跃告警</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" :class="latestSnapshot && latestSnapshot.isAttack ? 'stat-icon--red' : 'stat-icon--green'">
          <i :class="latestSnapshot && latestSnapshot.isAttack ? 'fa fa-exclamation-triangle' : 'fa fa-shield'"></i>
        </div>
        <div class="stat-info">
          <p class="stat-val">{{ latestSnapshot ? (latestSnapshot.isAttack ? '攻击中' : '安全') : '—' }}</p>
          <p class="stat-label">当前状态</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon--purple"><i class="fa fa-clock-o"></i></div>
        <div class="stat-info">
          <p class="stat-val">{{ lastRefreshTime || '—' }}</p>
          <p class="stat-label">上次刷新</p>
        </div>
      </div>
    </div>

    <!-- AI 分析卡 + 指标速览 -->
    <div class="main-grid">
      <!-- AI 分析结果 -->
      <div class="card ai-card">
        <div class="card-head">
          <i class="fa fa-brain" style="color:#6366f1"></i>
          DeepSeek AI 分析结果
          <span v-if="latestSnapshot" class="head-time">{{ formatTime(latestSnapshot.createTime) }}</span>
        </div>
        <div class="card-body">
          <!-- 空状态 -->
          <div v-if="!isMonitoring && !latestSnapshot" class="empty-state">
            <div class="empty-icon"><i class="fa fa-brain"></i></div>
            <p>输入 IP 并点击「开始监控」，AI 将每 30 秒自动分析一次</p>
          </div>

          <!-- 等待首次结果 -->
          <div v-else-if="isMonitoring && !latestSnapshot" class="empty-state">
            <div class="spinner"></div>
            <p>等待首次 AI 分析（约 30 秒）...</p>
          </div>

          <!-- 结果展示 -->
          <div v-else-if="latestSnapshot" class="ai-result">
            <!-- 判决 -->
            <div class="verdict" :class="latestSnapshot.isAttack ? 'verdict--danger' : 'verdict--safe'">
              <div class="verdict-icon">
                <i :class="latestSnapshot.isAttack ? 'fa fa-exclamation-triangle' : 'fa fa-check-circle'"></i>
              </div>
              <div class="verdict-text">
                <p class="verdict-title">{{ latestSnapshot.isAttack ? '检测到攻击' : '流量正常' }}</p>
                <p class="verdict-sub">
                  {{ latestSnapshot.isAttack ? '攻击类型：' + (aiResult.attackType || 'Slowloris') : 'DeepSeek 判断当前流量无异常' }}
                </p>
              </div>
              <div class="verdict-right">
                <span class="severity-badge" :class="'sev-' + (aiResult.severity || 'low')">
                  {{ severityLabel(aiResult.severity) }}
                </span>
                <p class="risk-score">风险评分 <strong>{{ aiResult.riskScore || 0 }}</strong></p>
              </div>
            </div>

            <!-- AI 推理 -->
            <div v-if="aiResult.reasoning" class="ai-reasoning">
              <p class="reasoning-title"><i class="fa fa-comment-o"></i> AI 推理</p>
              <p class="reasoning-text">{{ aiResult.reasoning }}</p>
            </div>

            <!-- 建议列表 -->
            <div v-if="aiResult.recommendations && aiResult.recommendations.length" class="recommendations">
              <p class="rec-title"><i class="fa fa-lightbulb-o"></i> 处置建议</p>
              <ul class="rec-list">
                <li v-for="(rec, i) in aiResult.recommendations" :key="i">{{ rec }}</li>
              </ul>
            </div>

            <!-- 置信度 -->
            <div class="confidence-row">
              <span class="conf-label">AI 置信度</span>
              <div class="conf-bar-wrap">
                <div class="conf-bar" :style="{ width: ((aiResult.confidence || 0) * 100) + '%' }"></div>
              </div>
              <span class="conf-val">{{ ((aiResult.confidence || 0) * 100).toFixed(0) }}%</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 指标速览 -->
      <div class="card metrics-card">
        <div class="card-head">
          <i class="fa fa-bar-chart" style="color:#3b82f6"></i>
          实时指标快照
        </div>
        <div class="card-body">
          <div v-if="!latestSnapshot" class="empty-state">
            <div class="empty-icon"><i class="fa fa-bar-chart"></i></div>
            <p>暂无快照数据</p>
          </div>
          <div v-else class="metrics-grid">
            <div class="metric-item">
              <p class="metric-val">{{ latestSnapshot.halfOpenConns ?? '—' }}</p>
              <p class="metric-label">半开连接数</p>
              <div class="metric-bar-wrap">
                <div class="metric-bar metric-bar--orange" :style="{ width: Math.min((latestSnapshot.halfOpenConns / 200) * 100, 100) + '%' }"></div>
              </div>
            </div>
            <div class="metric-item">
              <p class="metric-val">{{ latestSnapshot.requestRate ?? '—' }}<span class="metric-unit">/min</span></p>
              <p class="metric-label">请求速率</p>
              <div class="metric-bar-wrap">
                <div class="metric-bar metric-bar--blue" :style="{ width: Math.min((latestSnapshot.requestRate / 1000) * 100, 100) + '%' }"></div>
              </div>
            </div>
            <div class="metric-item">
              <p class="metric-val">{{ latestSnapshot.avgConnDuration ?? '—' }}<span class="metric-unit">ms</span></p>
              <p class="metric-label">平均连接时长</p>
              <div class="metric-bar-wrap">
                <div class="metric-bar metric-bar--purple" :style="{ width: Math.min((latestSnapshot.avgConnDuration / 600000) * 100, 100) + '%' }"></div>
              </div>
            </div>
            <div class="metric-item">
              <p class="metric-val">{{ latestSnapshot.uniqueSourceIps ?? '—' }}</p>
              <p class="metric-label">唯一来源 IP 数</p>
              <div class="metric-bar-wrap">
                <div class="metric-bar metric-bar--green" :style="{ width: Math.min((latestSnapshot.uniqueSourceIps / 500) * 100, 100) + '%' }"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 活跃监控目标 -->
    <div class="card" style="margin-top:1.25rem">
      <div class="card-head" style="justify-content:space-between">
        <span><i class="fa fa-crosshairs" style="color:#3b82f6"></i> 活跃监控目标</span>
        <button class="icon-btn" @click="fetchTargets"><i class="fa fa-refresh"></i></button>
      </div>
      <div class="card-body" style="padding:0">
        <table class="data-table">
          <thead>
            <tr>
              <th>IP 地址</th>
              <th>状态</th>
              <th>最新判决</th>
              <th>风险评分</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="targetsLoading">
              <td colspan="5" class="empty-cell"><i class="fa fa-spinner fa-spin"></i> 加载中...</td>
            </tr>
            <tr v-else-if="monitoredTargets.length === 0">
              <td colspan="5" class="empty-cell">暂无活跃监控目标</td>
            </tr>
            <tr v-for="t in monitoredTargets" :key="t.ipAddress" v-else>
              <td class="mono">{{ t.ipAddress }}</td>
              <td><span class="badge badge-active">监控中</span></td>
              <td>
                <span v-if="t.latestSnapshot" class="badge" :class="t.latestSnapshot.isAttack ? 'badge-danger' : 'badge-safe'">
                  {{ t.latestSnapshot.isAttack ? '攻击中' : '安全' }}
                </span>
                <span v-else class="text-muted">待分析</span>
              </td>
              <td>
                <span v-if="t.latestSnapshot && t.latestSnapshot.aiVerdict">
                  {{ parseRiskScore(t.latestSnapshot.aiVerdict) }}
                </span>
                <span v-else class="text-muted">—</span>
              </td>
              <td>
                <button class="link-btn" @click="switchTarget(t.ipAddress)">切换监控</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 探针安装命令弹窗 -->
    <div v-if="installModal" class="modal-mask" @click.self="installModal = false">
      <div class="modal-box">
        <div class="modal-head">
          <span><i class="fa fa-download" style="color:#3b82f6"></i> 安装采集探针 · {{ installIp }}</span>
          <button class="modal-close" @click="installModal = false"><i class="fa fa-times"></i></button>
        </div>
        <div class="modal-body">
          <p class="modal-desc">
            在<strong>目标服务器（{{ installIp }}）</strong>上以 root 执行下面这条命令，即可安装常驻探针。
            它会采集本机真实的半开连接、来源 IP 等指标并回传平台，用于攻击检测与溯源。
          </p>

          <div v-if="installLoading" class="modal-loading"><div class="spinner"></div><p>生成安装命令...</p></div>
          <template v-else-if="installError">
            <p class="control-err"><i class="fa fa-exclamation-circle"></i> {{ installError }}</p>
          </template>
          <template v-else>
            <div class="cmd-box">
              <code class="cmd-text">{{ installCommand }}</code>
              <button class="copy-btn" @click="copyCommand">
                <i :class="copied ? 'fa fa-check' : 'fa fa-copy'"></i> {{ copied ? '已复制' : '复制' }}
              </button>
            </div>
            <ul class="modal-notes">
              <li>探针依赖 <code>ss</code>(iproute2) 与 <code>curl</code>，多数 Linux 自带。</li>
              <li>安装后约 30 秒开始上报；平台检测到数据后状态会变为「探针在线」。</li>
              <li><i class="fa fa-shield" style="color:#f59e0b"></i> 命令含专属 token，请勿外泄。生产环境务必让平台走 HTTPS。</li>
              <li>卸载：<code>systemctl disable --now slowloris-agent</code></li>
            </ul>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import api from '../services/api.js'
import { createAgentStream } from '../services/agentStream.js'

const targetIp = ref('')
const isMonitoring = ref(false)
const startLoading = ref(false)
const stopLoading = ref(false)
const controlError = ref('')
const activeAlertCount = ref(0)
const lastRefreshTime = ref('')
const monitoredTargets = ref([])
const targetsLoading = ref(false)
const latestSnapshot = ref(null)
const agentOnline = ref(false)
const agentLastSeen = ref(null)
const installModal = ref(false)
const installIp = ref('')
const installCommand = ref('')
const installLoading = ref(false)
const installError = ref('')
const copied = ref(false)
let pollTimer = null
let agentStream = null

const IP_RE = /^(\d{1,3}\.){3}\d{1,3}$/

const aiResult = computed(() => {
  if (!latestSnapshot.value?.aiVerdict) return {}
  try { return JSON.parse(latestSnapshot.value.aiVerdict) } catch { return {} }
})

function severityLabel(s) {
  return { critical: '严重', high: '高危', medium: '中危', low: '低危' }[s] || '未知'
}

function formatTime(t) {
  if (!t) return ''
  try { return new Date(t).toLocaleTimeString('zh-CN') } catch { return '' }
}

function parseRiskScore(verdict) {
  try { return JSON.parse(verdict).riskScore ?? '—' } catch { return '—' }
}

async function startMonitor() {
  controlError.value = ''
  const ip = targetIp.value.trim()
  if (!IP_RE.test(ip)) { controlError.value = '请输入合法的 IPv4 地址'; return }
  startLoading.value = true
  try {
    await api.post('/api/monitor/start', { ip })
    isMonitoring.value = true
    latestSnapshot.value = null
    beginPolling(ip)
    fetchTargets()
  } catch (e) {
    controlError.value = e?.response?.data?.message || '启动失败，请检查后端服务'
  } finally {
    startLoading.value = false
  }
}

async function stopMonitor() {
  stopLoading.value = true
  try {
    await api.post('/api/monitor/stop', { ip: targetIp.value.trim() })
    isMonitoring.value = false
    stopPolling()
    fetchTargets()
  } catch (e) {
    controlError.value = e?.response?.data?.message || '停止失败'
  } finally {
    stopLoading.value = false
  }
}

function beginPolling(ip) {
  stopPolling()
  pollStatus(ip)
  pollTimer = setInterval(() => pollStatus(ip), 30000)
}

function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

async function pollStatus(ip) {
  try {
    const res = await api.get(`/api/monitor/status/${ip}`)
    if (res.data) {
      latestSnapshot.value = res.data.latestSnapshot || null
      agentOnline.value = !!res.data.agentOnline
      agentLastSeen.value = res.data.agentLastSeen || null
      lastRefreshTime.value = new Date().toLocaleTimeString('zh-CN')
    }
  } catch { /* silent */ }
  fetchAlertCount()
}

async function fetchAlertCount() {
  try {
    const res = await api.get('/api/alerts?pageSize=1&status=0')
    activeAlertCount.value = res.data?.unreadCount ?? 0
  } catch { /* silent */ }
}

async function fetchTargets() {
  targetsLoading.value = true
  try {
    const res = await api.get('/api/monitor/targets')
    monitoredTargets.value = res.data || []
  } catch { monitoredTargets.value = [] } finally { targetsLoading.value = false }
}

function switchTarget(ip) {
  stopPolling()
  isMonitoring.value = false
  targetIp.value = ip
  isMonitoring.value = true
  beginPolling(ip)
}

async function openInstall() {
  const ip = targetIp.value.trim()
  if (!ip) return
  installIp.value = ip
  installModal.value = true
  installLoading.value = true
  installError.value = ''
  installCommand.value = ''
  copied.value = false
  try {
    const res = await api.get(`/api/monitor/agent/install/${ip}`)
    installCommand.value = res.data?.command || ''
    if (!installCommand.value) installError.value = '未能生成安装命令'
  } catch (e) {
    installError.value = e?.response?.data?.message || '获取安装命令失败'
  } finally {
    installLoading.value = false
  }
}

async function copyCommand() {
  try {
    await navigator.clipboard.writeText(installCommand.value)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch { /* 剪贴板不可用时忽略，用户可手动选中复制 */ }
}

// 实时事件：agent 完成一轮分析时立即刷新当前目标快照，免等 30 秒轮询
function handleAgentEvent(event, payload) {
  if (event === 'analysis' && payload?.ip && payload.ip === targetIp.value.trim()) {
    pollStatus(payload.ip)
  } else if (event === 'disposition' || event === 'block' || event === 'unblock') {
    fetchAlertCount()
  }
}

onMounted(() => {
  fetchTargets()
  fetchAlertCount()
  agentStream = createAgentStream({ onEvent: handleAgentEvent })
})

onUnmounted(() => {
  stopPolling()
  if (agentStream) agentStream.close()
})
</script>

<style scoped>
.page { padding: 0; }

.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.5rem; }
.header-left { display: flex; align-items: center; gap: 0.875rem; }
.header-icon {
  width: 2.75rem; height: 2.75rem;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border-radius: 0.75rem;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 1.125rem;
  box-shadow: 0 4px 12px rgba(59,130,246,0.3);
}
.page-header h1 { font-size: 1.375rem; font-weight: 700; color: #1e293b; margin: 0; }
.page-header p  { font-size: 0.8125rem; color: #64748b; margin: 0.125rem 0 0; }

.poll-badge {
  display: flex; align-items: center; gap: 0.4rem;
  padding: 0.375rem 0.875rem; border-radius: 9999px;
  font-size: 0.8125rem; font-weight: 500; border: 1px solid;
}
.badge--active { background: #f0fdf4; color: #16a34a; border-color: #bbf7d0; }
.badge--idle   { background: #f8fafc; color: #64748b; border-color: #e2e8f0; }
.badge-dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
.badge--active .badge-dot { animation: pulse 2s infinite; }

/* Control card */
.control-card {
  background: #fff; border: 1px solid #e2e8f0; border-radius: 0.875rem;
  padding: 1.25rem; margin-bottom: 1.25rem;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.control-inner { display: flex; align-items: flex-end; gap: 1rem; }
.ip-field { flex: 1; }
.ip-field label { display: block; font-size: 0.8125rem; font-weight: 600; color: #374151; margin-bottom: 0.375rem; }
.ip-input {
  width: 100%; padding: 0.5625rem 0.75rem;
  border: 1px solid #d1d5db; border-radius: 0.5rem;
  font-size: 0.9375rem; color: #1e293b; background: #fff;
  outline: none; transition: border-color 0.15s, box-shadow 0.15s;
  font-family: 'Courier New', monospace;
}
.ip-input:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.12); }
.ip-input:disabled { background: #f8fafc; color: #94a3b8; cursor: not-allowed; }
.control-btns { flex-shrink: 0; }
.btn {
  padding: 0.5625rem 1.5rem; border: none; border-radius: 0.5rem;
  font-size: 0.9375rem; font-weight: 600; cursor: pointer;
  display: inline-flex; align-items: center; gap: 0.5rem;
  transition: opacity 0.15s, transform 0.1s;
}
.btn:disabled { opacity: 0.55; cursor: not-allowed; }
.btn-start { background: linear-gradient(135deg, #3b82f6, #2563eb); color: #fff; box-shadow: 0 2px 8px rgba(59,130,246,0.3); }
.btn-start:hover:not(:disabled) { opacity: 0.9; transform: translateY(-1px); }
.btn-stop  { background: linear-gradient(135deg, #ef4444, #dc2626); color: #fff; box-shadow: 0 2px 8px rgba(239,68,68,0.3); }
.btn-stop:hover:not(:disabled)  { opacity: 0.9; transform: translateY(-1px); }
.control-err { margin: 0.5rem 0 0; font-size: 0.8125rem; color: #dc2626; display: flex; align-items: center; gap: 0.3rem; }

/* Stats row */
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; margin-bottom: 1.25rem; }
.stat-card {
  background: #fff; border: 1px solid #e2e8f0; border-radius: 0.875rem;
  padding: 1rem 1.25rem; display: flex; align-items: center; gap: 0.875rem;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.stat-icon {
  width: 2.5rem; height: 2.5rem; border-radius: 0.625rem; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 1rem;
}
.stat-icon--blue   { background: #eff6ff; color: #3b82f6; }
.stat-icon--red    { background: #fef2f2; color: #ef4444; }
.stat-icon--green  { background: #f0fdf4; color: #22c55e; }
.stat-icon--purple { background: #f5f3ff; color: #8b5cf6; }
.stat-val   { font-size: 1.25rem; font-weight: 700; color: #1e293b; margin: 0; }
.stat-label { font-size: 0.75rem; color: #94a3b8; margin: 0.125rem 0 0; }

/* Main grid */
.main-grid { display: grid; grid-template-columns: 3fr 2fr; gap: 1.25rem; }
.card { background: #fff; border: 1px solid #e2e8f0; border-radius: 0.875rem; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.card-head {
  display: flex; align-items: center; gap: 0.5rem;
  padding: 1rem 1.25rem; border-bottom: 1px solid #f1f5f9;
  font-size: 0.9375rem; font-weight: 600; color: #1e293b;
}
.head-time { margin-left: auto; font-size: 0.75rem; font-weight: 400; color: #94a3b8; }
.card-body { padding: 1.25rem; }

/* Empty state */
.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 3rem 1rem; gap: 0.75rem; color: #94a3b8; text-align: center; }
.empty-icon { width: 3.5rem; height: 3.5rem; background: #f1f5f9; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 1.375rem; }
.empty-state p { font-size: 0.875rem; max-width: 20rem; line-height: 1.6; }
.spinner { width: 2rem; height: 2rem; border: 3px solid #e0e7ff; border-top-color: #3b82f6; border-radius: 50%; animation: spin 0.8s linear infinite; }

/* AI Result */
.ai-result { display: flex; flex-direction: column; gap: 1rem; }
.verdict { display: flex; align-items: flex-start; gap: 0.875rem; padding: 1rem; border-radius: 0.75rem; }
.verdict--danger { background: #fef2f2; border: 1px solid #fecaca; }
.verdict--safe   { background: #f0fdf4; border: 1px solid #bbf7d0; }
.verdict-icon {
  width: 2.25rem; height: 2.25rem; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; font-size: 1rem;
}
.verdict--danger .verdict-icon { background: #fee2e2; color: #dc2626; }
.verdict--safe   .verdict-icon { background: #dcfce7; color: #16a34a; }
.verdict-text { flex: 1; }
.verdict-title { font-size: 0.9375rem; font-weight: 700; margin: 0; }
.verdict--danger .verdict-title { color: #991b1b; }
.verdict--safe   .verdict-title { color: #166534; }
.verdict-sub { font-size: 0.8125rem; margin: 0.2rem 0 0; }
.verdict--danger .verdict-sub { color: #b91c1c; }
.verdict--safe   .verdict-sub { color: #15803d; }
.verdict-right { text-align: right; flex-shrink: 0; }
.severity-badge { display: inline-block; padding: 0.2rem 0.625rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 700; }
.sev-critical { background: #fef2f2; color: #dc2626; }
.sev-high     { background: #fff7ed; color: #ea580c; }
.sev-medium   { background: #fefce8; color: #ca8a04; }
.sev-low      { background: #f0fdf4; color: #16a34a; }
.risk-score { font-size: 0.75rem; color: #94a3b8; margin: 0.375rem 0 0; }
.risk-score strong { color: #1e293b; }

.ai-reasoning { background: #f8fafc; border-radius: 0.5rem; padding: 0.875rem; }
.reasoning-title { font-size: 0.8125rem; font-weight: 600; color: #374151; margin: 0 0 0.375rem; display: flex; align-items: center; gap: 0.3rem; }
.reasoning-text { font-size: 0.8125rem; color: #475569; line-height: 1.6; margin: 0; }

.recommendations { }
.rec-title { font-size: 0.8125rem; font-weight: 600; color: #374151; margin: 0 0 0.5rem; display: flex; align-items: center; gap: 0.3rem; }
.rec-list { margin: 0; padding-left: 1.25rem; display: flex; flex-direction: column; gap: 0.3rem; }
.rec-list li { font-size: 0.8125rem; color: #475569; }

.confidence-row { display: flex; align-items: center; gap: 0.75rem; }
.conf-label { font-size: 0.8125rem; color: #64748b; flex-shrink: 0; }
.conf-bar-wrap { flex: 1; height: 6px; background: #f1f5f9; border-radius: 3px; overflow: hidden; }
.conf-bar { height: 100%; background: linear-gradient(90deg, #3b82f6, #6366f1); border-radius: 3px; transition: width 0.4s ease; }
.conf-val { font-size: 0.8125rem; font-weight: 700; color: #3b82f6; flex-shrink: 0; width: 2.75rem; text-align: right; }

/* Metrics */
.metrics-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.metric-item { background: #f8fafc; border-radius: 0.625rem; padding: 0.875rem; }
.metric-val { font-size: 1.375rem; font-weight: 700; color: #1e293b; margin: 0; line-height: 1.2; }
.metric-unit { font-size: 0.75rem; font-weight: 500; color: #94a3b8; margin-left: 0.15rem; }
.metric-label { font-size: 0.75rem; color: #94a3b8; margin: 0.2rem 0 0.625rem; }
.metric-bar-wrap { height: 4px; background: #e2e8f0; border-radius: 2px; overflow: hidden; }
.metric-bar { height: 100%; border-radius: 2px; transition: width 0.4s ease; }
.metric-bar--orange { background: #f97316; }
.metric-bar--blue   { background: #3b82f6; }
.metric-bar--purple { background: #8b5cf6; }
.metric-bar--green  { background: #22c55e; }

/* Table */
.data-table { width: 100%; border-collapse: collapse; font-size: 0.875rem; }
.data-table th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.8125rem; font-weight: 600; color: #64748b; background: #f8fafc; border-bottom: 1px solid #e2e8f0; }
.data-table td { padding: 0.75rem 1.25rem; border-bottom: 1px solid #f1f5f9; color: #1e293b; }
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: #fafafa; }
.badge { display: inline-flex; align-items: center; padding: 0.2rem 0.625rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 600; }
.badge-active  { background: #eff6ff; color: #2563eb; }
.badge-danger  { background: #fee2e2; color: #dc2626; }
.badge-safe    { background: #dcfce7; color: #16a34a; }
.mono { font-family: 'Courier New', monospace; }
.text-muted { color: #94a3b8; }
.empty-cell { text-align: center; padding: 2.5rem; color: #94a3b8; }
.icon-btn { background: none; border: 1px solid #e2e8f0; border-radius: 0.375rem; padding: 0.25rem 0.5rem; cursor: pointer; color: #64748b; transition: all 0.15s; }
.icon-btn:hover { border-color: #3b82f6; color: #3b82f6; }
.link-btn { background: none; border: none; cursor: pointer; color: #3b82f6; font-size: 0.8125rem; }
.link-btn:hover { text-decoration: underline; }

/* Agent 安装按钮 + 在线状态 */
.btn-agent { background: #eff6ff; color: #2563eb; border: 1px solid #bfdbfe; margin-left: 0.625rem; }
.btn-agent:hover:not(:disabled) { background: #dbeafe; }
.agent-hint { display: flex; align-items: center; gap: 0.75rem; margin: 0.75rem 0 0; flex-wrap: wrap; }
.agent-status { display: inline-flex; align-items: center; gap: 0.4rem; font-size: 0.8125rem; font-weight: 600; padding: 0.25rem 0.625rem; border-radius: 9999px; border: 1px solid; }
.agent-on  { background: #f0fdf4; color: #16a34a; border-color: #bbf7d0; }
.agent-on .badge-dot { animation: pulse 2s infinite; }
.agent-off { background: #fef2f2; color: #dc2626; border-color: #fecaca; }
.agent-tip { font-size: 0.8125rem; color: #64748b; }

/* 安装命令弹窗 */
.modal-mask { position: fixed; inset: 0; background: rgba(15,23,42,0.5); display: flex; align-items: center; justify-content: center; z-index: 50; padding: 1rem; }
.modal-box { background: #fff; border-radius: 0.875rem; width: 100%; max-width: 40rem; box-shadow: 0 20px 50px rgba(0,0,0,0.25); overflow: hidden; }
.modal-head { display: flex; align-items: center; justify-content: space-between; padding: 1rem 1.25rem; border-bottom: 1px solid #f1f5f9; font-size: 0.9375rem; font-weight: 600; color: #1e293b; }
.modal-close { background: none; border: none; cursor: pointer; color: #94a3b8; font-size: 1rem; }
.modal-close:hover { color: #1e293b; }
.modal-body { padding: 1.25rem; }
.modal-desc { font-size: 0.875rem; color: #475569; line-height: 1.6; margin: 0 0 1rem; }
.modal-loading { display: flex; flex-direction: column; align-items: center; gap: 0.75rem; padding: 1.5rem; color: #94a3b8; }
.cmd-box { position: relative; background: #0f172a; border-radius: 0.5rem; padding: 0.875rem 1rem; display: flex; align-items: flex-start; gap: 0.75rem; }
.cmd-text { flex: 1; color: #e2e8f0; font-family: 'Courier New', monospace; font-size: 0.8125rem; line-height: 1.5; word-break: break-all; white-space: pre-wrap; }
.copy-btn { flex-shrink: 0; background: #1e293b; color: #93c5fd; border: 1px solid #334155; border-radius: 0.375rem; padding: 0.3rem 0.625rem; font-size: 0.75rem; cursor: pointer; white-space: nowrap; }
.copy-btn:hover { background: #334155; }
.modal-notes { margin: 1rem 0 0; padding-left: 1.1rem; font-size: 0.8125rem; color: #64748b; line-height: 1.8; }
.modal-notes code { background: #f1f5f9; padding: 0.05rem 0.35rem; border-radius: 0.25rem; font-size: 0.75rem; color: #475569; }

@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.4} }
@keyframes spin   { to { transform: rotate(360deg); } }

@media (max-width: 1024px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .main-grid  { grid-template-columns: 1fr; }
}
@media (max-width: 600px) {
  .control-inner { flex-direction: column; align-items: stretch; }
  .stats-row { grid-template-columns: 1fr 1fr; }
}
</style>
