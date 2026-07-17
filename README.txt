PunchThemAll
============

PunchThemAll is a NeoForge 1.21.1 mod that lets modpack authors create
JSON-defined interactions: click a block, fluid, or the air with an optional
hand item and receive configurable drops, transformations, damage, hunger costs,
biome restrictions, block-state checks, and NBT checks.

The mod is designed for modpacks: interactions live in the normal NeoForge config
folder and can be reloaded, while global behavior is controlled by the common
TOML config.

Main features
-------------

* JSON interactions loaded from `config/punchthemall/interactions`.
* Optional recursive interaction folders for larger packs.
* Left click, right click, sneak click, regular click, block, fluid, and air
  interaction support.
* Weighted output pools with min/max counts.
* Optional hand item consumption or durability damage.
* Optional block/fluid transformations with states, NBT, particles, and sounds.
* Optional player damage and food consumption costs.
* Fake-player support for automation, with dedicated config gates.
* JEI category for browsing configured interactions, outputs, chances,
  transformations, biome filters, state/NBT filters, and interaction IDs.

Configuration quick start
-------------------------

NeoForge writes the common config to:

`config/punchthemall/pta-common.toml`

The config is split into five sections:

* `PunchThemAll.Interactions` controls the global interaction pipeline.
* `PunchThemAll.Players` controls real-player and fake-player behavior.
* `PunchThemAll.Drops` controls inventory insertion and world drop physics.
* `PunchThemAll.Loader` controls JSON discovery and ID generation.
* `PunchThemAll.Debug` controls optional diagnostic logging.

See `docs/configuration.md` for every key, default value, and recommendation.

Interaction JSON quick start
----------------------------

Put interaction JSON files in:

`config/punchthemall/interactions`

With recursive discovery enabled, subfolders are valid and become part of the
interaction ID. For example:

`config/punchthemall/interactions/early_game/flint.json`

loads as:

`pta:early_game/flint`

Example files are provided in `configExamples/interactions`. See
`docs/interactions.md` for loader behavior, reload notes, and JEI visibility.

Development setup
-----------------

This project follows the NeoForge/NeoGradle workflow for NeoForge 1.21.1.

Common commands:

* `./gradlew genIntellijRuns` - generate IntelliJ run configs.
* `./gradlew genEclipseRuns` - generate Eclipse run configs.
* `./gradlew compileJava` - compile the mod sources.
* `./gradlew build` - build the mod jar.

If dependencies are missing, run `./gradlew --refresh-dependencies`.

Additional resources
--------------------

* NeoForge documentation: https://docs.neoforged.net/docs/1.21.1/gettingstarted/
* NeoForge website: https://neoforged.net/
* NeoForge Discord: https://discord.neoforged.net/
