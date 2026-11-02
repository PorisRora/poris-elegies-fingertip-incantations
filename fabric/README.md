# Fingertip Incantations for Fabric

Version **1.2.1**, for Minecraft **1.21.1** with Fabric Loader **0.16.14 or later** and Java 21. Install Fingertip on both client and server, using matching versions and the same loader.

## Required mod

- **Fabric API 0.106.0+1.21.1 or later for Minecraft 1.21.1.**

The Fabric edition includes the customizable slots, action wheel, editor, themes, item bindings, and standard mod keybind support. It preserves the skill and inventory-source APIs but does not include the Iron's Spells adapter from the NeoForge edition.

See the [main introduction](../README.md), [user guide](../docs/USER_GUIDE.md), and [Fabric integration guide](docs/INTEGRATION.md).

## Build

Run `./gradlew :build` from this directory, or `gradlew.bat :build` on Windows, using JDK 21. The independent Fabric project shares artwork and translations from the repository's `src/main/resources`; clone the whole repository. Output files are in `fabric/build/libs`.

## Compatibility

Mods using private physical-input handlers need their own adapters. Third-party backpack contents are not automatically scanned. HUD layer hiding requires the other renderer to cooperate with the public API.

Inventory Profiles Next's automatic refill can interfere with returning an item from the offhand in multiplayer, especially with latency. Disable its automatic refill if this occurs; this interaction is not fixed in this release.

Source code is MIT licensed. Logos and artwork retain the terms in [ASSET_LICENSE.md](ASSET_LICENSE.md).
