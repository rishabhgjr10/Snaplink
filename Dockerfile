# Stage 1: Build application with Maven and Eclipse Temurin 21
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build jar
COPY src ./src
# Tests run in CI (they need Docker for Testcontainers, which is not available inside this build)
RUN mvn clean package -DskipTests

# Stage 2: Minimal runtime image with Temurin 21 JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as an unprivileged user
RUN addgroup -S app && adduser -S app -G app

# Copy executable jar from builder stage
COPY --from=builder /app/target/*.jar app.jar
USER app

# Expose HTTP port
EXPOSE 8080

# Production JVM container flags for container memory management (Render / Cloud)
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
