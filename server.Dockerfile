FROM azul/zulu-openjdk:25.0.4.1-jdk AS build
WORKDIR /home/gradle/thesis-management/server

# Copy dependency files first for layer caching
COPY server/gradlew server/build.gradle server/settings.gradle server/gradle.properties ./
COPY server/gradle ./gradle
RUN ./gradlew dependencies --no-daemon

# Copy source code and .git (needed by gradle-git-properties plugin)
COPY .git ../.git
COPY server/src ./src

# SBOMs are generated on the CI runner before this docker build (see
# .github/workflows/build_docker.yml) and picked up by processResources.
# For local builds, run `./gradlew cyclonedxBom` and `pnpm run sbom` first
# to populate these dirs; otherwise the resulting jar has no SBOM data
# and the admin dependency overview is empty.
COPY server/sbom ./sbom
COPY client/sbom ../client/sbom

RUN ./gradlew build -x test -x checkstyleMain -x checkstyleTest --no-daemon

FROM azul/zulu-openjdk:25.0.4.1-jre

EXPOSE 8080

RUN mkdir /app

COPY --from=build /home/gradle/thesis-management/server/build/libs/*.jar /app/server.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom","-jar","/app/server.jar"]
