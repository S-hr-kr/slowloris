// Unified API service — auto-attaches JWT, unwraps Result<T> envelope
async function request(url, options = {}) {
    const token = localStorage.getItem('authToken')
    const config = {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            ...options.headers,
            ...(token ? { Authorization: `Bearer ${token}` } : {})
        }
    }

    const response = await fetch(url, config)

    if (response.status === 401) {
        localStorage.removeItem('authToken')
        window.location.href = '/login'
        throw new Error('登录已过期')
    }

    if (!response.ok) {
        const text = await response.text()
        throw { response: { status: response.status, data: tryJson(text) } }
    }

    const contentType = response.headers.get('content-type') || ''
    if (!contentType.includes('application/json')) return await response.text()

    const json = await response.json()

    // Unwrap Result<T> envelope: { success, data, message }
    if (json !== null && typeof json === 'object' && 'success' in json) {
        if (!json.success) {
            throw { response: { status: response.status, data: json } }
        }
        return { data: json.data, message: json.message }
    }

    // Bare JSON (e.g. login returning token string directly)
    return { data: json }
}

function tryJson(text) {
    try { return JSON.parse(text) } catch { return { message: text } }
}

export default {
    get(url, params) {
        if (params) {
            const qs = new URLSearchParams(params).toString()
            url = `${url}?${qs}`
        }
        return request(url, { method: 'GET' })
    },
    post(url, data) {
        return request(url, { method: 'POST', body: JSON.stringify(data) })
    },
    put(url, data) {
        return request(url, { method: 'PUT', body: JSON.stringify(data) })
    },
    delete(url, data) {
        const opts = { method: 'DELETE' }
        if (data !== undefined) opts.body = JSON.stringify(data)
        return request(url, opts)
    },
    request
}
