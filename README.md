
Based on the provided code map, I can see this is a comprehensive **Slowloris Attack Detection System** - a microservices-based security monitoring platform. Let me create a detailed README.md file.

# Slowloris_Detecter

Slowloris 攻击检测系统 - 基于微服务架构的分布式 Web 安全监控系统

## 项目简介

Slowloris_Detecter 是一个用于检测和防御 Slowloris 攻击的分布式系统。Slowloris 是一种通过建立大量不完整的 HTTP 连接来耗尽服务器资源的拒绝服务 (DoS) 攻击方式。

本系统采用 **Spring Cloud** 微服务架构，集成了 AI 智能检测、流量分析、实时告警等功能，为 Web 应用提供全方位的安全防护。

## 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         Nginx (反向代理)                          │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Gateway Service (API 网关)                     │
│              JWT 认证 │ 流量收集 │ 路由转发                       │
└─────────────────────────────────────────────────────────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          ▼                       ▼                       ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│  Auth Service  │   │ Monitor Service │   │ System Service │
│   (用户认证)    │   │   (监控检测)     │   │  (系统配置)     │
└─────────────────┘   └─────────────────┘   └─────────────────┘
```

## 技术栈

- **后端框架**: Spring Boot, Spring Cloud
- **数据库**: MySQL + MyBatis Plus
- **缓存**: Redis
- **AI 引擎**: LangChain4J + DeepSeek API
- **实时通信**: WebSocket (STOMP)
- **网关**: Spring Cloud Gateway
- **服务注册**: Nacos (隐式)
- **容器化**: Docker + Docker Compose

## 核心功能

### 🔐 用户认证模块
- 用户注册与登录
- JWT 令牌认证
- Token 刷新机制
- 用户权限管理

### 🌐 IP 监控模块
- 实时 IP 访问监控
- IP 地理位置识别
- 访问频率统计
- IP 黑名单/白名单管理

### 📊 流量分析模块
- 实时流量统计
- 流量趋势图表
- 按时间段/IP 段分析
- CSV 数据导出

### 🛡️ 攻击检测模块
- **AI 智能检测**: 基于 DeepSeek 大模型分析攻击特征
- **多维度检测**: 连接数、请求速率、数据包大小、会话时长等
- **风险评分**: 自动计算风险等级
- **攻击阻断**: 手动/自动封禁攻击源

### 🤖 模型预测模块
- 攻击趋势预测
- 历史预测记录查询
- 风险预警

### 🔔 告警管理
- 实时告警推送 (WebSocket)
- 告警列表与详情
- 告警处理状态跟踪

### ⚙️ 系统配置
- 动态配置管理
- 系统状态监控
- 配置缓存刷新

## 服务模块

| 服务名称 | 端口 | 说明 |
|---------|------|------|
| gateway-service | 8080 | API 网关 |
| auth-service | 8081 | 认证服务 |
| monitor-service | 8082 | 监控服务 |
| system-service | 8083 | 系统配置服务 |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- Docker & Docker Compose
- MySQL 8.0+
- Redis 6.0+

### 构建项目

```bash
# 克隆项目
git clone https://gitee.com/Liebesarmband/Slowloris_Detecter.git
cd Slowloris_Detecter

# 构建所有模块
mvn clean package -DskipTests
```

### 使用 Docker Compose 启动

```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps
```

### 手动启动

1. **配置数据库**
   
   执行 `init.sql` 初始化数据库表结构。

2. **配置 Redis**
   
   确保 Redis 服务正常运行。

3. **修改配置**
   
   根据需要修改各服务的 `application.yml` 配置文件。

4. **启动服务**
   
   按顺序启动各个服务模块。

## API 接口

### 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /login | 用户登录 |
| POST | /register | 用户注册 |
| GET | /verify | 验证 Token |

### IP 监控接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /ip-monitor/list | 获取 IP 列表 |
| GET | /ip-monitor/detail/{ip} | 获取 IP 详情 |
| POST | /ip-monitor/add | 添加 IP 监控 |

### 流量分析接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /traffic | 获取流量数据 |
| GET | /traffic/stats | 获取流量统计 |
| GET | /chart | 获取图表数据 |
| GET | /traffic/export | 导出流量数据 |

### 攻击检测接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /attacks | 获取攻击列表 |
| POST | /attacks/block | 阻止攻击 |
| POST | /ai-attack-detection/detect | AI 攻击检测 |
| POST | /attack-detection/scan | 触发扫描 |

### 模型预测接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /predict | 执行攻击预测 |
| GET | /prediction/history | 获取预测历史 |

> 详细 API 文档请参考 [API_DOC.md](./API_DOC.md) 或 [complete-api-documentation.md](./complete-api-documentation.md)

## WebSocket 实时推送

系统支持 WebSocket 实时推送攻击告警和预测结果：

```javascript
// 连接 WebSocket
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

// 订阅告警主题
stompClient.subscribe('/topic/alerts', function(message) {
    const alert = JSON.parse(message.body);
    console.log('收到告警:', alert);
});

// 订阅攻击主题
stompClient.subscribe('/topic/attacks', function(message) {
    const attack = JSON.parse(message.body);
    console.log('检测到攻击:', attack);
});
```

## 配置说明

### JWT 配置

```yaml
jwt:
  secret: your-jwt-secret-key
  expiration: 3600        # 访问令牌有效期(秒)
  refresh-expiration: 86400 # 刷新令牌有效期(秒)
```

### DeepSeek AI 配置

```yaml
deepseek:
  api:
    key: your-api-key
    model: deepseek-chat
    base-url: https://api.deepseek.com/v1
    temperature: 0.1
```

### Redis 配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your-password
```

## 目录结构

```
Slowloris_Detecter/
├── auth-service/           # 认证服务
│   ├── src/main/java/
│   │   └── com/slowloris/auth/
│   │       ├── controller/   # 控制器
│   │       ├── service/      # 业务逻辑
│   │       ├── mapper/       # 数据访问
│   │       ├── entity/       # 实体类
│   │       ├── config/       # 配置类
│   │       └── vo/           # 视图对象
│   └── pom.xml
│
├── gateway-service/        # 网关服务
│   ├── src/main/java/
│   │   └── com/slowloris/gateway/
│   │       ├── config/       # 配置
│   │       └── filter/        # 过滤器
│   └── pom.xml
│
├── monitor-service/        # 监控服务
│   ├── src/main/java/
│   │   └── com/slowloris/monitor/
│   │       ├── controller/   # 控制器
│   │       ├── service/      # 业务逻辑
│   │       ├── mapper/       # 数据访问
│   │       ├── entity/       # 实体类
│   │       ├── config/       # 配置
│   │       ├── client/       # Feign 客户端
│   │       └── vo/           # 视图对象
│   └── pom.xml
│
├── system-service/         # 系统配置服务
│   ├── src/main/java/
│   │   └── com/slowloris/system/
│   │       ├── controller/
│   │       ├── service/
│   │       ├── mapper/
│   │       └── entity/
│   └── pom.xml
│
├── slowloris-api/          # 公共 API 模块
│   └── src/main/java/com/slowloris/
│       ├── common/          # 通用类
│       ├── config/          # 配置
│       └── utils/           # 工具类
│
├── nginx/                  # Nginx 配置
│   └── nginx.conf
│
├── docker-compose.yml      # Docker Compose 配置
├── init.sql               # 数据库初始化脚本
└── pom.xml                # 父 POM
```

## 安全特性

- ✅ JWT Token 认证
- ✅ 密码 BCrypt 加密存储
- ✅ SQL 注入防护
- ✅ XSS 攻击防护
- ✅ 请求频率限制
- ✅ IP 黑名单机制
- ✅ AI 智能异常检测

## 扩展开发

### 添加新的检测规则

在 `AttackDetectionServiceImpl` 中扩展 `detectAttack` 方法：

```java
@Override
public AttackDetectionResult detectAttack(AttackDetectionRequest request) {
    // 添加自定义检测逻辑
    // ...
}
```

### 集成其他 AI 模型

修改 `LangChain4JConfig` 配置类，替换为其他支持的 AI 模型。

## 许可证

本项目仅供学习和技术交流使用。

## 贡献指南

欢迎提交 Issue 和 Pull Request！

## 联系方式

- 项目地址: https://gitee.com/Liebesarmband/Slowloris_Detecter