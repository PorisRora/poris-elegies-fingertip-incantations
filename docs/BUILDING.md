# Building from source

The repository root builds the NeoForge edition. The `fabric/` directory is an independent Fabric project; clone the whole repository because both editions share the resources in `src/main/resources`. The commands below refer to NeoForge unless stated otherwise.

## Requirements

- JDK 21
- Network access for the first Gradle/dependency resolution
- The included Gradle Wrapper; a system-wide Gradle installation is not required

The build pins ModDevGradle and the compile-time Iron's Spells version. Iron's Spells is resolved from its author's [release Maven repository](https://iron.wiki/developers/), as a compile-only dependency. Its JAR is not committed to this repository or bundled in the output.

The compile-time Iron's version stays at **1.21.1-3.15.5**. Runtime integration starts at **1.21-3.4.4** on Minecraft 1.21.1 / NeoForge 21.1.181; that separate compatibility floor is declared in `src/main/templates/META-INF/neoforge.mods.toml`.

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

## Fabric build

Use JDK 21. From the repository root:

```powershell
cd fabric
.\gradlew.bat :build
```

On Linux/macOS, run `./gradlew :build` inside `fabric/`. This runs the unit tests and produces the runtime and sources JARs in `fabric/build/libs/`. Version and dependency settings are in `fabric/build.gradle`; runtime requirements are in `fabric/src/main/resources/fabric.mod.json`. The compile-time Fabric API is pinned to the supported minimum, **0.106.0+1.21.1**.

Fabric development runs use `:runClient` and `:runServer` from `fabric/`. Mod Menu is compile-only and is not required to run Fingertip.

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
- `src/test`: NeoForge unit tests
- `fabric/src`: Fabric runtime code, integration interfaces, metadata, and unit tests
- `fabric/docs`: Fabric integration documentation
- `docs`: user and developer documentation
- `publication`: platform description and release-note drafts

Generated files, game saves, logs, local configuration, and downloaded dependency JARs are ignored by Git.
