# Troubleshooting

Nothing happened when you clicked. This is the order to check things in, because it is the order that
finds the problem fastest.

- [First: does it load at all?](#first-does-it-load-at-all)
- [It loads, but nothing happens when I click](#it-loads-but-nothing-happens-when-i-click)
- [The drops are wrong](#the-drops-are-wrong)
- [The block does not change](#the-block-does-not-change)
- [It works in singleplayer but not on my server](#it-works-in-singleplayer-but-not-on-my-server)
- [Error messages, and what they mean](#error-messages-and-what-they-mean)

---

## First: does it load at all?

**Open JEI or EMI and look in the Interaction category.** If your interaction is not there, it did not
load — everything else is a waste of time until it does.

Two exceptions: `"enabled": false` and `"hidden": true`. The first does not load; the second loads and
fires but is deliberately kept out of the viewers.

Then read the log. `/reload` and look for lines starting with:

```
PunchThemAll - Incorrect Json format - <id> - <field> - <reason>
```

They name the file, the field and the reason. Almost every load failure is one of:

| Symptom in the log | Cause |
| --- | --- |
| `Unknown block <id>` | Typo, or the mod that adds it is not installed |
| `Unknown or empty item tag` | The tag does not exist, or is empty in this pack |
| `schema_version 1 is not supported` | An old file. This version reads `schema_version: 2` only |
| `<field> - Unknown ...` | A value that is not one of the accepted words. The message lists them |
| Nothing at all, and no entry in JEI | The file is not where the mod looks — see below |

**The path has to be exactly right:**

```
<world>/datapacks/<yourpack>/
  pack.mcmeta
  data/<namespace>/pta/interaction/<name>.json
```

`pta/interaction`, singular, both of them. A file in `pta/interactions/` is silently invisible,
because as far as the game is concerned it is not our data at all.

Check `pack.mcmeta` too — a datapack with a `pack_format` the game rejects does not load, and the
error mentions the pack rather than your interaction.

---

## It loads, but nothing happens when I click

**Turn on the debug log.** In `config/punchthemall/pta-common.toml`:

```toml
[PunchThemAll.Debug]
    log_skipped_interactions = true
```

From then on the log says which block was refused and why — out of range, occupied, vetoed, nothing
to break, not loaded. This answers most of what follows without guessing.

Then, in order of how often it turns out to be the cause:

**1. Sneaking.** `right_click` and `shift_right_click` are *different types*. An interaction written
for one will never fire for the other. This is the single most common cause by a wide margin.

**2. The hand.** No `hand` block at all means "empty hand required" — holding anything then stops it.
`hand.hand` of `main` will not match an item in the off hand.

**3. Aim, for air interactions.** As soon as the crosshair is on a block within reach, the game sends
a block interaction instead. Aim at the sky.

**4. Conditions.** Everything in `conditions` must hold at once. Check time, weather, Y range, light
level, and food/XP. `conditions.neighbours` is easy to get wrong by one block — the offsets are
relative to the block you clicked, not to you, unless you asked for `relative_to: "player"`.

**5. The config gates.** These switch off whole categories quietly:

```toml
[PunchThemAll.Interactions]
    enabled = true
    allow_left_click = true
    allow_right_click = true
    allow_block_interactions = true
    allow_air_interactions = true
    allow_fluid_interactions = true
    allow_transformations = true
    allow_offset_transformations = true
```

**6. The cooldown.** `cooldown_ticks` throttles how often one player can trigger anything. If you are
clicking fast and only some clicks land, this is why.

**7. `requires_sneaking` quietly changing the type.** Sneaking belongs to the type, so a file
setting both is read with the condition winning — `right_click` with `requires_sneaking: true`
loads as `shift_right_click`. The log says so. If an interaction wants a click you did not
expect, that is why; write the type you mean and drop the condition.

---

## The drops are wrong

**Nothing drops, most of the time.** Look for a `minecraft:air` entry in `rewards.weighted`. That is
the idiomatic "nothing happened" filler, and it is probably carrying most of the weight.

**The odds are not what I wrote.** Weights are relative, not percentages. `10` and `30` is a 25% / 75%
split, not 10% and 30%.

**I get too many items.** `rolls` draws the weighted pool that many times. `guaranteed` is given once
on top, whatever `rolls` says.

**Fortune does nothing.** It reads the enchantment from the **held** item, so an interaction with no
hand requirement gets no bonus. The item has to be in the hand the interaction matched.

**The drops land somewhere unhelpful.** They appear at the interacted block by default. If the
interaction is really acting elsewhere, move them with `rewards.at`.

**The drops go straight into my inventory.** That is `Drops.place_in_inventory`, on by default.

---

## The block does not change

**Check `chance`.** On a transformation it is the chance the *block changes* — not a drop chance.

**Check `allow_transformations`**, then `allow_offset_transformations` if you used `at`. Disabling the
second does **not** fall back to the clicked block; it skips the transformation.

**A placed block vanishes a moment later.** It could not survive where it was put — a torch with
nothing under it, for instance. Since 2.3.0 the placement is refused outright instead, with a line in
the debug log.

**`op: "break"` does nothing on bedrock.** Unbreakable blocks are refused for survival players.

**Only some of a region changes.** Every block in a region is checked on its own, so `require`, or a
block that cannot be replaced, will skip some and keep the rest. The debug log names each one.

**A big region stops part-way.** `max_transformations_per_interaction` caps how many blocks one
interaction may change — 64 by default. Note that an **existing config file keeps its old value**: if
yours was written before 2.4.0 it still says 8.

**Nothing happens near spawn, or in someone else's claim.** Protection is respected: claim mods can
veto a transformation, and spawn protection applies. That is deliberate, and the debug log says
`was vetoed`.

**Two interactions match and only one transforms.** Any one block is changed at most once per click.
They can act on different blocks, but not both on the same one.

---

## It works in singleplayer but not on my server

- The datapack has to be on the **server**, in `<server>/world/datapacks/`. Clients do not need it.
- The **config** is server-side too. A gate you turned on locally is not on for the server.
- `/reload` on the server after changing files.
- JEI and EMI show what the *server* sent them, so if a client sees an out-of-date list, it is the
  server that is out of date.
- A client without PunchThemAll can still join; it simply sees no PTA entries.

---

## Error messages, and what they mean

| Message | Meaning |
| --- | --- |
| `Incorrect Json format - <id> - <field>` | The file loaded but one field was wrong. Only that field is dropped, not the whole pack |
| `... resolved to nothing; treating target as air` | Every id or tag in a `match` failed to resolve. The interaction will not fire on anything |
| `... names no block or fluid; requirement ignored` | A `require` that matches nothing. Write `match: ["minecraft:air"]` to require an empty destination |
| `op place needs a block or fluid to place` | `op: "place"` without `into` |
| `op break destroys the block and writes nothing; into is ignored` | Harmless, but the `into` is doing nothing |
| `sneaking belongs to the type, so this is being read as ...` | `requires_sneaking` disagreed with the type; the condition won |
| `reaches past max_transformation_offset` | The offset is further than the config allows |
| `is not loaded` / `is outside the world height` | The destination is not somewhere the mod may write |
| `cannot survive at` | `op: "place"` refused a destination where the block would pop |
| `was vetoed` | A claim or protection mod refused it |
| `another transformation already claimed` | Two entries aimed at the same block on one click |

---

If none of this explains it, the [example datapack](Examples) has a working file for every feature —
start from one that works and change it towards what you wanted. If it still makes no sense, the
[Discord and issue tracker](Community) are there.
