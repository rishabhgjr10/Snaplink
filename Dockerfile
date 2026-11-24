# Stage 1: Build application with Maven and Eclipse Temurin 21
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Every mvn call below: batch mode, the Google Maven Central mirror (.mvn/settings.xml),
# and more patient retries when a repository answers 429/503 (rate limited / unavailable).
ENV MAVEN_ARGS="-B -s .mvn/settings.xml \
 -Daether.connector.http.retryHandler.count=8 \
 -Daether.connector.http.retryHandler.interval=10000 \
 -Daether.connector.http.retryHandler.serviceUnavailable=429,503"

# Cache dependencies. dependency:resolve fetches only what the app needs; go-offline also
# downloaded every optional/plugin dependency (hundreds of extra requests to rate-limit).
COPY .mvn/settings.xml .mvn/settings.xml
COPY pom.xml .
RUN mvn dependency:resolve

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
