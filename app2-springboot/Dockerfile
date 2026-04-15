# ==========================================
# Stage 1: The Builder
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# 1. Copy the Parent POM and the Library first
COPY pom.xml .
COPY smart-jdbc-library ./smart-jdbc-library
COPY app2-springboot ./app2-springboot

# 2. Build the library AND the app
# We install the library to the local container repo so the app can see it
RUN mvn clean install -DskipTests -pl smart-jdbc-library,app2-springboot -am

# ==========================================
# Stage 2: The Runtime
# ==========================================
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Security: Don't run as root
RUN useradd -m appuser
USER appuser

# Copy the JAR from the app2 module target folder
COPY --from=builder /app/app2-springboot/target/app2-springboot-1.0-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]