FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY archive-app/ archive-app/
COPY archive-web/public/ archive-web/public/
RUN mvn -f archive-app/pom.xml -pl api-web -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/archive-app/api-web/target/sfa-api.jar /app/sfa-api.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/sfa-api.jar"]
