# Keycloak Provider Artifacts

This directory contains checked-in Keycloak provider JARs that are copied into the Keycloak container at startup.

## custom-passkey-v1.2.0.jar

- Upstream source repository: https://github.com/ITegs/keycloak-redirectless-passkey
- Upstream tag: `v1.2.0`
- Upstream commit: `33841e3d9531ba96d012fd4fc3317b2844770305`
- Artifact coordinates embedded in the JAR: `com.example.keycloak:custom-passkey:v1.2.0`
- Keycloak version declared by the artifact: `26.5.0`
- SHA-256:

```text
57f89b591fd094b35a5a4c225be9d0409c7f099f093a8859a1845fc943d7f4cf  custom-passkey-v1.2.0.jar
```

### Release Download

The extension README documents the release artifact as the normal installation path:

1. Open https://github.com/ITegs/keycloak-redirectless-passkey/releases/tag/v1.2.0.
2. Download the provider JAR asset from the release.
3. Copy it into this directory as `custom-passkey-v1.2.0.jar`.
4. Restart Keycloak so it loads the provider.

Verify the checked-in artifact:

```bash
shasum -a 256 keycloak/providers/custom-passkey-v1.2.0.jar
```

### Rebuild From Source

Rebuilding is optional and is mainly useful for provenance checks or local extension development:

```bash
git clone https://github.com/ITegs/keycloak-redirectless-passkey.git
cd keycloak-redirectless-passkey
git checkout v1.2.0
mvn clean package
```

### License

The custom passkey extension is distributed under the MIT License.
