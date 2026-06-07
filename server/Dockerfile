# ================================
# Build stage -- Gradle + Java 21
# Build runs on native platform only (JAR is platform-independent)
# ================================
ARG BUILDER_BASE_IMAGE=eclipse-temurin:21-jdk-alpine
ARG RUNNER_BASE_IMAGE=eclipse-temurin:21-jre-alpine

FROM --platform=$BUILDPLATFORM $BUILDER_BASE_IMAGE AS build

WORKDIR /app

# SAF (service-aggregation-framework) is consumed via Maven Local. The build
# context's .m2/repository/com/nba/ is populated by `make sync-saf` locally
# (or the equivalent CI step) and copied into the builder image's ~/.m2 so
# Gradle can resolve `com.nba:service-aggregation-framework` without network.
# See nba-client-backend for the same pattern.
COPY .m2/repository/com/nba /root/.m2/repository/com/nba

# Copy Gradle wrapper and build files first (layer caching)
COPY server/gradlew gradlew
COPY server/gradle/ gradle/
COPY server/build.gradle.kts server/settings.gradle.kts ./

# Download dependencies (cached unless build files change)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Copy schema tokens (referenced by processResources as ../schema)
COPY schema/ ../schema/

# Copy source and build
COPY server/src/ src/

ARG GIT_SHA
ARG GIT_BRANCH_NAME

ENV GIT_SHA=$GIT_SHA \
    GIT_BRANCH_NAME=$GIT_BRANCH_NAME

RUN ./gradlew bootJar --no-daemon

# ================================
# Runtime stage -- minimal JRE, non-root
# ================================
FROM $RUNNER_BASE_IMAGE AS final

RUN apk upgrade --no-cache \
 && addgroup -S appgroup -g 2000 \
 && adduser -S appuser -u 2000 -G appgroup

WORKDIR /app

COPY --chown=appuser:appgroup --from=build /app/build/libs/*.jar app.jar

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-Djava.io.tmpdir=/tmp"

ENTRYPOINT ["java", "-jar", "app.jar"]
