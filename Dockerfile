# 1) Build the React UI
FROM node:18 AS ui-builder
WORKDIR /ui
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# 2) Build the Java backend with Maven cache
FROM maven:3.9.5-eclipse-temurin-17 AS backend-builder
WORKDIR /app

# Step 1: copy only pom.xml to cache dependencies
COPY pom.xml ./

# Step 2: download and cache dependencies only
RUN mvn dependency:go-offline -B

# Step 3: now copy source code (will invalidate cache if code changes)
COPY src ./src

# Step 4: embed React static assets
COPY --from=ui-builder /ui/build src/main/resources/static

# Step 5: build the app
RUN mvn package -DskipTests

# 3) Runtime image
FROM eclipse-temurin:17-jdk
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=backend-builder /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
