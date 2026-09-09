FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw --batch-mode -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw --batch-mode -DskipTests package

FROM eclipse-temurin:25-jre-noble
WORKDIR /app

RUN groupadd --system weeklyreport && useradd --system --gid weeklyreport --home-dir /app weeklyreport
COPY --from=build --chown=weeklyreport:weeklyreport /workspace/target/weekly-report-api-0.0.1-SNAPSHOT.jar app.jar

USER weeklyreport
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
