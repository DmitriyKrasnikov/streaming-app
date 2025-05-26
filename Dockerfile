# Используем Debian-based образ с Eclipse Temurin JDK 17
FROM eclipse-temurin:17-jdk

# Устанавливаем нативные зависимости для RocksDB-JNI
RUN apt-get update && \
    apt-get install -y --no-install-recommends libstdc++6 && \
    rm -rf /var/lib/apt/lists/*

# Копируем ваш скомпилированный JAR
COPY target/streaming-app-*.jar /app.jar

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "/app.jar"]
