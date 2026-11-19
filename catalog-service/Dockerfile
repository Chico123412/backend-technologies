FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/catalog-service-*.jar app.jar
EXPOSE 8080 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
