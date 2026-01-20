FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN apk add --no-cache maven && \
    mvn clean package -DskipTests && \
    java -Djarmode=layertools -jar target/*.jar extract

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

COPY --from=builder --chown=appuser:appgroup /build/dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /build/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appgroup /build/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /build/application/ ./

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
