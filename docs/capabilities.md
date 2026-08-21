# What PunchThemAll can and cannot do

The other documents say how to write things. This one says what is worth trying at all — so you find
out a thing is impossible before you spend an evening on it, not after.

- [The shape of an interaction](#the-shape-of-an-interaction)
- [What it can do](#what-it-can-do)
- [What it cannot do](#what-it-cannot-do)
- [Things that look possible and are not](#things-that-look-possible-and-are-not)
- [Limits with numbers on them](#limits-with-numbers-on-them)
- [When something does not happen](#when-something-does-not-happen)

---

## The shape of an interaction

Every interaction is the same sentence:

> **When** this kind of click happens **on** this target, **holding** this item, **and** these
> conditions hold — **then** drop these items, **change** these blocks, **charge** the player this,
> **and** show this.

Everything below is about how far each clause stretches.

```
       WHEN                    IF                          THEN
  ┌─────────────┐      ┌──────────────────┐      ┌──────────────────────┐
  │ type        │      │ hand.match       │      │ rewards              │
  │ (4 clicks)  │─────▶│ target.match     │─────▶│ transformation(s)    │
  └─────────────┘      │ conditions       │      │ costs / effects      │
                       │ conditions.      │      │ sound / particles    │
                       │   neighbours     │      └──────────────────────┘
                       └──────────────────┘
```

---

## What it can do

### Triggers

| You want | Can it | How |
| --- | --- | --- |
| React to a left or right click on a block | yes | `type` + `target` |
| Tell sneaking apart from not sneaking | yes | `shift_left_click` / `shift_right_click` are separate types |
| React to a click on a fluid | yes | `target.kind: "fluid"` — source blocks, found by ray trace |
| React to a click on nothing | yes | omit `target`, or `kind: "air"`. Aim at the sky |
| React to a left click on nothing | yes | works since 2.1.0; the client reports it to the server |
| Require an empty hand | yes | omit `hand.match` |
| Require an item, a tag, or several | yes | `hand.match` takes a string, a `#tag`, or a list |
| Require the item in a specific hand | yes | `hand.hand`: `main`, `off`, `any` |
| Require an enchantment, or a damage range | yes | `hand.nbt_predicates` |
| Let machines and fake players trigger it | yes | on by default; `Players.allow_fake_players` |

### Conditions

| You want | Can it | How |
| --- | --- | --- |
| Only in certain biomes or dimensions | yes | `conditions.biomes`, ids or `#tags` |
| Only at night, or in the rain | yes | `conditions.time`, `conditions.weather` |
| Only below a certain height, or in the dark | yes | `conditions.y_range`, `conditions.light` |
| Only when the player is fed, or has XP | yes | `conditions.player_state` |
| Only when a specific block is next to it | yes | `conditions.neighbours` |
| Only when a block is **not** next to it | yes | `conditions.neighbours` with `invert: true` |
| Only on a block in a particular state | yes | `target.state.whitelist` / `blacklist` |
| Only on a chest that contains something | yes | `target.nbt_predicates` |

### Outcomes

| You want | Can it | How |
| --- | --- | --- |
| Drop items, weighted | yes | `rewards.weighted` |
| Drop items every time | yes | `rewards.guaranteed` |
| Drop nothing, sometimes | yes | a `minecraft:air` entry in the pool |
| Scale drops with Fortune | yes | `rewards.fortune` |
| Drop somewhere other than the clicked block | yes | `rewards.at` |
| Turn the block into another block | yes | `transformation.into` |
| Keep a property when transforming | yes | `state: { "facing": "copy_state_value" }` |
| Destroy the block properly, with its loot | yes | `op: "break"` |
| Destroy it honouring Silk Touch or Fortune | yes | `drops: "tool"` |
| Place a block only where there is room | yes | `op: "place"` |
| Act on a neighbouring block | yes | `at` |
| Act on a whole 3x3, or any box | yes | `at` with `to` |
| Do several things from one click | yes | `transformation` as a list |
| Have them all happen or none | yes | `transformation` as a group with `chance` |
| Move a block rather than replace it | yes | `into: { "kind": "copy" }` plus a `break` |
| Cost health or hunger | yes | `costs` |
| Spend the held item, or its durability | yes | `hand.consume` |
| Grant a potion effect | yes | `effects` |
| Play a sound and particles | yes | `sound`, `particles` |
| Write data into the new block entity | yes | `transformation.nbt` |
| Hide a recipe from JEI while it still works | yes | `hidden: true` |
| Load a file only if another mod is present | **not on this branch** | supported on NeoForge 1.21.1 only |

---

## What it cannot do

These are absent, not hidden. Do not go looking.

| You want | Why not | Nearest thing that works |
| --- | --- | --- |
| Trigger on anything but a click | The mod is built on the four player-interact events. There is no tick, no timer, no block update, no entity hook. | Nothing. This is the boundary of the mod. |
| Right-click an **entity** | Only blocks, fluids and air are targets. | Nothing here — that is a different mod. |
| Chain interactions automatically | A transformation never triggers another interaction. That is deliberate: a click that cascades has no natural end. | Make the player click again. `hidden: true` keeps the middle steps out of JEI. |
| Read or write the inventory beyond the held item | Only the main and off hand are inspected; drops go to the inventory or the world. | `hand.match` for the requirement, `rewards` for the result. |
| Give an enchanted item as a drop | Drop `nbt` applies `Damage` and custom data only. | Give the plain item; see [backlog.md](backlog.md). |
| Carry the **contents** of a block entity when moving a block | `into: { "kind": "copy" }` carries the block state, not its inventory. Moving a chest moves an empty chest. | Move blocks that have no block entity. |
| Use a region for anything but transformations | `to` is resolved only where every position is walked. `conditions.neighbours` and `rewards.at` read a single block. | One neighbour entry per block you care about. |
| Make one interaction target a block **and** a fluid | An interaction is either block-shaped or fluid-shaped. `kind: "any"` resolves *which of the two an id is*; it does not mix them. | Two files. |
| Match a biome by tag in `target` | Biome tags work in `conditions.biomes`, not on the target. | `conditions.biomes`. |
| Run arbitrary logic, maths, or scripting | It is JSON, and deliberately so. | KubeJS, or a real mod. |
| Give XP, or run a command | Neither is exposed. | Nothing. |
| Vary by player, or track progress | There is no per-player state at all. | `conditions.player_state` gets you food and XP level, and nothing else. |

---

## Things that look possible and are not

The traps, in the order people hit them.

**`chance` on a transformation is not a drop chance.** It decides whether the *block changes*. Drop
odds are the `weight` numbers in `rewards.weighted`.

**Weights are not percentages.** Weights `10` and `30` give a 25% / 75% split. They are relative to
the total, which is why a `minecraft:air` entry is the idiomatic way to set a failure rate.

**`requires_sneaking` is not a second sneak setting.** Sneaking is part of the type, and a file
that sets both is read with the *condition* winning: `right_click` plus
`requires_sneaking: true` is loaded as `shift_right_click`, with a warning telling you to write
that instead. Before 2.4.0 the two fought and the interaction could never fire.

**An absent NBT tag does not match a range.** A brand-new tool has no `Damage` tag at all, so
`{"path": "Damage", "int_range": [0, 500]}` will not match it.

**`op: "break"` and `into.kind: "air"` are different.** The first breaks the block, with its
particles, its sound and its loot. The second makes it vanish.

**A region is not a shape.** `to` gives you a filled box. There are no spheres, circles, hollow
boxes or diagonals.

**`relative_to: "face"` is not a full face-local frame.** Forward comes from the clicked face; right
and up stay on the player. Clicking a floor from the north and from the south mirrors `x`, on
purpose — it keeps "my right" meaning your right.

**Two interactions cannot both change the same block on one click.** The second is skipped. They
*can* act on different blocks.

**`allow_offset_transformations: false` does not fall back to the clicked block.** It skips the
transformation entirely.

---

## Limits with numbers on them

Every one of these is a config key in `PunchThemAll.Interactions` — see
[configuration.md](configuration.md).

| Limit | Default | What happens at the limit |
| --- | --- | --- |
| Interactions run per click | 64 (`max_matches_per_click`) | Later matches are skipped |
| Blocks transformed per interaction | 64 (`max_transformations_per_interaction`) | The rest are skipped, with a debug log line |
| How far a transformation reaches | 8 blocks (`max_transformation_offset`) | The transformation is skipped entirely |
| Delay between two interactions | 1 tick (`cooldown_ticks`) | The click does nothing |

Hard limits with no config key:

- **The world height.** A transformation aimed outside it is skipped.
- **Loaded chunks.** A transformation or neighbour condition reaching into an unloaded chunk is
  skipped; chunks are never force-loaded from a click.
- **Unbreakable blocks.** `op: "break"` will not remove bedrock for a survival player.
- **Blocks that cannot stay.** `op: "place"` refuses a destination where the block would pop.
- **Claim and protection mods.** Any of them can veto a transformation, and by default they are
  asked.

---

## When something does not happen

In order, because this is the order that finds it fastest.

1. **Read the log at load.** A malformed file reports its id, the field and the reason. `/reload`,
   then look for `INCORRECT_FORMAT`.
2. **Turn on `Debug.log_skipped_interactions`.** Every refusal at click time then says which block
   and why: out of range, wrong destination, vetoed, nothing to break.
3. **Check the click type.** Sneaking makes it a different type. This is the most common one by a
   wide margin.
4. **Check the config gates.** `allow_block_interactions`, `allow_transformations`,
   `allow_offset_transformations` and friends switch whole categories off quietly.
5. **Check it is in JEI.** If the interaction is not in the Interaction category, it did not load —
   unless you marked it `hidden`.

See also the troubleshooting section of [getting-started.md](getting-started.md).
