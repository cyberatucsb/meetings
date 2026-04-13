FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:11-jdk
COPY --from=build /build/target/stager-1.0.jar /app/stager.jar
RUN mkdir /app/classes
EXPOSE 34249 45342 34390
CMD ["java", "-jar", "/app/stager.jar", "/app/classes", "34249", "45342", "34390"]
