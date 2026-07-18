FROM eclipse-temurin:25-jdk-noble AS build

WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Just download dependencies
RUN ./mvnw dependency:go-offline

COPY src src

RUN ./mvnw clean package

FROM eclipse-temurin:25-jre-noble AS runtime

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]