# Slowloris系统完整API接口文档

## 文档信息

- **版本**: 1.0.0
- **日期**: 2026-03-14
- **基础路径**: /
- **说明**: 本文档描述了Slowloris系统的完整API接口规范，包含所有模块的接口定义

## 项目模块划分

Slowloris系统包含以下主要模块：

1. **用户认证模块** - 处理用户登录、注册和认证
2. **IP监控模块** - 监控IP地址的活动和状态
3. **流量分析模块** - 分析网络流量数据
4. **攻击检测模块** - 检测和识别攻击行为
5. **告警管理模块** - 管理系统告警信息
6. **模型预测模块** - 使用机器学习模型进行攻击预测
7. **用户管理模块** - 管理系统用户
8. **系统配置模块** - 管理系统配置信息
9. **数据报表模块** - 生成和管理数据报表
10. **日志中心模块** - 管理系统日志

## 1. 用户认证模块

### 1.1 用户登录

**接口地址**: `POST /auth/login`

**接口描述**: 用户登录接口，验证用户凭据并返回认证令牌

**请求体**:

```json
{
  "username": "admin",
  "password": "password123"
}
```

**请求参数说明**:

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| username | string | 是 | 用户名 | admin |
| password | string | 是 | 密码 | password123 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "admin",
      "role": "admin",
      "email": "admin@example.com"
    }
  },
  "message": "登录成功"
}
```

**响应状态码**:
- 200: 登录成功
- 400: 请求参数错误
- 401: 用户名或密码错误
- 500: 服务器内部错误

### 1.2 用户注册

**接口地址**: `POST /auth/register`

**接口描述**: 用户注册接口，创建新用户

**请求体**:

```json
{
  "username": "newuser",
  "password": "password123",
  "email": "newuser@example.com",
  "role": "user"
}
```

**请求参数说明**:

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| username | string | 是 | 用户名 | newuser |
| password | string | 是 | 密码 | password123 |
| email | string | 是 | 邮箱 | newuser@example.com |
| role | string | 否 | 用户角色 | user |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "id": 2,
    "username": "newuser",
    "role": "user",
    "email": "newuser@example.com"
  },
  "message": "注册成功"
}
```

**响应状态码**:
- 200: 注册成功
- 400: 请求参数错误
- 409: 用户名已存在
- 500: 服务器内部错误

### 1.3 验证令牌

**接口地址**: `GET /auth/verify`

**接口描述**: 验证令牌有效性

**请求头**:

| 头部名称 | 值 | 描述 |
|---------|-----|------|
| Authorization | Bearer {token} | 认证令牌 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "valid": true,
    "user": {
      "id": 1,
      "username": "admin",
      "role": "admin"
    }
  }
}
```

**响应状态码**:
- 200: 令牌有效
- 401: 令牌无效或过期
- 500: 服务器内部错误

## 2. IP监控模块

### 2.1 获取IP列表

**接口地址**: `GET /ip-monitor/list`

**接口描述**: 获取监控的IP地址列表

**请求参数**:

| 参数名 | 类型 | 必填 | 位置 | 描述 | 示例值 |
|--------|------|------|------|------|--------|
| page | integer | 否 | query | 页码 | 1 |
| limit | integer | 否 | query | 每页数量 | 20 |
| status | string | 否 | query | IP状态 | active |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "total": 100,
    "list": [
      {
        "id": 1,
        "ip": "192.168.1.1",
        "status": "active",
        "last_seen": "2026-03-14T10:00:00Z",
        "risk_score": 85.5,
        "country": "China"
      }
    ]
  }
}
```

**响应状态码**:
- 200: 成功获取IP列表
- 500: 服务器内部错误

### 2.2 获取IP详情

**接口地址**: `GET /ip-monitor/detail/{ip}`

**接口描述**: 获取指定IP的详细信息

**路径参数**:

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| ip | string | 是 | IP地址 | 192.168.1.1 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "ip": "192.168.1.1",
    "status": "active",
    "risk_score": 85.5,
    "country": "China",
    "isp": "China Telecom",
    "last_seen": "2026-03-14T10:00:00Z",
    "first_seen": "2026-03-10T08:00:00Z",
    "connections": 150,
    "requests": 1200,
    "alerts": 5
  }
}
```

**响应状态码**:
- 200: 成功获取IP详情
- 404: IP不存在
- 500: 服务器内部错误

### 2.3 添加IP监控

**接口地址**: `POST /ip-monitor/add`

**接口描述**: 添加新的IP地址到监控列表

**请求体**:

```json
{
  "ip": "192.168.1.100",
  "description": "重要服务器",
  "thresholds": {
    "request_rate": 100,
    "connection_time": 30,
    "packet_size": 40
  }
}
```

**请求参数说明**:

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| ip | string | 是 | IP地址 | 192.168.1.100 |
| description | string | 否 | 描述 | 重要服务器 |
| thresholds | object | 否 | 阈值设置 | 见示例 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "id": 101,
    "ip": "192.168.1.100",
    "status": "active",
    "description": "重要服务器"
  },
  "message": "IP添加成功"
}
```

**响应状态码**:
- 200: IP添加成功
- 400: 请求参数错误
- 409: IP已存在
- 500: 服务器内部错误

## 3. 流量分析模块

### 3.1 获取流量统计

**接口地址**: `GET /traffic/stats`

**接口描述**: 获取流量统计信息

**请求参数**:

| 参数名 | 类型 | 必填 | 位置 | 描述 | 示例值 |
|--------|------|------|------|------|--------|
| timeRange | string | 是 | query | 时间范围 | 1h |
| ip | string | 否 | query | 过滤IP | 192.168.1.1 |

**时间范围枚举值**:
- 1h: 最近1小时
- 3h: 最近3小时
- 6h: 最近6小时
- 12h: 最近12小时
- 24h: 最近24小时
- 7d: 最近7天
- 30d: 最近30天

**响应示例**:

```json
{
  "success": true,
  "data": {
    "total_requests": 12500,
    "total_connections": 3500,
    "average_response_time": 150.5,
    "peak_traffic": "2026-03-14T09:30:00Z",
    "top_ips": [
      {
        "ip": "192.168.1.1",
        "requests": 1200,
        "percentage": 9.6
      }
    ]
  }
}
```

**响应状态码**:
- 200: 成功获取流量统计
- 500: 服务器内部错误

### 3.2 获取流量图表数据

**接口地址**: `GET /chart`

**接口描述**: 获取流量分析图表数据

**请求参数**:

| 参数名 | 类型 | 必填 | 位置 | 描述 | 示例值 |
|--------|------|------|------|------|--------|
| ip | string | 是 | query | 目标IP地址 | 192.168.1.1 |
| timeRange | string | 是 | query | 时间范围 | 1h |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "labels": ["10:00", "10:05", "10:10", "10:15"],
    "datasets": [
      {
        "label": "请求数",
        "data": [120, 150, 130, 180],
        "borderColor": "#3b82f6",
        "backgroundColor": "rgba(59, 130, 246, 0.1)"
      },
      {
        "label": "响应时间",
        "data": [120, 150, 130, 180],
        "borderColor": "#10b981",
        "backgroundColor": "rgba(16, 185, 129, 0.1)"
      }
    ]
  }
}
```

**响应状态码**:
- 200: 成功获取图表数据
- 400: 请求参数错误
- 500: 服务器内部错误

## 4. 攻击检测模块

### 4.1 获取检测结果

**接口地址**: `GET /attack-detection/results`

**接口描述**: 获取攻击检测结果

**请求参数**:

| 参数名 | 类型 | 必填 | 位置 | 描述 | 示例值 |
|--------|------|------|------|------|--------|
| page | integer | 否 | query | 页码 | 1 |
| limit | integer | 否 | query | 每页数量 | 20 |
| type | string | 否 | query | 攻击类型 | slowloris |
| status | string | 否 | query | 状态 | detected |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "total": 50,
    "list": [
      {
        "id": 1,
        "ip": "192.168.1.1",
        "type": "slowloris",
        "status": "detected",
        "severity": "high",
        "detected_at": "2026-03-14T10:00:00Z",
        "details": {
          "request_rate": 150,
          "connection_time": 30,
          "packet_size": 40
        }
      }
    ]
  }
}
```

**响应状态码**:
- 200: 成功获取检测结果
- 500: 服务器内部错误

### 4.2 获取攻击详情

**接口地址**: `GET /attack-detection/detail/{id}`

**接口描述**: 获取攻击详情

**路径参数**:

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| id | integer | 是 | 攻击ID | 1 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "id": 1,
    "ip": "192.168.1.1",
    "type": "slowloris",
    "status": "detected",
    "severity": "high",
    "detected_at": "2026-03-14T10:00:00Z",
    "resolved_at": null,
    "details": {
      "request_rate": 150,
      "connection_time": 30,
      "packet_size": 40,
      "user_agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
    },
    "actions": []
  }
}
```

**响应状态码**:
- 200: 成功获取攻击详情
- 404: 攻击不存在
- 500: 服务器内部错误

### 4.3 手动触发检测

**接口地址**: `POST /attack-detection/scan`

**接口描述**: 手动触发对指定IP的攻击检测

**请求体**:

```json
{
  "ip": "192.168.1.1",
  "duration": 60,
  "thresholds": {
    "request_rate": 100,
    "connection_time": 30,
    "packet_size": 40
  }
}
```

**请求参数说明**:

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| ip | string | 是 | IP地址 | 192.168.1.1 |
| duration | integer | 否 | 检测持续时间(秒) | 60 |
| thresholds | object | 否 | 检测阈值 | 见示例 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "scan_id": "scan_123456",
    "ip": "192.168.1.1",
    "status": "scanning",
    "started_at": "2026-03-14T10:00:00Z"
  },
  "message": "检测已开始"
}
```

**响应状态码**:
- 200: 检测开始成功
- 400: 请求参数错误
- 500: 服务器内部错误

## 5. 告警管理模块

### 5.1 获取告警列表

**接口地址**: `GET /alerts/list`

**接口描述**: 获取系统告警列表

**请求参数**:

| 参数名 | 类型 | 必填 | 位置 | 描述 | 示例值 |
|--------|------|------|------|------|--------|
| page | integer | 否 | query | 页码 | 1 |
| limit | integer | 否 | query | 每页数量 | 20 |
| severity | string | 否 | query | 严重程度 | high |
| status | string | 否 | query | 状态 | unhandled |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "total": 100,
    "list": [
      {
        "id": 1,
        "title": "Slowloris攻击检测",
        "severity": "high",
        "status": "unhandled",
        "ip": "192.168.1.1",
        "created_at": "2026-03-14T10:00:00Z",
        "message": "检测到来自192.168.1.1的Slowloris攻击"
      }
    ]
  }
}
```

**响应状态码**:
- 200: 成功获取告警列表
- 500: 服务器内部错误

### 5.2 获取告警详情

**接口地址**: `GET /alerts/detail/{id}`

**接口描述**: 获取告警详情

**路径参数**:

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| id | integer | 是 | 告警ID | 1 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "Slowloris攻击检测",
    "severity": "high",
    "status": "unhandled",
    "ip": "192.168.1.1",
    "created_at": "2026-03-14T10:00:00Z",
    "updated_at": "2026-03-14T10:00:00Z",
    "message": "检测到来自192.168.1.1的Slowloris攻击",
    "details": {
      "attack_type": "slowloris",
      "request_rate": 150,
      "connection_time": 30,
      "packet_size": 40
    },
    "actions": []
  }
}
```

**响应状态码**:
- 200: 成功获取告警详情
- 404: 告警不存在
- 500: 服务器内部错误

### 5.3 处理告警

**接口地址**: `PUT /alerts/handle/{id}`

**接口描述**: 处理告警，更新告警状态

**路径参数**:

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| id | integer | 是 | 告警ID | 1 |

**请求体**:

```json
{
  "status": "handled",
  "action": "block_ip",
  "notes": "已阻止该IP地址"
}
```

**请求参数说明**:

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| status | string | 是 | 告警状态 | handled |
| action | string | 否 | 采取的行动 | block_ip |
| notes | string | 否 | 处理备注 | 已阻止该IP地址 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "id": 1,
    "status": "handled",
    "updated_at": "2026-03-14T10:30:00Z"
  },
  "message": "告警处理成功"
}
```

**响应状态码**:
- 200: 告警处理成功
- 400: 请求参数错误
- 404: 告警不存在
- 500: 服务器内部错误

## 6. 模型预测模块

### 6.1 执行攻击预测

**接口地址**: `POST /predict`

**接口描述**: 根据IP地址和相关参数执行攻击预测

**请求体**:

```json
{
  "ip": "192.168.1.1",
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

**请求参数说明**:

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| ip | string | 是 | 目标IP地址 | 192.168.1.1 |
| timeRange | string | 是 | 预测时间范围 | 1h |
| model | string | 是 | 预测模型 | xgboost |
| features | object | 是 | 特征选择 | 见示例 |

**模型枚举值**:
- xgboost: XGBoost（推荐）
- random_forest: 随机森林
- lstm: LSTM

**响应示例**:

```json
{
  "success": true,
  "data": {
    "prediction": "攻击",
    "confidence": 0.95,
    "risk_score": 85.5,
    "feature_importance": {
      "request_rate": 0.45,
      "connection_time": 0.25,
      "user_agent": 0.20,
      "referrer": 0.10
    },
    "details": {
      "request_count": 1200,
      "average_response_time": 150.5,
      "anomaly_score": 0.85
    },
    "timestamp": "2026-03-14T10:00:00Z"
  }
}
```

**响应状态码**:
- 200: 预测成功
- 400: 请求参数错误
- 500: 服务器内部错误

### 6.2 获取预测历史记录

**接口地址**: `GET /prediction/history`

**接口描述**: 获取历史预测记录列表

**请求参数**:

| 参数名 | 类型 | 必填 | 位置 | 描述 | 示例值 |
|--------|------|------|------|------|--------|
| page | integer | 否 | query | 页码 | 1 |
| limit | integer | 否 | query | 每页数量 | 20 |
| ip | string | 否 | query | 过滤IP | 192.168.1.1 |
| prediction | string | 否 | query | 预测结果 | 攻击 |

**响应示例**:

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "timestamp": "2026-03-14T10:00:00Z",
      "ip": "192.168.1.1",
      "prediction": "攻击",
      "confidence": 0.95,
      "risk_score": 85.5
    },
    {
      "id": 2,
      "timestamp": "2026-03-14T09:30:00Z",
      "ip": "192.168.1.2",
      "prediction": "正常",
      "confidence": 0.88,
      "risk_score": 35.2
    }
  ]
}
```

**响应状态码**:
- 200: 成功获取历史记录
- 500: 服务器内部错误

## 7. 用户管理模块

### 7.1 获取用户列表

**接口地址**: `GET /users/list`

**接口描述**: 获取系统用户列表

**请求参数**:

| 参数名 | 类型 | 必填 | 位置 | 描述 | 示例值 |
|--------|------|------|------|------|--------|
| page | integer | 否 | query | 页码 | 1 |
| limit | integer | 否 | query | 每页数量 | 20 |
| role | string | 否 | query | 用户角色 | admin |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "total": 50,
    "list": [
      {
        "id": 1,
        "username": "admin",
        "email": "admin@example.com",
        "role": "admin",
        "status": "active",
        "created_at": "2026-03-01T00:00:00Z"
      }
    ]
  }
}
```

**响应状态码**:
- 200: 成功获取用户列表
- 500: 服务器内部错误

### 7.2 创建用户

**接口地址**: `POST /users/create`

**接口描述**: 创建新用户

**请求体**:

```json
{
  "username": "newuser",
  "password": "password123",
  "email": "newuser@example.com",
  "role": "user",
  "status": "active"
}
```

**请求参数说明**:

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| username | string | 是 | 用户名 | newuser |
| password | string | 是 | 密码 | password123 |
| email | string | 是 | 邮箱 | newuser@example.com |
| role | string | 是 | 用户角色 | user |
| status | string | 否 | 用户状态 | active |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "id": 51,
    "username": "newuser",
    "email": "newuser@example.com",
    "role": "user",
    "status": "active",
    "created_at": "2026-03-14T10:00:00Z"
  },
  "message": "用户创建成功"
}
```

**响应状态码**:
- 200: 用户创建成功
- 400: 请求参数错误
- 409: 用户名已存在
- 500: 服务器内部错误

### 7.3 更新用户信息

**接口地址**: `PUT /users/update/{id}`

**接口描述**: 更新用户信息

**路径参数**:

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| id | integer | 是 | 用户ID | 1 |

**请求体**:

```json
{
  "email": "admin@example.com",
  "role": "admin",
  "status": "active"
}
```

**请求参数说明**:

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| email | string | 否 | 邮箱 | admin@example.com |
| role | string | 否 | 用户角色 | admin |
| status | string | 否 | 用户状态 | active |
| password | string | 否 | 密码 | password123 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "role": "admin",
    "status": "active",
    "updated_at": "2026-03-14T10:00:00Z"
  },
  "message": "用户更新成功"
}
```

**响应状态码**:
- 200: 用户更新成功
- 400: 请求参数错误
- 404: 用户不存在
- 500: 服务器内部错误

### 7.4 删除用户

**接口地址**: `DELETE /users/delete/{id}`

**接口描述**: 删除用户

**路径参数**:

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| id | integer | 是 | 用户ID | 51 |

**响应示例**:

```json
{
  "success": true,
  "message": "用户删除成功"
}
```

**响应状态码**:
- 200: 用户删除成功
- 404: 用户不存在
- 500: 服务器内部错误

## 8. 系统配置模块

### 8.1 获取系统配置

**接口地址**: `GET /system/config`

**接口描述**: 获取系统配置信息

**响应示例**:

```json
{
  "success": true,
  "data": {
    "detection": {
      "enabled": true,
      "interval": 60,
      "thresholds": {
        "request_rate": 100,
        "connection_time": 30,
        "packet_size": 40
      }
    },
    "alert": {
      "enabled": true,
      "email_notification": true,
      "webhook": ""
    },
    "model": {
      "default_model": "xgboost",
      "retrain_interval": 24
    }
  }
}
```

**响应状态码**:
- 200: 成功获取系统配置
- 500: 服务器内部错误

### 8.2 更新系统配置

**接口地址**: `PUT /system/config`

**接口描述**: 更新系统配置信息

**请求体**:

```json
{
  "detection": {
    "enabled": true,
    "interval": 60,
    "thresholds": {
      "request_rate": 100,
      "connection_time": 30,
      "packet_size": 40
    }
  },
  "alert": {
    "enabled": true,
    "email_notification": true,
    "webhook": ""
  }
}
```

**响应示例**:

```json
{
  "success": true,
  "data": {
    "detection": {
      "enabled": true,
      "interval": 60,
      "thresholds": {
        "request_rate": 100,
        "connection_time": 30,
        "packet_size": 40
      }
    },
    "alert": {
      "enabled": true,
      "email_notification": true,
      "webhook": ""
    }
  },
  "message": "配置更新成功"
}
```

**响应状态码**:
- 200: 配置更新成功
- 400: 请求参数错误
- 500: 服务器内部错误

### 8.3 获取系统状态

**接口地址**: `GET /system/status`

**接口描述**: 获取系统状态信息

**响应示例**:

```json
{
  "success": true,
  "data": {
    "uptime": 86400,
    "cpu_usage": 25.5,
    "memory_usage": 60.2,
    "disk_usage": 45.8,
    "connections": 150,
    "detection_status": "running",
    "last_update": "2026-03-14T10:00:00Z"
  }
}
```

**响应状态码**:
- 200: 成功获取系统状态
- 500: 服务器内部错误

## 9. 数据报表模块

### 9.1 获取报表列表

**接口地址**: `GET /reports/list`

**接口描述**: 获取报表列表

**请求参数**:

| 参数名 | 类型 | 必填 | 位置 | 描述 | 示例值 |
|--------|------|------|------|------|--------|
| page | integer | 否 | query | 页码 | 1 |
| limit | integer | 否 | query | 每页数量 | 20 |
| type | string | 否 | query | 报表类型 | daily |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "total": 30,
    "list": [
      {
        "id": 1,
        "title": "每日攻击报告",
        "type": "daily",
        "status": "generated",
        "created_at": "2026-03-14T00:00:00Z",
        "url": "/reports/download/1"
      }
    ]
  }
}
```

**响应状态码**:
- 200: 成功获取报表列表
- 500: 服务器内部错误

### 9.2 生成报表

**接口地址**: `POST /reports/generate`

**接口描述**: 生成新报表

**请求体**:

```json
{
  "type": "daily",
  "timeRange": "24h",
  "format": "pdf",
  "parameters": {
    "include_attacks": true,
    "include_alerts": true,
    "include_traffic": true
  }
}
```

**请求参数说明**:

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| type | string | 是 | 报表类型 | daily |
| timeRange | string | 是 | 时间范围 | 24h |
| format | string | 是 | 报表格式 | pdf |
| parameters | object | 否 | 报表参数 | 见示例 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "report_id": 31,
    "status": "generating",
    "estimated_time": 30
  },
  "message": "报表生成已开始"
}
```

**响应状态码**:
- 200: 报表生成开始成功
- 400: 请求参数错误
- 500: 服务器内部错误

### 9.3 下载报表

**接口地址**: `GET /reports/download/{id}`

**接口描述**: 下载报表文件

**路径参数**:

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| id | integer | 是 | 报表ID | 1 |

**响应**: 报表文件（PDF、CSV等格式）

**响应状态码**:
- 200: 成功下载报表
- 404: 报表不存在
- 500: 服务器内部错误

## 10. 日志中心模块

### 10.1 获取日志列表

**接口地址**: `GET /logs/list`

**接口描述**: 获取系统日志列表

**请求参数**:

| 参数名 | 类型 | 必填 | 位置 | 描述 | 示例值 |
|--------|------|------|------|------|--------|
| page | integer | 否 | query | 页码 | 1 |
| limit | integer | 否 | query | 每页数量 | 50 |
| level | string | 否 | query | 日志级别 | error |
| module | string | 否 | query | 模块名称 | detection |
| start_time | string | 否 | query | 开始时间 | 2026-03-13T00:00:00Z |
| end_time | string | 否 | query | 结束时间 | 2026-03-14T23:59:59Z |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "total": 1000,
    "list": [
      {
        "id": 1,
        "timestamp": "2026-03-14T10:00:00Z",
        "level": "error",
        "module": "detection",
        "message": "攻击检测模块异常",
        "details": "无法连接到数据库"
      }
    ]
  }
}
```

**响应状态码**:
- 200: 成功获取日志列表
- 500: 服务器内部错误

### 10.2 搜索日志

**接口地址**: `POST /logs/search`

**接口描述**: 搜索系统日志

**请求体**:

```json
{
  "query": "攻击",
  "level": "error",
  "module": "detection",
  "start_time": "2026-03-13T00:00:00Z",
  "end_time": "2026-03-14T23:59:59Z",
  "page": 1,
  "limit": 50
}
```

**请求参数说明**:

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| query | string | 是 | 搜索关键词 | 攻击 |
| level | string | 否 | 日志级别 | error |
| module | string | 否 | 模块名称 | detection |
| start_time | string | 否 | 开始时间 | 2026-03-13T00:00:00Z |
| end_time | string | 否 | 结束时间 | 2026-03-14T23:59:59Z |
| page | integer | 否 | 页码 | 1 |
| limit | integer | 否 | 每页数量 | 50 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "total": 50,
    "list": [
      {
        "id": 1,
        "timestamp": "2026-03-14T10:00:00Z",
        "level": "error",
        "module": "detection",
        "message": "检测到攻击",
        "details": "来自192.168.1.1的Slowloris攻击"
      }
    ]
  }
}
```

**响应状态码**:
- 200: 搜索成功
- 400: 请求参数错误
- 500: 服务器内部错误

### 10.3 导出日志

**接口地址**: `GET /logs/export`

**接口描述**: 导出日志为CSV文件

**请求参数**:

| 参数名 | 类型 | 必填 | 位置 | 描述 | 示例值 |
|--------|------|------|------|------|--------|
| level | string | 否 | query | 日志级别 | error |
| module | string | 否 | query | 模块名称 | detection |
| start_time | string | 否 | query | 开始时间 | 2026-03-13T00:00:00Z |
| end_time | string | 否 | query | 结束时间 | 2026-03-14T23:59:59Z |

**响应**: CSV格式的日志文件

**响应状态码**:
- 200: 导出成功
- 500: 服务器内部错误

## 通用响应结构

所有API接口的响应都遵循以下通用结构：

```json
{
  "success": true,
  "data": {},
  "message": "操作成功"
}
```

**响应字段说明**:

| 字段名 | 类型 | 描述 |
|--------|------|------|
| success | boolean | 操作是否成功 |
| data | object/array | 响应数据 |
| message | string | 响应消息 |

**错误响应结构**:

```json
{
  "success": false,
  "data": null,
  "message": "操作失败"
}
```

## 认证方式

所有需要认证的接口都需要在请求头中添加以下认证信息：

```
Authorization: Bearer {token}
```

其中 `{token}` 是通过登录接口获取的认证令牌。

## 总结

本文档详细描述了Slowloris系统的完整API接口规范，包括所有模块的接口定义、请求参数、响应格式和状态码。这些接口为前端应用提供了完整的后端服务支持，确保系统能够正常运行和管理。

文档会随着系统的发展和功能的增加而不断更新，建议定期查看最新版本的文档以获取最准确的接口信息。