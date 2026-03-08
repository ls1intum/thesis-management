FROM azul/zulu-openjdk:25.0.2-jdk AS build
WORKDIR /home/gradle/thesis-management/server

# Copy dependency files first for layer caching
COPY server/gradlew server/build.gradle server/settings.gradle server/gradle.properties ./
COPY server/gradle ./gradle
RUN ./gradlew dependencies --no-daemon

# Copy source code and build
COPY server/src ./src
RUN ./gradlew build -x test --no-daemon

FROM azul/zulu-openjdk:25.0.2-jre

EXPOSE 8080

RUN mkdir /app

COPY --from=build /home/gradle/thesis-management/server/build/libs/*.jar /app/server.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom","-jar","/app/server.jar"]
