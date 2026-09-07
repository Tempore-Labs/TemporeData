# 贡献指南 (Contributing)

欢迎参与 `temporedata` 的开发。请阅读本文件以保持一致。

## 环境准备

- JDK 11+、Gradle、Node.js 20+
- MySQL 8（本地或 `docker compose`）
- 一键构建：`./tools/build.sh` 或根 `Makefile`

## 提交规范

采用 Conventional Commits：

```text
feat: 新功能
fix: 缺陷修复
docs: 文档
refactor: 重构
test: 测试
chore: 构建/工具
ci: CI 配置
```

示例：`fix(auth): correct 401 redirect to hash login`

## 分支模型

- `main`：稳定分支
- `feat/*`、`fix/*`：功能/修复分支，通过 PR 合入
- `release/*`：发版分支

## Pull Request 流程

1. 从最新 `main` 开分支；
2. 完成后跑通：后端 `mvn -pl temporedata-server -am test`、前端 `npm run build`；
3. 提交 PR，按模板填写，关联 Issue；
4. 评审通过、CI 绿后合入。

## 代码约定

- Java 包名保持 `org.temporedata` 不变；目录/模块名小写连字符。
- Vue 组件/页面 PascalCase；代码注释统一英文。
- 新增数据库变更必须走 `docs/.. / db/migration` 的 Flyway 迁移，不直接改表。
- 禁止将生产密码/JWT 密钥/加密密钥提交到仓库。