<template>
  <div class="app-container">
    <Sidebar v-if="!$route.meta.hideSidebar" />
    <main class="main-content" :class="{ 'sidebar-closed': $route.meta.hideSidebar }">
      <router-view />
    </main>
    <ToastNotification />
  </div>
</template>

<script>
import AppSidebar from './components/Sidebar.vue'
import ToastNotification from './components/ToastNotification.vue'

export default {
  name: 'App',
  components: {
    Sidebar: AppSidebar,
    ToastNotification
  }
}
</script>

<style>
:root {
  --bg-light: #f1f5f9;
  --bg-white: #ffffff;
  --border-color: #e2e8f0;
  --border-radius: 0.75rem;
  --shadow-sm: 0 1px 3px rgba(0,0,0,0.07);
  --shadow-md: 0 4px 16px rgba(0,0,0,0.10);
  --text-primary: #1e293b;
  --text-secondary: #64748b;
  --accent: #3b82f6;
  --transition: all 0.2s ease;
}

* { margin: 0; padding: 0; box-sizing: border-box; }

body {
  font-family: 'Inter', 'Segoe UI', sans-serif;
  background-color: var(--bg-light);
  color: var(--text-primary);
  line-height: 1.6;
}

.app-container { display: flex; min-height: 100vh; }

.main-content {
  flex: 1;
  margin-left: 240px;
  padding: 2rem;
  transition: margin-left 0.3s ease;
  min-height: 100vh;
  background: var(--bg-light);
}

.main-content.sidebar-closed { margin-left: 0; }

/* Global card */
.card {
  background: var(--bg-white);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-sm);
  transition: var(--transition);
}
.card:hover { box-shadow: var(--shadow-md); }

/* Global input */
.input {
  width: 100%;
  padding: 0.5rem 0.75rem;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 0.5rem;
  color: var(--text-primary);
  font-size: 0.875rem;
  outline: none;
  transition: var(--transition);
}
.input:focus { border-color: var(--accent); box-shadow: 0 0 0 3px rgba(59,130,246,0.12); }
.input option { background: #fff; color: #1e293b; }

/* Global buttons */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: var(--transition);
  letter-spacing: 0.01em;
}
.btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* 主色 - 蓝色渐变 */
.btn-primary {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  box-shadow: 0 2px 8px rgba(59,130,246,0.3);
}
.btn-primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  box-shadow: 0 4px 12px rgba(59,130,246,0.4);
  transform: translateY(-1px);
}

/* 次要 - 白色描边 */
.btn-secondary {
  background: #fff;
  color: #475569;
  border: 1px solid #d1d5db;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.btn-secondary:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #94a3b8;
  color: #1e293b;
}

/* 危险 - 红色渐变 */
.btn-danger {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
  box-shadow: 0 2px 8px rgba(239,68,68,0.3);
}
.btn-danger:hover:not(:disabled) {
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  box-shadow: 0 4px 12px rgba(239,68,68,0.4);
  transform: translateY(-1px);
}

/* 成功 - 绿色渐变 */
.btn-success {
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #fff;
  box-shadow: 0 2px 8px rgba(34,197,94,0.3);
}
.btn-success:hover:not(:disabled) {
  background: linear-gradient(135deg, #16a34a, #15803d);
  box-shadow: 0 4px 12px rgba(34,197,94,0.4);
  transform: translateY(-1px);
}

/* 警告 - 橙黄渐变 */
.btn-warning {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #fff;
  box-shadow: 0 2px 8px rgba(245,158,11,0.3);
}
.btn-warning:hover:not(:disabled) {
  background: linear-gradient(135deg, #d97706, #b45309);
  box-shadow: 0 4px 12px rgba(245,158,11,0.4);
  transform: translateY(-1px);
}

/* 紫色 - 用于AI/模型相关操作 */
.btn-purple {
  background: linear-gradient(135deg, #8b5cf6, #7c3aed);
  color: #fff;
  box-shadow: 0 2px 8px rgba(139,92,246,0.3);
}
.btn-purple:hover:not(:disabled) {
  background: linear-gradient(135deg, #7c3aed, #6d28d9);
  box-shadow: 0 4px 12px rgba(139,92,246,0.4);
  transform: translateY(-1px);
}

/* 幽灵按钮 - 透明背景带描边 */
.btn-ghost {
  background: transparent;
  color: #3b82f6;
  border: 1px solid #3b82f6;
}
.btn-ghost:hover:not(:disabled) {
  background: #eff6ff;
  border-color: #2563eb;
  color: #2563eb;
}

/* 图标按钮 */
.btn-icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 2rem; height: 2rem; padding: 0;
  border-radius: 0.5rem; border: 1px solid var(--border-color);
  background: #fff; color: #64748b; cursor: pointer;
  font-size: 0.8125rem; transition: var(--transition);
}
.btn-icon:hover { background: #f1f5f9; border-color: #94a3b8; color: #1e293b; }
.btn-icon--danger { color: #dc2626; border-color: #fca5a5; }
.btn-icon--danger:hover { background: #fee2e2; border-color: #dc2626; }

/* Stat card */
.stat-card {
  background: var(--bg-white);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  padding: 1.25rem;
  transition: var(--transition);
}
.stat-card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }

/* Badge */
.badge {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem 0.6rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
}
.badge-red    { background: #fee2e2; color: #dc2626; }
.badge-green  { background: #dcfce7; color: #16a34a; }
.badge-yellow { background: #fef9c3; color: #ca8a04; }
.badge-blue   { background: #dbeafe; color: #2563eb; }
.badge-gray   { background: #f1f5f9; color: #64748b; }
.badge-purple { background: #ede9fe; color: #7c3aed; }

/* Table */
.data-table { width: 100%; border-collapse: collapse; font-size: 0.875rem; }
.data-table th {
  padding: 0.75rem 1rem;
  text-align: left;
  font-weight: 600;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--border-color);
  background: #f8fafc;
}
.data-table td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--border-color);
  color: var(--text-primary);
}
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: #f8fafc; }

/* Page header */
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.5rem; }
.page-header h1 { font-size: 1.5rem; font-weight: 700; color: var(--text-primary); }
.page-header p  { font-size: 0.875rem; color: var(--text-secondary); margin-top: 0.25rem; }

/* Scrollbar */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: #94a3b8; }

@media (max-width: 768px) {
  .main-content { margin-left: 0; padding: 1rem; }
}
</style>
