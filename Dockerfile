# ── Stage 1: Build ────────────────────────────────────────
# Dùng image có sẵn Maven + JDK 21 để build project ra file .jar
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy mvnw và pom.xml trước để tận dụng Docker layer cache:
# nếu dependencies không đổi, bước download sẽ được cache lại,
# không cần tải lại mỗi lần build.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Copy toàn bộ source code và build
COPY src src
RUN ./mvnw clean package -B -DskipTests

# ── Stage 2: Runtime ──────────────────────────────────────
# Image nhỏ gọn hơn, chỉ chứa JRE (không cần Maven/JDK để chạy app)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Tạo user riêng để chạy app, không dùng root (bảo mật tốt hơn)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Copy file .jar đã build từ Stage 1 sang
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]