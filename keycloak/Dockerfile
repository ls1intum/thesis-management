FROM gradle:9.7.1-jdk25 AS provider-build

WORKDIR /workspace
COPY keycloak/provider-deps/gradlew keycloak/provider-deps/gradlew.bat ./
COPY keycloak/provider-deps/gradle ./gradle
COPY keycloak/provider-deps/settings.gradle keycloak/provider-deps/build.gradle ./
RUN ./gradlew --no-daemon build copyProviders

FROM quay.io/keycloak/keycloak:26.6.3
COPY --from=provider-build /workspace/build/providers/ /opt/keycloak/providers/
RUN /opt/keycloak/bin/kc.sh build
