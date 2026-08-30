# ── Stage 1: Build ────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy Maven wrapper và pom trước để tận dụng Docker layer cache
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw

# Download dependencies trước
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build application
RUN ./mvnw clean package -B -DskipTests


# ── Stage 2: Runtime ──────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Upgrade base OS packages to fix Trivy CRITICAL vulnerabilities
RUN apk update && apk upgrade --no-cache

# Chạy application bằng non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Copy JAR từ build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]