# 伺"机"守护 — Slowloris 智能感知与决策系统

基于 Spring Cloud Alibaba 微服务架构的 Slowloris DDoS 攻击检测与自动处置系统，集成 DeepSeek AI 大模型，实现**感知 → 分析 → 决策 → 处置**的完整安全闭环。

## 系统架构

```
┌─────────────┐     ┌─────────────┐     ┌──────────────┐
│   浏览器      │────▶│   Nginx:80  │────▶│  Gateway:8080 │
└─────────────┘     └─────────────┘     └──────┬───────┘
                                                │
                         ┌──────────────────────┼──────────────────────┐
                         │                      │                      │
                   ┌─────▼─────┐        ┌──────▼──────┐       ┌──────▼──────┐
                   │Auth:8081  │        │Monitor:8082 │       │AlertLog:8083│
                   │ 认证授权   │        │  核心监控    │       │  告警日志    │
                   └───────────┘        └──────┬──────┘       └─────────────┘
                                               │
                                        ┌──────▼──────┐
                                        │   Nacos     │  注册中心 + 配置中心
                                        │   Redis     │  缓存 + 滑动窗口
                                        │   MySQL     │  持久化存储
                                        └─────────────┘
```

## 核心闭环

```
感知（数据采集）──▶ 分析（AI判断）──▶ 决策（风险评估）──▶ 处置（封禁/告警）
      ▲                                                         │
      └──────────────── 反馈验证（30秒循环）──────────────────────┘
```

### 三层数据采集

| 优先级 | 采集方式 | 说明 |
|--------|----------|------|
| 最高 | **Agent 探针** | 目标服务器上的 Bash 脚本，采集真实半开连接数、来源IP |
| 中等 | **SNMP** | 通过网络设备 SNMP v2c 协议获取 TCP 连接状态 |
| 兜底 | **网关流量** | Gateway 全量请求拦截，60秒滑动窗口聚合 |

### AI 驱动检测

- 调用 **DeepSeek API**（deepseek-chat）进行智能攻击判断
- System Prompt 内嵌 Slowloris 检测逻辑 + 反幻觉约束
- API 不可用时自动降级为本地规则检测

### 自动处置

- 支持自动/手动两种模式
- 软封禁（仅落库，不操作真实防火墙，可解封）
- 到期自动解封
- 所有决策通过 SSE 实时推送前端

## 技术栈

| 层级 | 技术 |
|------|------|
| 框架 | Spring Boot 3.0.9 + Spring Cloud 2022.0.3 + Spring Cloud Alibaba 2022.0.0.0 |
| 注册/配置 | Nacos 2.3.0 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7 |
| ORM | MyBatis-Plus 3.5.5 |
| 网关 | Spring Cloud Gateway（WebFlux 响应式） |
| 服务调用 | OpenFeign + LoadBalancer |
| 认证 | JJWT 0.12.3 + BCrypt |
| AI | DeepSeek API (deepseek-chat) |
| 实时推送 | SSE (Server-Sent Events) |
| 网络采集 | SNMP4J 3.8.1 |
| 容器化 | Docker + Docker Compose |
| JDK | 17 |

## 模块概览

| 模块 | 端口 | 说明 |
|------|------|------|
| `slowloris-api` | - | 共享 API 模块（JWT工具、统一响应、公共配置） |
| `gateway-service` | 8080 | API 网关（路由分发、JWT鉴权、流量采集） |
| `auth-service` | 8081 | 认证服务（登录注册、微信OAuth、用户管理） |
| `monitor-service` | 8082 | **核心监控服务**（数据采集、AI分析、决策处置、SSE推送） |
| `alert-log-service` | 8083 | 告警日志服务（告警CRUD、系统日志） |

## 快速开始

### 前置条件

- JDK 17+
- Maven 3.8+
- Docker & Docker Compose
- DeepSeek API Key（[获取地址](https://platform.deepseek.com/api_keys)）

### 1. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env，填入真实的密码和 DeepSeek API Key
```

### 2. 启动基础设施

```bash
docker-compose up -d mysql redis nacos
```

### 3. 构建项目

```bash
# Windows
build.bat

# Linux / macOS
mvn clean package -DskipTests
```

### 4. 启动所有服务

```bash
docker-compose up -d
```

### 5. 访问

- 前端页面：`http://localhost`
- API 网关：`http://localhost:8090`
- Nacos 控制台：`http://localhost:8848/nacos`（账号 `nacos`）

### 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | Admin@123456 | 管理员 |
| operator | Admin@123456 | 操作员 |
| viewer | Admin@123456 | 只读 |

## API 文档

详见 [API_DOC.md](./API_DOC.md)

## 部署目标服务器 Agent 探针

1. 在平台中创建监控目标后，点击"获取探针安装命令"
2. 在目标服务器上以 root 执行返回的一行命令：

```bash
curl -fsSL http://{平台地址}/internal/agent/install.sh | sudo bash -s -- \
  --server=http://{平台地址} --target={本机IP} --token={探针Token}
```

3. 验证探针状态：`systemctl status slowloris-agent`

## 数据库表

| 表名 | 说明 |
|------|------|
| `sys_user` | 系统用户 |
| `sys_log` | 系统日志 |
| `alert` | 告警记录 |
| `monitored_target` | 监控目标（含 Agent Token） |
| `metrics_snapshot` | 指标快照（每 30 秒） |
| `attack_detection` | 攻击检测/决策记录 |
| `blocked_ip` | 封禁 IP |
| `agent_config` | 自动处置配置 |
| `wx_user` | 微信用户 |
| `prediction_history` | AI 预测历史 |

## 安全提示

- `.env` 文件包含敏感信息，已通过 `.gitignore` 排除
- 生产环境务必使用 HTTPS，Agent Token 明文传输
- DeepSeek API Key 通过环境变量注入，不要在 YAML 中硬编码
- 定期轮换 JWT 签名密钥和数据库密码

## 项目结构

```
slowloris/
├── slowloris-api/            # 共享API模块
├── gateway-service/          # API网关
├── auth-service/             # 认证服务
├── monitor-service/          # 核心监控服务
│   └── src/main/resources/
│       └── agent/install.sh  # Agent探针安装脚本
├── alert-log-service/        # 告警日志服务
├── nginx/                    # Nginx配置
├── docker-compose.yml        # Docker编排
├── init.sql                  # 数据库初始化
├── nacos-init.sql            # Nacos数据库初始化
├── build.bat                 # Windows构建脚本
├── API_DOC.md                # API接口文档
└── .env.example              # 环境变量模板
```
