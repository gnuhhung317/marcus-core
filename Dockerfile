FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

# Copy root POM and module POMs first for Maven dependency caching
COPY pom.xml ./
COPY marcus-domain/pom.xml ./marcus-domain/pom.xml
COPY marcus-application/pom.xml ./marcus-application/pom.xml
COPY marcus-infrastructure/pom.xml ./marcus-infrastructure/pom.xml
COPY marcus-api/pom.xml ./marcus-api/pom.xml

# Copy the full source tree
COPY marcus-domain ./marcus-domain
COPY marcus-application ./marcus-application
COPY marcus-infrastructure ./marcus-infrastructure
COPY marcus-api ./marcus-api

# Build only the API module and its dependencies
RUN mvn -pl marcus-api -am clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /workspace/marcus-api/target/marcus-api-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
