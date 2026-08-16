# Build stage: Maven compiles the application into a JAR.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Runtime stage: only Java 21 is needed to run the JAR.
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/discovery-server-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]