# ---------- Build Stage ----------
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy Maven project
COPY pom.xml .
COPY src ./src

# Build without tests
RUN mvn -DskipTests clean package

# ---------- Run Stage ----------
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the JAR from build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

