FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY rate-limiter-core/pom.xml rate-limiter-core/
COPY api-gateway-demo/pom.xml api-gateway-demo/
RUN apk add --no-cache maven
RUN mvn -q -B dependency:go-offline -f pom.xml || true
COPY rate-limiter-core rate-limiter-core
COPY api-gateway-demo api-gateway-demo
RUN mvn -q -B -pl api-gateway-demo -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /workspace/api-gateway-demo/target/api-gateway-demo-*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
