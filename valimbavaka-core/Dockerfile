# syntax=docker/dockerfile:1
# Image backend : compilee sur les runners GitHub, jamais sur l'EliteBook.

# --- Etape 1 : compilation (JDK + Maven, jetes apres) ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Dependances d'abord : cette couche reste en cache tant que pom.xml ne change pas.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline -Dcheckstyle.skip || true

COPY src ./src
# Les tests et checkstyle tournent dans la CI (ci.yml), pas pendant la construction de l'image.
RUN mvn -B -ntp package -DskipTests -Dcheckstyle.skip

# --- Etape 2 : image finale legere (JRE seul) ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/*.jar /app/app.jar
USER app

# La JVM s'adapte a la limite memoire du conteneur (mem_limit dans docker-compose).
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

# wget est fourni par busybox dans l'image alpine.
HEALTHCHECK --interval=10s --timeout=5s --start-period=90s --retries=6 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
