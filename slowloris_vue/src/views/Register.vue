<template>
  <div class="login-root">
    <!-- 动态背景粒子 -->
    <div class="bg-particles">
      <span v-for="i in 18" :key="i" class="particle" :style="particleStyle(i)"></span>
    </div>

    <!-- 左侧品牌区 -->
    <div class="brand-panel">
      <div class="brand-content">
        <div class="brand-logo">
          <i class="fa fa-shield"></i>
        </div>
        <h1 class="brand-title">伺"机"守护</h1>
        <p class="brand-sub">智能感知与决策系统</p>
        <div class="brand-features">
          <div class="feature-item" v-for="f in features" :key="f.icon">
            <div class="feature-icon"><i :class="'fa fa-' + f.icon"></i></div>
            <div>
              <p class="feature-name">{{ f.name }}</p>
              <p class="feature-desc">{{ f.desc }}</p>
            </div>
          </div>
        </div>
      </div>
      <div class="brand-wave">
        <svg viewBox="0 0 200 80" preserveAspectRatio="none">
          <path d="M0,40 C40,10 80,70 120,40 C160,10 180,60 200,40 L200,80 L0,80 Z" fill="rgba(255,255,255,0.06)"/>
          <path d="M0,55 C50,25 100,75 150,45 C175,30 190,55 200,50 L200,80 L0,80 Z" fill="rgba(255,255,255,0.04)"/>
        </svg>
      </div>
    </div>

    <!-- 右侧注册区 -->
    <div class="form-panel">
      <div class="form-card">
        <div class="form-header">
          <h2 class="form-title">创建账户</h2>
          <p class="form-subtitle">注册一个新账户开始使用</p>
        </div>

        <form @submit.prevent="handleRegister" class="form-body">
          <!-- 用户名 -->
          <div class="field-group" :class="{ 'field-error': errors.username, 'field-focus': focus.username }">
            <label class="field-label">用户名</label>
            <div class="field-wrap">
              <i class="fa fa-user field-icon"></i>
              <input v-model="form.username" type="text" required placeholder="3-64 个字符" class="field-input"
                @focus="focus.username = true" @blur="focus.username = false" />
            </div>
            <p v-if="errors.username" class="field-err-msg"><i class="fa fa-exclamation-circle"></i> {{ errors.username }}</p>
          </div>

          <!-- 邮箱 -->
          <div class="field-group" :class="{ 'field-error': errors.email, 'field-focus': focus.email }">
            <label class="field-label">邮箱</label>
            <div class="field-wrap">
              <i class="fa fa-envelope field-icon"></i>
              <input v-model="form.email" type="email" placeholder="you@example.com（选填）" class="field-input"
                @focus="focus.email = true" @blur="focus.email = false" />
            </div>
            <p v-if="errors.email" class="field-err-msg"><i class="fa fa-exclamation-circle"></i> {{ errors.email }}</p>
          </div>

          <!-- 密码 -->
          <div class="field-group" :class="{ 'field-error': errors.password, 'field-focus': focus.password }">
            <label class="field-label">密码</label>
            <div class="field-wrap">
              <i class="fa fa-lock field-icon"></i>
              <input v-model="form.password" :type="passwordVisible ? 'text' : 'password'" required
                placeholder="至少 6 位" class="field-input"
                @focus="focus.password = true" @blur="focus.password = false" />
              <button type="button" class="eye-btn" @click="passwordVisible = !passwordVisible">
                <i :class="'fa fa-' + (passwordVisible ? 'eye-slash' : 'eye')"></i>
              </button>
            </div>
            <p v-if="errors.password" class="field-err-msg"><i class="fa fa-exclamation-circle"></i> {{ errors.password }}</p>
          </div>

          <!-- 确认密码 -->
          <div class="field-group" :class="{ 'field-error': errors.confirmPassword, 'field-focus': focus.confirmPassword }">
            <label class="field-label">确认密码</label>
            <div class="field-wrap">
              <i class="fa fa-lock field-icon"></i>
              <input v-model="form.confirmPassword" :type="passwordVisible ? 'text' : 'password'" required
                placeholder="请再次输入密码" class="field-input"
                @focus="focus.confirmPassword = true" @blur="focus.confirmPassword = false" />
            </div>
            <p v-if="errors.confirmPassword" class="field-err-msg"><i class="fa fa-exclamation-circle"></i> {{ errors.confirmPassword }}</p>
          </div>

          <!-- 错误/成功提示 -->
          <div v-if="errors.general" class="alert-error">
            <i class="fa fa-exclamation-triangle"></i><span>{{ errors.general }}</span>
          </div>
          <div v-if="successMsg" class="alert-success">
            <i class="fa fa-check-circle"></i><span>{{ successMsg }}</span>
          </div>

          <!-- 注册按钮 -->
          <button type="submit" class="submit-btn" :disabled="isLoading">
            <span v-if="isLoading"><i class="fa fa-spinner fa-spin"></i> 注册中...</span>
            <span v-else><i class="fa fa-user-plus"></i> 注 册</span>
          </button>

          <!-- 登录引导 -->
          <p class="register-tip">
            已有账户？
            <router-link to="/login" class="register-link">返回登录</router-link>
          </p>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import api from '../services/api.js'

export default {
  name: 'UserRegister',
  data() {
    return {
      form: { username: '', email: '', password: '', confirmPassword: '' },
      errors: {},
      focus: { username: false, email: false, password: false, confirmPassword: false },
      isLoading: false,
      passwordVisible: false,
      successMsg: '',
      features: [
        { icon: 'bolt',      name: '实时检测', desc: 'Slowloris 攻击毫秒级响应' },
        { icon: 'bar-chart', name: '流量分析', desc: '多维度网络流量可视化' },
        { icon: 'bell',      name: '智能告警', desc: 'AI 驱动的异常感知与预警' },
      ]
    }
  },
  methods: {
    particleStyle(i) {
      const size = 4 + (i % 5) * 6
      const x = (i * 37 + 11) % 100
      const y = (i * 53 + 7) % 100
      const dur = 6 + (i % 4) * 3
      const delay = -(i * 1.3)
      return {
        width: size + 'px', height: size + 'px',
        left: x + '%', top: y + '%',
        animationDuration: dur + 's', animationDelay: delay + 's',
        opacity: 0.12 + (i % 3) * 0.06
      }
    },
    validate() {
      this.errors = {}
      const u = this.form.username.trim()
      if (!u) this.errors.username = '请输入用户名'
      else if (u.length < 3 || u.length > 64) this.errors.username = '用户名长度需在 3-64 个字符之间'
      if (this.form.email.trim() && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(this.form.email.trim())) {
        this.errors.email = '邮箱格式不正确'
      }
      if (!this.form.password) this.errors.password = '请输入密码'
      else if (this.form.password.length < 6) this.errors.password = '密码至少 6 位'
      if (this.form.password !== this.form.confirmPassword) this.errors.confirmPassword = '两次密码不一致'
      return Object.keys(this.errors).length === 0
    },
    async handleRegister() {
      this.successMsg = ''
      if (!this.validate()) return
      this.isLoading = true
      try {
        // 普通用户注册：角色固定为 USER，管理员账户由管理员在用户管理中创建
        const resp = await api.post('/api/register', {
          username: this.form.username.trim(),
          password: this.form.password,
          email: this.form.email.trim() || null,
          role: 'USER'
        })
        if (resp.success !== false) {
          this.successMsg = '注册成功，正在跳转登录...'
          setTimeout(() => this.$router.push('/login'), 1200)
        } else {
          this.errors.general = resp.message || '注册失败，请重试'
        }
      } catch (e) {
        this.errors.general = e?.response?.data?.message || '注册失败，请稍后重试'
      } finally {
        this.isLoading = false
      }
    }
  }
}
</script>

<style scoped>
.login-root {
  display: flex; min-height: 100vh; width: 100vw;
  background: #f0f4ff; position: relative; overflow: hidden;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
.bg-particles { position: absolute; inset: 0; pointer-events: none; z-index: 0; }
.particle { position: absolute; border-radius: 50%; background: linear-gradient(135deg, #6366f1, #3b82f6); animation: floatUp linear infinite; }
@keyframes floatUp {
  0% { transform: translateY(0) scale(1); opacity: inherit; }
  50% { transform: translateY(-30px) scale(1.15); }
  100% { transform: translateY(0) scale(1); opacity: inherit; }
}
.brand-panel {
  flex: 0 0 42%;
  background: linear-gradient(145deg, #4f46e5 0%, #2563eb 50%, #0ea5e9 100%);
  display: flex; flex-direction: column; justify-content: center;
  padding: 3rem 3.5rem; position: relative; overflow: hidden; z-index: 1;
}
.brand-content { position: relative; z-index: 2; }
.brand-logo {
  width: 72px; height: 72px; background: rgba(255,255,255,0.18);
  border: 2px solid rgba(255,255,255,0.35); border-radius: 20px;
  display: flex; align-items: center; justify-content: center;
  font-size: 2rem; color: #fff; margin-bottom: 1.5rem;
  backdrop-filter: blur(8px); box-shadow: 0 8px 32px rgba(0,0,0,0.15);
}
.brand-title { font-size: 2rem; font-weight: 800; color: #fff; letter-spacing: 0.05em; margin-bottom: 0.25rem; }
.brand-sub { font-size: 1rem; color: rgba(255,255,255,0.75); margin-bottom: 2.5rem; }
.brand-features { display: flex; flex-direction: column; gap: 1.25rem; }
.feature-item {
  display: flex; align-items: flex-start; gap: 1rem;
  background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.18);
  border-radius: 12px; padding: 0.875rem 1rem; backdrop-filter: blur(6px); transition: background 0.2s;
}
.feature-item:hover { background: rgba(255,255,255,0.16); }
.feature-icon {
  width: 36px; height: 36px; flex-shrink: 0; background: rgba(255,255,255,0.2);
  border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 0.9rem;
}
.feature-name { font-size: 0.875rem; font-weight: 600; color: #fff; }
.feature-desc { font-size: 0.75rem; color: rgba(255,255,255,0.65); margin-top: 0.1rem; }
.brand-wave { position: absolute; bottom: 0; left: 0; right: 0; height: 80px; }
.brand-wave svg { width: 100%; height: 100%; }

.form-panel { flex: 1; display: flex; align-items: center; justify-content: center; padding: 2rem; z-index: 1; }
.form-card {
  background: #fff; border-radius: 24px;
  box-shadow: 0 20px 60px rgba(79,70,229,0.1), 0 4px 16px rgba(0,0,0,0.06);
  padding: 2.5rem 2.25rem; width: 100%; max-width: 420px;
  animation: slideUp 0.5s cubic-bezier(0.22,1,0.36,1);
}
@keyframes slideUp { from { opacity: 0; transform: translateY(24px); } to { opacity: 1; transform: translateY(0); } }
.form-header { margin-bottom: 2rem; }
.form-title { font-size: 1.625rem; font-weight: 800; color: #1e1b4b; margin-bottom: 0.25rem; }
.form-subtitle { font-size: 0.875rem; color: #6b7280; }
.form-body { display: flex; flex-direction: column; gap: 1.125rem; }
.field-group { display: flex; flex-direction: column; gap: 0.375rem; }
.field-label { font-size: 0.8125rem; font-weight: 600; color: #374151; }
.field-wrap {
  display: flex; align-items: center; border: 1.5px solid #e5e7eb;
  border-radius: 10px; background: #f9fafb;
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s; overflow: hidden;
}
.field-group.field-focus .field-wrap { border-color: #6366f1; background: #fff; box-shadow: 0 0 0 3px rgba(99,102,241,0.12); }
.field-group.field-error .field-wrap { border-color: #ef4444; box-shadow: 0 0 0 3px rgba(239,68,68,0.1); }
.field-icon { padding: 0 0.875rem; color: #9ca3af; font-size: 0.875rem; flex-shrink: 0; }
.field-group.field-focus .field-icon { color: #6366f1; }
.field-input { flex: 1; border: none; outline: none; background: transparent; padding: 0.75rem 0.5rem 0.75rem 0; font-size: 0.9375rem; color: #111827; }
.field-input::placeholder { color: #d1d5db; }
.eye-btn { background: none; border: none; cursor: pointer; padding: 0 0.875rem; color: #9ca3af; font-size: 0.875rem; transition: color 0.15s; }
.eye-btn:hover { color: #6366f1; }
.field-err-msg { font-size: 0.75rem; color: #ef4444; display: flex; align-items: center; gap: 0.25rem; }
.alert-error { display: flex; align-items: center; gap: 0.5rem; padding: 0.75rem 1rem; background: #fef2f2; border: 1px solid #fecaca; border-radius: 8px; color: #dc2626; font-size: 0.8125rem; }
.alert-success { display: flex; align-items: center; gap: 0.5rem; padding: 0.75rem 1rem; background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 8px; color: #16a34a; font-size: 0.8125rem; }
.submit-btn {
  width: 100%; padding: 0.875rem;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff; font-size: 1rem; font-weight: 600; border: none; border-radius: 10px;
  cursor: pointer; letter-spacing: 0.05em; box-shadow: 0 4px 14px rgba(99,102,241,0.4);
  transition: transform 0.15s, box-shadow 0.15s, opacity 0.15s;
}
.submit-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(99,102,241,0.5); }
.submit-btn:disabled { opacity: 0.7; cursor: not-allowed; }
.register-tip { text-align: center; font-size: 0.8125rem; color: #9ca3af; }
.register-link { color: #6366f1; font-weight: 600; text-decoration: none; }
.register-link:hover { text-decoration: underline; }
@media (max-width: 768px) {
  .brand-panel { display: none; }
  .form-panel { background: linear-gradient(145deg, #4f46e5, #2563eb); }
  .form-card { box-shadow: 0 24px 64px rgba(0,0,0,0.2); }
}
</style>
