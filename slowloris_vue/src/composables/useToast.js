import { reactive } from 'vue'

const toasts = reactive([])

let idCounter = 0

function showToast(type, message, duration = 3000) {
  const id = ++idCounter
  toasts.push({ id, type, message })
  setTimeout(() => dismiss(id), duration)
}

function dismiss(id) {
  const index = toasts.findIndex(t => t.id === id)
  if (index !== -1) toasts.splice(index, 1)
}

export function useToast() {
  return {
    toasts,
    showToast,
    dismiss,
    success: (msg) => showToast('success', msg),
    error: (msg) => showToast('error', msg),
    warning: (msg) => showToast('warning', msg),
    info: (msg) => showToast('info', msg),
  }
}
