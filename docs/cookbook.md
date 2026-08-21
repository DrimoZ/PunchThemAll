# PunchThemAll cookbook

Whole, working interactions, organised by what you are trying to build rather than by which field
does what. Copy one, change the ids, keep going.

Each recipe says what it combines, so you can see how the pieces fit rather than meeting them one at
a time. For the field-by-field reference see [interaction-format.md](interaction-format.md); for what
is and is not possible at all, [capabilities.md](capabilities.md).

- [Gathering](#gathering)
- [Tools that reach](#tools-that-reach)
- [Building and terraforming](#building-and-terraforming)
- [Multi-block setups](#multi-block-setups)
- [Progression and gating](#progression-and-gating)
- [Making it feel good](#making-it-feel-good)
- [Patterns worth stealing](#patterns-worth-stealing)

---

## Gathering

### Sifting: a block that gives less each time

Gravel that degrades to sand as you pick it over, with a real chance of nothing.

*Combines: `hand.consume` durability + weighted rewards with an `air` filler + a transformation.*

```json
{
  "schema_version": 2,
  "type": "shift_left_click",
  "hand": { "hand": "main", "match": "#minecraft:shovels", "consume": { "mode": "durability" } },
  "target": { "kind": "block", "match": "minecraft:gravel" },
  "transformation": { "chance": 0.7, "into": { "kind": "block", "id": "minecraft:sand" }, "particles": "minecraft:gravel" },
  "rewards": {
    "weighted": [
      { "match": "minecraft:flint", "weight": 25 },
      { "match": "minecraft:iron_nugget", "weight": 5 },
      { "match": "minecraft:air", "weight": 70 }
    ]
  }
}
```

The `air` entry is 70 of the 100 total weight, so seven clicks in ten give nothing. That is the
idiomatic way to write a failure rate — there is no "chance" field on a drop.

### A renewable resource that has to regrow

Take the crop, reset the block, make the player wait for the world rather than a cooldown.

*Combines: a state whitelist + a transformation that sets the state back.*

```json
{
  "schema_version": 2,
  "type": "right_click",
  "hand": { "hand": "main", "match": "minecraft:shears", "consume": { "mode": "durability" } },
  "target": {
    "kind": "block",
    "match": "minecraft:sweet_berry_bush",
    "state": { "whitelist": { "age": "3" } }
  },
  "transformation": {
    "chance": 1.0,
    "into": { "kind": "block", "id": "minecraft:sweet_berry_bush", "state": { "age": "1" } }
  },
  "rewards": { "guaranteed": [ { "match": "minecraft:sweet_berries", "count": { "min": 2, "max": 4 } } ] }
}
```

The state whitelist is what stops it working on an unripe bush.

### Better tools, better yield

*Combines: `nbt_predicates` on the hand + `rewards.fortune`.*

```json
{
  "schema_version": 2,
  "type": "shift_left_click",
  "hand": {
    "hand": "main",
    "match": "#minecraft:pickaxes",
    "consume": { "mode": "durability" },
    "nbt_predicates": [
      { "path": "Enchantments[].lvl", "int_range": [1, 5], "where": "{id:\"minecraft:fortune\"}" }
    ]
  },
  "target": { "kind": "block", "match": "minecraft:deepslate" },
  "rewards": {
    "weighted": [ { "match": "minecraft:raw_iron", "weight": 20 }, { "match": "minecraft:air", "weight": 80 } ],
    "fortune": { "enchant": "minecraft:fortune", "factor": 1.0 }
  }
}
```

The predicate means the interaction only fires with a Fortune pickaxe at all; `fortune` then scales
what it gives. Drop the predicate if you want it to work bare and merely pay better enchanted.

---

## Tools that reach

### A hammer that breaks the block behind

*Combines: `op: "break"` + `at` in the face frame + `drops: "tool"`.*

```json
{
  "schema_version": 2,
  "type": "shift_right_click",
  "hand": { "hand": "main", "match": "#minecraft:pickaxes", "consume": { "mode": "durability", "count": 2 } },
  "target": { "kind": "block", "match": "#minecraft:base_stone_overworld" },
  "transformation": {
    "chance": 1.0,
    "op": "break",
    "at": { "z": -1, "relative_to": "face" },
    "drops": "tool"
  }
}
```

`z: -1` in the face frame is *into* the block you clicked — the one behind it. `drops: "tool"` means
Silk Touch and Fortune apply, so this is genuinely a mining tool rather than a delete button.

### A 3x3 excavator

*Combines: a region + `op: "break"` + a `require` so it only eats one kind of stone.*

```json
{
  "schema_version": 2,
  "type": "shift_right_click",
  "hand": { "hand": "main", "match": "minecraft:netherite_pickaxe", "consume": { "mode": "durability", "count": 9 } },
  "target": { "kind": "block", "match": "minecraft:stone" },
  "transformation": {
    "chance": 1.0,
    "op": "break",
    "at": { "x": -1, "y": -1, "z": 0, "to": { "x": 1, "y": 1, "z": 0 }, "relative_to": "player" },
    "require": { "match": "minecraft:stone" },
    "drops": "tool"
  }
}
```

Nine blocks, one entry. `relative_to: "player"` makes the 3x3 stand up facing you rather than lying
flat. `require` keeps it from eating the ores you were trying to expose.

> Nine blocks is well inside the default budget of 64, but a bigger box is not: see
> `max_transformations_per_interaction` in [configuration.md](configuration.md).

### A wand that places a block in front of you

An **air** interaction: there is no block under the cursor, so the offset is measured from you.

*Combines: an air target + `op: "place"` + the player frame + a `require`.*

```json
{
  "schema_version": 2,
  "type": "shift_right_click",
  "hand": { "hand": "main", "match": "minecraft:blaze_rod", "consume": { "mode": "durability" } },
  "transformation": {
    "chance": 1.0,
    "op": "place",
    "at": { "y": 1, "z": 2, "relative_to": "player" },
    "require": { "match": "minecraft:air" },
    "into": { "kind": "block", "id": "minecraft:torch" }
  }
}
```

Aim at the sky, or the game sends a block interaction instead. A zero-offset transformation on an air
target is discarded — there is no block there to change.

---

## Building and terraforming

### Paving: turn a patch of ground into a path

*Combines: a region + `require` + a group chance so the whole patch lands or none of it does.*

```json
{
  "schema_version": 2,
  "type": "shift_right_click",
  "hand": { "hand": "main", "match": "#minecraft:shovels", "consume": { "mode": "durability", "count": 3 } },
  "target": { "kind": "block", "match": "minecraft:grass_block" },
  "transformation": {
    "chance": 0.8,
    "all": [
      {
        "chance": 1.0,
        "at": { "x": -1, "y": 0, "z": -1, "to": { "x": 1, "y": 0, "z": 1 } },
        "require": { "match": "minecraft:grass_block" },
        "into": { "kind": "block", "id": "minecraft:dirt_path" }
      }
    ]
  }
}
```

Four times in five you get the whole 3x3. Without the group, each block would roll on its own and you
would get a different ragged patch every click.

### Moving a block instead of replacing it

*Combines: `into: { "kind": "copy" }` + a `break` of the same block, as a list.*

```json
{
  "schema_version": 2,
  "type": "shift_right_click",
  "hand": { "hand": "main", "match": "minecraft:stick" },
  "target": { "kind": "block", "match": "#minecraft:wool" },
  "transformation": [
    { "chance": 1.0, "op": "place", "at": { "y": 1 }, "require": { "match": "minecraft:air" }, "into": { "kind": "copy" } },
    { "chance": 1.0, "op": "break", "drops": false }
  ]
}
```

Order does not matter here, and that is the point: every destination is read before anything is
written, so the copy still sees the wool the break is about to remove.

Note the limit — the block state moves, the block entity contents do not. Move a chest and you move
an empty chest.

### Digging a shaft one step at a time

*Combines: `op: "break"` on a vertical region + `rewards.at` so the drops appear where you can reach.*

```json
{
  "schema_version": 2,
  "type": "shift_right_click",
  "hand": { "hand": "main", "match": "minecraft:iron_shovel", "consume": { "mode": "durability", "count": 3 } },
  "target": { "kind": "block", "match": "#minecraft:dirt" },
  "transformation": {
    "chance": 1.0,
    "op": "break",
    "at": { "y": 0, "z": 0, "to": { "y": -2, "z": 0 } },
    "require": { "match": "#minecraft:dirt" },
    "drops": "vanilla"
  },
  "rewards": { "at": { "y": 1 }, "guaranteed": [ { "match": "minecraft:dirt", "count": 1 } ] }
}
```

Without `rewards.at`, the bonus dirt would appear inside the hole you just dug.

---

## Multi-block setups

This is what `conditions.neighbours` is for. `require` asks about a block a transformation is going
to change; `neighbours` asks whether the recipe applies at all.

### An altar: the right block underneath, nothing on top

*Combines: two neighbour conditions, one of them inverted, plus a guaranteed drop.*

```json
{
  "schema_version": 2,
  "type": "right_click",
  "hand": { "hand": "main", "match": "minecraft:ender_eye", "consume": { "mode": "shrink", "count": 1 } },
  "target": { "kind": "block", "match": "minecraft:obsidian" },
  "conditions": {
    "neighbours": [
      { "at": { "y": -1 }, "block": { "match": "minecraft:crying_obsidian" } },
      { "at": { "y": 1 }, "block": { "match": "minecraft:air" }, "invert": true }
    ]
  },
  "rewards": { "guaranteed": [ { "match": "minecraft:ender_pearl", "count": 2 } ] }
}
```

Read the second one carefully: `minecraft:air` with `invert: true` means "there must **not** be air
above", so the altar only works with something capping it.

### A furnace that needs fuel beside it

*Combines: a neighbour with a state whitelist + a transformation on the target itself.*

```json
{
  "schema_version": 2,
  "type": "shift_right_click",
  "hand": { "hand": "main", "match": "minecraft:iron_ingot", "consume": { "mode": "shrink", "count": 1 } },
  "target": { "kind": "block", "match": "minecraft:blast_furnace" },
  "conditions": {
    "neighbours": [
      { "at": { "x": 1 }, "block": { "match": "minecraft:furnace", "state": { "whitelist": { "lit": "true" } } } }
    ]
  },
  "transformation": { "chance": 0.25, "into": { "kind": "block", "id": "minecraft:air" } },
  "rewards": { "guaranteed": [ { "match": "minecraft:iron_block", "count": 1 } ] }
}
```

The `block` half of a neighbour is a full `target`, so states, tags and NBT all work there.

### A multi-step recipe the player has to build up

Two files: the first prepares, the second finishes and checks the first left its mark. Mark the
intermediate one `hidden` so JEI shows the recipe that matters.

```json
{
  "schema_version": 2,
  "hidden": true,
  "type": "shift_right_click",
  "hand": { "hand": "main", "match": "minecraft:water_bucket" },
  "target": { "kind": "block", "match": "minecraft:clay" },
  "transformation": { "chance": 1.0, "into": { "kind": "block", "id": "minecraft:mud" } }
}
```

```json
{
  "schema_version": 2,
  "type": "right_click",
  "hand": { "hand": "main", "match": "minecraft:wheat", "consume": { "mode": "shrink", "count": 1 } },
  "target": { "kind": "block", "match": "minecraft:mud" },
  "conditions": { "neighbours": [ { "at": { "y": -1 }, "block": { "match": "minecraft:hay_block" } } ] },
  "transformation": { "chance": 1.0, "into": { "kind": "block", "id": "minecraft:packed_mud" } },
  "rewards": { "guaranteed": [ { "match": "minecraft:brick", "count": 2 } ] }
}
```

Nothing chains automatically — the player clicks twice. That is deliberate; see
[capabilities.md](capabilities.md).

---

## Progression and gating

### Only in the Nether, only at night elsewhere

`conditions.biomes` takes dimension ids as well as biome ids and biome tags.

```json
"conditions": { "biomes": { "whitelist": ["minecraft:the_nether"] } }
```

```json
"conditions": { "time": "night", "biomes": { "blacklist": ["minecraft:the_nether", "minecraft:the_end"] } }
```

### Deep, dark, and hungry work

*Combines: three conditions with a cost, so it is expensive rather than merely rare.*

```json
{
  "schema_version": 2,
  "type": "shift_left_click",
  "hand": { "hand": "main", "match": "#minecraft:pickaxes", "consume": { "mode": "durability" } },
  "target": { "kind": "block", "match": "minecraft:deepslate" },
  "conditions": {
    "y_range": [-64, 0],
    "light": { "max": 7 },
    "player_state": { "min_food": 6 }
  },
  "costs": { "hunger": { "chance": 1.0, "amount": { "min": 1, "max": 2 } } },
  "rewards": { "weighted": [ { "match": "minecraft:echo_shard", "weight": 1 }, { "match": "minecraft:air", "weight": 40 } ] }
}
```

`min_food` gates it; `costs.hunger` charges for it. Both, or the player grinds it at zero hunger.

### A tier that only exists when another mod does

```json
{
  "schema_version": 2,
  "type": "right_click",
  "target": { "kind": "block", "match": "minecraft:andesite" },
  "rewards": { "guaranteed": [ { "match": "create:andesite_alloy", "count": 1 } ] }
}
```

On the NeoForge 1.21.1 line you would gate this file with a `neoforge:conditions` block and it
would be skipped entirely when Create is absent. **This branch has no load conditions**, so ship
the file in a separate datapack that the player only installs alongside Create — or accept that
the unknown item id is reported once at load and the drop simply never appears.

---

## Making it feel good

Small things, and they matter more than they look.

**Say something happened.** An interaction with no sound and no particles reads as a bug on the
clicks where the pool rolls `air`.

```json
"sound": "minecraft:block.gravel.break",
"particles": "minecraft:gravel"
```

`particles` takes a **block id** — it is the block-break particle, not a particle type id. This is
the single most common mistake in the format.

**A transformation has its own feedback**, fired where it lands rather than where the player clicked.
On an offset interaction, that is what tells them something happened over there.

**Reward the player where they can see it.** `rewards.at` matters as soon as the interaction is
acting somewhere other than under the cursor.

**Keep the middle steps out of JEI** with `hidden: true`, so the category reads like a list of
recipes rather than a list of implementation details.

---

## Patterns worth stealing

| Pattern | How |
| --- | --- |
| A failure rate | A `minecraft:air` entry carrying most of the weight |
| A tool that wears out at the right speed | `consume: { "mode": "durability", "count": N }` |
| Costing several items at once | `consume: { "mode": "shrink", "count": { "min": 3, "max": 5 } }` |
| A recipe that only works on a finished block | `target.state.whitelist` |
| A recipe that only works on an unfinished one | `target.state.blacklist` |
| Keeping a block facing the way it was | `state: { "facing": "copy_state_value" }` |
| A structure requirement | `conditions.neighbours`, one entry per block |
| A forbidden neighbour | the same, with `invert: true` |
| All-or-nothing patterns | `transformation` as a group with its own `chance` |
| A shape bigger than one block | `at` with `to` |
| Mining at a distance that respects enchantments | `op: "break"` with `drops: "tool"` |
| Not letting a placed block pop | nothing to do — `op: "place"` already refuses |
| Hiding an intermediate step | `hidden: true` |
| Turning a whole feature off server-side | the config, not the datapack |

---

Every recipe here is written out as a runnable file somewhere in the
[example datapack](../examples/punchthemall-examples/README.md). If one does not behave, turn on
`Debug.log_skipped_interactions` — it names the block and the reason.
