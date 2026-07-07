<template>
  <div class="page">
    <!-- 页头 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon"><i class="fa fa-users-cog"></i></div>
        <div>
          <h1>用户管理</h1>
          <p>管理员专属 · 创建、编辑、禁用与删除系统用户</p>
        </div>
      </div>
      <button class="btn btn-primary" @click="openCreate">
        <i class="fa fa-plus"></i> 新建用户
      </button>
    </div>

    <!-- 统计 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon stat-icon--blue"><i class="fa fa-users"></i></div>
        <div><p class="stat-val">{{ users.length }}</p><p class="stat-label">用户总数</p></div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon--purple"><i class="fa fa-user-shield"></i></div>
        <div><p class="stat-val">{{ adminCount }}</p><p class="stat-label">管理员</p></div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon--green"><i class="fa fa-user-check"></i></div>
        <div><p class="stat-val">{{ activeCount }}</p><p class="stat-label">启用中</p></div>
      </div>
    </div>

    <!-- 用户表 -->
    <div class="card">
      <div class="card-head" style="justify-content:space-between">
        <span><i class="fa fa-list" style="color:#3b82f6"></i> 用户列表</span>
        <button class="icon-btn" @click="fetchUsers"><i class="fa fa-refresh"></i></button>
      </div>
      <div style="overflow-x:auto">
        <table class="data-table">
          <thead>
            <tr><th>用户名</th><th>邮箱</th><th>角色</th><th>状态</th><th>创建时间</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="6" class="empty-cell"><i class="fa fa-spinner fa-spin"></i> 加载中...</td></tr>
            <tr v-else-if="users.length === 0"><td colspan="6" class="empty-cell">暂无用户</td></tr>
            <tr v-for="u in users" :key="u.id">
              <td class="strong">{{ u.username }}</td>
              <td class="text-muted">{{ u.email || '—' }}</td>
              <td><span class="role-badge" :class="'role-' + u.role">{{ roleLabel(u.role) }}</span></td>
              <td><span class="status-badge" :class="u.status === 'active' ? 'st-on' : 'st-off'">{{ u.status === 'active' ? '启用' : '禁用' }}</span></td>
              <td class="text-muted">{{ formatTime(u.createdAt) }}</td>
              <td>
                <div class="row-actions">
                  <button class="link-btn" @click="openEdit(u)"><i class="fa fa-pencil"></i> 编辑</button>
                  <button class="link-btn link-danger" :disabled="u.username === currentUsername" @click="removeUser(u)">
                    <i class="fa fa-trash"></i> 删除
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 新建/编辑弹窗 -->
    <div v-if="modal.show" class="modal-mask" @click.self="closeModal">
      <div class="modal-box">
        <div class="modal-head">
          <span><i :class="modal.mode === 'create' ? 'fa fa-user-plus' : 'fa fa-user-edit'" style="color:#3b82f6"></i>
            {{ modal.mode === 'create' ? '新建用户' : '编辑用户' }}</span>
          <button class="modal-close" @click="closeModal"><i class="fa fa-times"></i></button>
        </div>
        <div class="modal-body">
          <div class="field">
            <label>用户名</label>
            <input v-model="modal.form.username" class="input" :disabled="modal.mode === 'edit'" placeholder="3-64 个字符" />
          </div>
          <div class="field">
            <label>邮箱</label>
            <input v-model="modal.form.email" type="email" class="input" placeholder="you@example.com" />
          </div>
          <div class="field">
            <label>{{ modal.mode === 'create' ? '密码' : '重置密码（留空不修改）' }}</label>
            <input v-model="modal.form.password" type="password" class="input" :placeholder="modal.mode === 'create' ? '至少 6 位' : '留空表示不修改'" />
          </div>
          <div class="field-row">
            <div class="field">
              <label>角色</label>
              <select v-model="modal.form.role" class="input">
                <option value="user">普通用户</option>
                <option value="operator">操作员</option>
                <option value="admin">管理员</option>
              </select>
            </div>
            <div class="field">
              <label>状态</label>
              <select v-model="modal.form.status" class="input">
                <option value="active">启用</option>
                <option value="inactive">禁用</option>
              </select>
            </div>
          </div>
          <div v-if="modal.error" class="alert-error"><i class="fa fa-exclamation-triangle"></i> {{ modal.error }}</div>
        </div>
        <div class="modal-foot">
          <button class="btn btn-secondary" @click="closeModal">取消</button>
          <button class="btn btn-primary" :disabled="modal.saving" @click="submitModal">
            <span v-if="modal.saving"><i class="fa fa-spinner fa-spin"></i> 保存中...</span>
            <span v-else><i class="fa fa-save"></i> 保存</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 删除确认 -->
    <div v-if="confirmState.show" class="modal-mask" @click.self="resolveConfirm(false)">
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
import { ref, reactive, computed, onMounted } from 'vue'
import api from '../services/api.js'
import { getUsername, roleLabel as roleLabelFn } from '../services/auth.js'
import { useToast } from '../composables/useToast.js'

const toast = useToast()
const currentUsername = getUsername()

const users = ref([])
const loading = ref(false)

const adminCount = computed(() => users.value.filter(u => u.role === 'admin').length)
const activeCount = computed(() => users.value.filter(u => u.status === 'active').length)

const modal = reactive({
  show: false, mode: 'create', saving: false, error: '',
  form: { id: null, username: '', email: '', password: '', role: 'user', status: 'active' }
})

const confirmState = ref({ show: false, message: '', resolve: null })
function confirmDelete(message) {
  return new Promise((resolve) => { confirmState.value = { show: true, message, resolve } })
}
function resolveConfirm(result) {
  const r = confirmState.value.resolve
  confirmState.value = { show: false, message: '', resolve: null }
  if (r) r(result)
}

function roleLabel(r) { return roleLabelFn(r) }
function formatTime(t) {
  if (!t) return '—'
  try { return new Date(t).toLocaleString('zh-CN') } catch { return t }
}

async function fetchUsers() {
  loading.value = true
  try {
    const res = await api.get('/api/users')
    users.value = res.data || []
  } catch (e) {
    toast.error(e?.response?.data?.message || '加载用户列表失败')
    users.value = []
  } finally { loading.value = false }
}

function openCreate() {
  modal.mode = 'create'
  modal.error = ''
  modal.form = { id: null, username: '', email: '', password: '', role: 'user', status: 'active' }
  modal.show = true
}

function openEdit(u) {
  modal.mode = 'edit'
  modal.error = ''
  modal.form = { id: u.id, username: u.username, email: u.email || '', password: '', role: u.role || 'user', status: u.status || 'active' }
  modal.show = true
}

function closeModal() { modal.show = false }

function validateModal() {
  modal.error = ''
  const f = modal.form
  if (modal.mode === 'create') {
    if (!f.username.trim() || f.username.trim().length < 3) { modal.error = '用户名至少 3 个字符'; return false }
    if (!f.password || f.password.length < 6) { modal.error = '密码至少 6 位'; return false }
  } else if (f.password && f.password.length < 6) {
    modal.error = '新密码至少 6 位'; return false
  }
  if (f.email.trim() && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(f.email.trim())) { modal.error = '邮箱格式不正确'; return false }
  return true
}

async function submitModal() {
  if (!validateModal()) return
  modal.saving = true
  const f = modal.form
  try {
    if (modal.mode === 'create') {
      await api.post('/api/users', {
        username: f.username.trim(), password: f.password,
        email: f.email.trim(), role: f.role, status: f.status
      })
      toast.success('用户创建成功')
    } else {
      const payload = { email: f.email.trim(), role: f.role, status: f.status }
      if (f.password) payload.password = f.password
      await api.put(`/api/users/${f.id}`, payload)
      toast.success('用户已更新')
    }
    modal.show = false
    fetchUsers()
  } catch (e) {
    modal.error = e?.response?.data?.message || '保存失败'
  } finally { modal.saving = false }
}

async function removeUser(u) {
  if (u.username === currentUsername) { toast.warning('不能删除当前登录的账户'); return }
  const ok = await confirmDelete(`确认删除用户「${u.username}」？此操作不可恢复。`)
  if (!ok) return
  try {
    await api.delete(`/api/users/${u.id}`)
    users.value = users.value.filter(x => x.id !== u.id)
    toast.success('用户已删除')
  } catch (e) {
    toast.error(e?.response?.data?.message || '删除失败')
  }
}

onMounted(fetchUsers)
</script>

<style scoped>
.page { padding: 0; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.5rem; }
.header-left { display: flex; align-items: center; gap: 0.875rem; }
.header-icon {
  width: 2.75rem; height: 2.75rem; border-radius: 0.75rem;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  display: flex; align-items: center; justify-content: center; color: #fff; font-size: 1.125rem;
  box-shadow: 0 4px 12px rgba(59,130,246,0.3);
}
.page-header h1 { font-size: 1.375rem; font-weight: 700; color: #1e293b; margin: 0; }
.page-header p { font-size: 0.8125rem; color: #64748b; margin: 0.125rem 0 0; }

.stats-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; margin-bottom: 1.25rem; }
.stat-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 0.875rem; padding: 1rem 1.25rem; display: flex; align-items: center; gap: 0.875rem; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.stat-icon { width: 2.5rem; height: 2.5rem; border-radius: 0.625rem; flex-shrink: 0; display: flex; align-items: center; justify-content: center; font-size: 1rem; }
.stat-icon--blue { background: #eff6ff; color: #3b82f6; }
.stat-icon--purple { background: #f5f3ff; color: #8b5cf6; }
.stat-icon--green { background: #f0fdf4; color: #22c55e; }
.stat-val { font-size: 1.25rem; font-weight: 700; color: #1e293b; margin: 0; }
.stat-label { font-size: 0.75rem; color: #94a3b8; margin: 0.125rem 0 0; }

.card { background: #fff; border: 1px solid #e2e8f0; border-radius: 0.875rem; box-shadow: 0 1px 4px rgba(0,0,0,0.06); overflow: hidden; }
.card-head { display: flex; align-items: center; gap: 0.5rem; padding: 1rem 1.25rem; border-bottom: 1px solid #f1f5f9; font-size: 0.9375rem; font-weight: 600; color: #1e293b; }

.data-table { width: 100%; border-collapse: collapse; font-size: 0.875rem; }
.data-table th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.8125rem; font-weight: 600; color: #64748b; background: #f8fafc; border-bottom: 1px solid #e2e8f0; }
.data-table td { padding: 0.75rem 1.25rem; border-bottom: 1px solid #f1f5f9; color: #1e293b; }
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: #fafafa; }
.strong { font-weight: 600; }
.text-muted { color: #94a3b8; font-size: 0.8125rem; }
.empty-cell { text-align: center; padding: 3rem; color: #94a3b8; }

.role-badge, .status-badge { display: inline-flex; padding: 0.2rem 0.625rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 700; }
.role-admin { background: #ede9fe; color: #7c3aed; }
.role-operator { background: #eff6ff; color: #2563eb; }
.role-user, .role-viewer { background: #f1f5f9; color: #64748b; }
.st-on { background: #dcfce7; color: #16a34a; }
.st-off { background: #fee2e2; color: #dc2626; }

.row-actions { display: flex; align-items: center; gap: 0.875rem; }
.link-btn { background: none; border: none; cursor: pointer; color: #3b82f6; font-size: 0.8125rem; display: inline-flex; align-items: center; gap: 0.25rem; }
.link-btn:hover:not(:disabled) { text-decoration: underline; }
.link-btn:disabled { color: #cbd5e1; cursor: not-allowed; }
.link-danger { color: #ef4444; }
.icon-btn { background: none; border: 1px solid #e2e8f0; border-radius: 0.375rem; padding: 0.25rem 0.5rem; cursor: pointer; color: #64748b; transition: all 0.15s; }
.icon-btn:hover { border-color: #3b82f6; color: #3b82f6; }

.btn { padding: 0.5rem 1.25rem; border: none; border-radius: 0.5rem; font-size: 0.875rem; font-weight: 600; cursor: pointer; display: inline-flex; align-items: center; gap: 0.4rem; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-primary { background: linear-gradient(135deg, #3b82f6, #2563eb); color: #fff; box-shadow: 0 2px 8px rgba(59,130,246,0.3); }
.btn-primary:hover:not(:disabled) { opacity: 0.92; }
.btn-secondary { background: #f1f5f9; color: #475569; }
.btn-secondary:hover { background: #e2e8f0; }

/* Modal */
.modal-mask { position: fixed; inset: 0; background: rgba(15,23,42,0.5); display: flex; align-items: center; justify-content: center; z-index: 60; padding: 1rem; }
.modal-box { background: #fff; border-radius: 1rem; width: 100%; max-width: 30rem; box-shadow: 0 24px 64px rgba(0,0,0,0.25); overflow: hidden; }
.modal-head { display: flex; align-items: center; justify-content: space-between; padding: 1rem 1.25rem; border-bottom: 1px solid #f1f5f9; font-size: 0.9375rem; font-weight: 600; color: #1e293b; }
.modal-close { background: none; border: none; cursor: pointer; color: #94a3b8; font-size: 1rem; }
.modal-close:hover { color: #ef4444; }
.modal-body { padding: 1.25rem; display: flex; flex-direction: column; gap: 0.875rem; }
.field { display: flex; flex-direction: column; gap: 0.375rem; }
.field label { font-size: 0.8125rem; font-weight: 600; color: #475569; }
.field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 0.875rem; }
.input { width: 100%; padding: 0.5rem 0.75rem; border: 1px solid #d1d5db; border-radius: 0.5rem; font-size: 0.875rem; color: #1e293b; outline: none; background: #fff; }
.input:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.12); }
.input:disabled { background: #f8fafc; color: #94a3b8; }
.alert-error { display: flex; align-items: center; gap: 0.5rem; padding: 0.625rem 0.875rem; background: #fef2f2; border: 1px solid #fecaca; border-radius: 0.5rem; color: #dc2626; font-size: 0.8125rem; }
.modal-foot { display: flex; justify-content: flex-end; gap: 0.75rem; padding: 1rem 1.25rem; border-top: 1px solid #f1f5f9; }

/* Confirm */
.confirm-card { background: #fff; border-radius: 16px; padding: 1.75rem 1.75rem 1.5rem; width: 100%; max-width: 360px; text-align: center; box-shadow: 0 24px 64px rgba(15,23,42,0.25); }
.confirm-icon { width: 3rem; height: 3rem; margin: 0 auto 0.875rem; background: #fef2f2; color: #ef4444; border-radius: 9999px; display: flex; align-items: center; justify-content: center; font-size: 1.25rem; }
.confirm-title { font-size: 1.0625rem; font-weight: 700; color: #1e293b; margin: 0 0 0.5rem; }
.confirm-msg { font-size: 0.875rem; color: #64748b; margin: 0 0 1.25rem; line-height: 1.6; }
.confirm-actions { display: flex; gap: 0.75rem; }
.confirm-cancel, .confirm-ok { flex: 1; padding: 0.5625rem; border-radius: 0.5rem; font-size: 0.875rem; font-weight: 600; cursor: pointer; border: none; display: inline-flex; align-items: center; justify-content: center; gap: 0.4rem; }
.confirm-cancel { background: #f1f5f9; color: #475569; }
.confirm-cancel:hover { background: #e2e8f0; }
.confirm-ok { background: #ef4444; color: #fff; }
.confirm-ok:hover { background: #dc2626; }

@media (max-width: 768px) { .stats-row { grid-template-columns: 1fr; } }
</style>
