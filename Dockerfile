# ── Étape 1 : compilation (JDK 21, sans dépendances externes) ──
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /src
COPY . .
RUN sh build.sh

# ── Étape 2 : exécution (JRE léger) ──
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /src/build/vitatrack.jar ./vitatrack.jar

EXPOSE 8080
ENV PORT=8080
CMD ["java", "-jar", "vitatrack.jar"]
