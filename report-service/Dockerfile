FROM openjdk:17-jre-slim
WORKDIR /app
COPY target/report-service-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
