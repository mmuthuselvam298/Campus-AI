# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy backend source
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src

# Build production JAR
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Create storage directories
RUN mkdir -p /app/data /app/uploads

# Copy application JAR
COPY --from=build /app/target/campus-lost-found-1.0.0.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

# Start application using Render's assigned port
ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=${PORT:-8080} -jar app.jar"]
