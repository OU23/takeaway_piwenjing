# ===== 构建阶段：容器内完成编译打包，不依赖本机 JDK/Maven =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 使用阿里云 Maven 镜像加速依赖下载
COPY docker-maven-settings.xml /root/.m2/settings.xml

# 先复制 pom 单独下载依赖，利用 Docker 层缓存
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

# ===== 运行阶段：仅携带 JRE 与 jar，镜像体积小 =====
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/xingguang-delivery.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
