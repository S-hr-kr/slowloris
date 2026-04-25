# 后端 API 开发文档

> 系统：伺"机"守护 · Slowloris 智能感知与决策系统  
> 版本：v1.0  
> 基础路径：`http://localhost:8080`  
> 认证方式：Bearer Token（`Authorization: Bearer <token>`）

---

## 通用响应格式

```json
{
  "success": true,
  "message": "操作成功",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| success | boolean | 请求是否成功 |
| message | string | 提示信息 |
| data | object/array | 业务数据 |

---

## 1. 认证模块

### 1.1 登录

```
POST /api/login
```

**请求体**

```json
{
  "username": "admin",
  "password": "123456"
}
```

**响应**

```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "message": "登录成功"
}
```

---

### 1.2 注册

```
POST /api/register
```

**请求体**

```json
{
  "username": "newuser",
  "password": "123456",
  "email": "user@example.com"
}
```

**响应**

```json
{
  "success": true,
  "message": "注册成功"
}
```

---

## 2. 模型预测模块

### 2.1 发起预测

```
POST /api/predict
```

**请求体**

```json
{
  "ip": "192.168.1.100",
  "timeRange": "1h",
  "model": "xgboost",
  "features": {
    "request_rate": true,
    "connection_time": true,
    "user_agent": true,
    "referrer": true
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| ip | string | 目标 IP 地址（IPv4） |
| timeRange | string | `1h` / `3h` / `6h` / `12h` / `24h` |
| model | string | `xgboost` / `random_forest` / `lstm` |
| features | object | 各特征是否启用 |

**响应**

```json
{
  "success": true,
  "data": {
    "is_attack": true,
    "confidence": 0.9234,
    "model": "xgboost",
    "timestamp": "2026-04-25T10:30:00Z",
    "data_points": 1200,
    "feature_importance": {
      "request_rate": 0.45,
      "connection_time": 0.30,
      "user_agent": 0.15,
      "referrer": 0.10
    }
  }
}
```

---

### 2.2 获取历史预测记录

```
GET /api/prediction/history
```

**响应**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "ip": "192.168.1.100",
      "is_attack": true,
      "confidence": 0.9234,
      "model": "xgboost",
      "timestamp": "2026-04-25T10:30:00Z"
    }
  ]
}
```

---

## 3. 攻击检测模块

### 3.1 获取攻击列表与统计

```
GET /api/attacks
```

**响应**

```json
{
  "success": true,
  "data": {
    "stats": {
      "totalAttacks": 128,
      "currentAttacks": 3,
      "blockedAttacks": 120,
      "attackTypes": 2
    },
    "details": [
      {
        "id": 1,
        "type": "Slowloris",
        "sourceIp": "10.0.0.5",
        "target": "192.168.1.1:80",
        "startTime": "2026-04-25 10:00:00",
        "status": "active"
      }
    ]
  }
}
```

---

### 3.2 阻止攻击

```
POST /api/attacks/block
```

**请求体**

```json
{
  "attackId": 1
}
```

**响应**

```json
{
  "success": true,
  "message": "攻击已阻止"
}
```

---

## 4. IP 监控模块

### 4.1 获取 IP 列表

```
GET /api/ip-monitor?ip=&status=&timeRange=24h
```

**查询参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| ip | string | IP 地址过滤（可选） |
| status | string | `normal` / `suspicious` / `blocked`（可选） |
| timeRange | string | `1h` / `24h` / `7d` |

**响应**

```json
{
  "success": true,
  "data": {
    "stats": {
      "total": 500,
      "normal": 450,
      "suspicious": 30,
      "blocked": 20
    },
    "ipList": [
      {
        "ip": "10.0.0.5",
        "status": "suspicious",
        "accessCount": 3200,
        "lastActivity": "2026-04-25 10:28:00",
        "riskScore": 75
      }
    ]
  }
}
```

---

### 4.2 封锁 IP

```
POST /api/ip-monitor/block
```

**请求体**

```json
{ "ip": "10.0.0.5" }
```

**响应**

```json
{ "success": true, "message": "IP 已封锁" }
```

---

### 4.3 解封 IP

```
POST /api/ip-monitor/unblock
```

**请求体**

```json
{ "ip": "10.0.0.5" }
```

**响应**

```json
{ "success": true, "message": "IP 已解封" }
```

---

## 5. 流量分析模块

### 5.1 获取流量数据

```
GET /api/traffic?timeRange=24h&type=all
```

**查询参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| timeRange | string | `1h` / `24h` / `7d` / `30d` |
| type | string | `all` / `normal` / `attack` |

**响应**

```json
{
  "success": true,
  "data": {
    "stats": {
      "total": 1024.5,
      "normal": 980.2,
      "attack": 44.3,
      "peak": 850
    },
    "topIps": [
      {
        "ip": "10.0.0.5",
        "traffic": 512,
        "type": "attack",
        "protocol": "HTTP"
      }
    ]
  }
}
```

---

### 5.2 导出流量数据

```
GET /api/traffic/export?timeRange=24h
```

**响应**：返回 CSV 文件流，`Content-Type: text/csv`

---

## 6. 告警管理模块

### 6.1 获取告警列表

```
GET /api/alerts
```

**响应**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "type": "attack",
      "title": "检测到 Slowloris 攻击",
      "description": "来自 10.0.0.5 的 Slowloris 攻击，已持续 120 秒",
      "source": "10.0.0.5",
      "status": "unread",
      "timestamp": 1745548800000,
      "logs": []
    }
  ]
}
```

**告警类型（type）**：`attack` / `traffic` / `system` / `security`  
**告警状态（status）**：`unread` / `read` / `resolved` / `ignored`

---

## 7. 用户管理模块

### 7.1 获取用户列表

```
GET /api/users
```

**响应**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "username": "admin",
      "email": "admin@example.com",
      "role": "admin",
      "status": "active",
      "createdAt": "2026-01-01T00:00:00Z",
      "lastLogin": "2026-04-25T10:00:00Z"
    }
  ]
}
```

---

### 7.2 创建用户

```
POST /api/users
```

**请求体**

```json
{
  "username": "operator1",
  "password": "123456",
  "email": "op@example.com",
  "role": "operator"
}
```

---

### 7.3 更新用户

```
PUT /api/users/{id}
```

**请求体**（仅传需要修改的字段）

```json
{
  "email": "new@example.com",
  "role": "viewer",
  "status": "inactive"
}
```

---

### 7.4 删除用户

```
DELETE /api/users/{id}
```

---

## 8. 日志模块

### 8.1 获取日志列表

```
GET /api/logs?page=1&size=50&level=&keyword=
```

**查询参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| page | int | 页码，从 1 开始 |
| size | int | 每页条数，默认 50 |
| level | string | `info` / `warn` / `error`（可选） |
| keyword | string | 关键词搜索（可选） |

**响应**

```json
{
  "success": true,
  "data": {
    "total": 1000,
    "page": 1,
    "size": 50,
    "list": [
      {
        "id": 1,
        "level": "warn",
        "message": "检测到异常连接",
        "source": "AttackDetector",
        "timestamp": "2026-04-25T10:00:00Z"
      }
    ]
  }
}
```

---

## 9. 报告模块

### 9.1 获取报告列表

```
GET /api/reports
```

---

### 9.2 生成报告

```
POST /api/reports/generate
```

**请求体**

```json
{
  "type": "daily",
  "startTime": "2026-04-24T00:00:00Z",
  "endTime": "2026-04-25T00:00:00Z"
}
```

---

## 10. WebSocket 实时推送

连接地址：`ws://localhost:8080/ws`（STOMP over SockJS）

| 订阅 Topic | 说明 | 数据结构 |
|-----------|------|---------|
| `/topic/attacks/new` | 新攻击事件 | 攻击详情对象 |
| `/topic/attacks/update` | 攻击状态更新 | 攻击详情对象 |
| `/topic/prediction/update` | 预测结果更新 | 预测结果对象 |
| `/topic/alerts/new` | 新告警推送 | 告警对象 |

---

## 11. 错误码

| HTTP 状态码 | 说明 |
|------------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证或 Token 过期 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 12. 角色权限

| 角色 | 说明 | 可访问模块 |
|------|------|-----------|
| admin | 管理员 | 全部模块 |
| operator | 操作员 | 除用户管理外全部 |
| viewer | 只读用户 | 仅查看，不可操作 |
