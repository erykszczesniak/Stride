# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY shared ./shared
COPY services/challenge-goal-service ./services/challenge-goal-service
COPY .mvn ./.mvn
COPY mvnw ./mvnw
RUN ./mvnw -q -pl services/challenge-goal-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S stride && adduser -S stride -G stride
WORKDIR /app
COPY --from=build /workspace/services/challenge-goal-service/target/challenge-goal-service.jar /app/app.jar
RUN chown -R stride:stride /app
USER stride
EXPOSE 8086
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=10 \
  CMD wget -q --spider http://localhost:8086/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
