# Slowloris_Detecter

Slowloris Attack Detection System - A microservices-based distributed web security monitoring platform

## Project Overview

Slowloris_Detecter is a distributed system designed to detect and defend against Slowloris attacks. Slowloris is a type of Denial of Service (DoS) attack that exhausts server resources by establishing a large number of incomplete HTTP connections.

This system employs a **Spring Cloud** microservices architecture, integrating AI-powered detection, traffic analysis, and real-time alerting to provide comprehensive security protection for web applications.

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Nginx (Reverse Proxy)                     │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Gateway Service (API Gateway)                  │
│              JWT Authentication │ Traffic Collection │ Routing   │
└─────────────────────────────────────────────────────────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          ▼                       ▼                       ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│  Auth Service   │   │ Monitor Service │   │ System Service  │
│  (Authentication) │   │   (Monitoring)  │   │  (System Config) │
└─────────────────┘   └─────────────────┘   └─────────────────┘
```

## Technology Stack

- **Backend Framework**: Spring Boot, Spring Cloud
- **Database**: MySQL + MyBatis Plus
- **Cache**: Redis
- **AI Engine**: LangChain4J + DeepSeek API
- **Real-time Communication**: WebSocket (STOMP)
- **Gateway**: Spring Cloud Gateway
- **Service Registry**: Nacos (implicit)
- **Containerization**: Docker + Docker Compose

## Core Features

### 🔐 Authentication Module
- User registration and login
- JWT token authentication
- Token refresh mechanism
- User permission management

### 🌐 IP Monitoring Module
- Real-time IP access monitoring
- IP geolocation identification
- Access frequency statistics
- IP blacklist/whitelist management

### 📊 Traffic Analysis Module
- Real-time traffic statistics
- Traffic trend charts
- Analysis by time period/IP range
- CSV data export

### 🛡️ Attack Detection Module
- **AI Intelligent Detection**: Analyzes attack patterns using the DeepSeek large model
- **Multi-dimensional Detection**: Connection count, request rate, packet size, session duration, etc.
- **Risk Scoring**: Automatically calculates risk levels
- **Attack Blocking**: Manual/automatic blocking of attack sources

### 🤖 Model Prediction Module
- Attack trend prediction
- Historical prediction record queries
- Risk alerts

### 🔔 Alert Management
- Real-time alert push (WebSocket)
- Alert list and details
- Alert handling status tracking

### ⚙️ System Configuration
- Dynamic configuration management
- System status monitoring
- Configuration cache refresh

## Service Modules

| Service Name | Port | Description |
|--------------|------|-------------|
| gateway-service | 8080 | API Gateway |
| auth-service | 8081 | Authentication Service |
| monitor-service | 8082 | Monitoring Service |
| system-service | 8083 | System Configuration Service |

## Quick Start

### Prerequisites

- JDK 17+
- Maven 3.8+
- Docker & Docker Compose
- MySQL 8.0+
- Redis 6.0+

### Build the Project

```bash
# Clone the project
git clone https://gitee.com/Liebesarmband/Slowloris_Detecter.git
cd Slowloris_Detecter

# Build all modules
mvn clean package -DskipTests
```

### Start with Docker Compose

```bash
# Start all services
docker-compose up -d

# Check service status
docker-compose ps
```

### Manual Startup

1. **Configure Database**
   
   Execute `init.sql` to initialize the database schema.

2. **Configure Redis**
   
   Ensure the Redis service is running.

3. **Modify Configuration**
   
   Update the `application.yml` files in each service as needed.

4. **Start Services**
   
   Start each service module in sequence.

## API Endpoints

### Authentication Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | /login | User login |
| POST | /register | User registration |
| GET | /verify | Validate Token |

### IP Monitoring Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | /ip-monitor/list | Get IP list |
| GET | /ip-monitor/detail/{ip} | Get IP details |
| POST | /ip-monitor/add | Add IP monitoring |

### Traffic Analysis Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | /traffic | Get traffic data |
| GET | /traffic/stats | Get traffic statistics |
| GET | /chart | Get chart data |
| GET | /traffic/export | Export traffic data |

### Attack Detection Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | /attacks | Get attack list |
| POST | /attacks/block | Block attack |
| POST | /ai-attack-detection/detect | AI attack detection |
| POST | /attack-detection/scan | Trigger scan |

### Model Prediction Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | /predict | Execute attack prediction |
| GET | /prediction/history | Get prediction history |

> For detailed API documentation, refer to [API_DOC.md](./API_DOC.md) or [complete-api-documentation.md](./complete-api-documentation.md)

## WebSocket Real-time Push

The system supports WebSocket real-time push of attack alerts and prediction results:

```javascript
// Connect to WebSocket
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

// Subscribe to alert topic
stompClient.subscribe('/topic/alerts', function(message) {
    const alert = JSON.parse(message.body);
    console.log('Received alert:', alert);
});

// Subscribe to attack topic
stompClient.subscribe('/topic/attacks', function(message) {
    const attack = JSON.parse(message.body);
    console.log('Attack detected:', attack);
});
```

## Configuration Details

### JWT Configuration

```yaml
jwt:
  secret: your-jwt-secret-key
  expiration: 3600        # Access token expiration (seconds)
  refresh-expiration: 86400 # Refresh token expiration (seconds)
```

### DeepSeek AI Configuration

```yaml
deepseek:
  api:
    key: your-api-key
    model: deepseek-chat
    base-url: https://api.deepseek.com/v1
    temperature: 0.1
```

### Redis Configuration

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your-password
```

## Directory Structure

```
Slowloris_Detecter/
├── auth-service/           # Authentication Service
│   ├── src/main/java/
│   │   └── com/slowloris/auth/
│   │       ├── controller/   # Controllers
│   │       ├── service/      # Business logic
│   │       ├── mapper/       # Data access
│   │       ├── entity/       # Entity classes
│   │       ├── config/       # Configuration classes
│   │       └── vo/           # View objects
│   └── pom.xml
│
├── gateway-service/        # Gateway Service
│   ├── src/main/java/
│   │   └── com/slowloris/gateway/
│   │       ├── config/       # Configuration
│   │       └── filter/       # Filters
│   └── pom.xml
│
├── monitor-service/        # Monitoring Service
│   ├── src/main/java/
│   │   └── com/slowloris/monitor/
│   │       ├── controller/   # Controllers
│   │       ├── service/      # Business logic
│   │       ├── mapper/       # Data access
│   │       ├── entity/       # Entity classes
│   │       ├── config/       # Configuration
│   │       ├── client/       # Feign clients
│   │       └── vo/           # View objects
│   └── pom.xml
│
├── system-service/         # System Configuration Service
│   ├── src/main/java/
│   │   └── com/slowloris/system/
│   │       ├── controller/
│   │       ├── service/
│   │       ├── mapper/
│   │       └── entity/
│   └── pom.xml
│
├── slowloris-api/          # Common API Module
│   └── src/main/java/com/slowloris/
│       ├── common/          # Utility classes
│       ├── config/          # Configuration
│       └── utils/           # Helper utilities
│
├── nginx/                  # Nginx configuration
│   └── nginx.conf
│
├── docker-compose.yml      # Docker Compose configuration
├── init.sql                # Database initialization script
└── pom.xml                 # Parent POM
```

## Security Features

- ✅ JWT Token Authentication
- ✅ Password encrypted with BCrypt
- ✅ SQL Injection Protection
- ✅ XSS Attack Prevention
- ✅ Request Rate Limiting
- ✅ IP Blacklist Mechanism
- ✅ AI-powered Anomaly Detection

## Extensibility

### Add New Detection Rules

Extend the `detectAttack` method in `AttackDetectionServiceImpl`:

```java
@Override
public AttackDetectionResult detectAttack(AttackDetectionRequest request) {
    // Add custom detection logic
    // ...
}
```

### Integrate Other AI Models

Modify the `LangChain4JConfig` configuration class to replace with other supported AI models.

## License

This project is intended solely for educational and technical exchange purposes.

## Contribution Guidelines

Issues and Pull Requests are welcome!

## Contact

- Project Repository: https://gitee.com/Liebesarmband/Slowloris_Detecter