# 02 Java 后端代码级架构

## 1. 包结构

```text
org.temporedata
├── api
│   ├── contract
│   ├── datasource
│   ├── executor
│   └── response
├── common
├── security
├── support
└── service
    ├── system
    │   ├── controller
    │   ├── application
    │   ├── domain
    │   ├── repository
    │   ├── dto
    │   └── mapper
    ├── cluster
    ├── compute
    ├── job
    ├── monitoring
    ├── logging
    ├── incident
    ├── automation
    └── data
```

## 2. Controller 规则

Controller 只负责：

- 参数校验
- 权限声明
- 调用 Application Service
- Response 映射

禁止：

```java
repository.save(...)
entity.setX(...)
jdbcTemplate.query(...)
```

推荐：

```java
@PostMapping
@PreAuthorize("hasAuthority('job:create')")
public ApiResponse<JobRes> create(@Valid @RequestBody CreateJobReq req) {
    return ApiResponse.ok(jobApplicationService.create(req));
}
```

## 3. Application Service

负责事务和用例编排：

```java
@Service
@RequiredArgsConstructor
public class JobApplicationService {
    private final JobRepository jobRepository;
    private final JobValidator validator;
    private final AuditService auditService;

    @Transactional
    public JobRes create(CreateJobReq req) {
        validator.validate(req);
        Job job = Job.create(req.name(), req.type(), req.definition());
        jobRepository.save(job);
        auditService.record("JOB_CREATE", job.getId());
        return JobRes.from(job);
    }
}
```

## 4. Domain Entity

实体承载不变量：

```java
public class Job {
    public void enable() {
        if (status == JobStatus.DELETED) {
            throw new DomainException("JOB_DELETED", "Deleted job cannot be enabled");
        }
        status = JobStatus.ENABLED;
    }
}
```

禁止在 Entity 中注入 Repository、HttpClient、Spring Bean。

## 5. Repository

领域只依赖接口：

```java
public interface JobRepository {
    Optional<Job> findById(JobId id);
    Job save(Job job);
    void delete(JobId id);
}
```

JPA 实现放在 infrastructure/adapter 层。

## 6. DTO / Entity 分离

禁止直接：

```java
return repository.findAll();
```

必须：

```text
Entity → Mapper → Response DTO
Request DTO → Command → Domain
```

## 7. 统一错误码

```text
SYS_*
CLUSTER_*
COMPUTE_*
JOB_*
MONITOR_*
LOG_*
INCIDENT_*
AUTO_*
DATA_*
AUTH_*
```

示例：

```text
JOB_NOT_FOUND
JOB_EXECUTION_NOT_ALLOWED
JOB_EXECUTOR_UNAVAILABLE
AUTO_APPROVAL_REQUIRED
AUTO_ACTION_DENIED
CLUSTER_NODE_UNREACHABLE
```

## 8. 并发与幂等

所有执行类 API 支持：

```text
Idempotency-Key
requestId
traceId
tenantId
operatorId
```

执行状态使用：

```text
PENDING → QUEUED → RUNNING → SUCCESS
                         └──→ FAILED
                         └──→ CANCELLED
```

状态迁移由 Domain Service 控制，不允许 Controller 任意修改。
