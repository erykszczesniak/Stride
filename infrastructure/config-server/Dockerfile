# syntax=docker/dockerfile:1.7
# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY shared ./shared
COPY infrastructure/config-server ./infrastructure/config-server
COPY .mvn ./.mvn
COPY mvnw ./mvnw
RUN ./mvnw -q -pl infrastructure/config-server -am -DskipTests package

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S stride && adduser -S stride -G stride
WORKDIR /app
COPY --from=build /workspace/infrastructure/config-server/target/config-server.jar /app/app.jar
RUN chown -R stride:stride /app
USER stride
EXPOSE 8888
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=10 \
  CMD wget -q --spider http://localhost:8888/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
