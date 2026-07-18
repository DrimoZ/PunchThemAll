# PunchThemAll — Interaction format

Interactions are JSON files. Two schema versions are supported:

- **`schema_version: 2`** — the current, strictly-valid-JSON format documented below. Parsed by a
  Mojang-serialization `Codec`, so shape errors are reported with a path and reason.
- **legacy (no `schema_version`, or `1`)** — the original lenient format. Still loads, but is
  **deprecated** and logs a warning on load. See the `*.json` samples without a `_v2` suffix in
  `configExamples/interactions`.

A legacy file and its v2 translation behave identically in-game. The v2 format additionally unlocks
features that legacy files cannot express (guaranteed drops, multiple rolls, Fortune, potion effects,
extended conditions, interaction sound/particles, typed NBT predicates), and gives far clearer error
messages. New to the mod? Read [getting-started.md](getting-started.md) first.

## Where files live

- `config/punchthemall/interactions/**/*.json` — always loaded (editable by pack makers, hot-reload
  with `/reload`).
- `data/<namespace>/pta/interaction/**/*.json` — loaded **only** when
  `Loader.load_from_datapacks = true` in `punchthemall/pta-common.toml`. Layered on top of config;
  a datapack interaction overrides a config one with the same id, and datapack files are
  synchronised to clients by vanilla.

## Selectors (`match`)

Anywhere an item/block/fluid is selected, use `match`: a single string or a list. A leading `#`
means a tag; otherwise it is a registry id.

```json
"match": "minecraft:stick"
"match": ["minecraft:stick", "#forge:tools/hammers"]
```

## Full shape (all sections optional except `type`)

```jsonc
{
  "schema_version": 2,
  "enabled": true,                       // default true
  "type": "shift_left_click",            // right_click | shift_right_click | left_click | shift_left_click

  "hand": {
    "hand": "main",                      // any | main | off (default any)
    "match": "#minecraft:shovels",       // omit for "empty hand" semantics
    "consume": { "mode": "durability", "chance": 1.0 },  // mode: durability | shrink | none
    "nbt": {                             // SNBT strings (valid JSON strings)
      "whitelist": "{Damage:{RangeTag:[0,500]}}",
      "blacklist": "{Enchantments:[{id:\"minecraft:silk_touch\"}]}"
    },
    "nbt_predicates": [                  // typed predicates (see below)
      { "path": "Damage", "int_range": [0, 500] }
    ]
  },

  "target": {
    "kind": "block",                     // block | fluid | air | any (default block)
    "match": "minecraft:gravel",
    "state": {                           // block/fluid state properties
      "whitelist": { "lit": "true" },
      "blacklist": { "waterlogged": "true" }
    },
    "nbt": {                             // block-entity SNBT strings
      "whitelist": "{Items:[]}"
    },
    "nbt_predicates": [                  // typed block-entity predicates
      { "path": "Items[].Count", "int_range": [1, 64] }
    ]
  },

  "transformation": {
    "chance": 0.7,
    "into": { "kind": "block", "id": "minecraft:sand", "state": { "facing": "copy_state_value" } },
    "nbt": "{}",                         // SNBT string
    "sound": "minecraft:block.gravel.break",
    "particles": "minecraft:sand"        // a block id used for block particles
  },

  "rewards": {
    "rolls": 1,                          // number of weighted picks (default 1)
    "guaranteed": [                      // always dropped, in addition to the rolls
      { "match": "minecraft:flint", "count": 1 }
    ],
    "weighted": [                        // weighted drop pool
      { "match": "minecraft:clay_ball", "weight": 10, "count": { "min": 1, "max": 3 } },
      { "match": "minecraft:air",       "weight": 90 }   // count defaults to 1
    ],
    "fortune": { "enchant": "minecraft:fortune", "factor": 1.0 }  // +round(level*factor) per weighted pick
  },

  "costs": {
    "damage": { "chance": 1.0, "amount": 1 },                 // amount: int | {min,max} | {count}
    "hunger": { "chance": 0.5, "amount": { "min": 2, "max": 8 } }
  },

  "effects": [                           // potion effects applied to the player on success
    { "id": "minecraft:haste", "duration": 200, "amplifier": 0, "chance": 0.3 }
  ],
  "sound": "minecraft:block.gravel.hit", // feedback played on the interaction itself
  "particles": "minecraft:gravel",       // a block id used for interaction particles

  "conditions": {
    "biomes": {                          // dimension or biome ids (exact match)
      "whitelist": ["minecraft:overworld"],
      "blacklist": []
    },
    "time": "day",                       // any | day | night
    "weather": ["clear", "rain"],        // clear | rain | thunder (any if omitted)
    "y_range": [-64, 128],
    "light": { "min": 0, "max": 7 },     // block light level 0-15
    "requires_sneaking": true,
    "player_state": { "min_food": 6, "min_xp_levels": 1 }
  }
}
```

### Typed NBT predicates (`nbt_predicates`)

A validated alternative to raw SNBT whitelist/blacklist, usable on `hand` and `target`:

- `path` — dotted path into the tag; a segment ending in `[]` iterates a list.
- `int_range` — `[min, max]`; the predicate holds if a reachable numeric leaf is in range. Omit to
  test only for presence.
- `where` — an SNBT string filtering which list elements qualify.

```json
{ "path": "Enchantments[].lvl", "int_range": [2, 7], "where": "{id:\"minecraft:unbreaking\"}" }
```

All predicates in a list must hold (AND). They combine with the SNBT `nbt` whitelist/blacklist.

> **Absent tags don't match.** A predicate on a key that isn't present fails. For example, a brand-new
> tool has no `Damage` tag until it takes damage, so `{ "path": "Damage", "int_range": [0, 500] }`
> won't match it. This matches how the SNBT whitelist behaves.

### How it shows in JEI

Everything is visible in the **Interaction** category:

- `guaranteed` drops appear as extra output slots (tooltip: *Guaranteed*).
- `weighted` drops show their chance and count range.
- `nbt_predicates` are listed in the tooltip of the hand / target slot.
- Hovering the **arrow** shows a summary: `rolls`, Fortune bonus, `effects`, all `conditions`
  (time/weather/Y/light/sneaking/food/XP), and whether the interaction plays a sound / particles.

The interaction id is shown in the click-type tooltip, which is handy when reporting an issue.

### Count / range

`count` (rewards) and `amount` (costs) accept three shapes, unified to a `[min, max]` range:

- an integer: `3`
- `{ "count": 3 }`
- `{ "min": 1, "max": 3 }` (`max` defaults to `min`)

Effective floor is `0` for reward pools and `1` for player costs.

### NBT

NBT is written as an explicit **SNBT string** (`"{Damage:0}"`), so files stay valid JSON. The
`RangeTag` convention (`{Damage:{RangeTag:[0,500]}}`) still works inside these strings.

## Behaviour notes & gotchas

- **Only `type` is required.** Every other section is optional; omit what you don't need.
- **`type` values:** `left_click`, `right_click`, `shift_left_click`, `shift_right_click`.
- **Empty hand vs. any item.** Omit `hand`, or use `hand` with no `match`, to require an **empty**
  hand. Add `match` to require specific items/tags.
- **`consume` only spends the item on success.** `durability` damages a damageable item;
  `shrink` removes one from the stack; `none` leaves it untouched. `chance` is the probability of
  spending it.
- **Rolls default to 1.** Without a `rewards` block, nothing is dropped. With `weighted` only, you
  get exactly one weighted pick (the classic behaviour). `guaranteed` items are always given.
- **Fortune** reads the enchantment from the **held** item, so it only helps interactions that use a
  hand item; the bonus is `round(level × factor)` extra items per weighted pick.
- **Transformations happen at most once per click**, after a successful drop, subject to `chance`
  and the `allow_transformations` config gate.
- **`particles`** takes a **block id** (block-break particles), not a particle-type id.
- **Biomes/dimensions are matched by exact id** (e.g. `minecraft:desert`, `minecraft:overworld`).
  Biome **tags** are not supported here yet.
- **`effects` can be harmful.** They are whatever you declare (e.g. `minecraft:poison`), applied to
  the player on success.
- **Global config can still block an interaction** even if the file is valid — see
  [configuration.md](configuration.md). Turn on `Debug.log_skipped_interactions` to find out why.
- **Multiplayer:** gameplay uses the server's interactions; the JEI list is synchronised from the
  server, so a dedicated server is authoritative. See [interactions.md](interactions.md).

## Legacy → v2 quick reference

| legacy | v2 |
| --- | --- |
| `item` / `items` / `tag` / `tags` | `match` (with `#` for tags) |
| `block` / `blocks` / `fluid` / `fluids` (in `block`) | `target.match` + `target.kind` |
| `block` section | `target` |
| `pool` (`chance` = weight) | `rewards.weighted` (`weight`) |
| `damage` / `hunger` | `costs.damage` / `costs.hunger` (`amount`) |
| `biome` | `conditions.biomes` |
| `damageable: true` | `consume.mode = "durability"` |
| `consumable: true` | `consume.mode = "shrink"` |
| pseudo-JSON NBT object | SNBT string |
| `copy_state_value` | `copy_state_value` (unchanged) |
```
