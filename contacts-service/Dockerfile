FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY pom.xml ./
COPY contacts-service/pom.xml contacts-service/
COPY weather-service/pom.xml weather-service/
RUN mvn -pl contacts-service -am dependency:go-offline

COPY . .
RUN mvn -pl contacts-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=prod
COPY --from=builder /workspace/contacts-service/target/contacts-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
