# Build the jar, then throw the build tools away so the running image stays small.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Dependencies are their own layer: editing source does not re-download them.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Containers get a hard memory limit, so let the heap use most of it instead of
# the conservative default the JVM picks when it cannot see the limit.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

# The host decides the real port through PORT, this is only the documented default.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
