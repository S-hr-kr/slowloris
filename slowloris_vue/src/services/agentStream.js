// Agent 实时事件流客户端（SSE over fetch）。
// 原生 EventSource 无法携带 Authorization 头，故用 fetch + ReadableStream 手动解析 SSE 帧，
// 以便通过网关的 JWT 校验。自动重连，断线时回退由调用方的轮询兜底。

const STREAM_URL = '/api/monitor/stream'

export function createAgentStream({ onEvent, onStatus } = {}) {
    let controller = null
    let stopped = false
    let retryDelay = 2000

    async function connect() {
        if (stopped) return
        const token = localStorage.getItem('authToken')
        if (!token) { onStatus && onStatus('unauthorized'); return }

        controller = new AbortController()
        try {
            const resp = await fetch(STREAM_URL, {
                headers: {
                    Authorization: `Bearer ${token}`,
                    Accept: 'text/event-stream',
                },
                signal: controller.signal,
            })

            if (!resp.ok || !resp.body) {
                throw new Error(`stream status ${resp.status}`)
            }

            onStatus && onStatus('connected')
            retryDelay = 2000

            const reader = resp.body.getReader()
            const decoder = new TextDecoder()
            let buffer = ''

            while (!stopped) {
                const { value, done } = await reader.read()
                if (done) break
                buffer += decoder.decode(value, { stream: true })

                // SSE 帧以空行分隔
                let sepIndex
                while ((sepIndex = buffer.indexOf('\n\n')) !== -1) {
                    const rawEvent = buffer.slice(0, sepIndex)
                    buffer = buffer.slice(sepIndex + 2)
                    parseFrame(rawEvent)
                }
            }
        } catch (e) {
            if (!stopped) onStatus && onStatus('disconnected')
        } finally {
            if (!stopped) scheduleReconnect()
        }
    }

    function parseFrame(raw) {
        let event = 'message'
        const dataLines = []
        for (const line of raw.split('\n')) {
            if (line.startsWith('event:')) event = line.slice(6).trim()
            else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
        }
        if (dataLines.length === 0) return
        const dataStr = dataLines.join('\n')
        let payload = dataStr
        try { payload = JSON.parse(dataStr) } catch { /* keep raw */ }
        if (event === 'connected') return
        onEvent && onEvent(event, payload)
    }

    function scheduleReconnect() {
        setTimeout(() => { if (!stopped) connect() }, retryDelay)
        retryDelay = Math.min(retryDelay * 1.5, 15000)
    }

    connect()

    return {
        close() {
            stopped = true
            if (controller) controller.abort()
            onStatus && onStatus('closed')
        }
    }
}
