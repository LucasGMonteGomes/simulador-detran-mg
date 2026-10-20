# syntax=docker/dockerfile:1

# ---------- 1. Compilação: JDK + Maven ----------
# O jar é o mesmo em qualquer processador, então a compilação roda sempre na arquitetura de quem gera a imagem.
# Num build multi-arquitetura, isso evita compilar o projeto de novo dentro de um arm64 emulado, que é lento.
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk AS build
WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/

# O cache do ~/.m2 sobrevive entre builds: as dependências só são baixadas uma vez.
# Os testes ficam no CI (usam Testcontainers, que precisa de Docker); aqui só empacota.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q package -DskipTests \
    && cp target/simulador-detran-mg-*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --destination extraido

# ---------- 2. Execução: só o JRE ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system simulador && useradd --system --gid simulador --no-create-home simulador

# Uma camada por grupo, da que menos muda para a que mais muda: ao alterar só o código, as camadas de
# dependências continuam em cache e a publicação de uma versão nova envia poucos megabytes.
COPY --from=build /build/extraido/dependencies/ ./
COPY --from=build /build/extraido/spring-boot-loader/ ./
COPY --from=build /build/extraido/snapshot-dependencies/ ./
COPY --from=build /build/extraido/application/ ./

USER simulador
EXPOSE 8080

# A conexão com o banco vem de variáveis de ambiente (SPRING_DATASOURCE_URL, _USERNAME e _PASSWORD).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
