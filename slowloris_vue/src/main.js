import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './assets/styles.css'
import 'font-awesome/css/font-awesome.min.css'
import { Chart, registerables } from 'chart.js'

Chart.register(...registerables)
Chart.defaults.events = ['mousemove', 'mouseout', 'click', 'touchstart', 'touchmove']
Chart.defaults.interaction = { mode: 'nearest', intersect: false }
Chart.defaults.plugins.tooltip = Chart.defaults.plugins.tooltip || {}
Chart.defaults.plugins.tooltip.mode = 'nearest'
Chart.defaults.plugins.tooltip.intersect = false

const app = createApp(App)
app.use(router)
app.config.errorHandler = (err, instance, info) => {
    console.error('全局错误:', err)
    console.error('组件实例:', instance)
    console.error('错误信息:', info)
}
app.mount('#app')