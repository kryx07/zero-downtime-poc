# ==========================================
# Stage 1: The Builder
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# 1. Copy necessary modules for the WAR build
COPY pom.xml .
COPY smart-jdbc-library ./smart-jdbc-library
COPY app1-websphere ./app1-websphere

# 2. Build the library and the WebSphere app
RUN mvn clean install -DskipTests -pl smart-jdbc-library,app1-websphere -am

# ==========================================
# Stage 2: The Runtime (WebSphere Liberty)
# ==========================================
FROM icr.io/appcafe/websphere-liberty:kernel-java17-openj9-ubi
WORKDIR /config

# Copy the WAR from the builder stage to the dropins folder
# We use --chown=1001:0 because Liberty is picky about its user
COPY --from=builder --chown=1001:0 /app/app1-websphere/target/app1-websphere-1.0-SNAPSHOT.war /config/dropins/

# Liberty's configuration script to optimize the server
RUN configure.sh