# temporedata 常用命令入口

.PHONY: build test run docker release clean dev dev-ui

## 一键构建（前端 + 后端 → dist/temporedata.jar）
build:
        bash bin/build.sh

## 后端模块测试
test:
        mvn -pl temporedata-service -am test -q

## 运行单包
run:
        java -jar dist/temporedata.jar

## 本地容器化部署（MySQL + 应用）
docker:
        docker compose up -d --build

## 发布（当前复用一键构建产物）
release: build

clean:
        rm -rf dist temporedata-server/target temporedata-*/target

## 开发（免打包热载）：后端 spring-boot:run + 前端 Vite dev(5174, 代理→8080)
dev:
        bash bin/dev.sh

## 开发（真实构建 UI 联调）：后端 spring-boot:run + 前端 dist watch（后端从 file:dist 读前端）
dev-ui:
        bash bin/dev-ui.sh