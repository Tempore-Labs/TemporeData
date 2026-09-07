# 20 Observability Contract

## Trace

每个请求：

```text
traceId
requestId
tenantId
operatorId
```

每个 Job Execution：

```text
executionId
jobId
clusterId
executorType
```

## Structured Log

```json
{
  "timestamp": "...",
  "level": "INFO",
  "service": "temporedata-server",
  "traceId": "...",
  "tenantId": "...",
  "resourceId": "...",
  "event": "JOB_EXECUTION_STARTED"
}
```

## Metrics

最低指标：

```text
temporedata_http_requests_total
temporedata_http_request_duration
temporedata_job_execution_total
temporedata_job_execution_duration
temporedata_cluster_health
temporedata_node_health
temporedata_incident_open
temporedata_executor_failure
temporedata_action_denied
```
