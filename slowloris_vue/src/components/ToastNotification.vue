<template>
  <teleport to="body">
    <div class="toast-container">
      <transition-group name="toast">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          class="toast"
          :class="`toast--${toast.type}`"
        >
          <i :class="iconMap[toast.type]"></i>
          <span>{{ toast.message }}</span>
          <button class="toast__close" @click="dismiss(toast.id)">
            <i class="fa fa-times"></i>
          </button>
        </div>
      </transition-group>
    </div>
  </teleport>
</template>

<script setup>
import { useToast } from '../composables/useToast'

const { toasts, dismiss } = useToast()

const iconMap = {
  success: 'fa fa-check-circle',
  error: 'fa fa-times-circle',
  warning: 'fa fa-exclamation-triangle',
  info: 'fa fa-info-circle',
}
</script>

<style scoped>
.toast-container {
  position: fixed;
  top: 1.25rem;
  right: 1.25rem;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  pointer-events: none;
}

.toast {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  min-width: 260px;
  max-width: 380px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.18);
  pointer-events: all;
  backdrop-filter: blur(8px);
}

.toast--success { background: #052e16; color: #4ade80; border: 1px solid #166534; }
.toast--error   { background: #2d0a0a; color: #f87171; border: 1px solid #7f1d1d; }
.toast--warning { background: #2d1a00; color: #fbbf24; border: 1px solid #78350f; }
.toast--info    { background: #0c1a2e; color: #60a5fa; border: 1px solid #1e3a5f; }

.toast__close {
  margin-left: auto;
  background: none;
  border: none;
  cursor: pointer;
  color: inherit;
  opacity: 0.6;
  padding: 0;
  line-height: 1;
}
.toast__close:hover { opacity: 1; }

.toast-enter-active, .toast-leave-active { transition: all 0.25s ease; }
.toast-enter-from { opacity: 0; transform: translateX(40px); }
.toast-leave-to   { opacity: 0; transform: translateX(40px); }
</style>
