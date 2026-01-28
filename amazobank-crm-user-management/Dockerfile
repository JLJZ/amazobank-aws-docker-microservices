# This file is not used because this service is deployed as a zip to AWS lambda
FROM gradle:8-jdk17 AS base-image
WORKDIR /app
COPY gradle/ gradle/
COPY build.gradle.kts gradle.lockfile ./
RUN ["gradle", "build", "--no-daemon"] || return 0

FROM base-image AS build
ARG SPRING_PROFILE=$ENVIRONMENT
COPY src/ src/
RUN ["gradle", "buildZip"]

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/app-1.0.0.jar ./app.jar
EXPOSE 8080
CMD ["java","-jar","app.jar"]
