# 轻舟云 temporedata - multi-stage build producing a single lightweight JRE image.
# Stage 1: build the Vue3 SPA.
FROM node:20-alpine AS frontend
WORKDIR /app/temporedata-ui
COPY temporedata-ui/package*.json ./
RUN npm install --no-audit --no-fund
COPY temporedata-ui/ ./
RUN npm run build

# Stage 2: build the Spring Boot fat jar with the SPA bundled as static resources.
FROM maven:3.8-openjdk-11 AS backend
WORKDIR /app
COPY pom.xml ./
COPY temporedata-common/pom.xml ./temporedata-common/
COPY temporedata-api/   ./temporedata-api/
COPY temporedata-support/ ./temporedata-support/
COPY temporedata-security/ ./temporedata-security/
COPY temporedata-service/ ./temporedata-service/
COPY temporedata-server/pom.xml ./temporedata-server/
COPY temporedata-common/src ./temporedata-common/src
COPY temporedata-api/src ./temporedata-api/src
COPY temporedata-support/src ./temporedata-support/src
COPY temporedata-security/src ./temporedata-security/src
COPY temporedata-service/src ./temporedata-service/src
COPY temporedata-server/src ./temporedata-server/src
# bundle the SPA into the server static dir
COPY --from=frontend /app/temporedata-ui/dist/ ./temporedata-server/src/main/resources/static/
RUN mvn -pl temporedata-server -am package -DskipTests -q

# Stage 3: minimal runtime image.
FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=backend /app/temporedata-server/target/temporedata.jar app.jar
ENV QZ_JWT_SECRET=change-me-in-production
VOLUME /app/data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]