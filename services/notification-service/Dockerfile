FROM eclipse-temurin:17-jdk-jammy

# create temporary drive
VOLUME /tmp

# expose ports 8085 for spring boot
EXPOSE 8085

# create app dir
RUN mkdir -p /app/

# create logs dir
RUN mkdir -p /app/logs/

# cp notification-service-0.0.1-SNAPSHOT.jar /app/app.jar
ADD target/notification-service-0.0.1-SNAPSHOT.jar /app/app.jar

# run the .jar file with the active profile dev
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dspring.profiles.active=dev", "-jar", "/app/app.jar"]
