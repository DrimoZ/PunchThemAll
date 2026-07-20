# PunchThemAll — example datapack

Since **2.1.0 (NeoForge 1.21.1)**, interactions are loaded from **datapacks**. This folder is a
ready-to-use one: 40 interactions, each kept small so it demonstrates one thing you can copy.

Every id is `pta_examples:<file name>` — `hand_off_hand.json` is `pta_examples:hand_off_hand`. All
files use `schema_version: 2`.

> JSON has no comments, so the explanation for each file lives in the catalogue below rather than in
> the file itself. Each entry says **what it shows** and **how to trigger it in game**.

## Install

Copy the whole `punchthemall-examples` folder (the one containing `pack.mcmeta`) into a world:

```
<your world>/datapacks/punchthemall-examples/
    pack.mcmeta
    data/pta_examples/pta/interaction/*.json
```

- **Existing world:** drop it in `saves/<world>/datapacks/`, then `/reload`.
- **New world:** on the creation screen, open **Data Packs**, drag the folder in, enable it.
- **Server:** put it in `<server>/world/datapacks/` and `/reload`.

The server syncs its interactions to clients, so JEI/EMI show the server's set with no extra setup.

**Two things that catch everyone out while testing:**

- **Sneaking changes the click type.** `right_click` and `shift_right_click` are different types; an
  interaction written for one will not fire for the other.
- **Aim matters for `air` targets.** As soon as the crosshair is on a block in reach, the game sends
  a block interaction instead. Aim at the sky.

---

# Catalogue

## Starting points

| File | What it shows | Try it |
| --- | --- | --- |
| `minimal.json` | The smallest useful file: a type, a target, one drop. | Left-click dirt → coarse dirt. |
| `showcase.json` | A realistic interaction combining hand, target, rewards, costs, conditions and feedback. | Sneak-right-click stone with a pickaxe, at night, below Y 40. |
| `full_showcase.json` | Nearly every field at once — a reference to read, not a model to copy. | Sneak-right-click stone with a pickaxe. |

## Click types

`type` is one of `left_click`, `right_click`, `shift_left_click`, `shift_right_click`. There is no
"any" — write one file per type if you want both.

| File | What it shows | Try it |
| --- | --- | --- |
| `minimal.json` | `left_click`. | Left-click dirt. |
| `hand_empty.json` | `right_click`. | Right-click a hay bale, empty-handed. |
| `rewards_guaranteed_and_rolls.json` | `shift_left_click`. | Sneak-left-click stone. |
| `target_block_state.json` | `shift_right_click`. | Sneak-right-click a lit furnace. |
| `left_click_air.json` | `left_click` on **air** — the one case the server cannot observe on its own; the client reports it. | Hold a feather, left-click the sky. |

## The hand

| File | What it shows | Try it |
| --- | --- | --- |
| `hand_empty.json` | Requiring an **empty** hand (`match: []`). | Right-click a hay bale with nothing held. |
| `hand_off_hand.json` | `hand: "off"` — the item must be in the **off** hand. | Bone meal in the off hand, right-click podzol. |
| `hand_item_and_consume.json` | `consume.mode: "durability"` — the tool takes damage. | Sneak-left-click gravel with a shovel. |
| `hand_consume_chance.json` | `consume.chance` — spend the item only *sometimes* (25%). | Right-click cobblestone with flint. |
| `hand_nbt_whitelist.json` | SNBT `nbt.whitelist` / `nbt.blacklist` with the `RangeTag` convention. | Right-click a log with a barely-used axe that has no Mending. |
| `hand_nbt_predicates.json` | `nbt_predicates` — path-based, with a `where` filter tying a level to one enchantment. | Right-click with an Unbreaking II-VII hoe. |

`hand` values are `any` (default), `main`, `off`. `consume.mode` is `none` (default), `shrink`
(consumes one item) or `durability` (damages it).

## The target

| File | What it shows | Try it |
| --- | --- | --- |
| `minimal.json` | `kind: "block"` with a single id. | Left-click dirt. |
| `target_any_with_tag.json` | A `#tag` as the match, and `kind: "any"`. | Left-click any log. |
| `target_fluid.json` | `kind: "fluid"`. | Right-click water with a bowl. |
| `air_interaction.json` | `kind: "air"` — no block at all. | Hoe in the off hand, right-click the sky. |
| `target_kind_any.json` | `kind: "any"` — resolve the match as a block **or** a fluid, without having to know which. | Sneak-right-click netherrack or basalt with a bucket. |
| `target_block_state.json` | `state.whitelist` — only a *lit* furnace. | Sneak-right-click a burning furnace. |
| `target_state_blacklist.json` | `state.blacklist` — everything **except** fully grown wheat. | Right-click young wheat with shears. |
| `target_block_entity_nbt.json` | Block-entity `nbt.whitelist` (SNBT). | Sneak-right-click a furnace that is currently burning. |
| `block_entity_predicates.json` | Block-entity `nbt_predicates`. | Sneak-right-click a chest with items in it. |

`kind` is `block` (default), `fluid`, `air` or `any`. Omitting `target` entirely means **air**.

`any` means "I do not know whether this id is a block or a fluid, try both" — it does **not** let one
interaction target both at once. A single interaction is either block-shaped or fluid-shaped; if a
match resolves to some of each, the fluids are dropped with an error in the log. Note that
`minecraft:water` and `minecraft:lava` exist as *both*, so they are exactly the ids to avoid pairing
with a block under `any`.

## Rewards

| File | What it shows | Try it |
| --- | --- | --- |
| `rewards_guaranteed_and_rolls.json` | `guaranteed` (always dropped) next to `weighted`, and `rolls`. | Sneak-left-click stone. |
| `rewards_count_shapes.json` | The three `count` shapes: `2`, `{"count": 3}`, `{"min": 1, "max": 6}`. | Left-click a moss block. |
| `rewards_multi_match.json` | A list as `match` — on the target **and** on a drop — plus `rolls: 2`. | Left-click sand or red sand. |
| `rewards_fortune.json` | `fortune` — a Looting-style bonus scaled by an enchantment on the held tool. | Sneak-left-click deepslate with a Fortune pickaxe. |
| `rewards_drop_with_nbt.json` | `nbt` on a drop, to hand out a pre-damaged item. | Sneak-right-click an anvil with an iron ingot. |

**Weights are relative, not percentages.** With weights 10 and 30 you get a 25% / 75% split. A
`minecraft:air` entry is the idiomatic "nothing happened" filler and controls the failure rate.

**Drop `nbt` is limited.** Only `Damage` and arbitrary custom data are applied. Enchanted drops are
not supported yet — see [the backlog](../../docs/backlog.md).

## Transformations

| File | What it shows | Try it |
| --- | --- | --- |
| `transformation_state_copy.json` | `into` a different block, carrying a property over with `copy_state_value`. | Sneak-right-click a chest (20% chance). |
| `transformation_break.json` | **No `into`** — the target simply becomes air. | Sneak-left-click a cobweb holding a bucket. |
| `transformation_into_fluid.json` | `into.kind: "fluid"`. | Right-click dirt with ice. |
| `transformation_block_entity_nbt.json` | `nbt` on the transformation, writing data into the new block entity. | Sneak-right-click a chest with a name tag. |

`chance` is required. Transformation `sound` / `particles` are separate from the interaction's own.

## Costs, conditions, effects

| File | What it shows | Try it |
| --- | --- | --- |
| `costs_damage_and_hunger.json` | `costs.damage` and `costs.hunger`, each with a chance and an amount. | Right-click stone with flint. |
| `conditions_time_weather.json` | `time` (`day`/`night`) and `weather` (`clear`/`rain`/`thunder`). | Sneak-right-click a hay bale at the right time. |
| `conditions_y_light_player.json` | `y_range`, `light`, and `player_state` (food and XP floors). | Sneak-left-click deepslate, deep and dark, well fed. |
| `conditions_sneaking.json` | `requires_sneaking` — separate from the shift click *types*. | Sneak and right-click soul sand with a torch. |
| `conditions_biomes.json` | A biome whitelist. | Right-click sand in a desert with a bottle. |
| `conditions_dimension_and_biome_tag.json` | A biome **`#tag`** and a **dimension** id in the same list. | Right-click leaves in any forest. |
| `conditions_nether_only.json` | A biome **blacklist** used to restrict to one dimension. | Sneak-left-click netherrack in the Nether. |
| `effects_and_feedback.json` | `effects`, plus interaction `sound` and `particles`. | Right-click the sky with sugar. |
| `effects_multiple.json` | Several effects with independent chances, and a hunger cost. | Right-click the sky with glow berries. |

`requires_sneaking` and the `shift_*` click types are **not** the same thing: the type decides which
click reaches the interaction, `requires_sneaking` is an extra gate on top.

## Pack plumbing

| File | What it shows | Try it |
| --- | --- | --- |
| `enabled_false.json` | `"enabled": false` — parsed, then skipped. Useful to disable an inherited file without deleting it. | Nothing happens (that is the point). |
| `conditional_load_mod_present.json` | `neoforge:conditions` — the file only loads when another mod is present. | Right-click a bookshelf with a book (JEI installed). |
| `hoe_in_the_air.json` | A demanding real-world filter: tag match + enchantment ranges + costs. | Needs a specifically enchanted hoe — read the file first. |
| `sand.json` | A plain, shippable interaction with no tricks. | Sneak-left-click gravel with a shovel. |

---

## Authoring your own

Pick your own namespace and drop files in `data/<namespace>/pta/interaction/*.json`. Nested folders
are fine: `data/mypack/pta/interaction/early/flint.json` becomes `mypack:early/flint`.

- Full field reference: [docs/interaction-format.md](../../docs/interaction-format.md)
- Step-by-step guide: [docs/getting-started.md](../../docs/getting-started.md)
- Editor autocomplete and validation: [docs/interaction.schema.json](../../docs/interaction.schema.json)

Edit a file, run `/reload`, and the change applies immediately — no restart, and clients are updated
too. A malformed file is skipped with an error in the log; the rest keep working.
