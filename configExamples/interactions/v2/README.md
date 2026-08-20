# PunchThemAll — schema_version 2 examples

Copy any file into `config/punchthemall/interactions/` (or a subfolder) and run `/reload`.
Every file here is strictly-valid JSON.

New to authoring? Read the step-by-step [getting-started guide](../../../docs/getting-started.md)
first. For the exhaustive field list, see [`docs/interaction-format.md`](../../../docs/interaction-format.md).

Each example focuses on one feature so it is easy to understand and remix:

| File | Shows |
| --- | --- |
| `01_minimal.json` | The smallest possible interaction (type + one drop). |
| `02_hand_item_and_consume.json` | Requiring a hand item/tag and spending it (durability vs shrink). |
| `03_hand_nbt_predicates.json` | Typed `nbt_predicates` on the held item (damage + enchant level). |
| `04_target_block_state.json` | Targeting a block with a required block-state (`lit: true`). |
| `05_target_fluid.json` | Targeting a source fluid. |
| `06_target_any_with_tag.json` | `kind: "any"` with a `#tag` selector. |
| `07_air_interaction.json` | Clicking the air (no target block). |
| `08_transformation_state_copy.json` | Transforming the block and copying a state value. |
| `09_rewards_guaranteed_and_rolls.json` | `guaranteed` drops + multiple `rolls`. |
| `10_rewards_fortune.json` | A Fortune/Looting-style count bonus from the held tool. |
| `11_costs_damage_and_hunger.json` | Hurting the player and consuming hunger. |
| `12_effects_and_feedback.json` | Potion `effects` + interaction `sound`/`particles`. |
| `13_conditions_time_weather.json` | Gating by time of day and weather. |
| `14_conditions_y_light_player.json` | Gating by Y range, light level and player state. |
| `15_conditions_biomes.json` | Gating by dimension/biome id. |
| `16_block_entity_predicates.json` | Predicates against a block-entity (chest contents). |
| `17_full_showcase.json` | Many features combined in one interaction. |
| `18_hidden_from_jei.json` | `hidden: true` — loads and fires normally, but never appears in JEI. For secrets, and for the middle steps of a multi-stage recipe. Not the same as `enabled: false`, which turns it off. |
| `19_consume_count.json` | `consume.count` — a 33% chance of spending **3 to 5** items. `chance` decides whether, `count` decides how many. |
| `20_offset_world.json` | `at` — a transformation acting on a block **other than the one clicked**, read in world axes. Sneak-left-click a gold block with redstone in hand. |
| `21_offset_place.json` | `op: "place"` at an offset, with `require` on the destination. Nothing is written unless the destination matches. |
| `22_region.json` | `at.to` — a whole **box** in one entry, every block of it filtered by `require`. Bone meal on dirt turns a 3x3 to grass. |
| `23_move_block.json` | `into: { "kind": "copy" }` plus a `break`: moves a block up one, rather than naming what to write. Two transformations, applied in order. |
| `24_group_chance.json` | One roll for the **whole set**: either the torch and the space for it both happen, or neither does. |
| `25_break_neighbour.json` | `op: "break"` at an offset, with `drops` deciding what the broken block leaves. |
| `26_air_offset_place.json` | An **air** interaction that transforms: with no block under the cursor, the offset is measured from the player. |
| `27_conditions_neighbours.json` | `conditions.neighbours` — the recipe only applies when the blocks *around* the target are right. `invert` for "and not". |
| `28_rewards_grid_mixed.json` | 20 weighted drops and 3 guaranteed ones. Past `max_drop_rows` the extra drops share slots, and a guaranteed drop never shares with a weighted one. Set `max_drop_rows = 2` in `pta-client.toml` to see it. |

## Selector cheat-sheet

- `"match": "minecraft:stick"` — one id.
- `"match": ["minecraft:stick", "minecraft:bone"]` — several ids.
- `"match": "#minecraft:planks"` — a tag (note the leading `#`).

## Count cheat-sheet

`count` (rewards, `hand.consume`) / `amount` (costs) accept: `3`, `{ "count": 3 }`, or
`{ "min": 1, "max": 3 }`.
