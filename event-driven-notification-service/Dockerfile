FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl \
    && addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
