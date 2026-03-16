# Slowloris 后端微服务项目

## 项目架构

本项目采用 Spring Boot 3.0 + Spring Cloud Alibaba 架构，基于 JDK 17 开发，包含以下微服务：

1. **gateway-service** - API 网关服务（端口：8080）
2. **auth-service** - 认证服务（端口：8081）
3. **monitor-service** - 监控服务（端口：8082）
4. **alert-service** - 告警服务（端口：8083）
5. **log-service** - 日志服务（端口：8084）
6. **report-service** - 报告服务（端口：8085）
7. **system-service** - 系统服务（端口：8086）

## 技术栈

- Spring Boot 3.0
- Spring Cloud Alibaba
- MyBatis-Plus
- Redis
- MySQL
- JDK 17

## 环境准备

1. 安装 JDK 17
2. 安装 MySQL 8.0
3. 安装 Redis 6.0+
4. 安装 Nacos 2.0+（用于服务注册与发现）

## 数据库配置

1. 创建数据库 `slowloris`
2. 用户名：root
3. 密码：Root@123456
4. 数据库连接 URL：`jdbc:mysql://localhost:3306/slowloris?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai`

## 启动步骤

1. 启动 Nacos 服务
2. 启动 Redis 服务
3. 启动 MySQL 服务
4. 依次启动各个微服务：
   ```bash
   # 在项目根目录下执行
   mvn clean install
   
   # 启动网关服务
   cd gateway-service
   mvn spring-boot:run
   
   # 启动认证服务
   cd ../auth-service
   mvn spring-boot:run
   
   # 启动监控服务
   cd ../monitor-service
   mvn spring-boot:run
   
   # 启动告警服务
   cd ../alert-service
   mvn spring-boot:run
   
   # 启动日志服务
   cd ../log-service
   mvn spring-boot:run
   
   # 启动报告服务
   cd ../report-service
   mvn spring-boot:run
   
   # 启动系统服务
   cd ../system-service
   mvn spring-boot:run
   ```

## 服务说明

### 1. 网关服务（gateway-service）

- 负责路由转发
- JWT 令牌验证
- 请求拦截和过滤

### 2. 认证服务（auth-service）

- 用户注册
- 用户登录
- JWT 令牌生成

### 3. 监控服务（monitor-service）

- IP 监控
- 流量分析
- 访问统计

### 4. 告警服务（alert-service）

- 告警生成
- 告警管理
- 告警统计

### 5. 日志服务（log-service）

- 操作日志
- 访问日志
- 错误日志

### 6. 报告服务（report-service）

- 报告生成
- 报告管理
- 报告统计

### 7. 系统服务（system-service）

- 系统配置

## API 文档

所有 API 都通过网关服务访问，路径格式为：
```
http://localhost:8080/api/{service-name}/{api-path}
```

### 认证服务 API

- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录

### 监控服务 API

- `GET /api/monitor/ip-monitor/info` - 获取 IP 监控信息
- `GET /api/monitor/ip-monitor/abnormal` - 获取异常 IP 列表
- `GET /api/monitor/ip-monitor/traffic-analysis` - 获取流量分析数据

### 告警服务 API

- `POST /api/alerts` - 创建告警
- `GET /api/alerts` - 获取告警列表
- `GET /api/alerts/unprocessed-count` - 获取未处理告警数量
- `PUT /api/alerts/{id}/status` - 更新告警状态

### 日志服务 API

- `POST /api/logs` - 保存日志
- `GET /api/logs` - 获取日志列表
- `GET /api/logs/recent` - 获取最近日志

### 报告服务 API

- `POST /api/reports` - 生成报告
- `GET /api/reports` - 获取报告列表
- `GET /api/reports/{id}` - 获取报告详情

### 系统服务 API

- `GET /api/system/config` - 获取所有配置
- `GET /api/system/config/map` - 获取配置映射
- `PUT /api/system/config/{id}` - 更新配置

## 注意事项

1. 确保所有服务的依赖都已正确安装
2. 确保 Nacos、Redis 和 MySQL 服务都已启动
3. 首次启动时，数据库表会自动创建（如果配置了 MyBatis-Plus 的自动建表功能）
4. 每个服务的配置文件都可以在 `src/main/resources/application.yml` 中修改

## 开发环境配置

- IDE：推荐使用 IntelliJ IDEA
- JDK：17
- Maven：3.8.6+
- Git：2.30.0+

## 部署建议

1. 使用 Docker 容器化部署
2. 使用 Kubernetes 进行服务编排
3. 使用 Prometheus 和 Grafana 进行监控
4. 使用 ELK 或 Loki 进行日志管理

## 联系方式

如有问题，请联系项目负责人。
