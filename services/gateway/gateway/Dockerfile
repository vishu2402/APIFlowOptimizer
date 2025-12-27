FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# 1. Copy App JAR
COPY target/*.jar app.jar
# 2. Copy Agent JAR
COPY libs/opentelemetry-javaagent.jar agent.jar

# 3. FORCE the service name using the environment variable
ENTRYPOINT ["java", "-javaagent:/app/agent.jar", "-Dotel.service.name=${OTEL_SERVICE_NAME}", "-jar", "/app/app.jar"]