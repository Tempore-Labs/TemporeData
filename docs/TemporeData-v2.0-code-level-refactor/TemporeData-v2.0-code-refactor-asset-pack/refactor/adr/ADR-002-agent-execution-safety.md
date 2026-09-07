# ADR-002 Agent 不直接执行

## Decision

Agent 输出 ActionPlan；Policy/Approval/Executor 决定是否以及如何执行。

## Reason

AI 输出具有不确定性。平台的控制权必须由确定性策略、权限和审计系统掌握。

## Required chain

Agent → ActionPlan → Policy → Permission → Approval → Executor → Audit
