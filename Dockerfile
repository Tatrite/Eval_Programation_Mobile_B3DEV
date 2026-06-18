# Étape 1 : Utiliser une image de base avec Java 17 ou 21 (selon ton projet)
FROM eclipse-temurin:17-jdk-alpine

# Définir le dossier de travail dans le conteneur
WORKDIR /app

RUN apk add --no-cache curl

COPY *.jar app.jar

# Exposer le port interne de Spring Boot
EXPOSE 8080

HEALTHCHECK --interval=1s --timeout=2s --retries=10 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Lancer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]