# Flux Plumes

A NeoForge 1.21.1 addon for Fluxnautics with configurable Rocket, Afterburner and Flare exhaust styles. Veil supplies the plume shaders and lighting.

## Installation

Install on the client and server with Fluxnautics and Veil, plus their dependencies. The development setup used NeoForge 21.1.228, Veil 4.5.1, and a locally patched Fluxnautics 1.0.0. Compatibility with other versions has not been verified.

## Building

Run `gradlew.bat build` (Windows) or `./gradlew build`. This project expects compatible local dependency jars in `libs/`; they are not included in this repository. The development setup also uses jars in `dev-jars/` and `run/mods/`. See `build.gradle` for the configured dependencies.

This is a development build. Flare's latest appearance and colour changes still need visual verification in game.
