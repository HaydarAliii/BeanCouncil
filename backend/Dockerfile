# --- Build aşaması ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Önce sadece pom.xml kopyalanıp bağımlılıklar indirilir — kaynak kod değişse bile bu katman
# cache'den gelir, her build'de tüm bağımlılıkları yeniden indirmez.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

# --- Çalışma zamanı aşaması ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
