# 1) Build the React UI
FROM node:18 AS ui-builder
WORKDIR /ui
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ .
RUN npm run build

# 2) Build the Java backend, embedding the React build
FROM maven:3.9.5-eclipse-temurin-17 AS backend-builder
WORKDIR /app

# copy pom and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# copy Java code
COPY src ./src

# copy the React static assets into Spring’s static folder
COPY --from=ui-builder /ui/build src/main/resources/static

# build the fat-jar
RUN mvn package -DskipTests

# 3) Final runtime image
FROM eclipse-temurin:17-jdk
# for the entrypoint script
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=backend-builder /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
