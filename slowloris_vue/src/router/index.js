import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated, isAdmin } from '../services/auth.js'

const routes = [
    { path: '/',           redirect: '/dashboard' },
    { path: '/dashboard',  name: 'Dashboard',  component: () => import('../views/Dashboard.vue'),   meta: { requiresAuth: true } },
    { path: '/monitor',    name: 'Monitor',    component: () => import('../views/Monitor.vue'),     meta: { requiresAuth: true } },
    { path: '/auto',       name: 'AutoDisposition', component: () => import('../views/AutoDisposition.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/logs',       name: 'LogsAlerts', component: () => import('../views/LogsAlerts.vue'),  meta: { requiresAuth: true } },
    { path: '/users',      name: 'UserManagement', component: () => import('../views/UserManagement.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/profile',    name: 'Profile',    component: () => import('../views/Profile.vue'),     meta: { requiresAuth: true } },
    { path: '/login',      name: 'Login',      component: () => import('../views/Login.vue'),       meta: { hideSidebar: true } },
    { path: '/register',   name: 'Register',   component: () => import('../views/Register.vue'),    meta: { hideSidebar: true } },
]

const router = createRouter({
    history: createWebHistory(process.env.BASE_URL),
    routes
})

router.beforeEach((to, from, next) => {
    if (to.meta.requiresAuth && !isAuthenticated()) {
        next('/login')
    } else if (to.meta.requiresAdmin && !isAdmin()) {
        // 非管理员访问管理员专属页面 → 退回控制台
        next('/dashboard')
    } else {
        next()
    }
})

export default router
