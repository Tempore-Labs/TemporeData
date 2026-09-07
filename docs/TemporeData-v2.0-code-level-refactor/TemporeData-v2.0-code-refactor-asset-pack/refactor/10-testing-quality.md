# 10 测试与质量门禁

## 1. 测试层级

```text
Unit
 ↓
Integration
 ↓
Contract
 ↓
API
 ↓
UI
 ↓
E2E
```

## 2. Java

JUnit 5：

```text
Domain tests
Application tests
Repository tests
Security tests
Executor tests
```

Testcontainers：

```text
MySQL
Redis
```

## 3. ArchUnit

强制：

```text
Controller → Application allowed
Controller → Repository forbidden
Controller → Entity mutation forbidden
Domain → Controller forbidden
Domain → Spring MVC forbidden
```

## 4. API Contract

OpenAPI diff：

```text
breaking change → CI fail
```

## 5. Frontend

Playwright：

```text
login
dashboard
cluster detail
job create
job execution
incident acknowledge
runbook approval
lineage
permission denied
```

## 6. Security Gate

保留：

```text
SpotBugs
OWASP Dependency Check
CycloneDX SBOM
SARIF
```

新增：

```text
Checkstyle
ArchUnit
OpenAPI diff
npm audit / lockfile validation
```

## 7. 性能基线

目标：

```text
普通列表 API p95 < 300ms
详情 API p95 < 500ms
Dashboard API p95 < 800ms
Command Palette search < 150ms
```

执行类 API 的响应只确认“accepted/queued”，不得同步等待长任务。
