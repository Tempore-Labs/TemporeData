# 13 Backend Class Blueprint

## Cluster

```text
ClusterController
ClusterApplicationService
ClusterDomainService
ClusterRepository
ClusterMapper
ClusterCollector
ClusterHealthService
NodeApplicationService
ServiceApplicationService
```

核心方法：

```text
create()
update()
delete()
get()
list()
refreshHealth()
collectSnapshot()
```

## Job

```text
JobController
JobApplicationService
JobExecutionApplicationService
JobScheduler
JobDomainService
JobRepository
JobExecutionRepository
JobExecutorRegistry
JobExecutionStateMachine
JobMapper
```

核心方法：

```text
create()
validate()
enable()
disable()
run()
cancel()
retry()
getExecution()
listExecutions()
```

## Monitoring

```text
MetricController
MetricQueryService
MetricAggregationService
HealthService
EventService
AlertRuleService
```

## Incident

```text
AlertRuleService
AlertEventService
IncidentService
IncidentCorrelationService
IncidentRcaService
NotificationService
```

## Automation

```text
RunbookService
ActionPlanService
PolicyEngine
ApprovalService
ExecutionService
ActionAuditService
```

## Data

```text
DatasourceService
MetadataService
CatalogService
LineageService
QualityService
DataSecurityService
```

## 横切组件

```text
TraceContext
TenantContext
PermissionEvaluator
AuditService
IdempotencyService
DistributedLockService
CacheService
EventPublisher
```
