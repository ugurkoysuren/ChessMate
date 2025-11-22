FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre

RUN apt-get update && apt-get install -y stockfish && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app/target/blind-chess-1.0.0.jar app.jar

ENV STOCKFISH_PATH=/usr/games/stockfish

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
