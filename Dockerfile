# ============================================================
# Stage 1 — Build
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copie du wrapper Maven et du pom.xml en premier (cache Docker)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Téléchargement des dépendances (layer cachée si pom.xml inchangé)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copie du code source
COPY src/ src/

# Build sans les tests (ils seront gérés en CI séparément)
RUN ./mvnw clean package -DskipTests -B

# ============================================================
# Stage 2 — Runtime
# ============================================================
FROM eclipse-temurin:21-jre-alpine

# Sécurité : utilisateur non-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

WORKDIR /app

# Copie uniquement le JAR depuis le stage de build
COPY --from=builder /app/target/*.jar app.jar

# Port exposé (Render utilise la variable PORT, Spring Boot s'y adapte)
EXPOSE 8080

# Variables d'environnement par défaut (surchargeables dans Render)
ENV JAVA_OPTS="-Xms256m -Xmx512m" \
    SPRING_PROFILES_ACTIVE="prod"

# Point d'entrée
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
