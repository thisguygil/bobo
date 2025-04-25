# Stage 1: Build the bot
FROM gradle:8.13-jdk23 AS build
WORKDIR /app

# Copy Gradle files and cache dependencies
COPY build.gradle.kts settings.gradle.kts /app/
RUN gradle dependencies --no-daemon

# Copy source code and build
COPY src /app/src
RUN gradle build --no-daemon

# Stage 1.5: Prepare artifacts
FROM build AS prepare
RUN mv /app/build/libs/bobo-1.0.jar /app/bot.jar

# Stage 2: Run the bot
FROM openjdk:23-jdk-slim
WORKDIR /app

# Install fontconfig package
RUN apt-get update && \
    apt-get install -y fontconfig && \
    rm -rf /var/lib/apt/lists/*

# Copy the main JAR and dependency libraries
COPY --from=prepare /app/bot.jar /app/bot.jar
COPY --from=prepare /app/build/runtime-libs/ /app/libs/

# Copy the start script and make it executable
COPY start.sh /app/
RUN chmod +x /app/start.sh

# Set environment variable and define the entrypoint
ENV USE_SYSTEM_ENV=true
ENTRYPOINT ["/app/start.sh"]
