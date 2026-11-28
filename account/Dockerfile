# compila dentro do docker, então não precisa gerar o jar na máquina antes
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /src
COPY . .
# o cache guarda as dependências entre um build e outro; "locked" evita dois builds escrevendo juntos
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -q -pl account -am package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /src/account/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
