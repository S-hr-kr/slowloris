<template>
  <div class="page">
    <!-- 页头 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon"><i class="fa fa-user-circle"></i></div>
        <div>
          <h1>个人中心</h1>
          <p>管理你的登录账号、密码、邮箱与头像</p>
        </div>
      </div>
    </div>

    <div class="profile-grid">
      <!-- 左：头像卡片 -->
      <div class="card avatar-card">
        <div class="avatar-preview">
          <img v-if="form.avatar" :src="form.avatar" alt="头像" />
          <div v-else class="avatar-placeholder"><i class="fa fa-user"></i></div>
        </div>
        <p class="avatar-name">{{ form.username || '用户' }}</p>
        <p class="avatar-role">{{ roleLabel }}</p>

        <input ref="fileInput" type="file" accept="image/*" class="hidden-input" @change="onFileChange" />
        <button class="btn btn-secondary" @click="$refs.fileInput.click()">
          <i class="fa fa-upload"></i> 上传头像
        </button>
        <button v-if="form.avatar" class="btn-text" @click="removeAvatar">
          <i class="fa fa-trash"></i> 移除头像
        </button>
        <p class="avatar-hint">支持 JPG/PNG，自动压缩至 256px</p>
      </div>

      <!-- 右：资料表单 -->
      <div class="card form-card">
        <div v-if="loading" class="loading-state">
          <i class="fa fa-spinner fa-spin"></i> 加载中...
        </div>

        <template v-else>
          <!-- 账号信息 -->
          <div class="form-section">
            <h3 class="section-title"><i class="fa fa-id-card"></i> 账号信息</h3>

            <div class="field">
              <label>登录账号</label>
              <input v-model="form.username" type="text" class="input" placeholder="登录用户名" maxlength="64" />
              <span class="field-tip">修改登录账号后需要重新登录</span>
            </div>

            <div class="field">
              <label>邮箱地址</label>
              <input v-model="form.email" type="email" class="input" placeholder="you@example.com" />
            </div>
          </div>

          <!-- 修改密码 -->
          <div class="form-section">
            <h3 class="section-title"><i class="fa fa-lock"></i> 修改密码</h3>
            <p class="section-desc">不修改密码请留空</p>

            <div class="field">
              <label>原密码</label>
              <div class="pwd-wrap">
                <input v-model="form.oldPassword" :type="showPwd ? 'text' : 'password'"
                       class="input" placeholder="请输入当前密码" autocomplete="current-password" />
                <button type="button" class="eye" @click="showPwd = !showPwd">
                  <i :class="'fa fa-' + (showPwd ? 'eye-slash' : 'eye')"></i>
                </button>
              </div>
            </div>

            <div class="field">
              <label>新密码</label>
              <input v-model="form.newPassword" :type="showPwd ? 'text' : 'password'"
                     class="input" placeholder="至少 6 位，留空表示不修改" autocomplete="new-password" />
            </div>

            <div class="field">
              <label>确认新密码</label>
              <input v-model="form.confirmPassword" :type="showPwd ? 'text' : 'password'"
                     class="input" placeholder="再次输入新密码" autocomplete="new-password" />
            </div>
          </div>

          <!-- 操作栏 -->
          <div class="form-actions">
            <button class="btn btn-secondary" @click="resetForm" :disabled="saving">
              <i class="fa fa-undo"></i> 重置
            </button>
            <button class="btn btn-primary" @click="save" :disabled="saving">
              <span v-if="saving"><i class="fa fa-spinner fa-spin"></i> 保存中...</span>
              <span v-else><i class="fa fa-save"></i> 保存修改</span>
            </button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<!-- eslint-disable vue/multi-word-component-names -->
<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../services/api.js'
import { useToast } from '../composables/useToast.js'

const router = useRouter()
const toast = useToast()

const loading = ref(true)
const saving = ref(false)
const showPwd = ref(false)
const fileInput = ref(null)

const form = reactive({
  username: '', email: '', avatar: '',
  oldPassword: '', newPassword: '', confirmPassword: ''
})
const original = reactive({ username: '', email: '', avatar: '', role: 'user' })

const roleLabel = computed(() => {
  const r = (original.role || '').toLowerCase().replace('role_', '')
  return r === 'admin' ? '管理员' : r === 'operator' ? '操作员' : '查看员'
})

onMounted(loadProfile)

async function loadProfile() {
  loading.value = true
  try {
    const res = await api.get('/api/profile')
    const d = res.data || {}
    form.username = d.username || ''
    form.email = d.email || ''
    form.avatar = d.avatar || ''
    original.username = d.username || ''
    original.email = d.email || ''
    original.avatar = d.avatar || ''
    original.role = d.role || 'user'
  } catch (e) {
    toast.error(e?.response?.data?.message || '加载个人资料失败')
  } finally {
    loading.value = false
  }
}

function onFileChange(e) {
  const file = e.target.files && e.target.files[0]
  if (!file) return
  if (!file.type.startsWith('image/')) { toast.error('请选择图片文件'); return }
  if (file.size > 5 * 1024 * 1024) { toast.error('图片不能超过 5MB'); return }
  resizeImage(file, 256, (dataUrl) => { form.avatar = dataUrl })
  e.target.value = '' // 允许重复选择同一文件
}

// 客户端压缩：等比缩放到边长 <= max，输出 JPEG Data URL
function resizeImage(file, max, cb) {
  const reader = new FileReader()
  reader.onload = (ev) => {
    const img = new Image()
    img.onload = () => {
      let { width, height } = img
      if (width > height && width > max) { height = Math.round(height * max / width); width = max }
      else if (height > max) { width = Math.round(width * max / height); height = max }
      const canvas = document.createElement('canvas')
      canvas.width = width; canvas.height = height
      canvas.getContext('2d').drawImage(img, 0, 0, width, height)
      cb(canvas.toDataURL('image/jpeg', 0.85))
    }
    img.onerror = () => toast.error('图片解析失败')
    img.src = ev.target.result
  }
  reader.onerror = () => toast.error('文件读取失败')
  reader.readAsDataURL(file)
}

function removeAvatar() { form.avatar = '' }

function resetForm() {
  form.username = original.username
  form.email = original.email
  form.avatar = original.avatar
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
}

function buildPayload() {
  const payload = {}
  if (form.username.trim() !== original.username) payload.username = form.username.trim()
  if (form.email.trim() !== (original.email || '')) payload.email = form.email.trim()
  if (form.avatar !== original.avatar) payload.avatar = form.avatar
  if (form.newPassword) {
    payload.oldPassword = form.oldPassword
    payload.newPassword = form.newPassword
  }
  return payload
}

async function save() {
  // 前端校验
  if (form.username.trim() && form.username.trim().length < 3) {
    toast.error('登录账号至少 3 个字符'); return
  }
  if (form.email.trim() && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.email.trim())) {
    toast.error('邮箱格式不正确'); return
  }
  if (form.newPassword) {
    if (!form.oldPassword) { toast.error('请输入原密码'); return }
    if (form.newPassword.length < 6) { toast.error('新密码至少 6 位'); return }
    if (form.newPassword !== form.confirmPassword) { toast.error('两次输入的新密码不一致'); return }
  }

  const payload = buildPayload()
  if (Object.keys(payload).length === 0) { toast.info('没有需要保存的修改'); return }

  saving.value = true
  try {
    const res = await api.put('/api/profile', payload)
    const requireRelogin = res.data?.requireRelogin
    toast.success(res.message || '资料已更新')
    if (requireRelogin) {
      // 用户名/密码变更使旧 token 失效，登出后重新登录
      setTimeout(() => {
        localStorage.removeItem('authToken')
        router.push('/login')
      }, 1200)
    } else {
      original.username = form.username.trim() || original.username
      original.email = form.email.trim()
      original.avatar = form.avatar
      form.oldPassword = form.newPassword = form.confirmPassword = ''
      window.dispatchEvent(new CustomEvent('profile-updated', { detail: { avatar: form.avatar } }))
    }
  } catch (e) {
    toast.error(e?.response?.data?.message || '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.page { max-width: 960px; }

.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.5rem; }
.header-left { display: flex; align-items: center; gap: 0.875rem; }
.header-icon {
  width: 2.75rem; height: 2.75rem; border-radius: 0.75rem;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 1.25rem;
}
.page-header h1 { font-size: 1.5rem; font-weight: 700; color: #1e293b; }
.page-header p { font-size: 0.875rem; color: #64748b; margin-top: 0.15rem; }

.profile-grid {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 1.25rem;
  align-items: start;
}

/* ── 头像卡 ── */
.avatar-card {
  padding: 1.75rem 1.25rem;
  display: flex; flex-direction: column; align-items: center; gap: 0.5rem;
  text-align: center;
}
.avatar-preview {
  width: 120px; height: 120px; border-radius: 9999px;
  overflow: hidden; background: #f1f5f9;
  display: flex; align-items: center; justify-content: center;
  border: 3px solid #e2e8f0;
  margin-bottom: 0.5rem;
}
.avatar-preview img { width: 100%; height: 100%; object-fit: cover; }
.avatar-placeholder { color: #cbd5e1; font-size: 3rem; }
.avatar-name { font-size: 1rem; font-weight: 700; color: #1e293b; }
.avatar-role { font-size: 0.75rem; color: #64748b; margin-bottom: 0.75rem; }
.hidden-input { display: none; }
.avatar-card .btn { width: 100%; justify-content: center; margin-top: 0.25rem; }
.btn-text {
  background: none; border: none; cursor: pointer;
  color: #94a3b8; font-size: 0.75rem; padding: 0.35rem;
  transition: color 0.15s;
}
.btn-text:hover { color: #dc2626; }
.avatar-hint { font-size: 0.6875rem; color: #94a3b8; margin-top: 0.5rem; }

/* ── 表单卡 ── */
.form-card { padding: 1.5rem 1.75rem; }
.loading-state { text-align: center; color: #64748b; padding: 3rem 0; font-size: 0.9rem; }

.form-section { padding-bottom: 1.25rem; margin-bottom: 1.25rem; border-bottom: 1px solid #f1f5f9; }
.form-section:last-of-type { border-bottom: none; margin-bottom: 0; }
.section-title {
  font-size: 0.9375rem; font-weight: 700; color: #1e293b;
  display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.25rem;
}
.section-title i { color: #6366f1; }
.section-desc { font-size: 0.75rem; color: #94a3b8; margin-bottom: 0.875rem; }

.field { margin-bottom: 1rem; }
.field:last-child { margin-bottom: 0; }
.field label { display: block; font-size: 0.8125rem; font-weight: 600; color: #475569; margin-bottom: 0.375rem; }
.field-tip { display: block; font-size: 0.6875rem; color: #94a3b8; margin-top: 0.3rem; }

.pwd-wrap { position: relative; display: flex; align-items: center; }
.pwd-wrap .input { padding-right: 2.5rem; }
.eye {
  position: absolute; right: 0.5rem;
  background: none; border: none; cursor: pointer;
  color: #94a3b8; font-size: 0.875rem; padding: 0.25rem;
}
.eye:hover { color: #6366f1; }

.form-actions {
  display: flex; justify-content: flex-end; gap: 0.75rem;
  margin-top: 1.5rem; padding-top: 1.25rem; border-top: 1px solid #f1f5f9;
}

@media (max-width: 768px) {
  .profile-grid { grid-template-columns: 1fr; }
}
</style>
