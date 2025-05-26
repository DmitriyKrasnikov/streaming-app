FROM eclipse-temurin:17-jdk

RUN apt-get update && \
    apt-get install -y --no-install-recommends libstdc++6 && \
    rm -rf /var/lib/apt/lists/*

COPY target/streaming-app-*.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
