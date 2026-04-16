FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml .
COPY smart-jdbc-library/pom.xml smart-jdbc-library/
COPY cdc-worker/pom.xml cdc-worker/
COPY app1-websphere/pom.xml app1-websphere/
COPY app2-springboot/pom.xml app2-springboot/

RUN mvn dependency:go-offline -B

COPY smart-jdbc-library ./smart-jdbc-library
COPY app2-springboot ./app2-springboot

RUN mvn clean package -DskipTests -pl app2-springboot -am

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/app2-springboot/target/app2-springboot-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]