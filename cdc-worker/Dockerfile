# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Maven needs to see the skeleton of the ENTIRE project to resolve dependencies
COPY pom.xml .
COPY smart-jdbc-library/pom.xml smart-jdbc-library/
COPY cdc-worker/pom.xml cdc-worker/
COPY app1-websphere/pom.xml app1-websphere/
COPY app2-springboot/pom.xml app2-springboot/

# Now it will work
RUN mvn dependency:go-offline -B

# Copy actual source code
COPY smart-jdbc-library ./smart-jdbc-library
COPY cdc-worker ./cdc-worker

RUN mvn clean package -DskipTests -pl cdc-worker -am

# Stage 2: Run
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/cdc-worker/target/cdc-worker-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]