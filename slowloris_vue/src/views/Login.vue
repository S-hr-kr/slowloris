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
      <!-- 卡通小人场景 -->
      <div class="cartoon-scene">
        <!-- 小人1：打字中 -->
        <div class="cartoon-char char-typer">
          <svg width="64" height="90" viewBox="0 0 64 90" fill="none">
            <!-- 头 -->
            <circle cx="32" cy="14" r="10" fill="rgba(255,255,255,0.9)" stroke="rgba(255,255,255,0.5)" stroke-width="1.5"/>
            <!-- 眼睛 -->
            <circle cx="28" cy="13" r="1.8" fill="#4f46e5"/>
            <circle cx="36" cy="13" r="1.8" fill="#4f46e5"/>
            <!-- 嘴巴（微笑） -->
            <path d="M28 18 Q32 21 36 18" stroke="#4f46e5" stroke-width="1.5" stroke-linecap="round" fill="none"/>
            <!-- 身体 -->
            <rect x="24" y="26" width="16" height="22" rx="4" fill="rgba(255,255,255,0.85)"/>
            <!-- 左臂（打字） -->
            <line x1="24" y1="30" x2="12" y2="44" stroke="rgba(255,255,255,0.85)" stroke-width="4" stroke-linecap="round" class="arm-left"/>
            <!-- 右臂（打字） -->
            <line x1="40" y1="30" x2="52" y2="44" stroke="rgba(255,255,255,0.85)" stroke-width="4" stroke-linecap="round" class="arm-right"/>
            <!-- 左腿 -->
            <line x1="28" y1="48" x2="22" y2="68" stroke="rgba(255,255,255,0.85)" stroke-width="4" stroke-linecap="round"/>
            <!-- 右腿 -->
            <line x1="36" y1="48" x2="42" y2="68" stroke="rgba(255,255,255,0.85)" stroke-width="4" stroke-linecap="round"/>
            <!-- 键盘 -->
            <rect x="6" y="66" width="52" height="14" rx="3" fill="rgba(255,255,255,0.25)" stroke="rgba(255,255,255,0.4)" stroke-width="1"/>
            <rect x="10" y="69" width="6" height="4" rx="1" fill="rgba(255,255,255,0.5)"/>
            <rect x="19" y="69" width="6" height="4" rx="1" fill="rgba(255,255,255,0.5)"/>
            <rect x="28" y="69" width="6" height="4" rx="1" fill="rgba(255,255,255,0.5)"/>
            <rect x="37" y="69" width="6" height="4" rx="1" fill="rgba(255,255,255,0.5)"/>
            <rect x="46" y="69" width="6" height="4" rx="1" fill="rgba(255,255,255,0.5)"/>
          </svg>
          <div class="typing-dots"><span></span><span></span><span></span></div>
        </div>

        <!-- 小人2：举旗巡逻 -->
        <div class="cartoon-char char-patrol">
          <svg width="56" height="96" viewBox="0 0 56 96" fill="none">
            <!-- 头 -->
            <circle cx="28" cy="13" r="10" fill="rgba(255,255,255,0.9)" stroke="rgba(255,255,255,0.5)" stroke-width="1.5"/>
            <!-- 眼睛 -->
            <circle cx="24" cy="12" r="1.8" fill="#2563eb"/>
            <circle cx="32" cy="12" r="1.8" fill="#2563eb"/>
            <!-- 嘴巴 -->
            <path d="M24 17 Q28 20 32 17" stroke="#2563eb" stroke-width="1.5" stroke-linecap="round" fill="none"/>
            <!-- 帽子 -->
            <rect x="18" y="4" width="20" height="5" rx="2" fill="rgba(255,255,255,0.7)"/>
            <rect x="22" y="0" width="12" height="6" rx="2" fill="rgba(255,255,255,0.7)"/>
            <!-- 身体 -->
            <rect x="20" y="25" width="16" height="22" rx="4" fill="rgba(255,255,255,0.85)"/>
            <!-- 举旗臂 -->
            <line x1="20" y1="28" x2="8" y2="18" stroke="rgba(255,255,255,0.85)" stroke-width="4" stroke-linecap="round"/>
            <!-- 旗杆 -->
            <line x1="8" y1="4" x2="8" y2="20" stroke="rgba(255,255,255,0.7)" stroke-width="2"/>
            <!-- 旗帜 -->
            <path d="M8 4 L22 8 L8 12 Z" fill="rgba(255,220,50,0.85)"/>
            <!-- 右臂 -->
            <line x1="36" y1="30" x2="46" y2="38" stroke="rgba(255,255,255,0.85)" stroke-width="4" stroke-linecap="round"/>
            <!-- 腿（走路） -->
            <line x1="26" y1="47" x2="18" y2="68" stroke="rgba(255,255,255,0.85)" stroke-width="4" stroke-linecap="round" class="leg-l"/>
            <line x1="30" y1="47" x2="38" y2="68" stroke="rgba(255,255,255,0.85)" stroke-width="4" stroke-linecap="round" class="leg-r"/>
          </svg>
        </div>

        <!-- 小人3：举手欢呼 -->
        <div class="cartoon-char char-cheer">
          <svg width="56" height="80" viewBox="0 0 56 80" fill="none">
            <!-- 头 -->
            <circle cx="28" cy="13" r="10" fill="rgba(255,255,255,0.9)" stroke="rgba(255,255,255,0.5)" stroke-width="1.5"/>
            <!-- 眼睛（开心眯眼） -->
            <path d="M23 11 Q25 9 27 11" stroke="#0ea5e9" stroke-width="1.8" stroke-linecap="round" fill="none"/>
            <path d="M29 11 Q31 9 33 11" stroke="#0ea5e9" stroke-width="1.8" stroke-linecap="round" fill="none"/>
            <!-- 大笑嘴 -->
            <path d="M22 17 Q28 23 34 17" stroke="#0ea5e9" stroke-width="1.8" stroke-linecap="round" fill="rgba(255,255,255,0.3)"/>
            <!-- 身体 -->
            <rect x="20" y="25" width="16" height="20" rx="4" fill="rgba(255,255,255,0.85)"/>
            <!-- 举起双臂 -->
            <line x1="20" y1="28" x2="6" y2="14" stroke="rgba(255,255,255,0.85)" stroke-width="4" stroke-linecap="round"/>
            <line x1="36" y1="28" x2="50" y2="14" stroke="rgba(255,255,255,0.85)" stroke-width="4" stroke-linecap="round"/>
            <!-- 星星 -->
            <text x="2" y="12" font-size="10" fill="rgba(255,220,50,0.9)">★</text>
            <text x="44" y="12" font-size="10" fill="rgba(255,220,50,0.9)">★</text>
            <!-- 腿 -->
            <line x1="26" y1="45" x2="20" y2="66" stroke="rgba(255,255,255,0.85)" stroke-width="4" stroke-linecap="round"/>
            <line x1="30" y1="45" x2="36" y2="66" stroke="rgba(255,255,255,0.85)" stroke-width="4" stroke-linecap="round"/>
          </svg>
        </div>
      </div>

      <div class="brand-wave">
        <svg viewBox="0 0 200 80" preserveAspectRatio="none">
          <path d="M0,40 C40,10 80,70 120,40 C160,10 180,60 200,40 L200,80 L0,80 Z" fill="rgba(255,255,255,0.06)"/>
          <path d="M0,55 C50,25 100,75 150,45 C175,30 190,55 200,50 L200,80 L0,80 Z" fill="rgba(255,255,255,0.04)"/>
        </svg>
      </div>
    </div>

    <!-- 右侧登录区 -->
    <div class="form-panel">
      <div class="form-card">
        <div class="form-header">
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-subtitle">登录您的账户以继续</p>
        </div>

        <form @submit.prevent="handleLogin" class="form-body">
          <!-- 用户名 -->
          <div class="field-group" :class="{ 'field-error': errors.username, 'field-focus': focus.username }">
            <label class="field-label">用户名</label>
            <div class="field-wrap">
              <i class="fa fa-user field-icon"></i>
              <input
                v-model="form.username"
                type="text"
                required
                placeholder="请输入用户名"
                class="field-input"
                @focus="focus.username = true"
                @blur="focus.username = false"
              />
            </div>
            <p v-if="errors.username" class="field-err-msg"><i class="fa fa-exclamation-circle"></i> {{ errors.username }}</p>
          </div>

          <!-- 密码 -->
          <div class="field-group" :class="{ 'field-error': errors.password, 'field-focus': focus.password }">
            <label class="field-label">密码</label>
            <div class="field-wrap">
              <i class="fa fa-lock field-icon"></i>
              <input
                v-model="form.password"
                :type="passwordVisible ? 'text' : 'password'"
                required
                placeholder="请输入密码"
                class="field-input"
                @focus="focus.password = true"
                @blur="focus.password = false"
              />
              <button type="button" class="eye-btn" @click="passwordVisible = !passwordVisible">
                <i :class="'fa fa-' + (passwordVisible ? 'eye-slash' : 'eye')"></i>
              </button>
            </div>
            <p v-if="errors.password" class="field-err-msg"><i class="fa fa-exclamation-circle"></i> {{ errors.password }}</p>
          </div>

          <!-- 记住我 -->
          <div class="remember-row">
            <label class="remember-label">
              <input type="checkbox" v-model="form.rememberMe" class="remember-check" />
              <span class="remember-text">记住我</span>
            </label>
            <a href="#" class="forgot-link" @click.prevent="showForgot = true">忘记密码？</a>
          </div>

          <!-- 错误提示 -->
          <div v-if="errors.general" class="alert-error">
            <i class="fa fa-exclamation-triangle"></i>
            <span>{{ errors.general }}</span>
          </div>

          <!-- 登录按钮 -->
          <button type="submit" class="submit-btn" :class="{ loading: isLoading }" :disabled="isLoading">
            <span v-if="isLoading"><i class="fa fa-spinner fa-spin"></i> 登录中...</span>
            <span v-else><i class="fa fa-sign-in"></i> 登 录</span>
          </button>

          <!-- 注册引导 -->
          <p class="register-tip">
            还没有账户？
            <router-link to="/register" class="register-link">立即注册</router-link>
          </p>

          <!-- 分隔线 -->
          <div class="divider"><span>第三方登录</span></div>

          <!-- 微信登录（暂未开放） -->
          <button type="button" class="wechat-btn" disabled @click="handleWeChatLogin" title="微信登录暂未开放">
            <span class="wechat-icon-wrap">
              <svg class="wechat-icon" viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
                <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.295.295a.296.296 0 0 0 .166-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.833.402c.268 0 .532-.012.796-.04a7.42 7.42 0 0 1-.248-1.879c0-3.775 3.578-6.832 7.993-6.832.29 0 .576.015.858.042C16.965 4.643 13.154 2.188 8.691 2.188zM5.5 7.388a.875.875 0 1 1 0 1.75.875.875 0 0 1 0-1.75zm6 0a.875.875 0 1 1 0 1.75.875.875 0 0 1 0-1.75zm3.808 3.614c-3.549 0-6.424 2.606-6.424 5.821 0 3.214 2.875 5.82 6.424 5.82.898 0 1.75-.17 2.527-.476a.694.694 0 0 1 .571.078l1.522.891a.236.236 0 0 0 .133.043.236.236 0 0 0 .236-.236c0-.058-.023-.114-.039-.17l-.312-1.184a.472.472 0 0 1 .17-.532A5.545 5.545 0 0 0 21.731 16.82c0-3.215-2.875-5.82-6.424-5.82zm-2.162 4.5a.7.7 0 1 1 0-1.4.7.7 0 0 1 0 1.4zm4.324 0a.7.7 0 1 1 0-1.4.7.7 0 0 1 0 1.4z"/>
              </svg>
            </span>
            <span class="wechat-text">微信登录</span>
            <span class="wechat-soon">暂未开放</span>
          </button>

        </form>
      </div>
    </div>

    <!-- 忘记密码弹窗 -->
    <div v-if="showForgot" class="forgot-overlay" @click.self="closeForgot">
      <div class="forgot-card">
        <div class="forgot-head">
          <h3>重置密码</h3>
          <button class="forgot-close" @click="closeForgot"><i class="fa fa-times"></i></button>
        </div>
        <div class="forgot-body">
          <div class="field-group" :class="{ 'field-focus': forgotFocus.username }">
            <label class="field-label">用户名</label>
            <div class="field-wrap">
              <i class="fa fa-user field-icon"></i>
              <input v-model="forgotForm.username" type="text" placeholder="请输入用户名" class="field-input"
                @focus="forgotFocus.username = true" @blur="forgotFocus.username = false" />
            </div>
          </div>
          <div class="field-group" :class="{ 'field-focus': forgotFocus.newPassword }">
            <label class="field-label">新密码</label>
            <div class="field-wrap">
              <i class="fa fa-lock field-icon"></i>
              <input v-model="forgotForm.newPassword" :type="forgotPwdVisible ? 'text' : 'password'"
                placeholder="请输入新密码（至少6位）" class="field-input"
                @focus="forgotFocus.newPassword = true" @blur="forgotFocus.newPassword = false" />
              <button type="button" class="eye-btn" @click="forgotPwdVisible = !forgotPwdVisible">
                <i :class="'fa fa-' + (forgotPwdVisible ? 'eye-slash' : 'eye')"></i>
              </button>
            </div>
          </div>
          <div class="field-group" :class="{ 'field-focus': forgotFocus.confirmPassword }">
            <label class="field-label">确认新密码</label>
            <div class="field-wrap">
              <i class="fa fa-lock field-icon"></i>
              <input v-model="forgotForm.confirmPassword" :type="forgotPwdVisible ? 'text' : 'password'"
                placeholder="请再次输入新密码" class="field-input"
                @focus="forgotFocus.confirmPassword = true" @blur="forgotFocus.confirmPassword = false" />
            </div>
          </div>
          <div v-if="forgotError" class="alert-error" style="margin-top:0.25rem">
            <i class="fa fa-exclamation-triangle"></i><span>{{ forgotError }}</span>
          </div>
          <div v-if="forgotSuccess" class="alert-success">
            <i class="fa fa-check-circle"></i><span>{{ forgotSuccess }}</span>
          </div>
        </div>
        <div class="forgot-foot">
          <button class="forgot-cancel" @click="closeForgot">取消</button>
          <button class="forgot-submit" :disabled="forgotLoading" @click="handleResetPassword">
            <span v-if="forgotLoading"><i class="fa fa-spinner fa-spin"></i> 提交中...</span>
            <span v-else>确认重置</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import api from '../services/api.js'

export default {
  name: 'UserLogin',
  data() {
    return {
      form: { username: '', password: '', rememberMe: false },
      errors: {},
      focus: { username: false, password: false },
      isLoading: false,
      wechatLoading: false,
      passwordVisible: false,
      showForgot: false,
      forgotForm: { username: '', newPassword: '', confirmPassword: '' },
      forgotFocus: { username: false, newPassword: false, confirmPassword: false },
      forgotPwdVisible: false,
      forgotLoading: false,
      forgotError: '',
      forgotSuccess: '',
      features: [
        { icon: 'bolt',         name: '实时检测',   desc: 'Slowloris 攻击毫秒级响应' },
        { icon: 'bar-chart',    name: '流量分析',   desc: '多维度网络流量可视化' },
        { icon: 'bell',         name: '智能告警',   desc: 'AI 驱动的异常感知与预警' },
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
        animationDuration: dur + 's',
        animationDelay: delay + 's',
        opacity: 0.12 + (i % 3) * 0.06
      }
    },
    validateForm() {
      this.errors = {}
      if (!this.form.username.trim()) this.errors.username = '请输入用户名'
      if (!this.form.password) this.errors.password = '请输入密码'
      return !this.errors.username && !this.errors.password
    },
    async handleLogin() {
      if (!this.validateForm()) return
      this.isLoading = true
      try {
        const resp = await api.post('/api/login', {
          username: this.form.username,
          password: this.form.password
        })
        const token = resp.data?.token || resp.data
        if (token) {
          localStorage.setItem('authToken', token)
          if (this.form.rememberMe) localStorage.setItem('rememberedUser', this.form.username)
          this.$router.push('/')
        } else {
          this.errors.general = resp.message || '用户名或密码错误，请重试'
        }
      } catch (e) {
        this.errors.general = e?.response?.data?.message || '登录失败，请稍后重试'
      } finally {
        this.isLoading = false
      }
    },
    async handleWeChatLogin() {
      // 微信登录服务暂未开放
      this.errors.general = '微信登录暂未开放，请使用账号密码登录'
    },
    closeForgot() {
      this.showForgot = false
      this.forgotForm = { username: '', newPassword: '', confirmPassword: '' }
      this.forgotError = ''
      this.forgotSuccess = ''
    },
    async handleResetPassword() {
      this.forgotError = ''
      this.forgotSuccess = ''
      if (!this.forgotForm.username.trim()) { this.forgotError = '请输入用户名'; return }
      if (!this.forgotForm.newPassword) { this.forgotError = '请输入新密码'; return }
      if (this.forgotForm.newPassword.length < 6) { this.forgotError = '密码至少6位'; return }
      if (this.forgotForm.newPassword !== this.forgotForm.confirmPassword) { this.forgotError = '两次密码不一致'; return }
      this.forgotLoading = true
      try {
        const response = await api.post('/api/reset-password', {
          username: this.forgotForm.username,
          newPassword: this.forgotForm.newPassword
        })
        if (response.success) {
          this.forgotSuccess = '密码重置成功，请重新登录'
          setTimeout(() => this.closeForgot(), 1500)
        } else {
          this.forgotError = response.message || '重置失败，请检查用户名'
        }
      } catch {
        this.forgotError = '重置失败，请稍后重试'
      } finally {
        this.forgotLoading = false
      }
    }
  },
  mounted() {
    // 处理微信OAuth回调携带的token
    const urlParams = new URLSearchParams(window.location.search)
    const wechatToken = urlParams.get('wechat_token')
    if (wechatToken) {
      localStorage.setItem('authToken', wechatToken)
      this.$router.replace('/dashboard')
      return
    }
    const wechatError = urlParams.get('error')
    if (wechatError === 'wechat_failed') {
      this.errors.general = '微信登录失败，请重试或使用账号密码登录'
    }
    const u = localStorage.getItem('rememberedUser')
    if (u) { this.form.username = u; this.form.rememberMe = true }
  }
}
</script>

<style scoped>
/* ── 根布局 ── */
.login-root {
  display: flex;
  min-height: 100vh;
  width: 100vw;
  background: #f0f4ff;
  position: relative;
  overflow: hidden;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ── 粒子背景 ── */
.bg-particles { position: absolute; inset: 0; pointer-events: none; z-index: 0; }
.particle {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #3b82f6);
  animation: floatUp linear infinite;
}
@keyframes floatUp {
  0%   { transform: translateY(0) scale(1); opacity: inherit; }
  50%  { transform: translateY(-30px) scale(1.15); }
  100% { transform: translateY(0) scale(1); opacity: inherit; }
}

/* ── 左侧品牌面板 ── */
.brand-panel {
  flex: 0 0 42%;
  background: linear-gradient(145deg, #4f46e5 0%, #2563eb 50%, #0ea5e9 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 3rem 3.5rem;
  position: relative;
  overflow: hidden;
  z-index: 1;
}
.brand-content { position: relative; z-index: 2; }
.brand-logo {
  width: 72px; height: 72px;
  background: rgba(255,255,255,0.18);
  border: 2px solid rgba(255,255,255,0.35);
  border-radius: 20px;
  display: flex; align-items: center; justify-content: center;
  font-size: 2rem; color: #fff;
  margin-bottom: 1.5rem;
  backdrop-filter: blur(8px);
  box-shadow: 0 8px 32px rgba(0,0,0,0.15);
}
.brand-title {
  font-size: 2rem; font-weight: 800; color: #fff;
  letter-spacing: 0.05em; margin-bottom: 0.25rem;
}
.brand-sub {
  font-size: 1rem; color: rgba(255,255,255,0.75);
  margin-bottom: 2.5rem;
}
.brand-features { display: flex; flex-direction: column; gap: 1.25rem; }
.feature-item {
  display: flex; align-items: flex-start; gap: 1rem;
  background: rgba(255,255,255,0.1);
  border: 1px solid rgba(255,255,255,0.18);
  border-radius: 12px;
  padding: 0.875rem 1rem;
  backdrop-filter: blur(6px);
  transition: background 0.2s;
}
.feature-item:hover { background: rgba(255,255,255,0.16); }
.feature-icon {
  width: 36px; height: 36px; flex-shrink: 0;
  background: rgba(255,255,255,0.2);
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 0.9rem;
}
.feature-name { font-size: 0.875rem; font-weight: 600; color: #fff; }
.feature-desc { font-size: 0.75rem; color: rgba(255,255,255,0.65); margin-top: 0.1rem; }

.brand-wave {
  position: absolute; bottom: 0; left: 0; right: 0; height: 80px;
}
.brand-wave svg { width: 100%; height: 100%; }

/* ── 右侧表单面板 ── */
.form-panel {
  flex: 1;
  display: flex; align-items: center; justify-content: center;
  padding: 2rem;
  z-index: 1;
}
.form-card {
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(79,70,229,0.1), 0 4px 16px rgba(0,0,0,0.06);
  padding: 2.5rem 2.25rem;
  width: 100%; max-width: 420px;
  animation: slideUp 0.5s cubic-bezier(0.22,1,0.36,1);
}
@keyframes slideUp {
  from { opacity: 0; transform: translateY(24px); }
  to   { opacity: 1; transform: translateY(0); }
}

.form-header { margin-bottom: 2rem; }
.form-title { font-size: 1.625rem; font-weight: 800; color: #1e1b4b; margin-bottom: 0.25rem; }
.form-subtitle { font-size: 0.875rem; color: #6b7280; }

/* ── 表单字段 ── */
.form-body { display: flex; flex-direction: column; gap: 1.25rem; }
.field-group { display: flex; flex-direction: column; gap: 0.375rem; }
.field-label { font-size: 0.8125rem; font-weight: 600; color: #374151; }
.field-wrap {
  display: flex; align-items: center;
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  background: #f9fafb;
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
  overflow: hidden;
}
.field-group.field-focus .field-wrap {
  border-color: #6366f1;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(99,102,241,0.12);
}
.field-group.field-error .field-wrap {
  border-color: #ef4444;
  box-shadow: 0 0 0 3px rgba(239,68,68,0.1);
}
.field-icon { padding: 0 0.875rem; color: #9ca3af; font-size: 0.875rem; flex-shrink: 0; }
.field-group.field-focus .field-icon { color: #6366f1; }
.field-input {
  flex: 1; border: none; outline: none; background: transparent;
  padding: 0.75rem 0.5rem 0.75rem 0;
  font-size: 0.9375rem; color: #111827;
}
.field-input::placeholder { color: #d1d5db; }
.eye-btn {
  background: none; border: none; cursor: pointer;
  padding: 0 0.875rem; color: #9ca3af;
  font-size: 0.875rem; transition: color 0.15s;
}
.eye-btn:hover { color: #6366f1; }
.field-err-msg { font-size: 0.75rem; color: #ef4444; display: flex; align-items: center; gap: 0.25rem; }

/* ── 记住我行 ── */
.remember-row { display: flex; align-items: center; justify-content: space-between; }
.remember-label { display: flex; align-items: center; gap: 0.5rem; cursor: pointer; }
.remember-check { accent-color: #6366f1; width: 15px; height: 15px; cursor: pointer; }
.remember-text { font-size: 0.8125rem; color: #6b7280; }
.forgot-link { font-size: 0.8125rem; color: #6366f1; text-decoration: none; font-weight: 500; }
.forgot-link:hover { text-decoration: underline; }

/* ── 错误提示 ── */
.alert-error {
  display: flex; align-items: center; gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: #fef2f2; border: 1px solid #fecaca;
  border-radius: 8px; color: #dc2626; font-size: 0.8125rem;
}

/* ── 提交按钮 ── */
.submit-btn {
  width: 100%; padding: 0.875rem;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff; font-size: 1rem; font-weight: 600;
  border: none; border-radius: 10px; cursor: pointer;
  letter-spacing: 0.05em;
  box-shadow: 0 4px 14px rgba(99,102,241,0.4);
  transition: transform 0.15s, box-shadow 0.15s, opacity 0.15s;
  position: relative; overflow: hidden;
}
.submit-btn::after {
  content: '';
  position: absolute; inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.15), transparent);
  opacity: 0; transition: opacity 0.2s;
}
.submit-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(99,102,241,0.5); }
.submit-btn:hover:not(:disabled)::after { opacity: 1; }
.submit-btn:active:not(:disabled) { transform: translateY(0); }
.submit-btn:disabled { opacity: 0.7; cursor: not-allowed; }

/* ── 注册提示 ── */
.register-tip { text-align: center; font-size: 0.8125rem; color: #9ca3af; }
.register-link { color: #6366f1; font-weight: 600; text-decoration: none; }
.register-link:hover { text-decoration: underline; }

/* ── 分隔线 ── */
.divider {
  display: flex; align-items: center; gap: 0.75rem;
  color: #cbd5e1; font-size: 0.75rem; margin: 0.25rem 0;
}
.divider::before, .divider::after {
  content: ''; flex: 1; height: 1px; background: #e5e7eb;
}

/* ── 微信登录按钮（暂未开放） ── */
.wechat-btn {
  width: 100%; padding: 0.75rem;
  display: flex; align-items: center; justify-content: center; gap: 0.5rem;
  background: #f9fafb; color: #6b7280;
  border: 1.5px solid #e5e7eb; border-radius: 10px;
  font-size: 0.9375rem; font-weight: 500; cursor: not-allowed;
  transition: background 0.15s, border-color 0.15s;
}
.wechat-icon-wrap {
  display: inline-flex; align-items: center; justify-content: center;
  color: #07c160;
}
.wechat-text { color: #4b5563; }
.wechat-soon {
  font-size: 0.6875rem; font-weight: 600;
  color: #9ca3af; background: #f1f5f9;
  padding: 0.1rem 0.45rem; border-radius: 9999px; margin-left: 0.15rem;
}
.wechat-btn:disabled { opacity: 0.85; }

/* ── 卡通小人场景 ── */
.cartoon-scene {
  position: relative;
  z-index: 2;
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
  margin-top: 2rem;
  padding: 0 0.5rem;
  height: 110px;
}

.cartoon-char {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

/* 打字小人：上下轻弹 + 手臂交替 */
.char-typer {
  animation: charBounce 1.2s ease-in-out infinite;
}
.char-typer .arm-left {
  transform-origin: 24px 30px;
  animation: typeArmL 0.4s ease-in-out infinite alternate;
}
.char-typer .arm-right {
  transform-origin: 40px 30px;
  animation: typeArmR 0.4s ease-in-out infinite alternate;
}
@keyframes typeArmL {
  from { transform: rotate(0deg); }
  to   { transform: rotate(-15deg); }
}
@keyframes typeArmR {
  from { transform: rotate(0deg); }
  to   { transform: rotate(15deg); }
}

/* 打字省略号 */
.typing-dots {
  display: flex;
  gap: 3px;
}
.typing-dots span {
  width: 5px; height: 5px;
  border-radius: 50%;
  background: rgba(255,255,255,0.8);
  animation: dotBlink 1.2s ease-in-out infinite;
}
.typing-dots span:nth-child(2) { animation-delay: 0.2s; }
.typing-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes dotBlink {
  0%,80%,100% { opacity: 0.2; transform: scale(0.8); }
  40%         { opacity: 1;   transform: scale(1.2); }
}

/* 巡逻小人：左右走动 */
.char-patrol {
  animation: patrolWalk 3s ease-in-out infinite;
}
.char-patrol .leg-l {
  transform-origin: 26px 47px;
  animation: walkLegL 0.6s ease-in-out infinite alternate;
}
.char-patrol .leg-r {
  transform-origin: 30px 47px;
  animation: walkLegR 0.6s ease-in-out infinite alternate;
}
@keyframes patrolWalk {
  0%,100% { transform: translateX(0); }
  50%     { transform: translateX(12px); }
}
@keyframes walkLegL {
  from { transform: rotate(-18deg); }
  to   { transform: rotate(18deg); }
}
@keyframes walkLegR {
  from { transform: rotate(18deg); }
  to   { transform: rotate(-18deg); }
}

/* 欢呼小人：跳跃 + 手臂摆动 */
.char-cheer {
  animation: cheerJump 1.6s cubic-bezier(0.36,0.07,0.19,0.97) infinite;
}
@keyframes cheerJump {
  0%,100% { transform: translateY(0); }
  20%     { transform: translateY(-18px); }
  40%     { transform: translateY(-8px); }
  60%     { transform: translateY(-14px); }
  80%     { transform: translateY(-4px); }
}

/* 通用上下浮动 */
@keyframes charBounce {
  0%,100% { transform: translateY(0); }
  50%     { transform: translateY(-6px); }
}

/* ── 响应式 ── */
@media (max-width: 768px) {
  .brand-panel { display: none; }
  .form-panel { background: linear-gradient(145deg, #4f46e5, #2563eb); }
  .form-card { box-shadow: 0 24px 64px rgba(0,0,0,0.2); }
}

/* ── 忘记密码弹窗 ── */
.forgot-overlay {
  position: fixed; inset: 0; z-index: 999;
  background: rgba(0,0,0,0.45);
  display: flex; align-items: center; justify-content: center;
}
.forgot-card {
  background: #fff; border-radius: 20px;
  width: 100%; max-width: 400px;
  box-shadow: 0 24px 64px rgba(79,70,229,0.18);
  animation: slideUp 0.3s cubic-bezier(0.22,1,0.36,1);
  overflow: hidden;
}
.forgot-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #f1f5f9;
}
.forgot-head h3 { font-size: 1.125rem; font-weight: 700; color: #1e1b4b; margin: 0; }
.forgot-close {
  background: none; border: none; cursor: pointer;
  color: #9ca3af; font-size: 1rem; padding: 0.25rem;
  transition: color 0.15s;
}
.forgot-close:hover { color: #ef4444; }
.forgot-body {
  padding: 1.5rem;
  display: flex; flex-direction: column; gap: 1rem;
}
.forgot-foot {
  display: flex; justify-content: flex-end; gap: 0.75rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid #f1f5f9;
}
.forgot-cancel {
  padding: 0.5rem 1.25rem; border-radius: 8px;
  background: #f1f5f9; color: #475569;
  border: none; cursor: pointer; font-size: 0.875rem; font-weight: 500;
  transition: background 0.15s;
}
.forgot-cancel:hover { background: #e2e8f0; }
.forgot-submit {
  padding: 0.5rem 1.5rem; border-radius: 8px;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff; border: none; cursor: pointer;
  font-size: 0.875rem; font-weight: 600;
  box-shadow: 0 2px 8px rgba(99,102,241,0.35);
  transition: all 0.15s;
}
.forgot-submit:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(99,102,241,0.45); }
.forgot-submit:disabled { opacity: 0.6; cursor: not-allowed; }
.alert-success {
  display: flex; align-items: center; gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: #f0fdf4; border: 1px solid #bbf7d0;
  border-radius: 8px; color: #16a34a; font-size: 0.8125rem;
}
</style>
