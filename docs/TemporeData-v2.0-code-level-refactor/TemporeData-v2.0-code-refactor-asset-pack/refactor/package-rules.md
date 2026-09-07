# Java 包与依赖规则

## Allowed

```text
controller → application
application → domain
application → repository
application → event
repository implementation → domain
infrastructure → domain/api
```

## Forbidden

```text
controller → repository
controller → JPA Entity
domain → controller
domain → repository implementation
domain → Spring MVC
feature A → feature B internal implementation
```

## Cross-domain calls

优先：

```text
Application Service
Domain Event
Published Contract
```

禁止直接调用另一个域的 Repository。

## API module

`temporedata-api` 只放稳定契约和 SPI，不放 JPA Entity、Spring Service 实现。
