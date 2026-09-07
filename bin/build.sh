#!/usr/bin/env bash
# 轻舟云 temporedata - one-command build producing a single deployable jar.
# Result: temporedata-server/target/temporedata.jar (backend + bundled frontend SPA).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

MVN_BIN="${MVN_BIN:-mvn}"
STATIC_DIR="temporedata-server/src/main/resources/static"
OUT_JAR="${OUT_JAR:-temporedata-server/target/temporedata.jar}"

echo "==> [1/3] Building frontend (Vue3)..."
(cd temporedata-ui && npm install --no-audit --no-fund && npm run build)
# 前端构建产物在 temporedata-ui/dist（运行模式重设计 §4.2）；发布时把它内嵌进 jar 静态。
echo "    bundling dist into ${STATIC_DIR}"
rm -rf "${STATIC_DIR}" && mkdir -p "${STATIC_DIR}" && cp -R temporedata-ui/dist/. "${STATIC_DIR}/"
echo "    frontend bundled into ${STATIC_DIR}"

echo "==> [2/3] Building backend fat jar (Maven)..."
"$MVN_BIN" -pl temporedata-server -am package -DskipTests -q

echo "==> [3/3] Artifact ready at ${OUT_JAR}"
ls -lh "$OUT_JAR"

echo ""
echo "==> Done. Run with:  java -jar ${OUT_JAR}"
echo "    Web UI:  http://localhost:8080     (default login: admin / admin123)"