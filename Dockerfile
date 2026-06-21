# ---------- Stage 1: Build ----------
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Coping only pom.xml first - lets Docker cache downloaded dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B  # -B for Batch mode

#Now copying source code and build the jar
COPY src ./src
RUN mvn package -DskipTests  # taking package without executing tests again


# ---------- Stage 2: Run ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Creating a non-root user for security
# addgroup -S spring - Creating a new Linux group called spring
# adduser -S spring -G spring - Creates a new Linux user called spring, and puts that user into the spring group
RUN addgroup -S spring && adduser -S spring -G spring  # -S Creates a "system" user/group

# Coping only the built jar from Stage 1
COPY --from=build /app/target/*.jar app.jar

# Switch to non-root user
USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]