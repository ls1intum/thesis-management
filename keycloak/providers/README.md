# Keycloak Provider Artifacts

This directory contains checked-in Keycloak provider JARs that are copied into the Keycloak container at startup.

## custom-passkey-v1.1.1.jar

- Upstream source repository: https://github.com/ITegs/keycloak-redirectless-passkey
- Upstream tag: `v1.1.1`
- Upstream commit: `220a33121be7bbb6b971242027e995c5f3ab1f2b`
- Artifact coordinates embedded in the JAR: `com.example.keycloak:custom-passkey:v1.1.1`
- SHA-256:

```text
b66531922b1bfbe681222603b222731d21c113f0c4ef1d070adb6a497687c38c  custom-passkey-v1.1.1.jar
```

### Release Download

The extension README documents the release artifact as the normal installation path:

1. Open https://github.com/ITegs/keycloak-redirectless-passkey/releases/tag/v1.1.1.
2. Download the provider JAR asset from the release.
3. Copy it into this directory as `custom-passkey-v1.1.1.jar`.
4. Restart Keycloak so it loads the provider.

Verify the checked-in artifact:

```bash
shasum -a 256 keycloak/providers/custom-passkey-v1.1.1.jar
```

### Rebuild From Source

Rebuilding is optional and is mainly useful for provenance checks or local extension development:

```bash
git clone https://github.com/ITegs/keycloak-redirectless-passkey.git
cd keycloak-redirectless-passkey
git checkout v1.1.1
mvn clean package
```

### License

The custom passkey extension is distributed under the MIT License.
