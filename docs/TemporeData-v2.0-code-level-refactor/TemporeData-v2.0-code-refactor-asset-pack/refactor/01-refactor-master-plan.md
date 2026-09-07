# 01 总体代码级重构方案

## 1. 目标架构

```text
Vue3 UI
  │
  ├── Feature Modules
  │     ├── cluster
  │     ├── compute
  │     ├── jobs
  │     ├── monitoring
  │     ├── logging
  │     ├── incidents
  │     ├── automation
  │     └── data
  │
  ▼
/api/v1/*
  │
  ▼
Spring Security / Tenant / Trace
  │
  ▼
Application Services
  │
  ├── Cluster
  ├── Compute
  ├── Job
  ├── Monitoring
  ├── Incident
  ├── Automation
  └── Data
  │
  ▼
Domain Services / SPI
  │
  ├── Collector SPI
  ├── Executor SPI
  ├── Datasource SPI
  └── Notification SPI
  │
  ▼
Infrastructure
  ├── MySQL
  ├── Redis(optional)
  ├── TSDB(optional)
  └── Object Storage(optional)
```

## 2. 模块策略

保留：

- `temporedata-bom`
- `temporedata-common`
- `temporedata-api`
- `temporedata-support`
- `temporedata-security`
- `temporedata-service`
- `temporedata-datasource-plugin`
- `temporedata-server`
- `temporedata-ui`

不建议为了“看起来微服务化”拆成多个 Spring Boot 服务。当前产品定位是轻量级控制平面，模块化单体更容易部署、调试和交付。

## 3. service 内部新领域

```text
org.temporedata.service
├── system
├── cluster
├── compute
├── job
├── monitoring
├── logging
├── incident
├── automation
└── data
```

每个领域统一：

```text
controller/
application/
domain/
repository/
dto/
mapper/
executor/
event/
```

Controller 不允许直接访问 Repository；Domain 不依赖 Spring MVC。

## 4. 重构原则

### 4.1 API 先行
先定义 `/api/v1` DTO，再移动内部实现，避免前后端同时大规模改名导致不可控。

### 4.2 Anti-Corruption Layer
旧包作为兼容层：

```text
legacy API → Adapter → New Application Service
legacy DB  → Mapper/View → New Domain
```

### 4.3 领域事件
跨域通知通过事件，不直接调用内部实现：

```text
JobFailedEvent
NodeDownEvent
AlertTriggeredEvent
IncidentCreatedEvent
RunbookExecutionFinishedEvent
```

### 4.4 执行与控制分离
控制面负责“决定做什么”；Executor 负责“在哪里、以什么方式执行”。

## 5. Definition of Done

每个新领域必须同时满足：

- API v1
- DTO/Entity 分离
- Repository 接口
- Service/Application Service
- Domain 单元测试
- Integration Test
- Permission Test
- Audit Test
- API Contract Test
- 前端 Feature Module
- Flyway migration
- Metrics/Trace
- 错误码
