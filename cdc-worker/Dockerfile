# ==========================================
# Stage 1: The Builder
# ==========================================
# Use a Maven image that already includes the Temurin JDK
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Cache dependencies (The "Go-Offline" Trick)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: The Runtime (The Lean Image)
# ==========================================
# Switch to the JRE-only image to keep things tiny
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Non-root user for corporate-grade security
RUN useradd -m appuser
USER appuser

# Copy the JAR from the builder stage
COPY --from=builder /app/target/app2-springboot-1.0-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]