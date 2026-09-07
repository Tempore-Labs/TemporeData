#!/usr/bin/env bash
# temporedata - 开发一键运行（免打包热载）：后端 spring-boot:run(8080) + 前端 Vite dev(5174, 代理→8080)。
# 后端改类 → DevTools 自动重启；前端改 → HMR 即时。无 mvn package。
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

MVN_BIN="${MVN_BIN:-mvn}"

echo "==> 启动后端 (spring-boot:run @8080, DevTools 热重启)..."
"$MVN_BIN" -pl temporedata-server spring-boot:run &
BACK_PID=$!
trap 'echo; echo "停止后端 $BACK_PID..."; kill $BACK_PID 2>/dev/null' EXIT INT TERM

echo "==> 启动前端 (Vite dev @5174, 代理 /api→8080)..."
(cd temporedata-ui && npm install --no-audit --no-fund && npm run dev)