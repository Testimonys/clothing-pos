# ============================================
# Stage 1: 构建阶段 — Maven 编译打包
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先复制 pom.xml，利用 Docker 缓存层加速
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# 复制源码并打包
COPY src/ src/
RUN mvn package -DskipTests -B -q

# ============================================
# Stage 2: 运行阶段 — 精简 JRE 镜像
# ============================================
FROM eclipse-temurin:17-jre
WORKDIR /app

# 安装 curl（健康检查用）
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 复制构建产物
COPY --from=build /build/target/clothing-pos-*.jar app.jar

EXPOSE 8080

# JVM 参数：堆内存 256m-512m，适合小店 2-3 人使用
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
