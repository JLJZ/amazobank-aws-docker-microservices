FROM eclipse-temurin:17-jdk
WORKDIR /app

COPY gradle/wrapper/ gradle/wrapper
COPY gradlew build.gradle ./
COPY src/ src/

RUN chmod +x gradlew
RUN ./gradlew bootJar
COPY ./build/libs/project-2025-26T1-g2-t2-client-profile-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]