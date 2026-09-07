# 04 API 契约

## 1. URL 规范

```text
/api/v1/clusters
/api/v1/nodes
/api/v1/services
/api/v1/jobs
/api/v1/job-executions
/api/v1/metrics
/api/v1/logs
/api/v1/alerts
/api/v1/incidents
/api/v1/runbooks
/api/v1/runbook-executions
/api/v1/datasources
/api/v1/metadata
/api/v1/lineage
```

旧 `/api/lineage/*` 保留兼容入口。

## 2. 统一 Response

```java
public record ApiResponse<T>(
    boolean success,
    String code,
    String message,
    T data,
    String traceId
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", "success", data, TraceContext.id());
    }
}
```

分页：

```java
public record PageResponse<T>(
    List<T> items,
    long total,
    int page,
    int pageSize
) {}
```

## 3. HTTP 语义

```text
GET    查询
POST   创建 / 动作
PUT    完整更新
PATCH  局部更新
DELETE 删除
```

动作：

```text
POST /api/v1/jobs/{id}/actions/run
POST /api/v1/jobs/{id}/actions/stop
POST /api/v1/runbooks/{id}/execute
POST /api/v1/incidents/{id}/ack
POST /api/v1/incidents/{id}/resolve
```

## 4. 统一 Header

```text
Authorization: Bearer <JWT>
X-Tenant-Id: <tenant>
X-Request-Id: <request>
Idempotency-Key: <key>
```

## 5. OpenAPI 要求

所有 v1 Controller：

- `@Operation`
- `@ApiResponse`
- Request/Response schema
- 权限说明
- 错误码说明
- 示例请求/响应

OpenAPI 作为前端类型生成的单一契约来源。
