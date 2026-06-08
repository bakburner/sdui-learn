# ================================
# Build stage -- Gradle + Java 21
# Build runs on native platform only (JAR is platform-independent)
# ================================
# syntax=docker/dockerfile:1.4
ARG BUILDER_BASE_IMAGE=eclipse-temurin:21-jdk-alpine
ARG RUNNER_BASE_IMAGE=eclipse-temurin:21-jre-alpine

FROM --platform=$BUILDPLATFORM $BUILDER_BASE_IMAGE AS build

WORKDIR /app

# Copy Gradle wrapper and build files first (layer caching)
COPY server/gradlew gradlew
COPY server/gradle/ gradle/
COPY server/build.gradle.kts server/settings.gradle.kts ./
RUN chmod +x gradlew

# SAF (`com.nba:service-aggregation-framework`) resolves from GitHub Packages
# (https://github.com/NBA/saf). Credentials are mounted as BuildKit secrets
# so they never land in image layers. Caller supplies them via
# `docker buildx bake --set sdui-prototype.secrets=...` or `--secret`.
RUN --mount=type=secret,id=gpr_user --mount=type=secret,id=gpr_token \
    apk add --no-cache curl >/dev/null && \
    USER_LEN=$(wc -c < /run/secrets/gpr_user) && \
    TOKEN_LEN=$(wc -c < /run/secrets/gpr_token) && \
    echo "[diag] secret file bytes: user=$USER_LEN token=$TOKEN_LEN" && \
    GPR_USER="$(cat /run/secrets/gpr_user)" && \
    GPR_TOKEN="$(cat /run/secrets/gpr_token)" && \
    echo "[diag] var lengths after substitution: user=${#GPR_USER} token=${#GPR_TOKEN}" && \
    STATUS=$(curl -sS -o /dev/null -w '%{http_code}' \
      -u "$GPR_USER:$GPR_TOKEN" -I \
      https://maven.pkg.github.com/NBA/saf/com/nba/service-aggregation-framework/1.0.0-SNAPSHOT/maven-metadata.xml) && \
    echo "[diag] in-container curl status: $STATUS" && \
    ./gradlew dependencies --no-daemon \
      -Pgpr.user="$GPR_USER" -Pgpr.key="$GPR_TOKEN"

# Copy schema tokens (referenced by processResources as ../schema)
COPY schema/ ../schema/

# Copy source and build
COPY server/src/ src/

ARG GIT_SHA
ARG GIT_BRANCH_NAME

ENV GIT_SHA=$GIT_SHA \
    GIT_BRANCH_NAME=$GIT_BRANCH_NAME

RUN --mount=type=secret,id=gpr_user --mount=type=secret,id=gpr_token \
    GPR_USER="$(cat /run/secrets/gpr_user)" \
    GPR_TOKEN="$(cat /run/secrets/gpr_token)" \
    ./gradlew bootJar --no-daemon \
      -Pgpr.user="$GPR_USER" -Pgpr.key="$GPR_TOKEN"

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
