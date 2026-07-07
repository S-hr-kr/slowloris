<template>
  <div class="page">
    <!-- 页头 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon"><i class="fa fa-bell"></i></div>
        <div>
          <h1>日志与告警</h1>
          <p>查看系统告警记录和操作日志</p>
        </div>
      </div>
      <div class="unread-badge" v-if="unreadCount > 0">
        <i class="fa fa-bell"></i> {{ unreadCount }} 条未处理告警
      </div>
    </div>

    <!-- Tab 切换 -->
    <div class="tab-bar">
      <button class="tab-btn" :class="{ active: activeTab === 'alerts' }" @click="switchTab('alerts')">
        <i class="fa fa-exclamation-triangle"></i> 告警记录
        <span v-if="unreadCount" class="tab-count">{{ unreadCount }}</span>
      </button>
      <button class="tab-btn" :class="{ active: activeTab === 'logs' }" @click="switchTab('logs')">
        <i class="fa fa-list-alt"></i> 系统日志
      </button>
    </div>

    <!-- 告警 Tab -->
    <div v-show="activeTab === 'alerts'" class="card tab-card">
      <!-- 筛选条 -->
      <div class="filter-bar">
        <div class="filter-group">
          <label>状态</label>
          <select v-model="alertFilter.status" @change="fetchAlerts(true)" class="filter-select">
            <option value="">全部</option>
            <option value="0">未处理</option>
            <option value="1">已处理</option>
          </select>
        </div>
        <div class="filter-group">
          <label>级别</label>
          <select v-model="alertFilter.level" @change="fetchAlerts(true)" class="filter-select">
            <option value="">全部</option>
            <option value="4">严重</option>
            <option value="3">高危</option>
            <option value="2">中危</option>
            <option value="1">低危</option>
          </select>
        </div>
        <button class="icon-btn" @click="fetchAlerts(true)"><i class="fa fa-refresh"></i> 刷新</button>
        <button v-if="selectedAlertIds.length" class="btn-handle" @click="batchHandle">
          <i class="fa fa-check"></i> 批量标记已处理 ({{ selectedAlertIds.length }})
        </button>
        <button v-if="admin && selectedAlertIds.length" class="btn-delete" @click="batchDeleteAlerts">
          <i class="fa fa-trash"></i> 批量删除 ({{ selectedAlertIds.length }})
        </button>
      </div>

      <!-- 告警表格 -->
      <div style="overflow-x:auto">
        <table class="data-table">
          <thead>
            <tr>
              <th style="width:2.5rem">
                <input type="checkbox" @change="toggleSelectAll" :checked="allSelected" />
              </th>
              <th>级别</th>
              <th>告警消息</th>
              <th>目标 IP</th>
              <th>状态</th>
              <th>时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="alertsLoading">
              <td colspan="7" class="empty-cell"><i class="fa fa-spinner fa-spin"></i> 加载中...</td>
            </tr>
            <tr v-else-if="alerts.length === 0">
              <td colspan="7" class="empty-cell"><i class="fa fa-check-circle"></i> 暂无告警记录</td>
            </tr>
            <tr v-for="a in alerts" :key="a.id" :class="{ 'row-unread': a.status === 0 }">
              <td>
                <input type="checkbox" :value="a.id" v-model="selectedAlertIds" :disabled="!admin && a.status === 1" />
              </td>
              <td>
                <span class="level-badge" :class="'lv-' + levelClass(a.level)">{{ levelLabel(a.level) }}</span>
              </td>
              <td class="msg-cell">{{ a.message }}</td>
              <td class="mono">{{ a.ipAddress || '—' }}</td>
              <td>
                <span class="status-badge" :class="a.status === 1 ? 'st-handled' : 'st-pending'">
                  {{ a.status === 1 ? '已处理' : '未处理' }}
                </span>
              </td>
              <td class="text-muted">{{ formatTime(a.createTime) }}</td>
              <td>
                <div class="row-actions">
                  <button v-if="a.status === 0" class="link-btn" @click="handleAlert(a)">
                    <i class="fa fa-check"></i> 标记处理
                  </button>
                  <span v-else class="text-muted handled-time">{{ formatTime(a.handleTime) }}</span>
                  <button v-if="admin" class="link-btn link-danger" @click="deleteAlert(a)">
                    <i class="fa fa-trash"></i> 删除
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="alertTotal > alertPageSize">
        <button class="page-btn" :disabled="alertPage <= 1" @click="alertPage--; fetchAlerts()">
          <i class="fa fa-chevron-left"></i>
        </button>
        <span class="page-info">第 {{ alertPage }} / {{ alertTotalPages }} 页 · 共 {{ alertTotal }} 条</span>
        <button class="page-btn" :disabled="alertPage >= alertTotalPages" @click="alertPage++; fetchAlerts()">
          <i class="fa fa-chevron-right"></i>
        </button>
      </div>
    </div>

    <!-- 日志 Tab -->
    <div v-show="activeTab === 'logs'" class="card tab-card">
      <!-- 筛选 -->
      <div class="filter-bar">
        <div class="filter-group">
          <label>日志类型</label>
          <select v-model="logFilter.type" @change="fetchLogs(true)" class="filter-select">
            <option value="">全部</option>
            <option :value="1">操作</option>
            <option :value="2">访问</option>
            <option :value="3">错误</option>
          </select>
        </div>
        <button class="icon-btn" @click="fetchLogs(true)"><i class="fa fa-refresh"></i> 刷新</button>
        <button v-if="admin && selectedLogIds.length" class="btn-delete" @click="batchDeleteLogs">
          <i class="fa fa-trash"></i> 批量删除 ({{ selectedLogIds.length }})
        </button>
      </div>

      <div style="overflow-x:auto">
        <table class="data-table">
          <thead>
            <tr>
              <th v-if="admin" style="width:2.5rem">
                <input type="checkbox" @change="toggleSelectAllLogs" :checked="allLogsSelected" />
              </th>
              <th>类型</th>
              <th>操作描述</th>
              <th>操作人</th>
              <th>IP 地址</th>
              <th>时间</th>
              <th v-if="admin">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="logsLoading">
              <td :colspan="admin ? 7 : 5" class="empty-cell"><i class="fa fa-spinner fa-spin"></i> 加载中...</td>
            </tr>
            <tr v-else-if="logs.length === 0">
              <td :colspan="admin ? 7 : 5" class="empty-cell">暂无日志记录</td>
            </tr>
            <tr v-for="l in logs" :key="l.id">
              <td v-if="admin">
                <input type="checkbox" :value="l.id" v-model="selectedLogIds" />
              </td>
              <td>
                <span class="type-badge" :class="'type-' + logTypeClass(l.type)">
                  {{ logTypeLabel(l.type) }}
                </span>
              </td>
              <td class="msg-cell">{{ l.description || l.message }}</td>
              <td>{{ l.operator || '—' }}</td>
              <td class="mono">{{ l.ipAddress || '—' }}</td>
              <td class="text-muted">{{ formatTime(l.createTime) }}</td>
              <td v-if="admin">
                <button class="link-btn link-danger" @click="deleteLog(l)">
                  <i class="fa fa-trash"></i> 删除
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="logTotal > logPageSize">
        <button class="page-btn" :disabled="logPage <= 1" @click="logPage--; fetchLogs()">
          <i class="fa fa-chevron-left"></i>
        </button>
        <span class="page-info">第 {{ logPage }} / {{ logTotalPages }} 页 · 共 {{ logTotal }} 条</span>
        <button class="page-btn" :disabled="logPage >= logTotalPages" @click="logPage++; fetchLogs()">
          <i class="fa fa-chevron-right"></i>
        </button>
      </div>
    </div>

    <!-- 删除确认弹窗 -->
    <div v-if="confirmState.show" class="confirm-overlay" @click.self="resolveConfirm(false)">
      <div class="confirm-card">
        <div class="confirm-icon"><i class="fa fa-exclamation-triangle"></i></div>
        <h3 class="confirm-title">删除确认</h3>
        <p class="confirm-msg">{{ confirmState.message }}</p>
        <div class="confirm-actions">
          <button class="confirm-cancel" @click="resolveConfirm(false)">取消</button>
          <button class="confirm-ok" @click="resolveConfirm(true)"><i class="fa fa-trash"></i> 确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../services/api.js'
import { isAdmin } from '../services/auth.js'
import { useToast } from '../composables/useToast.js'

const toast = useToast()
const admin = isAdmin()

// 删除确认弹窗（Promise 化）
const confirmState = ref({ show: false, message: '', resolve: null })
function confirmDelete(message) {
  return new Promise((resolve) => {
    confirmState.value = { show: true, message, resolve }
  })
}
function resolveConfirm(result) {
  const r = confirmState.value.resolve
  confirmState.value = { show: false, message: '', resolve: null }
  if (r) r(result)
}

const activeTab = ref('alerts')

// --- Alerts ---
const alerts = ref([])
const alertsLoading = ref(false)
const alertFilter = ref({ status: '', level: '' })
const alertPage = ref(1)
const alertPageSize = 15
const alertTotal = ref(0)
const alertTotalPages = computed(() => Math.max(1, Math.ceil(alertTotal.value / alertPageSize)))
const unreadCount = ref(0)
const selectedAlertIds = ref([])
const allSelected = computed(() =>
  alerts.value.filter(a => a.status === 0).length > 0 &&
  alerts.value.filter(a => a.status === 0).every(a => selectedAlertIds.value.includes(a.id))
)

async function fetchAlerts(reset = false) {
  if (reset) { alertPage.value = 1; selectedAlertIds.value = [] }
  alertsLoading.value = true
  try {
    const params = new URLSearchParams({
      page: alertPage.value,
      pageSize: alertPageSize,
      ...(alertFilter.value.status !== '' && { status: alertFilter.value.status }),
      ...(alertFilter.value.level   !== '' && { level:  alertFilter.value.level  }),
    })
    const res = await api.get(`/api/alerts?${params}`)
    const d = res.data || {}
    alerts.value    = d.records || d || []
    alertTotal.value = d.total   || alerts.value.length
    unreadCount.value = d.unreadCount ?? 0
  } catch { alerts.value = [] } finally { alertsLoading.value = false }
}

async function handleAlert(a) {
  try {
    await api.put(`/api/alerts/${a.id}`, { status: 1 })
    a.status = 1
    a.handleTime = new Date().toISOString()
    unreadCount.value = Math.max(0, unreadCount.value - 1)
    selectedAlertIds.value = selectedAlertIds.value.filter(id => id !== a.id)
  } catch { /* silent */ }
}

async function batchHandle() {
  const ids = [...selectedAlertIds.value]
  await Promise.all(ids.map(id => {
    const a = alerts.value.find(x => x.id === id)
    return a ? handleAlert(a) : Promise.resolve()
  }))
  selectedAlertIds.value = []
}

function toggleSelectAll(e) {
  if (e.target.checked) {
    selectedAlertIds.value = admin
      ? alerts.value.map(a => a.id)
      : alerts.value.filter(a => a.status === 0).map(a => a.id)
  } else {
    selectedAlertIds.value = []
  }
}

async function deleteAlert(a) {
  const ok = await confirmDelete(`确认删除这条告警？\n「${a.message}」`)
  if (!ok) return
  try {
    await api.delete(`/api/alerts/${a.id}`)
    alerts.value = alerts.value.filter(x => x.id !== a.id)
    selectedAlertIds.value = selectedAlertIds.value.filter(id => id !== a.id)
    alertTotal.value = Math.max(0, alertTotal.value - 1)
    if (a.status === 0) unreadCount.value = Math.max(0, unreadCount.value - 1)
    toast.success('告警已删除')
  } catch (e) {
    toast.error(e?.response?.data?.message || '删除失败')
  }
}

async function batchDeleteAlerts() {
  const ids = [...selectedAlertIds.value]
  if (!ids.length) return
  const ok = await confirmDelete(`确认删除选中的 ${ids.length} 条告警？此操作不可恢复。`)
  if (!ok) return
  try {
    await api.delete('/api/alerts', { ids })
    toast.success(`已删除 ${ids.length} 条告警`)
    selectedAlertIds.value = []
    fetchAlerts(true)
  } catch (e) {
    toast.error(e?.response?.data?.message || '批量删除失败')
  }
}

// --- Logs ---
const logs = ref([])
const logsLoading = ref(false)
const logFilter = ref({ type: '' })
const logPage = ref(1)
const logPageSize = 15
const logTotal = ref(0)
const logTotalPages = computed(() => Math.max(1, Math.ceil(logTotal.value / logPageSize)))
const selectedLogIds = ref([])
const allLogsSelected = computed(() =>
  logs.value.length > 0 && logs.value.every(l => selectedLogIds.value.includes(l.id))
)

async function fetchLogs(reset = false) {
  if (reset) { logPage.value = 1; selectedLogIds.value = [] }
  logsLoading.value = true
  try {
    const params = new URLSearchParams({
      page: logPage.value,
      pageSize: logPageSize,
      ...(logFilter.value.type && { type: logFilter.value.type }),
    })
    const res = await api.get(`/api/logs?${params}`)
    const d = res.data || {}
    logs.value    = d.records || d || []
    logTotal.value = d.total   || logs.value.length
  } catch { logs.value = [] } finally { logsLoading.value = false }
}

function toggleSelectAllLogs(e) {
  selectedLogIds.value = e.target.checked ? logs.value.map(l => l.id) : []
}

async function deleteLog(l) {
  const ok = await confirmDelete('确认删除这条日志记录？')
  if (!ok) return
  try {
    await api.delete(`/api/logs/${l.id}`)
    logs.value = logs.value.filter(x => x.id !== l.id)
    selectedLogIds.value = selectedLogIds.value.filter(id => id !== l.id)
    logTotal.value = Math.max(0, logTotal.value - 1)
    toast.success('日志已删除')
  } catch (e) {
    toast.error(e?.response?.data?.message || '删除失败')
  }
}

async function batchDeleteLogs() {
  const ids = [...selectedLogIds.value]
  if (!ids.length) return
  const ok = await confirmDelete(`确认删除选中的 ${ids.length} 条日志？此操作不可恢复。`)
  if (!ok) return
  try {
    await api.delete('/api/logs', { ids })
    toast.success(`已删除 ${ids.length} 条日志`)
    selectedLogIds.value = []
    fetchLogs(true)
  } catch (e) {
    toast.error(e?.response?.data?.message || '批量删除失败')
  }
}

function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'alerts' && alerts.value.length === 0) fetchAlerts()
  if (tab === 'logs'   && logs.value.length   === 0) fetchLogs()
}

function levelClass(lvl) { return { 4:'critical', 3:'high', 2:'medium', 1:'low' }[lvl] || 'low' }
function levelLabel(lvl) { return { 4:'严重', 3:'高危', 2:'中危', 1:'低危' }[lvl] || '低危' }
function logTypeClass(t) { return { 1:'op', 2:'access', 3:'error' }[t] || 'system' }
function logTypeLabel(t) { return { 1:'操作', 2:'访问', 3:'错误' }[t] || '系统' }
function formatTime(t) {
  if (!t) return '—'
  try { return new Date(t).toLocaleString('zh-CN') } catch { return t }
}

onMounted(() => fetchAlerts())
</script>

<style scoped>
.page { padding: 0; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.5rem; }
.header-left { display: flex; align-items: center; gap: 0.875rem; }
.header-icon {
  width: 2.75rem; height: 2.75rem;
  background: linear-gradient(135deg, #ef4444, #dc2626);
  border-radius: 0.75rem;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 1.125rem;
  box-shadow: 0 4px 12px rgba(239,68,68,0.3);
}
.page-header h1 { font-size: 1.375rem; font-weight: 700; color: #1e293b; margin: 0; }
.page-header p  { font-size: 0.8125rem; color: #64748b; margin: 0.125rem 0 0; }
.unread-badge {
  display: flex; align-items: center; gap: 0.4rem;
  background: #fef2f2; border: 1px solid #fecaca;
  color: #dc2626; font-size: 0.8125rem; font-weight: 600;
  padding: 0.375rem 0.875rem; border-radius: 9999px;
}

/* Tabs */
.tab-bar { display: flex; gap: 0.25rem; margin-bottom: 1rem; border-bottom: 2px solid #f1f5f9; }
.tab-btn {
  display: inline-flex; align-items: center; gap: 0.4rem;
  padding: 0.625rem 1.125rem; border: none; background: none;
  font-size: 0.9375rem; font-weight: 500; color: #64748b; cursor: pointer;
  border-bottom: 2px solid transparent; margin-bottom: -2px;
  transition: color 0.15s, border-color 0.15s;
}
.tab-btn:hover { color: #1e293b; }
.tab-btn.active { color: #2563eb; border-bottom-color: #3b82f6; font-weight: 600; }
.tab-count { background: #ef4444; color: #fff; font-size: 0.6875rem; font-weight: 700; border-radius: 9999px; padding: 0.1rem 0.4rem; min-width: 1.25rem; text-align: center; }

/* Card */
.tab-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 0.875rem; box-shadow: 0 1px 4px rgba(0,0,0,0.06); overflow: hidden; }

/* Filter bar */
.filter-bar { display: flex; align-items: center; gap: 1rem; padding: 1rem 1.25rem; border-bottom: 1px solid #f1f5f9; flex-wrap: wrap; }
.filter-group { display: flex; align-items: center; gap: 0.5rem; }
.filter-group label { font-size: 0.8125rem; font-weight: 600; color: #374151; white-space: nowrap; }
.filter-select {
  padding: 0.375rem 0.625rem; border: 1px solid #d1d5db; border-radius: 0.5rem;
  font-size: 0.875rem; color: #1e293b; background: #fff; outline: none; cursor: pointer;
}
.filter-select:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.1); }
.icon-btn {
  background: none; border: 1px solid #e2e8f0; border-radius: 0.375rem;
  padding: 0.375rem 0.75rem; cursor: pointer; color: #64748b; font-size: 0.875rem;
  display: inline-flex; align-items: center; gap: 0.3rem; transition: all 0.15s;
}
.icon-btn:hover { border-color: #3b82f6; color: #3b82f6; }
.btn-handle {
  display: inline-flex; align-items: center; gap: 0.4rem;
  padding: 0.375rem 0.875rem; border: none; border-radius: 0.5rem;
  background: #3b82f6; color: #fff; font-size: 0.875rem; font-weight: 600; cursor: pointer;
  transition: opacity 0.15s;
}
.btn-handle:hover { opacity: 0.9; }
.btn-delete {
  display: inline-flex; align-items: center; gap: 0.4rem;
  padding: 0.375rem 0.875rem; border: none; border-radius: 0.5rem;
  background: #ef4444; color: #fff; font-size: 0.875rem; font-weight: 600; cursor: pointer;
  transition: opacity 0.15s;
}
.btn-delete:hover { opacity: 0.9; }

/* Table */
.data-table { width: 100%; border-collapse: collapse; font-size: 0.875rem; }
.data-table th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.8125rem; font-weight: 600; color: #64748b; background: #f8fafc; border-bottom: 1px solid #e2e8f0; }
.data-table td { padding: 0.75rem 1.25rem; border-bottom: 1px solid #f1f5f9; color: #1e293b; }
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: #fafafa; }
.row-unread td { background: #fffbeb; }
.row-unread:hover td { background: #fef9c3; }
.msg-cell { max-width: 22rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mono { font-family: 'Courier New', monospace; }
.text-muted { color: #94a3b8; font-size: 0.8125rem; }
.empty-cell { text-align: center; padding: 3rem; color: #94a3b8; }
.handled-time { font-size: 0.75rem; }

/* Level badges */
.level-badge { display: inline-flex; align-items: center; padding: 0.2rem 0.625rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 700; }
.lv-critical { background: #fee2e2; color: #dc2626; }
.lv-high     { background: #ffedd5; color: #ea580c; }
.lv-medium   { background: #fefce8; color: #ca8a04; }
.lv-low      { background: #dcfce7; color: #16a34a; }

/* Status badges */
.status-badge { display: inline-flex; align-items: center; padding: 0.2rem 0.625rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 600; }
.st-pending { background: #fef9c3; color: #a16207; }
.st-handled { background: #f1f5f9; color: #64748b; }

/* Type badges */
.type-badge { display: inline-flex; padding: 0.2rem 0.5rem; border-radius: 0.25rem; font-size: 0.75rem; font-weight: 600; }
.type-login   { background: #eff6ff; color: #2563eb; }
.type-monitor { background: #f0fdf4; color: #16a34a; }
.type-alert   { background: #fef2f2; color: #dc2626; }
.type-op      { background: #eff6ff; color: #2563eb; }
.type-access  { background: #f0fdf4; color: #16a34a; }
.type-error   { background: #fef2f2; color: #dc2626; }
.type-system  { background: #f1f5f9; color: #475569; }

/* Link btn */
.link-btn { background: none; border: none; cursor: pointer; color: #3b82f6; font-size: 0.8125rem; display: inline-flex; align-items: center; gap: 0.25rem; }
.link-btn:hover { text-decoration: underline; }
.link-danger { color: #ef4444; }
.row-actions { display: flex; align-items: center; gap: 0.875rem; flex-wrap: wrap; }

/* Pagination */
.pagination { display: flex; align-items: center; justify-content: center; gap: 0.875rem; padding: 1rem 1.25rem; border-top: 1px solid #f1f5f9; }
.page-btn {
  background: none; border: 1px solid #e2e8f0; border-radius: 0.375rem;
  padding: 0.375rem 0.625rem; cursor: pointer; color: #374151; transition: all 0.15s;
}
.page-btn:hover:not(:disabled) { border-color: #3b82f6; color: #3b82f6; }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { font-size: 0.875rem; color: #64748b; }

/* 删除确认弹窗 */
.confirm-overlay {
  position: fixed; inset: 0; z-index: 999;
  background: rgba(15,23,42,0.5);
  display: flex; align-items: center; justify-content: center; padding: 1rem;
}
.confirm-card {
  background: #fff; border-radius: 16px; padding: 1.75rem 1.75rem 1.5rem;
  width: 100%; max-width: 360px; text-align: center;
  box-shadow: 0 24px 64px rgba(15,23,42,0.25);
  animation: confirmPop 0.2s cubic-bezier(0.22,1,0.36,1);
}
@keyframes confirmPop { from { opacity: 0; transform: scale(0.94); } to { opacity: 1; transform: scale(1); } }
.confirm-icon {
  width: 3rem; height: 3rem; margin: 0 auto 0.875rem;
  background: #fef2f2; color: #ef4444; border-radius: 9999px;
  display: flex; align-items: center; justify-content: center; font-size: 1.25rem;
}
.confirm-title { font-size: 1.0625rem; font-weight: 700; color: #1e293b; margin: 0 0 0.5rem; }
.confirm-msg { font-size: 0.875rem; color: #64748b; margin: 0 0 1.25rem; line-height: 1.6; white-space: pre-line; }
.confirm-actions { display: flex; gap: 0.75rem; }
.confirm-cancel, .confirm-ok {
  flex: 1; padding: 0.5625rem; border-radius: 0.5rem; font-size: 0.875rem; font-weight: 600;
  cursor: pointer; border: none; display: inline-flex; align-items: center; justify-content: center; gap: 0.4rem;
}
.confirm-cancel { background: #f1f5f9; color: #475569; }
.confirm-cancel:hover { background: #e2e8f0; }
.confirm-ok { background: #ef4444; color: #fff; }
.confirm-ok:hover { background: #dc2626; }

</style>
