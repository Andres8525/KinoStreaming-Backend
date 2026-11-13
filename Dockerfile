# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Download dependencies first so they are cached between builds when only the code changes.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S kino && adduser -S kino -G kino
COPY --from=build /app/target/kino-backend-*.jar app.jar
USER kino

# Render injects PORT (10000 by default); Spring reads it through server.port=${PORT:8081}.
ENV PORT=8081
# Sized for small containers (Render free plan: 512 MB). Override JAVA_OPTS from the dashboard if needed.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k -XX:+ExitOnOutOfMemoryError"
EXPOSE 8081

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
