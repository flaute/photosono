# Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/photosono-*.jar app.jar

# Map internal paths to environment variables
ENV PHOTOSONO_INPUT_DIR=/input
ENV PHOTOSONO_OUTPUT_DIR=/output-unique
ENV PHOTOSONO_FINAL_OUTPUT_DIR=/timeline
ENV PHOTOSONO_DEDUPLICATION_ENABLED=true
ENV PHOTOSONO_DEDUPLICATION_SCAN_INTERVAL=PT10S
ENV PHOTOSONO_TIMELINE_ENABLED=true
ENV PHOTOSONO_TIMELINE_SCAN_INTERVAL=PT10S

# Create directories and set permissions
RUN mkdir -p /input /output /timeline

ENTRYPOINT ["java", "-jar", "app.jar"]
