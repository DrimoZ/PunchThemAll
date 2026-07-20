# Getting started — authoring interactions

This is a hands-on guide for **modpack developers**. It builds a real interaction from scratch and
adds one feature at a time, so by the end you can write almost anything. Every snippet is
copy-paste-ready.

> Prefer to read the exhaustive field list instead? See [interaction-format.md](interaction-format.md).
> Prefer to copy finished files? See the [example datapack](../examples/punchthemall-examples).

**Contents**

1. [Setup](#1-setup)
2. [Your first interaction](#2-your-first-interaction)
3. [Requiring a tool in hand](#3-requiring-a-tool-in-hand)
4. [Drops: weighted, counts, guaranteed, rolls, fortune](#4-drops)
5. [Choosing the target: blocks, states, fluids, air](#5-choosing-the-target)
6. [Transforming the block](#6-transforming-the-block)
7. [Costs: damage and hunger](#7-costs)
8. [Effects, sounds and particles](#8-effects-sounds-and-particles)
9. [Conditions: biome, time, weather, Y, light, player](#9-conditions)
10. [Matching item / block NBT](#10-matching-nbt)
11. [Organising a pack](#11-organising-a-pack)
12. [Cookbook: common recipes](#12-cookbook)
13. [Troubleshooting](#13-troubleshooting)

---

## 1. Setup

Interactions are **datapack** data — the datapack registry `pta:interaction`. So you author them in a
datapack, not a config folder.

1. Install NeoForge 1.21.1, a recipe viewer ([JEI](https://www.curseforge.com/minecraft/mc-mods/jei)
   or [EMI](https://modrinth.com/mod/emi)), and PunchThemAll.
2. Make a datapack. In a world's `datapacks/` folder, create:

   ```text
   datapacks/mypack/
     pack.mcmeta
     data/mypack/pta/interaction/
   ```

   `pack.mcmeta`:
   ```json
   { "pack": { "pack_format": 48, "description": "My interactions" } }
   ```

3. Put `.json` files in `data/mypack/pta/interaction/` (subfolders are fine). Each file is one
   interaction; its id is `mypack:<path>` (e.g. `flint.json` → `mypack:flint`).
4. After editing files, run **`/reload`** in-game to apply changes. No restart needed.
5. Open **JEI/EMI** and look at the **Interaction** category to see what loaded.

> Not sure how a datapack is laid out? Copy the ready-made
> [example datapack](../examples/punchthemall-examples) and edit it.
>
> On a server the registry is synchronised to clients by vanilla, so JEI/EMI just work — no extra step.

> **Always start a file with `"schema_version": 2`.** It gives strict JSON validation and the clearest
> error messages. (This version of the mod only accepts `schema_version: 2`.)

If a file fails to load, the game log shows a line beginning with
`PunchThemAll - Incorrect Json format - <id> - <reason>`. Keep the log open while authoring.

---

## 2. Your first interaction

The smallest useful interaction: **left-click dirt to get coarse dirt.**

`data/mypack/pta/interaction/coarse_dirt.json`

```json
{
  "schema_version": 2,
  "type": "left_click",
  "target": { "kind": "block", "match": "minecraft:dirt" },
  "rewards": {
    "weighted": [
      { "match": "minecraft:coarse_dirt", "weight": 1 }
    ]
  }
}
```

- `type` — which click triggers it: `left_click`, `right_click`, `shift_left_click`,
  `shift_right_click`.
- `target.match` — the block you click. One id, a list, or a `#tag`.
- `rewards.weighted` — the item(s) you get.

Run `/reload`, left-click some dirt, and coarse dirt pops out.

---

## 3. Requiring a tool in hand

Most interactions want a specific tool. Let's require **any shovel** and damage it each time.

```json
{
  "schema_version": 2,
  "type": "shift_left_click",
  "hand": {
    "hand": "main",
    "match": "#minecraft:shovels",
    "consume": { "mode": "durability", "chance": 1.0 }
  },
  "target": { "kind": "block", "match": "minecraft:gravel" },
  "rewards": {
    "weighted": [
      { "match": "minecraft:flint", "weight": 25, "count": 1 },
      { "match": "minecraft:air",   "weight": 75 }
    ]
  }
}
```

- `hand.hand` — `main`, `off`, or `any`.
- `hand.match` — required item(s)/tag. **Leave `hand` out entirely to require an empty hand.**
- `hand.consume.mode`:
  - `durability` — damage a damageable tool (breaks when it runs out),
  - `shrink` — consume one from the stack (good for ingredients like buckets or seeds),
  - `none` — don't spend it.
- `hand.consume.chance` — probability of spending it (e.g. `0.5` = half the time).

> The `"minecraft:air"` entry with weight 75 is a **"nothing" filler**: 75% of clicks yield nothing.
> Weights are relative, so `25` vs `75` means a 25% chance of flint.

---

## 4. Drops

Everything about outputs lives in `rewards`.

### Weighted pool

```json
"rewards": {
  "weighted": [
    { "match": "minecraft:iron_nugget", "weight": 20, "count": { "min": 1, "max": 2 } },
    { "match": "minecraft:air",         "weight": 80 }
  ]
}
```

`count` accepts three shapes: `3`, `{ "count": 3 }`, or `{ "min": 1, "max": 3 }`.

### Guaranteed drops

Items in `guaranteed` are **always** given, on top of the weighted picks:

```json
"rewards": {
  "guaranteed": [ { "match": "minecraft:cobblestone", "count": 1 } ],
  "weighted":   [ { "match": "minecraft:coal", "weight": 15 }, { "match": "minecraft:air", "weight": 85 } ]
}
```

### Multiple rolls

`rolls` draws the weighted pool several times (guaranteed drops are given once):

```json
"rewards": { "rolls": 3, "weighted": [ { "match": "minecraft:coal", "weight": 15 }, { "match": "minecraft:air", "weight": 85 } ] }
```

### Fortune bonus

Add extra items to weighted picks based on an enchantment on the **held** item:

```json
"rewards": {
  "weighted": [ { "match": "minecraft:diamond", "weight": 100 } ],
  "fortune":  { "enchant": "minecraft:fortune", "factor": 1.0 }
}
```

With Fortune III and `factor: 1.0`, each weighted pick gets `round(3 × 1.0) = 3` extra items.

---

## 5. Choosing the target

### A block, optionally with a required state

```json
"target": {
  "kind": "block",
  "match": "minecraft:furnace",
  "state": {
    "whitelist": { "lit": "true" },
    "blacklist": { "waterlogged": "true" }
  }
}
```

State values are compared as text: use `"true"`, `"north"`, `"3"`, etc.

### A source fluid

Enable fluid interactions in the config (on by default), then:

```json
"target": { "kind": "fluid", "match": "minecraft:water" }
```

### The air (no block)

```json
"target": { "kind": "air" }
```

Air interactions require an **empty hand or a matching hand item** and are great for "cast a rod",
"swing a tool", etc.

### Anything (block or fluid)

```json
"target": { "kind": "any", "match": "#minecraft:logs" }
```

---

## 6. Transforming the block

Turn the clicked block/fluid into another one after a successful interaction:

```json
"transformation": {
  "chance": 0.7,
  "into": { "kind": "block", "id": "minecraft:sand", "state": { "facing": "copy_state_value" } },
  "sound": "minecraft:block.gravel.break",
  "particles": "minecraft:sand"
}
```

- `chance` — probability of the transformation happening.
- `into.kind` — `block`, `fluid`, or `air` (air = break the block).
- `into.state` — set specific state values, or `"copy_state_value"` to keep the original block's value.
- `particles` — a **block id** (block-break particles).

Transformations obey the `allow_transformations` config gate and happen at most once per click.

---

## 7. Costs

Make the interaction cost the player something:

```json
"costs": {
  "damage": { "chance": 1.0, "amount": 1 },
  "hunger": { "chance": 0.5, "amount": { "min": 2, "max": 8 } }
}
```

- `damage.amount` is in half-hearts (`2` = one heart).
- `hunger.amount` is in hunger points.
- `amount` accepts the same shapes as `count` (`3`, `{count}`, `{min,max}`).

These respect the `allow_player_damage` / `allow_food_consumption` config gates.

---

## 8. Effects, sounds and particles

Grant potion effects and play feedback on the interaction itself:

```json
"effects": [
  { "id": "minecraft:haste", "duration": 200, "amplifier": 0, "chance": 0.3 }
],
"sound": "minecraft:entity.player.levelup",
"particles": "minecraft:sugar_cane"
```

- `duration` is in ticks (`20` = 1 second); `amplifier` `0` = level I.
- `sound` / `particles` here fire on **every** success (transformations have their own sound/particles).

---

## 9. Conditions

Gate an interaction so it only fires in the right situation. Everything is optional — add only what
you need.

```json
"conditions": {
  "biomes":       { "whitelist": ["minecraft:desert"], "blacklist": [] },
  "time":         "night",
  "weather":      ["clear", "rain"],
  "y_range":      [-64, 40],
  "light":        { "max": 7 },
  "requires_sneaking": true,
  "player_state": { "min_food": 6, "min_xp_levels": 1 }
}
```

- `biomes` — exact biome **or** dimension ids (`minecraft:the_nether` works too). Use whitelist **or**
  blacklist, not both.
- `time` — `any`, `day`, or `night`.
- `weather` — any of `clear`, `rain`, `thunder`. Omit for "any weather".
- `y_range` — `[minY, maxY]`.
- `light` — block light `min`/`max` (0–15).
- `requires_sneaking` — `true`/`false`.
- `player_state` — minimum food and XP levels the player must have.

---

## 10. Matching NBT

Two ways to match item or block-entity data. You can use both together.

### Typed predicates (recommended)

Clean, validated, and shown in JEI:

```json
"nbt_predicates": [
  { "path": "Damage", "int_range": [0, 500] },
  { "path": "Enchantments[].lvl", "int_range": [2, 7], "where": "{id:\"minecraft:unbreaking\"}" }
]
```

- `path` — dotted path; a segment ending in `[]` iterates a list.
- `int_range` — `[min, max]`; omit to only test that the value **exists**.
- `where` — an SNBT string that filters which list elements count.
- All predicates in the list must pass (AND).

Put `nbt_predicates` on `hand` (to match the held item) or on `target` (to match a block entity such
as a chest's contents).

> A predicate on an **absent** tag fails. A brand-new tool has no `Damage` tag until it's used, so a
> `Damage` predicate won't match it.

### Raw SNBT whitelist/blacklist

For exact-shape matching, write NBT as an SNBT **string**:

```json
"nbt": {
  "whitelist": "{Damage:{RangeTag:[0,500]}}",
  "blacklist": "{Enchantments:[{id:\"minecraft:silk_touch\"}]}"
}
```

The `{RangeTag:[min,max]}` helper still works inside these strings.

---

## 11. Organising a pack

- **Everything is a datapack.** Interactions live at `data/<namespace>/pta/interaction/**/*.json`.
  Ship them in your modpack's datapack, or as a standalone datapack players drop into `datapacks/`.
- **Folders become ids.** `data/mypack/pta/interaction/create/crushing/gravel.json` →
  `mypack:create/crushing/gravel`. Keep filenames lowercase with underscores.
- **One interaction per file.** It keeps ids meaningful and the JEI/EMI list readable.
- **Toggle without deleting.** Add `"enabled": false` to a file to skip it.
- **Override & gate.** Datapacks override each other by pack order (later packs win for the same id),
  and you can add `neoforge:conditions` to a file to load it only when, say, another mod is present.
- **Dedicated servers just work.** The `pta:interaction` registry is synchronised to clients by
  vanilla, so JEI/EMI show the server's interactions with no extra setup.
- **Global tuning** (cooldowns, click/target gates, fake players, drop physics) lives in
  `config/punchthemall/pta-common.toml` — see [configuration.md](configuration.md).

---

## 12. Cookbook

**Hammer crushing (cobble → gravel → sand → dust), consuming durability:**

```json
{
  "schema_version": 2,
  "type": "shift_left_click",
  "hand": { "hand": "main", "match": "#c:tools", "consume": { "mode": "durability" } },
  "target": { "kind": "block", "match": "minecraft:cobblestone" },
  "transformation": { "chance": 1.0, "into": { "kind": "block", "id": "minecraft:gravel" }, "particles": "minecraft:gravel" },
  "rewards": { "weighted": [ { "match": "minecraft:air", "weight": 1 } ] }
}
```

**Night-only, deep, dark mining bonus with Fortune:**

```json
{
  "schema_version": 2,
  "type": "shift_left_click",
  "hand": { "hand": "main", "match": "#minecraft:pickaxes", "consume": { "mode": "durability" } },
  "target": { "kind": "block", "match": "minecraft:deepslate" },
  "rewards": {
    "weighted": [ { "match": "minecraft:raw_gold", "weight": 20 }, { "match": "minecraft:cobbled_deepslate", "weight": 80 } ],
    "fortune": { "enchant": "minecraft:fortune", "factor": 1.0 }
  },
  "conditions": { "time": "night", "y_range": [-64, 8], "light": { "max": 7 } }
}
```

**Bottle water for a chance at clay (consume the bottle):**

```json
{
  "schema_version": 2,
  "type": "right_click",
  "hand": { "hand": "main", "match": "minecraft:glass_bottle", "consume": { "mode": "shrink" } },
  "target": { "kind": "fluid", "match": "minecraft:water" },
  "rewards": { "weighted": [ { "match": "minecraft:clay_ball", "weight": 15 }, { "match": "minecraft:air", "weight": 85 } ] }
}
```

**"Ritual" empty-hand click in the air that costs food and grants an effect:**

```json
{
  "schema_version": 2,
  "type": "right_click",
  "target": { "kind": "air" },
  "rewards": { "weighted": [ { "match": "minecraft:air", "weight": 1 } ] },
  "costs": { "hunger": { "chance": 1.0, "amount": 4 } },
  "effects": [ { "id": "minecraft:regeneration", "duration": 100, "amplifier": 0, "chance": 1.0 } ],
  "sound": "minecraft:block.enchantment_table.use"
}
```

More single-feature examples: the [example datapack](../examples/punchthemall-examples).

---

## 13. Troubleshooting

**The file doesn't load.**
Check the log for `Incorrect Json format - <id> - <reason>`. Common causes: invalid JSON (a trailing
comma, a missing quote), an unknown item/block id, or an unknown tag. Make sure the file is inside a
**datapack** at `data/<namespace>/pta/interaction/` and that the datapack is enabled, then `/reload`.

**It loads but never triggers.**
- Turn on `Debug.log_skipped_interactions` in `pta-common.toml` to see why a click is skipped.
- Confirm the `type` matches how you're clicking (sneaking or not; left vs right).
- Check `hand` — if you omitted it, the interaction requires an **empty** hand.
- Check `conditions` (time/weather/biome/Y/light/food/XP) and the global config gates
  (`allow_left_click`, `allow_block_interactions`, …).
- Remember the per-player **cooldown** (`Interactions.cooldown_ticks`).

**An NBT predicate never matches.**
The tag is probably absent (e.g. `Damage` on a fresh tool). Loosen the predicate or match a tag that
actually exists on the item.

**JEI/EMI shows nothing on a dedicated server.**
The `pta:interaction` registry is a datapack registry, so vanilla synchronises it to clients on join
and on `/reload`. Reconnect or `/reload` on the server. In single-player this is automatic.

**My file is rejected with "schema_version … is not supported".**
This version only accepts `schema_version: 2`. Set `"schema_version": 2` at the top of the file.

---

## Where to go next

- **Full field reference:** [interaction-format.md](interaction-format.md)
- **Config keys & presets:** [configuration.md](configuration.md)
- **Datapacks, IDs, JEI/EMI, multiplayer:** [interactions.md](interactions.md)
- **Copy-paste example datapack:** [../examples/punchthemall-examples](../examples/punchthemall-examples)
