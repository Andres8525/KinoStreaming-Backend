# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S kino && adduser -S kino -G kino
COPY --from=build /app/target/kino-backend-*.jar app.jar
USER kino
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
