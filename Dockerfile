# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy root pom and backend directory
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src

# Build production JAR without running tests
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Create persistent storage directories
RUN mkdir -p /app/data /app/uploads

# Copy built JAR from build stage
COPY --from=build /app/target/campus-lost-found-1.0.0.jar app.jar

ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-jar", "app.jar"]
