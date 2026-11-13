FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests \
    && cp target/kino-backend-*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --destination extracted

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S kino && adduser -S kino -G kino
COPY --from=build /app/extracted/ ./
USER kino

ENV PORT=8081
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xshare:auto -Xss512k -XX:+ExitOnOutOfMemoryError"
EXPOSE 8081

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
