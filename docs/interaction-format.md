# PunchThemAll — Interaction format

Interactions are JSON files loaded from **datapacks**, and this version accepts **only
`schema_version: 2`** — the strictly-valid-JSON format documented below, parsed by a
Mojang-serialization `Codec` (shape errors are reported with a path and reason). Files declaring an
older `schema_version` are rejected with a clear error.

New to the mod? Read [getting-started.md](getting-started.md) first, and use the
[JSON schema](interaction.schema.json) for editor autocomplete/validation.

## Where files live

Interactions are datapack data:

- `data/<namespace>/pta/interaction/**/*.json` — one interaction per file, inside any loaded
  datapack. The path is the id: `data/mypack/pta/interaction/early/flint.json` → `mypack:early/flint`.
- The server **syncs its loaded set to clients** on join and after `/reload`, so gameplay and JEI/EMI
  match on dedicated servers.
- Datapacks override each other by pack order, and you can gate a file with `neoforge:conditions`.

There is no config-folder loading; the mod ships no interactions by default. See the ready-made
[example datapack](../examples/punchthemall-examples).

## Selectors (`match`)

Anywhere an item/block/fluid is selected, use `match`: a single string or a list. A leading `#`
means a tag; otherwise it is a registry id.

```json
"match": "minecraft:stick"
"match": ["minecraft:stick", "#c:tools"]
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

### Which one: `nbt` whitelist/blacklist, or `nbt_predicates`?

Both filter the same thing — does the held item (or the target's block entity) qualify. They differ in
how you write the condition, and both remain supported.

| | `nbt.whitelist` / `nbt.blacklist` | `nbt_predicates` |
| --- | --- | --- |
| Written as | one SNBT string: `"{Damage:{RangeTag:[0,500]}}"` | a list of JSON objects |
| Targets by | **shape** — your SNBT must mirror the tag structure | **path** — `Enchantments[].lvl` |
| Ranges | the `{RangeTag:[min,max]}` convention | the `int_range` field |
| Filter a list element | not possible | `where` |
| Exclude | yes, via `blacklist` | no — use a blacklist for that |

**Prefer `nbt_predicates`, and keep the whitelist/blacklist for exclusions** or for packs carried over
from v1.

The reason is a trap in the SNBT form. This:

```json
"whitelist": "{Enchantments:[{lvl:{RangeTag:[2s,7s]}},{id:\"minecraft:fortune\"}]}"
```

does **not** mean "Fortune between 2 and 7". It means *some* enchantment has level 2-7 **and** *some*
enchantment is Fortune — two independent tests that a Fortune I + Efficiency V tool passes. To tie a
level to a specific enchantment you need `where`:

```json
{ "path": "Enchantments[].lvl", "int_range": [2, 7], "where": "{id:\"minecraft:fortune\"}" }
```

Numeric widths (`5` vs `5s`) do not matter — comparisons are numeric on both sides.

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
- Item conditions are spelled out on the hand / target slot as two plain-language blocks — green
  **The item must have:** and red **The item must NOT have:** — covering `nbt.whitelist`,
  `nbt.blacklist` and `nbt_predicates` together, since a player does not care which syntax you used.
  Enchantments are named and levelled (*Efficiency I - V*), not printed as raw tags.
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
- **Biomes/dimensions** in `conditions.biomes` match by exact id (e.g. `minecraft:desert`,
  `minecraft:overworld`) **or** by biome **`#tag`** (e.g. `#minecraft:is_forest`).
- **Item NBT is matched against a version-stable view** (`Damage`, `Enchantments:[{id,lvl}]`,
  `custom`), so the same `path` / `nbt` expressions work across mod versions even though 1.21 stores
  item data as data components.
- **`effects` can be harmful.** They are whatever you declare (e.g. `minecraft:poison`), applied to
  the player on success.
- **Global config can still block an interaction** even if the file is valid — see
  [configuration.md](configuration.md). Turn on `Debug.log_skipped_interactions` to find out why.
- **Multiplayer:** the server is authoritative and syncs its interactions to clients, so JEI/EMI
  match it. See
  [interactions.md](interactions.md).

## Migrating from the legacy (Forge 1.20.1) format

This NeoForge version only loads `schema_version: 2`. If you have files from the old lenient format
(the Forge 1.20.1 branch), convert them with this mapping:

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

…and move the files from `config/punchthemall/interactions/` into a datapack at
`data/<namespace>/pta/interaction/`.
