# 19 Security Matrix

## Permission layers

```text
Tenant
  ↓
Organization
  ↓
Role
  ↓
Permission
  ↓
Resource Scope
  ↓
Action Risk
  ↓
Approval
```

## Action matrix

| Risk | Read | Permission | Approval | Audit |
|---|---:|---:|---:|---:|
| L0 | yes | required | no | yes |
| L1 | yes | required | policy | yes |
| L2 | yes | required | often | yes |
| L3 | yes | required | yes | yes |
| L4 | yes | required | multi-party | immutable |

所有资源查询必须进行 tenant scope 注入，避免出现“查得到但没有权限”的数据泄露。
