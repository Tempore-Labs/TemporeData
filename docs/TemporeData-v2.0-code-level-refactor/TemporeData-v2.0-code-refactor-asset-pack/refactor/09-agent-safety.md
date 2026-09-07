# 09 Ops Agent 安全架构

## 1. Agent 不拥有直接执行权限

错误：

```text
Agent → SSH → restart service
```

正确：

```text
Agent
 ↓
Diagnosis
 ↓
Action Plan
 ↓
Policy Engine
 ↓
Permission Check
 ↓
Approval（高风险）
 ↓
Executor
 ↓
Audit
```

## 2. Action 风险等级

```text
L0 READ
L1 LOW
L2 CHANGE
L3 HIGH_RISK
L4 DESTRUCTIVE
```

示例：

```text
查看 CPU             L0
清理临时文件         L1
重启 Job              L2
重启集群服务          L3
删除数据              L4
```

## 3. ActionPlan

```java
public record ActionPlan(
    String id,
    String incidentId,
    String actionType,
    String targetId,
    Map<String, Object> parameters,
    RiskLevel risk,
    String reason,
    List<Evidence> evidence
) {}
```

## 4. Policy Engine

策略判断：

```text
tenant policy
resource policy
operator permission
maintenance window
risk level
approval requirement
```

结果：

```text
ALLOW
ALLOW_WITH_APPROVAL
DENY
```

## 5. Audit

必须记录：

```text
who
what
why
target
parameters
before
after
approval
executor
result
traceId
timestamp
```

Agent 不能修改自己的审计记录。
