FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY services/api-gateway/pom.xml          services/api-gateway/
COPY services/user-service/pom.xml         services/user-service/
COPY services/hotel-service/pom.xml        services/hotel-service/
COPY services/booking-service/pom.xml      services/booking-service/
COPY services/billing-service/pom.xml      services/billing-service/
COPY services/notification-service/pom.xml services/notification-service/
RUN mvn -B -q -pl services/booking-service -am dependency:go-offline

COPY services/booking-service/src/main services/booking-service/src/main
RUN mvn -B -q -pl services/booking-service -am package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN useradd --system spring && mkdir -p logs uploads && chown -R spring /app
COPY --from=build /workspace/services/booking-service/target/*.jar app.jar
USER spring

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
