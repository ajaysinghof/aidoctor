# Use official JDK image
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app



# Copy project files
COPY pom.xml .
COPY src src

# Build app
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests package

# Expose port
EXPOSE 8080

# Run the Spring Boot app
CMD ["java", "-jar", "target/aidoctor-backend-0.0.1-SNAPSHOT.jar"]

