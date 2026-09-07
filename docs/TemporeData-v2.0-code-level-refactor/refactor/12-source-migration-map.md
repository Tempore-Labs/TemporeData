# 12 Source Migration Map

## Goal

将现有 `org.temporedata.service` 的旧业务包迁移到 v2.0 新领域，而不是一次性重写。

## Mapping

```text
sys/*              → service/system/*
ops/cluster/*      → service/cluster/*
ops/engine/*       → service/compute/*
dev/workflow/*     → service/job/*
dev/schedule/*     → service/job/*
ops/monitor/*      → service/monitoring/*
ops/log/*          → service/logging/*
ops/alarm/*        → service/incident/*
ops/automation/*  → service/automation/*
gov/*              → service/data/*
asset/*            → service/data/*
integration/*      → service/data/datasource/*
```

## Migration rule

每个旧 Service 首先建立 Adapter：

```text
LegacyController
      ↓
LegacyFacade
      ↓
NewApplicationService
      ↓
NewDomain
```

完成前端/API切换后，删除 LegacyController。

## Class rename strategy

不要直接批量 rename。

顺序：

1. 新建 interface
2. 新建 application service
3. 迁移 domain logic
4. 接入旧实现
5. 迁移 controller
6. 迁移 Vue API
7. 删除旧实现
