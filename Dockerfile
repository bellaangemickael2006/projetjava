FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copier le JAR
COPY build/vitatrack.jar ./vitatrack.jar

# Port Vercel
EXPOSE 8080

# Variables d'environnement (à définir dans Vercel)
ENV PORT=8080

CMD ["java", "-jar", "vitatrack.jar"]
