#!/usr/bin/env bash
# temporedata - 开发联调（真实构建 UI）：后端 spring-boot:run(8080) 从 file:temporedata-ui/dist 服务前端 +
# 前端 npm run build --watch（改前端自动重出 dist，后端按请求实时读取；改后端类 → DevTools 自动重启）。
# 全程无 mvn package。
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

MVN_BIN="${MVN_BIN:-mvn}"

echo "==> 启动后端 (spring-boot:run @8080, 静态源 file:temporedata-ui/dist, DevTools 热重启)..."
"$MVN_BIN" -pl temporedata-server spring-boot:run &
BACK_PID=$!
trap 'echo; echo "停止后端 $BACK_PID..."; kill $BACK_PID 2>/dev/null' EXIT INT TERM

echo "==> 前端增量构建 (vite build --watch → dist)..."
(cd temporedata-ui && npm install --no-audit --no-fund && npm run build -- --watch)