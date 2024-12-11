# Stage 1: Build the bot
# Create a container with a Gradle image to build the bot
FROM gradle:8.11.1-jdk23 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy Gradle files and project source code into the container
COPY build.gradle.kts settings.gradle.kts /app/
COPY src /app/src

# Run the Gradle build
RUN gradle shadowJar

# Stage 2: Run the bot
# Create a new container with a Java image to run the bot
FROM openjdk:23-jdk-slim

# Set the working directory inside the new container
WORKDIR /app

# Install fontconfig package
RUN apt-get update && apt-get install -y fontconfig && rm -rf /var/lib/apt/lists/*

# Copy the fat JAR from the build stage into the new container
COPY --from=build /app/build/libs/bobo-1.0-all.jar /app/bot.jar

# Copy the start script into the new container
COPY start.sh /app/

# Make the start script executable
RUN chmod +x /app/start.sh

# Set the environment variable to use system environment variables
ENV USE_SYSTEM_ENV=true

# Run the bot
ENTRYPOINT ["/app/start.sh"]
