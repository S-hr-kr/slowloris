FROM openjdk:17-jre-slim
WORKDIR /app
COPY target/log-service-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
