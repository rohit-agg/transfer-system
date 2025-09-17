# ====== Builder stage ======
FROM amazoncorretto:17-alpine AS build
WORKDIR /app

# Copy only gradle wrapper and build files first to leverage Docker layer caching
COPY gradle gradle
COPY gradlew .
COPY settings.gradle build.gradle ./
RUN chmod +x gradlew

# Pre-fetch dependencies
RUN ./gradlew --no-daemon dependencies || true

# Now copy the rest of the source and build the bootable jar
COPY src src
RUN ./gradlew clean bootJar --no-daemon

# ====== Runtime stage ======
FROM amazoncorretto:17-alpine AS runtime
WORKDIR /app

# Copy only the built jar from the builder stage
COPY --from=build /app/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
