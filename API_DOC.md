# 后端 API 文档

> 系统：伺"机"守护 · Slowloris 智能感知与决策系统  
> 版本：v2.0  
> 基础路径：`http://localhost:8080`  
> 认证方式：Bearer Token（`Authorization: Bearer <accessToken>`）

---

## 通用说明

### 响应格式

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
| data | object/array | 业务数据（可能为 null） |

### 认证说明

- 登录后返回 `accessToken`（1小时有效）和 `refreshToken`（24小时有效）
- 除 `/api/login`、`/api/register`、`/api/wechat/**`、`/api/health` 外，所有接口需在请求头携带：
  ```
  Authorization: Bearer <accessToken>
  ```
- Token 过期后用 `refreshToken` 刷新；网关 JwtFilter 自动解析并注入以下请求头给下游服务：
  - `user-id`：当前用户ID
  - `username`：当前用户名
  - `roles`：角色列表（如 `ROLE_ADMIN,ROLE_OPERATOR`）

### 多用户数据隔离

- 普通用户只能查看/操作自己创建的数据
- `ROLE_ADMIN` 管理员可查看/操作全部用户的数据
- 部分接口（删除告警/日志/决策记录）仅限管理员

---

## 1. 认证模块 (auth-service)

### 1.1 登录

```
POST /api/login
```

**请求体**

```json
{
  "username": "admin",
  "password": "Admin@123456"
}
```

**响应**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "username": "admin",
    "roles": "ROLE_ADMIN",
    "userId": 1
  },
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

### 1.3 Token 验证

```
GET /api/verify
```
Header: `Authorization: Bearer <token>`

**响应**

```json
{
  "success": true,
  "data": {
    "valid": true,
    "username": "admin",
    "roles": "ROLE_ADMIN",
    "userId": 1
  }
}
```

---

### 1.4 健康检查（公开）

```
GET /api/health
```

---

### 1.5 获取用户列表（需管理员）

```
GET /api/users
```

---

### 1.6 创建用户（需管理员）

```
POST /api/users
```

```json
{
  "username": "operator1",
  "password": "123456",
  "email": "op@example.com",
  "role": "operator",
  "status": "active"
}
```

---

### 1.7 更新用户（需管理员）

```
PUT /api/users/{id}
```

```json
{
  "email": "new@example.com",
  "role": "viewer",
  "status": "inactive"
}
```

---

### 1.8 删除用户（需管理员）

```
DELETE /api/users/{id}
```

---

### 1.9 重置密码

```
POST /api/reset-password
```

```json
{
  "username": "targetUser",
  "newPassword": "newPassword123"
}
```

---

### 1.10 当前用户个人资料

```
GET /api/profile
```

---

### 1.11 更新个人资料

```
PUT /api/profile
```

```json
{
  "username": "newName",
  "email": "new@example.com",
  "avatar": "https://...",
  "oldPassword": "oldPass",
  "newPassword": "newPass"
}
```

---

### 1.12 微信扫码登录（获取授权URL）

```
GET /api/wechat/login
```

**响应**

```json
{
  "success": true,
  "data": "https://open.weixin.qq.com/connect/qrconnect?...",
  "message": "登录地址生成成功"
}
```

> Mock 模式（`wechat.mock-enabled=true`）下返回本地模拟回调地址，前端直接跳转即可模拟扫码。

---

### 1.13 微信回调（OAuth 回调）

```
GET /api/wechat/callback?code=xxx&state=xxx
```

> 微信服务器回调，完成后 302 重定向到 `/login?wechat_token=<token>`。

---

## 2. 监控模块 (monitor-service)

### 2.1 开始监控

```
POST /api/monitor/start
```

**请求体**

```json
{
  "ip": "192.168.1.100"
}
```

**响应**

```json
{
  "success": true,
  "message": "已开始监控 192.168.1.100"
}
```

> 后端自动：创建 `monitored_target` 记录、生成 Agent Token、启动 30 秒定时采集调度。

---

### 2.2 停止监控

```
POST /api/monitor/stop
```

```json
{
  "ip": "192.168.1.100"
}
```

---

### 2.3 监控状态

```
GET /api/monitor/status/{ip}
```

**响应**

```json
{
  "success": true,
  "data": {
    "ip": "192.168.1.100",
    "active": true,
    "latestSnapshot": { ... },
    "blocked": false,
    "agentOnline": true,
    "agentLastSeen": 1745548800000
  }
}
```

| 字段 | 说明 |
|------|------|
| active | 是否有监控任务运行中 |
| latestSnapshot | 最新一轮 metrics_snapshot（含AI判定） |
| blocked | 目标IP当前是否被封禁 |
| agentOnline | 探针是否在线（90s 内有上报） |
| agentLastSeen | 探针最后心跳时间戳 |

---

### 2.4 AI 攻击预测

```
GET /api/monitor/predict/{ip}
```

基于近 24 小时历史快照，调用 DeepSeek API 预测未来 6 小时攻击概率。

**响应**

```json
{
  "success": true,
  "data": [
    {
      "time": "2026-06-21T14:00:00",
      "probability": 0.85,
      "level": "high"
    }
  ]
}
```

---

### 2.5 监控目标列表

```
GET /api/monitor/targets
```

**响应**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "ipAddress": "192.168.1.100",
      "status": 1,
      "userId": "1",
      "agentToken": "a1b2c3d4...",
      "createTime": "2026-06-01T10:00:00",
      "updateTime": "2026-06-21T10:00:00"
    }
  ]
}
```

---

### 2.6 获取自动处置配置

```
GET /api/monitor/config
```

**响应**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "autoMode": 0,
    "blockRiskScore": 80,
    "blockSeverity": "high",
    "blockMinConfidence": 0.7,
    "autoUnblockMinutes": 0
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| autoMode | int | 0=仅建议 / 1=自动封禁 |
| blockRiskScore | int | 触发封禁的最低风险评分（0-100） |
| blockSeverity | string | 触发封禁的最低严重级别（low/medium/high/critical） |
| blockMinConfidence | double | 触发封禁的最低AI置信度（0-1） |
| autoUnblockMinutes | int | 自动解封时长（分钟），0=不自动解封 |

---

### 2.7 更新自动处置配置

```
PUT /api/monitor/config
```

```json
{
  "autoMode": 1,
  "blockRiskScore": 70,
  "blockSeverity": "medium",
  "blockMinConfidence": 0.6,
  "autoUnblockMinutes": 120
}
```

> 更新后通过 SSE 实时广播 `config` 事件给所有在线用户。

---

### 2.8 封禁 IP 列表

```
GET /api/monitor/blocked
```

普通用户仅见自己范围内的封禁，管理员见全部。

**响应**

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "ipAddress": "10.0.0.5",
      "targetIp": "192.168.1.100",
      "status": 1,
      "auto": 0,
      "operator": "manual",
      "reason": "人工封禁：疑似Slowloris攻击来源",
      "riskScore": 85,
      "severity": "high",
      "blockTime": "2026-06-21T10:30:00",
      "unblockTime": null,
      "unblockBy": null
    }
  ]
}
```

---

### 2.9 人工封禁 IP

```
POST /api/monitor/block
```

```json
{
  "ip": "10.0.0.5",
  "targetIp": "192.168.1.100",
  "reason": "疑似Slowloris攻击来源"
}
```

> 封禁后通过 SSE 实时推送 `block` 事件。

---

### 2.10 解封 IP

```
POST /api/monitor/unblock
```

```json
{
  "ip": "10.0.0.5"
}
```

> 普通用户仅可解封自己范围内的封禁，管理员可解封任意。解封后通过 SSE 实时推送 `unblock` 事件。

---

### 2.11 攻击检测/决策记录

```
GET /api/monitor/detections?limit=50
```

| 参数 | 默认值 | 说明 |
|------|--------|------|
| limit | 50 | 返回条数（最大200） |

---

### 2.12 删除单条决策记录（仅管理员）

```
DELETE /api/monitor/detections/{id}
```

---

### 2.13 批量删除决策记录（仅管理员）

```
DELETE /api/monitor/detections
```

```json
{
  "ids": [1, 2, 3]
}
```

---

### 2.14 Agent 闭环总览统计

```
GET /api/monitor/agent/overview
```

**响应**

```json
{
  "success": true,
  "data": {
    "autoMode": 1,
    "config": { ... },
    "activeTargets": 3,
    "blockedCount": 5,
    "sseConnections": 2
  }
}
```

---

### 2.15 获取探针安装命令

```
GET /api/monitor/agent/install/{ip}
```

**响应**

```json
{
  "success": true,
  "data": {
    "ip": "192.168.1.100",
    "token": "a1b2c3d4e5f6...",
    "command": "curl -fsSL http://平台/internal/agent/install.sh | sudo bash -s -- --server=http://平台 --target=192.168.1.100 --token=a1b2c3d4e5f6...",
    "online": true,
    "lastSeen": 1745548800000
  }
}
```

> 前端直接展示 `command`，用户复制后在目标服务器以 root 执行即可完成探针安装。

---

### 2.16 SSE 实时事件流

```
GET /api/monitor/stream
```

> 长连接，服务端推送事件。Gateway 对该路由禁用了响应超时（`response-timeout: -1`）。

**事件类型**

| 事件名 | 说明 | 推送范围 |
|--------|------|---------|
| `connected` | 连接成功确认 | 仅当前连接 |
| `analysis` | 每轮AI分析结果（无论是否攻击都推送） | 归属用户 + 管理员 |
| `disposition` | 决策处置结果（攻击时才推送） | 归属用户 + 管理员 |
| `block` | IP被封禁通知 | 归属用户 + 管理员 |
| `unblock` | IP被解封通知 | 归属用户 + 管理员 |
| `config` | 自动处置配置变更 | 全部在线用户 |

**analysis 事件数据示例**

```json
{
  "ip": "192.168.1.100",
  "isAttack": true,
  "severity": "high",
  "riskScore": 85,
  "confidence": 0.92,
  "reasoning": "半开连接数达到200，平均包大小仅512字节...",
  "halfOpenConns": 200,
  "requestRate": 5,
  "avgPacketSize": 512,
  "uniqueSourceIps": 3,
  "suspiciousIps": [
    {
      "ipAddress": "10.0.0.5",
      "country": "中国",
      "city": "北京",
      "isp": "中国电信"
    }
  ],
  "timestamp": 1745548800000
}
```

**disposition 事件数据示例**

```json
{
  "id": 42,
  "targetIp": "192.168.1.100",
  "action": "auto_block",
  "severity": "high",
  "riskScore": 85,
  "confidence": 0.92,
  "blockedIps": ["10.0.0.5"],
  "reasoning": "半开连接数达到200...",
  "recommendations": ["建议立即封禁来源IP", "检查Web服务器连接池配置"],
  "timestamp": 1745548800000
}
```

---

## 3. 主机探针 Agent 内部接口（monitor-service，无需JWT）

> 以下接口路径为 `/internal/agent/**`，网关 JwtFilter 放行，由 AgentController 自行鉴权。

### 3.1 探针上报本机指标

```
POST /internal/agent/report
```

**请求体**

```json
{
  "target": "192.168.1.100",
  "token": "探针鉴权token",
  "halfOpenConns": 42,
  "requestRate": 120,
  "uniqueSourceIps": 5,
  "avgPacketSize": 1400,
  "avgConnDuration": 5000,
  "sourceIps": ["10.0.0.5", "10.0.0.6"]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| target | string | 目标IP（被监控的服务器IP） |
| token | string | 探针鉴权Token |
| halfOpenConns | int | 半开连接数（Web端口ESTABLISHED连接数） |
| requestRate | int | 请求速率（次/分钟） |
| uniqueSourceIps | int | 唯一来源IP数 |
| avgPacketSize | int | 平均网络包大小（字节） |
| avgConnDuration | long | 平均连接时长（毫秒） |
| sourceIps | array | Top 20 真实来源IP列表 |

**鉴权**：校验 `target + token` 与 `monitored_target` 表中记录匹配。

**响应**：`{"success": true, "message": null}`

---

### 3.2 下载探针安装脚本（公开）

```
GET /internal/agent/install.sh
```

> 返回纯文本 Shell 脚本，供 `curl | bash` 一键安装。

---

## 4. 告警与日志模块 (alert-log-service)

### 4.1 获取告警列表

```
GET /api/alerts?page=1&pageSize=20&status=0&level=3
```

| 参数 | 类型 | 说明 |
|------|------|------|
| page | int | 页码，从1开始，默认1 |
| pageSize | int | 每页条数，默认20 |
| status | int | 告警状态过滤（可选）：0=未处理，1=已处理 |
| level | int | 告警级别过滤（可选）：1=info, 2=warn, 3=error, 4=critical |

**响应**

```json
{
  "success": true,
  "data": {
    "records": [
      {
        "id": 1,
        "type": "attack",
        "level": 3,
        "status": 0,
        "description": "检测到high级别Slowloris攻击，攻击来源IP: 10.0.0.5",
        "message": "检测到high级别Slowloris攻击...",
        "ipAddress": "10.0.0.5",
        "userId": "1",
        "details": "{...}",
        "createTime": "2026-06-21T10:30:00"
      }
    ],
    "total": 128,
    "page": 1,
    "pageSize": 20,
    "unreadCount": 3
  }
}
```

---

### 4.2 更新告警状态

```
PUT /api/alerts/{id}
```

```json
{
  "status": 1,
  "handler": "admin"
}
```

> status=1 表示标记为已处理，自动记录 `handleTime` 和 `handler`。

---

### 4.3 删除单条告警（仅管理员）

```
DELETE /api/alerts/{id}
```

---

### 4.4 批量删除告警（仅管理员）

```
DELETE /api/alerts
```

```json
{
  "ids": [1, 2, 3]
}
```

> ids 支持 `Array<Long>` 或逗号分隔字符串 `"1,2,3"`。

---

### 4.5 获取系统日志

```
GET /api/logs?page=1&pageSize=20&type=2
```

| 参数 | 类型 | 说明 |
|------|------|------|
| page | int | 页码，默认1 |
| pageSize | int | 每页条数，默认20 |
| type | int | 日志类型过滤（可选）：1=info, 2=warn, 3=error |

---

### 4.6 删除日志（仅管理员）

```
DELETE /api/logs/{id}        # 单条
DELETE /api/logs              # 批量 {"ids": [1,2,3]}
```

---

## 5. 网关流量采集（内部接口，无需JWT）

### 5.1 网关上报请求记录

```
POST /traffic/record
```

> 由 `TrafficCollectFilter` 在每个请求经过网关时异步调用，用于60秒滑动窗口流量统计。

---

## 6. 错误码

| HTTP 状态码 | 说明 |
|------------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证或 Token 过期 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 7. 角色权限矩阵

| 操作 | ROLE_ADMIN | ROLE_OPERATOR | ROLE_VIEWER |
|------|-----------|---------------|-------------|
| 查看监控状态/目标 | 全部用户数据 | 仅自己数据 | 仅自己数据 |
| 启停监控 | ✓ | ✓ | ✗ |
| 人工封禁/解封 | ✓（全部范围） | ✓（自己范围） | ✗ |
| 查看/更新Agent配置 | ✓ | ✓ | ✗ |
| 查看告警/日志 | 全部 | 仅自己 | 仅自己 |
| 删除告警/日志/决策 | ✓ | ✗ | ✗ |
| 用户管理(CRUD) | ✓ | ✗ | ✗ |
| SSE 订阅 | 收到全部事件 | 仅收到自己数据的事件 | 仅收到自己数据的事件 |

---

## 附录：默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | Admin@123456 | ROLE_ADMIN |
| operator | Admin@123456 | ROLE_OPERATOR |
| viewer | Admin@123456 | ROLE_VIEWER |
