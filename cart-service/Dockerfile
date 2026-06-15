# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy parent pom first for dependency caching
COPY pom.xml ./pom.xml
COPY cart-service/pom.xml ./cart-service/pom.xml
COPY stats-service/pom.xml ./stats-service/pom.xml
COPY common/pom.xml ./common/pom.xml

# Copy sources
COPY cart-service/src ./cart-service/src
COPY common/src ./common/src

# Build only cart-service and its required parent reactor info
RUN mvn -q -pl cart-service -am package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/cart-service/target/cart-service-*.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]