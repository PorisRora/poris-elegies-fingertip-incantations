# Building from source

The public repository is a standalone NeoForge project. Use its repository root for these commands.

## Requirements

- JDK 21
- Network access for the first Gradle/dependency resolution
- The included Gradle Wrapper; a system-wide Gradle installation is not required

The build pins ModDevGradle and the compile-time Iron's Spells version. Iron's Spells is resolved from its author's [release Maven repository](https://iron.wiki/developers/), as a compile-only dependency. Its JAR is not committed to this repository or bundled in the output.

## Build and test

Windows:

```powershell
.\gradlew.bat :build
```

Linux/macOS:

```sh
./gradlew :build
```

The build runs the unit tests and creates the mod JAR and a sources JAR in `build/libs/`. The sources JAR is for development; players install the regular mod JAR.

To run only the unit tests:

```sh
./gradlew :test
```

## Development runs

```sh
./gradlew :runClient
./gradlew :runServer
```

Each run uses its own directory under `run/`. The server still requires accepting Minecraft's EULA in its run directory.

The default development runs contain Fingertip itself. To test an optional integration, install that mod and its required dependencies into the selected run's `mods` directory.

## Versions and licenses

The project version and dependency versions are in `gradle.properties`. A SNAPSHOT version is a development build, not an official release.

The repository's source code is MIT-licensed. Art assets have separate rights under [ASSET_LICENSE.md](../ASSET_LICENSE.md). Keep the license files in local builds and redistributed official packages. Review [third-party notices](../THIRD_PARTY_NOTICES.md) before redistributing dependencies.

## Repository layout

- `src/main/java`: runtime code and public integration interfaces
- `src/main/resources`: game-ready textures, translations, and configuration
- `src/main/templates`: generated mod metadata template
- `src/test`: unit tests
- `docs`: user and developer documentation
- `publication`: platform description and release-note drafts

Generated files, game saves, logs, local configuration, and downloaded dependency JARs are ignored by Git.
