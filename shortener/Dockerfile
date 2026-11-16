# Stage 1: Build the application
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./

RUN ./mvnw -q dependency:go-offline

COPY src src

RUN ./mvnw -q package -DskipTests


# Stage 2: Run the application
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd -r app

COPY --from=build /app/target/*.jar app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]