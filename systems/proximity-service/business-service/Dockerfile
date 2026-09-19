# ============================================================
# Stage 1: Build
# ============================================================
FROM maven:3.9.11-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom first for better Docker layer caching
COPY pom.xml .

RUN mvn dependency:go-offline -B

# Copy source
COPY src ./src

# Build application
RUN mvn clean package -DskipTests -B


# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Create non-root user
RUN useradd \
        --system \
        --uid 1001 \
        --gid 0 \
        spring

COPY --from=builder \
     /app/target/*.jar \
     /app/app.jar

RUN chown -R spring:root /app

USER spring

EXPOSE 8080

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]