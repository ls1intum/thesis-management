FROM azul/zulu-openjdk:25.0.0-jdk AS build

COPY --chown=gradle:gradle . /home/gradle/thesis-management
WORKDIR /home/gradle/thesis-management/server

RUN gradle build -x test --no-daemon

FROM azul/zulu-openjdk:25.0.0-jre

EXPOSE 8080

RUN mkdir /app

COPY --from=build /home/gradle/thesis-management/server/build/libs/*.jar /app/server.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom","-jar","/app/server.jar"]
