# CobbleSnack

CobbleSnack is a client-side Poké Snack calculator for Cobblemon. It reads the Pokémon, forms, spawn routes, biomes, structures, and seasonings available in your installed files, then helps you choose a useful snack and location.

Press **K** in game to open CobbleSnack. The key can be changed in Minecraft's Controls menu.

## What it does

- Calculates a Poké Snack and an accessible location for the selected Pokémon and form.
- Uses spawn data from currently installed mods, enabled combined resource/data packs, configured data packs, and resources downloaded by AutoModpack instead of relying on one fixed Pokémon list.
- Handles ordinary biome routes, structure routes, water routes, and fishing-only routes.
- Shows form-specific sprites and lets compatible sprites cycle in the Pokémon browser.
- Filters out Pokémon, forms, biomes, and structures that are not available in the current environment.
- Offers calculation options for habitat fit, bite-reducer use, avoiding enchanted golden apples, and prioritizing shiny odds.
- Shows the blocks, fluids, structures, light, weather, height, and other conditions used by a route.
- Previews the resulting snack and can give or copy it when the current game allows it.
- Includes optional integration with Tom's Simple Storage terminals.
- Writes a detailed diagnostic report for troubleshooting.

CobbleSnack does not change Pokémon spawns or Poké Snack mechanics. It explains and calculates from the data available in your Minecraft profile.

## Server data and accuracy

CobbleSnack can only read resources that are installed or downloaded on your client. This includes compatible resources downloaded by AutoModpack. It cannot read private server-only data packs that the server has not provided to your Minecraft profile, so routes found only in private server files may be missing from its results.

## Requirements

- Minecraft 1.21.1
- Fabric Loader 0.16.0 or newer
- Fabric API
- Cobblemon 1.7.0 or newer
- Java 21

Tom's Simple Storage is optional.

## Installation

1. Install Fabric Loader, Fabric API, and Cobblemon for Minecraft 1.21.1.
2. Put the CobbleSnack JAR in the profile's `mods` folder.
3. Start the game and press **K**.

Install CobbleSnack on the client. A server does not need the JAR for players to use the calculator.

## Reporting a problem

Use the repository's **Issues** tab. Explain which modpack or server you used, what you selected, what CobbleSnack showed, and what you expected.

After reproducing the problem, attach:

`config/cobblesnack/diagnostics/session.log`

You can drag the log file into the GitHub issue text box. Restarting Minecraft replaces the session log, so copy it before starting another test session.

## Building from source

The included Gradle wrapper downloads the matching build tools automatically.

On Windows:

```text
gradlew.bat build
```

On Linux or macOS:

```text
bash ./gradlew build
```

The finished mod is written to `build/libs/cobblesnack-1.0.1.jar`. A Java 21 JDK is required.

## License

CobbleSnack's code is available under the MIT License.

The bundled E19-style minimap sprites are covered by the Mozilla Public License 2.0 and retain their original attribution. See `NOTICE` and `src/main/resources/META-INF/licenses/cobblesnack/`.
