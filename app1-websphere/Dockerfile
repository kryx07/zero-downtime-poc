FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml .
COPY smart-jdbc-library/pom.xml smart-jdbc-library/
COPY cdc-worker/pom.xml cdc-worker/
COPY app1-websphere/pom.xml app1-websphere/
COPY app2-springboot/pom.xml app2-springboot/

RUN mvn dependency:go-offline -B

COPY smart-jdbc-library ./smart-jdbc-library
COPY app1-websphere ./app1-websphere

RUN mvn clean package -DskipTests -pl app1-websphere -am

FROM icr.io/appcafe/websphere-liberty:kernel-java17-openj9-ubi
COPY --from=builder --chown=1001:0 /app/app1-websphere/target/app1-websphere-1.0-SNAPSHOT.war /config/dropins/
RUN configure.sh