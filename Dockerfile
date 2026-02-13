# Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/photosono-*.jar app.jar

ENV PHOTOSONO_INPUT_DIR=/input
ENV PHOTOSONO_ORIGINALS_DIR=/originals
ENV PHOTOSONO_TIMELINE_DIR=/timeline
ENV PHOTOSONO_UNKNOWN_DATE_DIR=/unknown-date
ENV PHOTOSONO_UNKNOWN_TYPE_DIR=/unknown-type
ENV PHOTOSONO_CORRUPTED_DIR=/corrupted
ENV PHOTOSONO_DEDUPLICATION_ENABLED=true
ENV PHOTOSONO_TIMELINE_ENABLED=true

# Create directories
RUN mkdir -p /input /originals /timeline /unknown-date /unknown-type /corrupted

VOLUME ["/input", "/originals", "/timeline", "/unknown-date", "/unknown-type", "/corrupted"]

ENTRYPOINT ["java", "-jar", "app.jar"]
