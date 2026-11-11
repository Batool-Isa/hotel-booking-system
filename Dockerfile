# ===== Stage 1: build (has Maven + your code) =====
FROM maven:3.9-eclipse-temurin-17 AS build
ARG SERVICE
WORKDIR /workspace

COPY pom.xml .
COPY services/api-gateway/pom.xml          services/api-gateway/
COPY services/user-service/pom.xml         services/user-service/
COPY services/hotel-service/pom.xml        services/hotel-service/
COPY services/booking-service/pom.xml      services/booking-service/
COPY services/billing-service/pom.xml      services/billing-service/
COPY services/notification-service/pom.xml services/notification-service/

RUN mvn -B -q -pl services/${SERVICE} -am dependency:go-offline

COPY services/${SERVICE}/src/main services/${SERVICE}/src/main
RUN mvn -B -q -pl services/${SERVICE} -am package -Dmaven.test.skip=true

# ===== Stage 2: run (only Java + the jar) =====
FROM eclipse-temurin:17-jre-jammy
ARG SERVICE
WORKDIR /app
VOLUME /tmp
RUN useradd --system spring && mkdir -p logs uploads && chown -R spring /app

COPY --from=build /workspace/services/${SERVICE}/target/*.jar app.jar

USER spring
ENTRYPOINT ["java", "-jar", "app.jar"]
