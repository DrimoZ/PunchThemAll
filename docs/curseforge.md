# PunchThemAll

**Turn any click into a recipe.** PunchThemAll lets pack makers define what happens when a player
left/right-clicks (with or without sneaking) on a block, a fluid or the air — optionally with a
specific item in hand. No Java, no KubeJS: just JSON files in a **datapack**
(`data/<namespace>/pta/interaction/`).

Every interaction can produce weighted or guaranteed drops, transform the clicked block/fluid, cost
the player health or hunger, grant potion effects, play sounds and particles, and be gated by biome,
dimension, time, weather, altitude, light or player state. Everything shows up in **JEI** and **EMI**.

---

## Highlights

- 🖱️ **Any click is a trigger** — left/right, sneaking or not, on block, fluid or air.
- 🎒 **Hand-aware** — require an item or tag in the main/off/any hand, match its NBT, and choose how
  it is spent (durability, shrink, or nothing).
- 🎁 **Rich rewards** — a weighted loot pool plus always-given `guaranteed` drops, multiple `rolls`,
  and a Fortune/Looting-style bonus.
- 🔄 **Transformations** — swap the clicked block/fluid for another, copying block-state values,
  with sounds and particles.
- 🌦️ **Conditions** — gate by biome/dimension tags, time of day, weather, Y range, light level,
  sneaking, food and XP.
- 💥 **Player feedback** — potion effects, damage, hunger cost, and interaction-level sound/particles.
- 🔎 **Typed NBT predicates** — match item/block-entity data with clean `path` + range + filter rules.
- 📖 **JEI & EMI integration** — players can browse every interaction, its inputs, drops and conditions.
- 🖥️ **Server-friendly** — interactions are a datapack registry synchronised to clients by vanilla, so
  JEI/EMI are correct on dedicated servers with zero setup. Datapacks also override and can be gated
  with `neoforge:conditions`.

---

## A tiny example

Sneak-left-click gravel with any shovel to sometimes get clay, turn the gravel into sand, and lose a
little hunger:

```json
{
  "schema_version": 2,
  "type": "shift_left_click",
  "hand": { "hand": "main", "match": "#minecraft:shovels", "consume": { "mode": "durability" } },
  "target": { "kind": "block", "match": "minecraft:gravel" },
  "transformation": { "chance": 0.7, "into": { "kind": "block", "id": "minecraft:sand" }, "particles": "minecraft:sand" },
  "rewards": { "weighted": [
    { "match": "minecraft:clay_ball", "weight": 10, "count": { "min": 1, "max": 3 } },
    { "match": "minecraft:air", "weight": 90 }
  ] },
  "costs": { "hunger": { "chance": 0.1, "amount": { "min": 1, "max": 3 } } }
}
```

## A "kitchen-sink" example

Mine stone at night, deep underground, in the dark, with a Fortune pickaxe — for guaranteed
cobblestone, a chance at iron nuggets (doubled per Fortune level), a Haste buff and particles:

```json
{
  "schema_version": 2,
  "type": "shift_right_click",
  "hand": {
    "hand": "main",
    "match": "#minecraft:pickaxes",
    "consume": { "mode": "durability" },
    "nbt_predicates": [ { "path": "Enchantments[].lvl", "int_range": [1, 5], "where": "{id:\"minecraft:efficiency\"}" } ]
  },
  "target": { "kind": "block", "match": "minecraft:stone" },
  "rewards": {
    "rolls": 2,
    "guaranteed": [ { "match": "minecraft:cobblestone", "count": 1 } ],
    "weighted": [
      { "match": "minecraft:iron_nugget", "weight": 20, "count": { "min": 1, "max": 2 } },
      { "match": "minecraft:air", "weight": 80 }
    ],
    "fortune": { "enchant": "minecraft:fortune", "factor": 1.0 }
  },
  "effects": [ { "id": "minecraft:haste", "duration": 100, "chance": 0.25 } ],
  "sound": "minecraft:block.stone.hit",
  "particles": "minecraft:stone",
  "conditions": { "time": "night", "y_range": [-64, 40], "light": { "max": 7 } }
}
```

---

## Documentation

- **Getting started (step-by-step):** [getting-started.md](getting-started.md)
- **Full JSON reference:** [interaction-format.md](interaction-format.md)
- **Datapacks, loading & JEI/EMI:** [interactions.md](interactions.md)
- **Config options:** [configuration.md](configuration.md)
- **Editor schema:** [interaction.schema.json](interaction.schema.json)
- **Changelog:** [CHANGELOG.md](../CHANGELOG.md)
- **Copy-paste example datapack:** [`examples/punchthemall-examples`](../examples/punchthemall-examples)

## Compatibility

- Minecraft **1.21.1**, NeoForge **21.1.x**.
- **JEI** or **EMI** (optional) for the recipe browser — both are supported natively.
- Pairs well with resource/tech mods (Create, AE2, Ex Deorum, Thermal, Click Machine, …).
