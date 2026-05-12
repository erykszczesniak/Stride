# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY shared ./shared
COPY services/user-service ./services/user-service
COPY .mvn ./.mvn
COPY mvnw ./mvnw
RUN ./mvnw -q -pl services/user-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S stride && adduser -S stride -G stride
WORKDIR /app
COPY --from=build /workspace/services/user-service/target/user-service.jar /app/app.jar
RUN chown -R stride:stride /app
USER stride
EXPOSE 8081
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=10 \
  CMD wget -q --spider http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
