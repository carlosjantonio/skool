FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
COPY pom.xml ./
COPY common ./common
COPY modules ./modules
COPY app ./app
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -pl app -am package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /src/app/target/skool.jar /app/skool.jar
ENV TZ=Africa/Luanda
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/skool.jar"]
