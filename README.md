# PunchThemAll

**Turn any click into a recipe.** PunchThemAll is a Minecraft **Forge 1.20.1** mod that lets modpack
authors define what happens when a player left/right-clicks (with or without sneaking) on a block, a
fluid, or the air — optionally with a specific item in hand. Everything is plain **JSON** dropped in
the config folder. No Java, no scripting.

Each interaction can produce weighted **and** guaranteed drops, transform the clicked block/fluid,
cost the player health or hunger, grant potion effects, play sounds and particles, and be gated by
biome, dimension, time, weather, altitude, light, or player state — and it all shows up in **JEI**.

> **New in 2.0.0:** a cleaner `schema_version: 2` format, advanced rewards, conditions, effects,
> typed NBT predicates, optional datapack loading, and correct JEI on dedicated servers.
> See the [changelog](CHANGELOG.md).

---

## Features

- 🖱️ **Any click is a trigger** — left/right, sneaking or not, on a block, fluid, or the air.
- 🎒 **Hand-aware** — require an item or `#tag` in the main/off/any hand, match its NBT, and choose
  how it is spent: durability, shrink, or nothing.
- 🎁 **Rich rewards** — a weighted loot pool, always-given `guaranteed` drops, multiple `rolls`, and
  a Fortune/Looting-style bonus.
- 🔄 **Transformations** — swap the clicked block/fluid for another, copy block-state values, with
  sounds and particles.
- 🌦️ **Conditions** — gate by biome/dimension, time of day, weather, Y range, light level, sneaking,
  food, and XP.
- 💥 **Player feedback** — potion effects, damage, hunger cost, plus interaction-level sound/particles.
- 🔎 **Typed NBT predicates** — match item/block-entity data with clean `path` + range + filter rules.
- 📖 **JEI integration** — players browse every interaction, its inputs, drops, and conditions.
- 🖥️ **Server-friendly** — the registry is synced to clients, so JEI is correct on dedicated servers.
  Interactions can live in the config folder **and** (optionally) in datapacks.
- 🤖 **Automation-aware** — fake players / machines are supported with dedicated config gates.
- ♻️ **Backward compatible** — old interaction files keep working; new features are opt-in.

## Requirements

| | |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.x (built against 47.3.6) |
| [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) | required (recipe browser) |

## Install

1. Install Minecraft Forge for 1.20.1 and [JEI](https://www.curseforge.com/minecraft/mc-mods/jei).
2. Drop `PunchThemAll-1.20.1-2.0.0.jar` into your `mods` folder.
3. Launch once to generate `config/punchthemall/`, then add interaction files (below).

## Quick start

Create `config/punchthemall/interactions/flint_from_gravel.json`:

```json
{
  "schema_version": 2,
  "type": "shift_left_click",
  "hand": { "hand": "main", "match": "#minecraft:shovels", "consume": { "mode": "durability" } },
  "target": { "kind": "block", "match": "minecraft:gravel" },
  "rewards": {
    "weighted": [
      { "match": "minecraft:flint", "weight": 25, "count": 1 },
      { "match": "minecraft:air",   "weight": 75 }
    ]
  }
}
```

Then run `/reload` in-game (or reload the world). Sneak-left-click gravel with a shovel and you'll
sometimes get flint. Open JEI and search the **Interaction** category to see it.

More ready-to-copy files live in [`configExamples/interactions`](configExamples/interactions),
including a folder of focused v2 examples in
[`configExamples/interactions/v2`](configExamples/interactions/v2).

## How it works

- Interaction files go in `config/punchthemall/interactions/**/*.json`. The relative path becomes
  the interaction id (e.g. `early_game/flint.json` → `pta:early_game/flint`).
- Edit files and run `/reload` to apply changes live.
- Global behaviour (cooldowns, click/target gates, fake players, drop physics, loader options) is
  controlled by `config/punchthemall/pta-common.toml`.
- Optionally, set `Loader.load_from_datapacks = true` to also load
  `data/<namespace>/pta/interaction/*.json` from datapacks.

## Documentation

| Doc | What's inside |
| --- | --- |
| [docs/getting-started.md](docs/getting-started.md) | **Start here.** A step-by-step guide that builds an interaction from scratch, with a cookbook and troubleshooting. |
| [docs/interaction-format.md](docs/interaction-format.md) | **Full JSON reference** for the `schema_version: 2` format, with a legacy→v2 migration table. |
| [docs/interactions.md](docs/interactions.md) | Loading, reloading, IDs, multiplayer, and the JEI category. |
| [docs/configuration.md](docs/configuration.md) | Every `pta-common.toml` key, defaults, and presets. |
| [docs/curseforge.md](docs/curseforge.md) | Short project overview (used for the CurseForge page). |
| [CHANGELOG.md](CHANGELOG.md) | What changed in each version. |

## Building from source

This project follows the standard Forge MDK workflow (Java 17).

```bash
./gradlew build            # build the mod jar (into build/libs)
./gradlew compileJava      # compile sources only
./gradlew genIntellijRuns  # generate IntelliJ run configs
./gradlew --refresh-dependencies   # if dependencies fail to resolve
```

## Compatibility

Optional integrations are tested against Create, AE2, Ex Deorum, Thermal, and Click Machine. Because
interactions can require specific items/tags and target specific blocks/fluids, PunchThemAll pairs
well with resource and tech mods.

## License & credits

Authored by **DrimoZ**. See [LICENSE](LICENSE) for licensing terms.
