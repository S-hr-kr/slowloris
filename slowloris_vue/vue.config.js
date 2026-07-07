const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,

  // 【关键修复1】生产环境部署路径，必须设为 '/'，解决样式错乱
  publicPath: '/',

  // 开发环境代理（保持不变，仅用于本地 npm run serve）
  devServer: {
    host: '0.0.0.0',        // 绑定所有网卡，允许局域网 IP 访问
    port: 9090,
    allowedHosts: 'all',    // 允许通过任意主机名/IP 访问，避免 Invalid Host header
    client: {
      // 让 HMR 热更新的 WebSocket 跟随浏览器当前地址，而不是写死 localhost
      // 这样从局域网 IP（如 192.168.110.230:9090）访问时，HMR socket 才能正确连接
      webSocketURL: 'auto://0.0.0.0:0/ws'
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
      // 移除了 /ws 的 WebSocket 代理配置，因为项目使用 SSE 而不是 WebSocket
    }
  }
})