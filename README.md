# PunchThemAll

**Turn any click into a recipe.** PunchThemAll is a Minecraft **NeoForge 26.1** mod that lets modpack
authors define what happens when a player left/right-clicks (with or without sneaking) on a block, a
fluid, or the air — optionally with a specific item in hand. Everything is plain **JSON** shipped in a
**datapack**. No Java, no scripting.

Each interaction can produce weighted **and** guaranteed drops, transform the clicked block/fluid,
cost the player health or hunger, grant potion effects, play sounds and particles, and be gated by
biome, dimension, time, weather, altitude, light, or player state — and it all shows up in **JEI**.

> **New in 2.3.0 (NeoForge 26.1):** a port, with no change to authoring — the same
> `schema_version: 2` files, the same config, the same behaviour. Your datapack needs one edit: the
> `pack.mcmeta` header, which Minecraft changed in 1.21.9. **EMI support is not in this build** —
> EMI has no 26.1 release yet, so there is nothing to build against; it returns when it does.
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
- 🌦️ **Conditions** — gate by biome/dimension (ids **or** `#tags`), time of day, weather, Y range,
  light level, sneaking, food, and XP.
- 💥 **Player feedback** — potion effects, damage, hunger cost, plus interaction-level sound/particles.
- 🔎 **Typed NBT predicates** — match item/block-entity data with clean `path` + range + filter rules.
- 📖 **JEI integration** — players browse every interaction, its inputs, drops and conditions,
  with item requirements written as plain sentences ("The item must have: Efficiency I - V") rather
  than raw tag structure.
- 🖥️ **Server-friendly** — interactions live in datapacks and the server syncs them to clients, so
  JEI is correct on dedicated servers with no extra setup.
- 🤖 **Automation-aware** — fake players / machines are supported with dedicated config gates.

## Requirements

| | |
| --- | --- |
| Minecraft | 26.1.2 |
| NeoForge | 26.1.x (built against 26.1.2.92) |
| Java | 25 — shipped with Minecraft 26.1, nothing to install |
| Recipe viewer | [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) 29.x (optional, for the recipe browser) |

## Install

1. Install NeoForge for 26.1 and (optionally) JEI.
2. Drop `pta-26.1.2-2.3.0.jar` into your `mods` folder.
3. Provide interactions with a datapack (below).

## Quick start

Interactions live in a **datapack**, under `data/<namespace>/pta/interaction/`. The quickest way is to
use the ready-made [example datapack](examples/punchthemall-examples): copy the
`punchthemall-examples` folder into a world's `datapacks/` folder and enable it.

To author your own, create `data/mypack/pta/interaction/flint_from_gravel.json` inside a datapack:

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

Run `/reload`. Sneak-left-click gravel with a shovel and you'll sometimes get flint. Open JEI and
search the **Interaction** category to see it.

## How it works

- Interactions are datapack data: files go in
  `data/<namespace>/pta/interaction/**/*.json`. The path becomes the id
  (e.g. `data/mypack/pta/interaction/early/flint.json` → `mypack:early/flint`).
- The server **syncs its loaded set to clients** on join and after `/reload`, so gameplay and JEI
  match on dedicated servers.
- Edit files and run `/reload` to apply changes live. Datapacks override each other by pack order, and
  you can gate a file with `neoforge:conditions` (e.g. only if another mod is present).
- Global behaviour (cooldowns, click/target gates, fake players, drop physics) is controlled by
  `config/punchthemall/pta-common.toml`.

## Documentation

| Doc | What's inside |
| --- | --- |
| [docs/getting-started.md](docs/getting-started.md) | **Start here.** A step-by-step guide that builds an interaction from scratch, with a cookbook and troubleshooting. |
| [docs/interaction-format.md](docs/interaction-format.md) | **Full JSON reference** for the `schema_version: 2` format. |
| [docs/interactions.md](docs/interactions.md) | Datapacks, loading, IDs, multiplayer, and the JEI category. |
| [docs/configuration.md](docs/configuration.md) | Every `pta-common.toml` key, defaults, and presets. |
| [docs/interaction.schema.json](docs/interaction.schema.json) | JSON Schema for editor autocomplete/validation. |
| [example catalogue](examples/punchthemall-examples/README.md) | **40 runnable examples**, one per feature, each with what it shows and how to trigger it. |
| [docs/backlog.md](docs/backlog.md) | Ideas, deferred work, and what is knowingly untested. |
| [CHANGELOG.md](CHANGELOG.md) | What changed in each version. |

## Building from source

Standard NeoForge / ModDevGradle workflow (Java 25). There is no CI; builds are run locally.

```bash
./gradlew build            # build the mod jar (into build/libs), tests included
./gradlew test             # unit tests only
./gradlew runClient        # launch the client to test
./gradlew runServer        # launch a dedicated server
./gradlew --refresh-dependencies   # if dependencies fail to resolve
```

The unit suite runs under FML (ModDevGradle's `unitTest` integration), because since 26.1 there is no
way to reach a usable Minecraft from a bare JVM. It covers the logic below the game — NBT matching,
drop and weight arithmetic, the codec, the resolver, the registry, sync batching. It does **not**
cover anything needing a live world (the click path, rendering), nor tags, which need a running
server.

**30 of the 242 tests currently fail**, all for one reason: 26.1 binds an item's default data
components when a server loads its datapacks, not in `Bootstrap`, so constructing an `ItemStack`
outside a server throws. That leaves the reward pipeline without unit coverage until the suite adopts
`testframework`'s `EphemeralTestServerProvider`. See `docs/porting/neoforge-26.1-plan.md` §10.3 and
`docs/backlog.md`.

For that layer, [`examples/dev-probe-pack`](examples/dev-probe-pack) is a by-hand harness: drop it
into a dev world, `/reload`, and read `run/logs/latest.log`. A green build has repeatedly not
predicted correctness on this mod — run the client.

## Compatibility

Works with any resource/tech mod, since interactions can require specific items/tags and target
specific blocks/fluids. JEI is supported natively.

## License & credits

Authored by **DrimoZ**. Free and open source under the [MIT License](LICENSE).
