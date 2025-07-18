FROM maven:3.9.5-eclipse-temurin-17 as builder

WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Alpine kullanma! Onun yerine debian/ubuntu tabanlı tam versiyon:
FROM eclipse-temurin:17-jdk

RUN apt-get update && apt-get install -y curl

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
COPY themes /opt/keycloak/themes

ENTRYPOINT ["java", "-jar", "app.jar"]