FROM azul/zulu-openjdk:25.0.5-jdk AS build
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

# The runtime uses Oracle JDK 25.0.5.1 until a Zulu 25 image with the same fixes is published.
# The archive is pinned by version and verified against Oracle's published SHA-256 checksum.
FROM ubuntu:24.04 AS jdk
ARG TARGETARCH
ARG JDK_VERSION=25.0.5.1
ARG JDK_SHA256_AMD64=3d40d69461200594380ac84c17be6224539022028730cffe043ac70c41ffd1d5
ARG JDK_SHA256_ARM64=2c30d5fe69796452732d32e746c5f4b4031e4991e2be9a3313f76e51fe5196ee
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*
RUN case "${TARGETARCH}" in \
        amd64) jdk_arch=x64; jdk_sha256="${JDK_SHA256_AMD64}" ;; \
        arm64) jdk_arch=aarch64; jdk_sha256="${JDK_SHA256_ARM64}" ;; \
        *) echo "Unsupported architecture: ${TARGETARCH}" >&2; exit 1 ;; \
    esac \
    && curl -fsSL -o /tmp/jdk.tar.gz "https://download.oracle.com/java/25/archive/jdk-${JDK_VERSION}_linux-${jdk_arch}_bin.tar.gz" \
    && echo "${jdk_sha256}  /tmp/jdk.tar.gz" | sha256sum -c - \
    && mkdir -p /opt/java/jdk \
    && tar -xzf /tmp/jdk.tar.gz -C /opt/java/jdk --strip-components=1 \
    && rm /tmp/jdk.tar.gz

FROM ubuntu:24.04

ENV LANG=C.UTF-8
ENV JAVA_HOME=/opt/java/jdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Font libraries that the previous Zulu runtime image shipped, used by the JDK's image and font code.
RUN apt-get update \
    && apt-get install -y --no-install-recommends fontconfig libfreetype6 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=jdk /opt/java/jdk /opt/java/jdk

EXPOSE 8080

RUN mkdir /app

COPY --from=build /home/gradle/thesis-management/server/build/libs/*.jar /app/server.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom","-jar","/app/server.jar"]
