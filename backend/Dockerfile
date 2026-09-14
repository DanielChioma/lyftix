FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system lyftix \
    && useradd --system --gid lyftix --home-dir /app --shell /usr/sbin/nologin lyftix

WORKDIR /app

COPY --from=build --chown=lyftix:lyftix /workspace/target/backend-*.jar app.jar

USER lyftix:lyftix

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
    CMD curl --fail --silent --show-error http://localhost:8080/api/health > /dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
