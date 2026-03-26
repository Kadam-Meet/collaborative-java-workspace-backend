FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# Copy only build metadata first for better Docker layer caching
COPY .mvn .mvn
COPY mvnw mvnw
COPY pom.xml pom.xml

RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

# Copy source and package the app
COPY src src
RUN ./mvnw -q -DskipTests clean package

FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the fat jar built by Spring Boot repackage
COPY --from=build /app/target/collaborative-java-workspace-backend-1.0.0.jar app.jar

EXPOSE 8081

# Render injects PORT at runtime; default to 8081 for local runs
CMD ["sh", "-c", "java -Dserver.port=${PORT:-8081} -jar app.jar"]