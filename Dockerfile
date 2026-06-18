# Étape 1 : Utiliser une image de base avec Java 17 ou 21 (selon ton projet)
FROM eclipse-temurin:17-jdk-alpine

# Définir le dossier de travail dans le conteneur
WORKDIR /app

# Copier le fichier JAR généré par Maven dans le conteneur
COPY target/*.jar app.jar

# Exposer le port interne de Spring Boot
EXPOSE 8080

# Lancer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]