FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copia o .jar compilado da pasta auth/target
COPY auth/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]