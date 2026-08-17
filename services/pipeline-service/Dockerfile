# syntax=docker/dockerfile:1.7
# Debian-based build stage: protoc/protoc-gen-grpc-java from Maven Central are
# glibc binaries and do not run on musl/Alpine. Runtime stays on Alpine JRE.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
# The parent POM's reactor references every module, so the whole module tree
# must be present even when building a single service with -pl/-am.
COPY pom.xml ./pom.xml
COPY .mvn ./.mvn
COPY mvnw ./mvnw
COPY shared ./shared
COPY infrastructure ./infrastructure
COPY services ./services
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -pl services/pipeline-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S stride && adduser -S stride -G stride
WORKDIR /app
COPY --from=build /workspace/services/pipeline-service/target/pipeline-service.jar /app/app.jar
RUN chown -R stride:stride /app
USER stride
EXPOSE 8089
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=10 \
  CMD wget -q --spider http://localhost:8089/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
