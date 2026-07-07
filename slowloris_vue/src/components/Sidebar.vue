<template>
  <div class="sidebar">
    <div class="sidebar-logo">
      <div class="logo-icon">
        <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg" width="32" height="32">
          <!-- 头 -->
          <circle cx="20" cy="8" r="4" stroke="#fff" stroke-width="1.8"/>
          <!-- 身体 -->
          <path d="M14 20c0-3.314 2.686-6 6-6s6 2.686 6 6" stroke="#fff" stroke-width="1.8" stroke-linecap="round"/>
          <!-- 左臂举起持盾 -->
          <path d="M14 20l-3-4" stroke="#fff" stroke-width="1.8" stroke-linecap="round"/>
          <!-- 右臂自然下垂 -->
          <path d="M26 20l2 3" stroke="#fff" stroke-width="1.8" stroke-linecap="round"/>
          <!-- 腿 -->
          <path d="M17 26l-1 5M23 26l1 5" stroke="#fff" stroke-width="1.8" stroke-linecap="round"/>
          <!-- 盾牌 -->
          <path d="M7 14l4-2 4 2v4c0 2.5-2 4-4 5-2-1-4-2.5-4-5v-4z" stroke="#93c5fd" stroke-width="1.5" stroke-linejoin="round"/>
          <!-- 盾牌勾 -->
          <path d="M9.5 18l1.5 1.5 2.5-2.5" stroke="#93c5fd" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
      <div>
        <span class="logo-text">伺<span class="logo-highlight">"机"</span>守护</span>
        <span class="logo-sub">智能感知与决策系统</span>
      </div>
    </div>

    <nav class="sidebar-nav">
      <div class="nav-mode" :class="admin ? 'nav-mode--admin' : 'nav-mode--user'">
        <i :class="admin ? 'fa fa-user-shield' : 'fa fa-user'"></i>
        {{ admin ? '管理员端' : '用户端' }}
      </div>
      <router-link v-for="item in navItems" :key="item.to" :to="item.to" class="nav-item">
        <i :class="item.icon"></i>
        <span>{{ item.label }}</span>
      </router-link>
    </nav>

    <div class="sidebar-footer">
      <div class="user-row">
        <router-link to="/profile" class="user-link" title="个人中心">
          <div class="user-avatar">
            <img v-if="userAvatar" :src="userAvatar" alt="头像" />
            <i v-else class="fa fa-user"></i>
          </div>
          <div class="user-info">
            <p class="user-name">{{ userName }}</p>
            <p class="user-email">{{ userRole || userEmail || 'admin@example.com' }}</p>
          </div>
        </router-link>
        <button class="logout-btn" title="退出登录" @click="logout">
          <i class="fa fa-sign-out"></i>
        </button>
      </div>
    </div>
  </div>
</template>

<!-- eslint-disable vue/multi-word-component-names -->
<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../services/api.js'
import { isAdmin, getRole, roleLabel as roleLabelFn } from '../services/auth.js'

const router = useRouter()

const userName = ref('用户')
const userEmail = ref('')
const userRole = ref('')
const userAvatar = ref('')
const admin = isAdmin()

function onProfileUpdated(e) {
  if (e?.detail && 'avatar' in e.detail) userAvatar.value = e.detail.avatar || ''
}

onMounted(async () => {
  const token = localStorage.getItem('authToken')
  if (token) {
    userRole.value = roleLabelFn(getRole())
    try {
      const payload = JSON.parse(atob(token.split('.')[1]))
      userName.value = payload.username || '用户'
      userEmail.value = payload.email || ''
    } catch (e) { /* token 解析失败则保持默认值 */ }

    // 拉取头像与最新资料（token 中不含头像）
    try {
      const res = await api.get('/api/profile')
      const d = res.data || {}
      if (d.username) userName.value = d.username
      if (d.email) userEmail.value = d.email
      if (d.role) userRole.value = roleLabelFn(d.role)
      userAvatar.value = d.avatar || ''
    } catch (e) { /* 静默：资料加载失败不影响导航 */ }
  }
  window.addEventListener('profile-updated', onProfileUpdated)
})

onUnmounted(() => {
  window.removeEventListener('profile-updated', onProfileUpdated)
})

// 区分用户端/管理员端导航：自动处置与用户管理仅管理员可见
const navItems = computed(() => {
  const base = [
    { to: '/dashboard', icon: 'fa fa-tachometer', label: '监控控制台' },
    { to: '/monitor',   icon: 'fa fa-shield',     label: '攻击监控' },
  ]
  const adminOnly = [
    { to: '/auto',  icon: 'fa fa-magic',      label: '自动处置' },
  ]
  const tail = [
    { to: '/logs',  icon: 'fa fa-bell',       label: '日志与告警' },
  ]
  const userMgmt = [
    { to: '/users', icon: 'fa fa-users',      label: '用户管理' },
  ]
  return admin ? [...base, ...adminOnly, ...tail, ...userMgmt] : [...base, ...tail]
})

function logout() {
  localStorage.removeItem('authToken')
  router.push('/login')
}
</script>

<style scoped>
.sidebar {
  width: 240px;
  height: 100vh;
  background: #ffffff;
  border-right: 1px solid #e2e8f0;
  position: fixed;
  left: 0; top: 0;
  display: flex;
  flex-direction: column;
  z-index: 1000;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1.5rem 1.25rem;
  border-bottom: 1px solid #f1f5f9;
}

.logo-icon {
  width: 2rem; height: 2rem;
  background: #3b82f6;
  border-radius: 0.5rem;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
  font-size: 0.875rem;
  flex-shrink: 0;
}

.logo-text {
  font-size: 1rem;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: 0.02em;
  line-height: 1.2;
}

.logo-highlight {
  color: #3b82f6;
}

.logo-sub {
  font-size: 0.6875rem;
  color: #94a3b8;
  display: block;
  margin-top: 0.1rem;
  letter-spacing: 0.01em;
}

.sidebar-nav {
  flex: 1;
  padding: 1rem 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.625rem 0.875rem;
  border-radius: 0.5rem;
  color: #64748b;
  text-decoration: none;
  font-size: 0.875rem;
  font-weight: 500;
  transition: all 0.15s ease;
  border-left: 3px solid transparent;
}

.nav-item i { width: 1rem; text-align: center; font-size: 0.875rem; }

.nav-mode {
  display: flex; align-items: center; gap: 0.4rem;
  font-size: 0.6875rem; font-weight: 700; letter-spacing: 0.02em;
  padding: 0.35rem 0.625rem; border-radius: 0.5rem;
  margin: 0 0.125rem 0.5rem; text-transform: uppercase;
}
.nav-mode--admin { background: #ede9fe; color: #7c3aed; }
.nav-mode--user  { background: #eff6ff; color: #2563eb; }

.nav-item:hover {
  background: #f1f5f9;
  color: #1e293b;
}

.nav-item.router-link-active {
  background: #eff6ff;
  color: #2563eb;
  border-left-color: #3b82f6;
}

.sidebar-footer {
  padding: 1rem 0.75rem;
  border-top: 1px solid #f1f5f9;
}

.user-row {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.user-link {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  flex: 1;
  min-width: 0;
  text-decoration: none;
  padding: 0.25rem;
  margin: -0.25rem;
  border-radius: 0.5rem;
  transition: background 0.15s;
}
.user-link:hover { background: #f1f5f9; }

.user-avatar {
  width: 2rem; height: 2rem;
  background: #f1f5f9;
  border-radius: 9999px;
  display: flex; align-items: center; justify-content: center;
  color: #64748b;
  font-size: 0.75rem;
  flex-shrink: 0;
  overflow: hidden;
}
.user-avatar img { width: 100%; height: 100%; object-fit: cover; }

.user-info { flex: 1; min-width: 0; }
.user-name  { font-size: 0.8125rem; font-weight: 600; color: #1e293b; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.user-email { font-size: 0.6875rem; color: #94a3b8; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.logout-btn {
  background: none; border: none; cursor: pointer;
  color: #94a3b8; padding: 0.25rem;
  transition: color 0.15s;
}
.logout-btn:hover { color: #dc2626; }
</style>
