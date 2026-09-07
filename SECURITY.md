# 安全策略 (Security Policy)

## 支持的版本

仅对最新 `main` 与最新 `release/*` 提供安全修复。

## 报告漏洞

请勿在公开 Issue 中透露漏洞细节。请通过私有渠道（邮件 / Insiders 报告）向维护者**负责任披露（responsible disclosure）**：

- 描述受影响版本、复现步骤、影响范围；
- 我们会在确认后尽快修复并发布公告。

## 安全基线

- 生产环境必须通过环境变量覆盖 `QZ_JWT_SECRET`、`QZ_CRYPTO_KEY`、`DB_PASSWORD`。
- 默认账号 `admin/admin123` 部署后应立即修改。
- 数据库变更走 Flyway 迁移；敏感配置加密存储。