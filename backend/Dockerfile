# Backend Ponto Municipal — build multi-stage (Maven -> JRE 21), imagem enxuta e non-root.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# camada de dependências (cache): resolve offline antes de copiar o código
COPY pom.xml .
RUN mvn -B -ntp -q dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
# usuário sem privilégios
RUN useradd -r -u 1001 ponto
# o Spring Boot plugin gera o jar executável em target/*.jar (o *.jar.original não casa)
COPY --from=build /app/target/*.jar app.jar
USER 1001
EXPOSE 8080
# perfil de produção: SEM ponto.security.open — exige Keycloak (ver application.yml)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
