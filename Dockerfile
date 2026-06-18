FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

RUN apk add --no-cache curl

COPY *.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=1s --timeout=2s --retries=10 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]