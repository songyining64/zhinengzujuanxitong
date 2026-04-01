# 构建：docker build -t exam-backend:latest ./backend
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -q package -DskipTests -Dspring.boot.version=3.1.10 -Dmybatis-plus.version=3.5.5 -Djjwt.version=0.11.5

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
