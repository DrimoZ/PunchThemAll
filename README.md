# PunchThemAll

**Turn any click into a recipe.** PunchThemAll is a Minecraft **NeoForge 1.21.1** mod that lets modpack
authors define what happens when a player left/right-clicks (with or without sneaking) on a block, a
fluid, or the air — optionally with a specific item in hand. Everything is plain **JSON** shipped in a
**datapack**. No Java, no scripting.

Each interaction can produce weighted **and** guaranteed drops, transform the clicked block/fluid,
cost the player health or hunger, grant potion effects, play sounds and particles, and be gated by
biome, dimension, time, weather, altitude, light, or player state — and it all shows up in **JEI** and
**EMI**.

> **New in 2.3.0:** transformations can act on a **block other than the one you clicked**, and can
> **break** or **place** rather than only overwrite — at an offset read in world axes, against your
> facing, or out of the clicked face, several per click. Claim mods can veto them and server owners
> get a distance cap. Existing files are unaffected: without `op` or `at`, a transformation does
> exactly what it did before. See the [changelog](CHANGELOG.md).
>
> *Since 2.1.0 (NeoForge 1.21.1):* interactions live in **datapacks** rather than the config folder,
> native **EMI** support alongside JEI, and `left_click` on **air** works.

---

## Features

- 🖱️ **Any click is a trigger** — left/right, sneaking or not, on a block, fluid, or the air.
- 🎒 **Hand-aware** — require an item or `#tag` in the main/off/any hand, match its NBT, and choose
  how it is spent: durability, shrink, or nothing.
- 🎁 **Rich rewards** — a weighted loot pool, always-given `guaranteed` drops, multiple `rolls`, and
  a Fortune/Looting-style bonus.
- 🔄 **Transformations** — swap the clicked block/fluid for another, copy block-state values, with
  sounds and particles.
- 📐 **…on a block other than the one you clicked** — `break`, `place` or `replace` at a relative
  offset, read in world axes, against the player's facing, or out of the clicked face. Several per
  click, each with its own chance and its own condition on the destination. Claim mods can veto them.
- 🌦️ **Conditions** — gate by biome/dimension (ids **or** `#tags`), time of day, weather, Y range,
  light level, sneaking, food, and XP.
- 💥 **Player feedback** — potion effects, damage, hunger cost, plus interaction-level sound/particles.
- 🔎 **Typed NBT predicates** — match item/block-entity data with clean `path` + range + filter rules.
- 📖 **JEI & EMI integration** — players browse every interaction, its inputs, drops and conditions,
  with item requirements written as plain sentences ("The item must have: Efficiency I - V") rather
  than raw tag structure.
- 🖥️ **Server-friendly** — interactions live in datapacks and the server syncs them to clients, so
  JEI/EMI are correct on dedicated servers with no extra setup.
- 🤖 **Automation-aware** — fake players / machines are supported with dedicated config gates.

## Requirements

| | |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.x (built against 21.1.241) |
| Recipe viewer | [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) **or** [EMI](https://modrinth.com/mod/emi) (optional, for the recipe browser) |

## Install

1. Install NeoForge for 1.21.1 and (optionally) JEI or EMI.
2. Drop `pta-1.21.1-2.3.0.jar` into your `mods` folder.
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

Run `/reload`. Sneak-left-click gravel with a shovel and you'll sometimes get flint. Open JEI/EMI and
search the **Interaction** category to see it.

## How it works

- Interactions are datapack data: files go in
  `data/<namespace>/pta/interaction/**/*.json`. The path becomes the id
  (e.g. `data/mypack/pta/interaction/early/flint.json` → `mypack:early/flint`).
- The server **syncs its loaded set to clients** on join and after `/reload`, so gameplay and JEI/EMI
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
| [docs/interactions.md](docs/interactions.md) | Datapacks, loading, IDs, multiplayer, and the JEI/EMI category. |
| [docs/configuration.md](docs/configuration.md) | Every `pta-common.toml` key, defaults, and presets. |
| [docs/interaction.schema.json](docs/interaction.schema.json) | JSON Schema for editor autocomplete/validation. |
| [example catalogue](examples/punchthemall-examples/README.md) | **51 runnable examples**, one per feature, each with what it shows and how to trigger it. |
| [docs/backlog.md](docs/backlog.md) | Ideas, deferred work, and what is knowingly untested. |
| [CHANGELOG.md](CHANGELOG.md) | What changed in each version. |

## Building from source

Standard NeoForge / ModDevGradle workflow (Java 21). There is no CI; builds are run locally.

```bash
./gradlew build            # build the mod jar (into build/libs), unit tests included
./gradlew test             # unit tests only — a few seconds
./gradlew runGameTestServer # in-world tests, headless, no client
./gradlew verify           # both suites, unit first
./gradlew runClient        # launch the client to test by hand
./gradlew runServer        # launch a dedicated server
./gradlew --refresh-dependencies   # if dependencies fail to resolve
```

There are two automated suites, and they cover different halves of the mod.

The **unit suite** boots the game's registries in process, so it exercises real `ItemStack` and NBT
behaviour. It covers the logic below the game — NBT matching, drop and weight arithmetic, the codec,
the resolver, the registry, sync batching, and that every shipped example parses. It is fast enough
to run on every change.

The **game tests** run in a real `ServerLevel` on a headless server, which is the only way to test
code whose every decision is a question about the world: whether a block can be replaced, whether it
would survive where it is put, what breaking it drops. They live in `src/main/java/.../gametest` and
cover the transformation pipeline. The tests themselves take about two seconds; almost all of the
wall time is Minecraft starting up.

Neither reaches the click path itself, rendering, or tags. For that layer,
[`examples/dev-probe-pack`](examples/dev-probe-pack) is a by-hand harness: drop it into a dev world,
`/reload`, and read `run/logs/latest.log`. A green build has repeatedly not predicted correctness on
this mod — `docs/backlog.md` lists what is still unwatched.

## Compatibility

Works with any resource/tech mod, since interactions can require specific items/tags and target
specific blocks/fluids. JEI and EMI are both supported natively.

## License & credits

Authored by **DrimoZ**. Free and open source under the [MIT License](LICENSE).
