FROM gradle:9.2.1-jdk17 AS build
# Set container working directory to /app
WORKDIR /app
# Copy Gradle configuration files
COPY gradlew /app/
COPY gradle /app/gradle
# Ensure Gradle wrapper is executable
RUN chmod +x ./gradlew
# Copy build script and source code
COPY build.gradle settings.gradle /app/
COPY src /app/src
# Build the server
RUN ./gradlew clean build --no-daemon

# make image smaller by using multi stage build
FROM eclipse-temurin:17-jdk
# Set the env to "production"
ENV SPRING_PROFILES_ACTIVE=production
# get non-root user
RUN groupadd appgroup && \
    useradd -r -g appgroup appuser
USER appuser
# Set container working directory to /app
WORKDIR /app
# copy built artifact from build stage
COPY --from=build /app/build/libs/*.jar /app/soprafs26.jar
# Expose the port on which the server will be running (based on application.properties)
EXPOSE 8080
# start server
CMD ["java", "-jar", "/app/soprafs26.jar"]