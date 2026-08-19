# PunchThemAll — Interaction format

Interactions are JSON files loaded from **datapacks**, and this version accepts **only
`schema_version: 2`** — the strictly-valid-JSON format documented below, parsed by a
Mojang-serialization `Codec` (shape errors are reported with a path and reason). Files declaring an
older `schema_version` are rejected with a clear error.

New to the mod? Read [getting-started.md](getting-started.md) first, and use the
[JSON schema](interaction.schema.json) for editor autocomplete/validation.

Looking for a whole working interaction rather than a field? See the
[cookbook](cookbook.md). Wondering whether a thing is possible at all? See
[what it can and cannot do](capabilities.md).

## Where files live

Interactions are datapack data:

- `data/<namespace>/pta/interaction/**/*.json` — one interaction per file, inside any loaded
  datapack. The path is the id: `data/mypack/pta/interaction/early/flint.json` → `mypack:early/flint`.
- The server **syncs its loaded set to clients** on join and after `/reload`, so gameplay and JEI/EMI
  match on dedicated servers.
- Datapacks override each other by pack order, and you can gate a file with `neoforge:conditions`.

There is no config-folder loading; the mod ships no interactions by default. See the ready-made
[example datapack](../examples/punchthemall-examples).

## Every field, and a file that uses it

Each row points at a small, runnable example in the
[example datapack](../examples/punchthemall-examples). The
[catalogue](../examples/punchthemall-examples/README.md) says what each one does and how to trigger
it in game.

| Field | Values | Example |
| --- | --- | --- |
| `type` *(required)* | `left_click`, `right_click`, `shift_left_click`, `shift_right_click` | `minimal` |
| `enabled` | `true` (default) / `false` | `enabled_false` |
| `hidden` | `false` (default) / `true` — loads and fires, but JEI/EMI don't list it | `hidden_from_viewers` |
| `hand.hand` | `any` (default), `main`, `off` | `hand_off_hand` |
| `hand.match` | id, `#tag`, list, or `[]` for an empty hand | `hand_empty` |
| `hand.consume.mode` | `none` (default), `shrink`, `durability` | `hand_item_and_consume` |
| `hand.consume.chance` | `0.0`–`1.0`, default `1.0` — whether anything is spent | `hand_consume_chance` |
| `hand.consume.count` | int or `{min,max}`, floor `1`, default `1` — how much is spent | `hand_consume_count` |
| `hand.nbt` | SNBT `whitelist` / `blacklist` | `hand_nbt_whitelist` |
| `hand.nbt_predicates` | `path` + `int_range` + `where` | `hand_nbt_predicates` |
| `target.kind` | `block` (default), `fluid`, `air`, `any` — `any` resolves either way, it does not mix the two | `target_kind_any` |
| `target.match` | id, `#tag`, list | `target_any_with_tag` |
| `target.state` | property → value, `whitelist` / `blacklist` | `target_state_blacklist` |
| `target.nbt` | block-entity SNBT | `target_block_entity_nbt` |
| `target.nbt_predicates` | block-entity predicates | `block_entity_predicates` |
| `transformation.chance` *(required in the block)* | `0.0`–`1.0` | `transformation_state_copy` |
| `transformation.into` | omit to break, or `kind` + `id` + `state` | `transformation_break`, `transformation_into_fluid` |
| `transformation.into.state` | property → value, or `copy_state_value` | `transformation_state_copy` |
| `transformation.nbt` | SNBT written into the new block entity | `transformation_block_entity_nbt` |
| `transformation.op` | `replace` (default), `break`, `place` | `transformation_break_neighbour` |
| `transformation.at` | `x`/`y`/`z` + `relative_to` | `transformation_offset_place` |
| `transformation.require` | same shape as `target`, asked of the destination | `transformation_offset_place` |
| `transformation.drops` | `op: break` only: `true`/`"vanilla"`, `false`/`"none"`, `"tool"` | `transformation_break_neighbour` |
| `transformation` as a list | several transformations from one click | `transformation_multi` |
| `transformation.at.to` | second corner — the offset becomes a box | `transformation_region` |
| `transformation` as a group | `chance` + `all`, one roll for the whole set | `transformation_group_chance` |
| `transformation.into.kind: copy` | write the block found at `from` | `transformation_move_block` |
| `rewards.at` | where the drops land, default the interacted block | `transformation_break_neighbour` |
| `conditions.neighbours` | `at` + `block` + `invert` — gate on the surroundings | `conditions_neighbours` |
| `rewards.weighted` | `match` + `weight` + `count` + `nbt` | `rewards_count_shapes` |
| `rewards.guaranteed` | same shape, always dropped | `rewards_guaranteed_and_rolls` |
| `rewards.rolls` | integer, default `1` | `rewards_multi_match` |
| `rewards.fortune` | `enchant` + `factor` | `rewards_fortune` |
| `costs.damage` / `costs.hunger` | `chance` + `amount` | `costs_damage_and_hunger` |
| `conditions.biomes` | ids, dimension ids, `#tags`; `whitelist` / `blacklist` | `conditions_dimension_and_biome_tag` |
| `conditions.time` | `any` (default), `day`, `night` | `conditions_time_weather` |
| `conditions.weather` | list of `clear`, `rain`, `thunder` | `conditions_time_weather` |
| `conditions.y_range` | `[min, max]` | `conditions_y_light_player` |
| `conditions.light` | `min` / `max` | `conditions_y_light_player` |
| `conditions.requires_sneaking` | `true` / `false` — **folded into `type`**, see below | `conditions_sneaking` |
| `conditions.player_state` | `min_food`, `min_xp_levels` | `conditions_y_light_player` |
| `effects` | `id` + `duration` + `amplifier` + `chance` | `effects_multiple` |
| `sound` / `particles` | registry ids | `effects_and_feedback` |
| `neoforge:conditions` | vanilla-style load conditions | `conditional_load_mod_present` |

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
  "hidden": false,                       // default false — hide from JEI/EMI without disabling it
  "type": "shift_left_click",            // right_click | shift_right_click | left_click | shift_left_click

  "hand": {
    "hand": "main",                      // any | main | off (default any)
    "match": "#minecraft:shovels",       // omit for "empty hand" semantics
    "consume": {                         // mode: durability | shrink | none
      "mode": "durability",
      "chance": 1.0,                     // probability that anything is spent at all
      "count": 1                         // how much is spent when it is — int, or { "min": 3, "max": 5 }
    },
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

  // One object, an array of them applied in order, or { "chance": 0.7, "all": [ ... ] }
  // to roll once for the whole set.
  "transformation": {
    "chance": 0.7,
    "op": "replace",                     // replace (default) | break | place
    "at": { "x": 0, "y": 0, "z": 0, "relative_to": "world",  // omit for the block itself
             "to": { "x": 1, "y": 0, "z": 1 } },                 // optional: makes it a box
    "require": { "match": "minecraft:air" },                   // what the destination must already be
    "drops": true,                       // op: break only — true | false | "tool" (honours the held item)
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
    "at": { "y": 1 },                    // where the drops land (default: the interacted block)
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

### Transformations: what, and where

A transformation does one of three things, chosen with `op`:

| `op` | what it does | needs `into` | drops |
| --- | --- | --- | --- |
| `replace` *(default)* | overwrites whatever is at the destination | no — omit it to write air | no |
| `break` | destroys the block that is there, like a player would | no — it is rejected | yes, unless `"drops": false` |
| `place` | writes a block, but only where there is room for one | **yes** | no |

`replace` with no `into` and `break` look similar and are not: the first makes the block vanish, the
second breaks it, with the particles, the sound and — by default — its loot.

`at` moves the destination off the block that was clicked. The three numbers are always *right, up,
forward*; `relative_to` decides what right and forward point at:

| `relative_to` | forward (`z`) | right (`x`) |
| --- | --- | --- |
| `world` *(default)* | south | east |
| `player` | the way the player is facing, flattened to four directions | the player's right |
| `face` | out of the clicked face | the player's right |

`y` is world up in every frame, so looking at your feet never tips the frame over.

```json
"transformation": {
  "chance": 1.0,
  "op": "place",
  "at": { "y": 1 },
  "require": { "match": "minecraft:air" },
  "into": { "id": "minecraft:torch" }
}
```

`require` is the same shape as `target`, asked of the destination instead of the clicked block. It is
optional: without it, `replace` overwrites whatever is in the way. Note that `place` already refuses
an occupied destination — and one where the block could not survive, so it will not leave you a torch
that pops a tick later — so `require` is for the finer cases ("only if it is dirt").

Writing a list applies several transformations from one click:

```json
"transformation": [
  { "chance": 1.0, "op": "break", "at": { "y": 1 } },
  { "chance": 1.0, "op": "place", "at": { "y": 1 }, "into": { "id": "minecraft:torch" } }
]
```

Each entry rolls its own `chance` independently. Every destination is worked out and checked against
the world *as it was when you clicked*, before any of them is written — so the order you declare them
in does not change what `require` sees.

> **On a server this reaches past what the player is pointing at.** Break and place events are posted
> for every transformation, so claim mods can veto them, and `max_transformation_offset` caps how far
> a datapack can reach. Both are in the config; see [configuration.md](configuration.md).

### Acting on a whole region

An offset can name a second corner, and then covers the box between the two:

```json
"at": { "x": -1, "y": 1, "z": -1, "to": { "x": 1, "y": 1, "z": 1 } }
```

That is a 3x3 slab one block up — nine blocks from one entry. Both corners are read in the same
frame, and in a rotating frame the box turns as a whole, so a shape drawn one way lands that way.

Every block in a region counts against `max_transformations_per_interaction` (default 64), and each
is checked on its own: a `place` over a region fills the gaps and leaves the occupied blocks alone
rather than failing outright.

### Rolling a whole set at once

Each entry rolls its own `chance`, which for a pattern means a different, half-built shape every
time. To roll once for the set, wrap it:

```json
"transformation": {
  "chance": 0.7,
  "all": [
    { "chance": 1.0, "op": "break", "at": { "y": 1 } },
    { "chance": 1.0, "op": "place", "at": { "y": 1 }, "into": { "id": "minecraft:torch" } }
  ]
}
```

Seven times in ten the pair is attempted; each entry then rolls its own chance inside that.

### Moving a block instead of naming one

`into: { "kind": "copy" }` writes whatever block stands at `from` — the interacted block unless you
say otherwise:

```json
"transformation": [
  { "chance": 1.0, "at": { "y": 1 }, "into": { "kind": "copy" } },
  { "chance": 1.0, "op": "break", "drops": false }
]
```

That moves the clicked block one up. The copy is read when you click, before anything is written, so
pairing it with a break of its own source works — the break cannot empty the source first.

### Where the drops land

Rewards appear at the interacted block. When the interaction really acts somewhere else, move them:

```json
"rewards": { "at": { "y": 1 }, "guaranteed": [ { "match": "minecraft:flint" } ] }
```

### Requiring a block nearby

`require` asks about a block a transformation is going to change. To gate the **interaction itself**
on its surroundings — which is what multi-block setups are made of — use `conditions.neighbours`:

```json
"conditions": {
  "neighbours": [
    { "at": { "y": -1 }, "block": { "match": "minecraft:obsidian" } },
    { "at": { "y": 1 }, "block": { "match": "#minecraft:logs" }, "invert": true }
  ]
}
```

"Obsidian underneath, and no log on top." The `block` half is a `target`, so the syntax is the one
you already know, states included. All of them must hold. `invert` flips one. A neighbour outside the
world or in an unloaded chunk counts as not matching, so a recipe never fires on evidence nobody
could see.

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
- The hand slot's tooltip shows the consume chance, plus an *Amount* line whenever `consume.count`
  is anything other than `1`. (EMI builds its slots in one pass, so it carries the same line — held
  item, chance and amount — on the arrow summary instead.)
- Hovering the **arrow** shows a summary: `rolls`, Fortune bonus, `effects`, all `conditions`
  (time/weather/Y/light/sneaking/food/XP), and whether the interaction plays a sound / particles.
- Interactions marked `hidden: true` are left out entirely — of the category, and of the height it
  reserves for drop rows.

The interaction id is shown in the click-type tooltip, which is handy when reporting an issue.

### Count / range

`count` (rewards, `hand.consume`) and `amount` (costs) accept three shapes, unified to a
`[min, max]` range:

- an integer: `3`
- `{ "count": 3 }`
- `{ "min": 1, "max": 3 }` (`max` defaults to `min`)

Effective floor is `0` for reward pools, and `1` for player costs and `hand.consume`. So
`{ "min": 0, "max": 3 }` on a
drop is a genuine "nothing to three" roll — the entry keeps its slot in the viewers and its weight in
the pool, and simply yields nothing on a zero. A cost written the same way is raised to `1`, since a
cost of zero would read as configured while doing nothing.

To make an entry that never drops, give it `minecraft:air` (the idiomatic "nothing" filler) rather
than a zero count.

### NBT

NBT is written as an explicit **SNBT string** (`"{Damage:0}"`), so files stay valid JSON. The
`RangeTag` convention (`{Damage:{RangeTag:[0,500]}}`) still works inside these strings.

## Behaviour notes & gotchas

- **Only `type` is required.** Every other section is optional; omit what you don't need.
- **`type` values:** `left_click`, `right_click`, `shift_left_click`, `shift_right_click`.
- **`hidden` is not `enabled: false`.** A hidden interaction loads, syncs and fires exactly like any
  other; it is only left out of JEI and EMI. Use it for secrets and for the intermediate steps of a
  multi-stage recipe. `enabled: false` is the one that turns an interaction off.
- **Sneaking belongs to `type`, and `requires_sneaking` is folded into it.** A sneaking click
  always resolves to the `shift_` variant, so a `left_click` interaction never sees one. Setting
  the condition as well used to be either redundant or fatal — when it disagreed with the type,
  the interaction could never fire while still being listed in JEI as a working recipe. Since
  2.4.0 the condition is taken as the intent and the type is adjusted to match, with a warning
  naming the type to write instead. **Choose the `type` and leave `requires_sneaking` out;** it
  is kept only so existing files keep loading.
- **`kind: "any"` prefers blocks for `minecraft:water` and `minecraft:lava`,** because those ids exist
  in both the block and the fluid registry, and a target cannot mix the two. That is harmless — a
  water source really is `minecraft:water` as a block at that position — but if you specifically want
  a fluid target, say `kind: "fluid"`.
- **Empty hand vs. any item.** Omit `hand`, or use `hand` with no `match`, to require an **empty**
  hand. Add `match` to require specific items/tags.
- **`consume` only spends the item on success.** `durability` damages a damageable item;
  `shrink` removes items from the stack; `none` leaves it untouched.
- **`consume.chance` and `consume.count` are independent.** `chance` decides *whether* anything is
  spent, `count` decides *how much* once that roll succeeds — they compose, so
  `{ "mode": "shrink", "chance": 0.33, "count": { "min": 3, "max": 5 } }` is "a one-in-three chance
  of losing three to five items". `count` defaults to `1`, uses the same shapes as everywhere else
  (see *Count / range*), and has a floor of `1` — to spend nothing, use `chance` or `mode: "none"`.
  Under `mode: "durability"` it counts **durability points**, not items, and a tool that runs out
  still breaks exactly once. Under `mode: "shrink"` a roll larger than the stack takes the whole
  stack — holding too few never blocks the interaction, it just costs less.
- **Rolls default to 1.** Without a `rewards` block, nothing is dropped. With `weighted` only, you
  get exactly one weighted pick (the classic behaviour). `guaranteed` items are always given.
- **Fortune** reads the enchantment from the **held** item, so it only helps interactions that use a
  hand item; the bonus is `round(level × factor)` extra items per weighted pick.
- **Transformations run after a successful drop**, subject to `chance` and the `allow_transformations`
  config gate. Any one **block** is transformed at most once per click, so two interactions matching
  the same click cannot both act on it — but they can act on different blocks.
- **A transformation that cannot act does nothing, quietly.** Out of the world height, in an unloaded
  chunk, past `max_transformation_offset`, blocked by `require`, vetoed by a claim mod: all of these
  skip. Turn on `Debug.log_skipped_interactions` and the log names the block and the reason.
- **Placing fluids at an offset flows.** `into.kind: "fluid"` at a distance behaves like a bucket
  poured there — it will spread, and on a server that is somebody's problem. `require` and a small
  offset keep it predictable.
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
