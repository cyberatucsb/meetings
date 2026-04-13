FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:11-jre
COPY --from=build /build/target/echo-server-1.0.jar /app/echo-server.jar
EXPOSE 34598
CMD ["java", \
     "-Dcom.sun.jndi.ldap.object.trustURLCodebase=true", \
     "-jar", "/app/echo-server.jar", "34598"]
