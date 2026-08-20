# Changelog

All notable changes to **PunchThemAll** are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and the project aims to follow [Semantic Versioning](https://semver.org/).

Version tags use the form `MC-version - mod-version`, e.g. `1.20.1-2.0.0`.

---

## [1.21.1-2.4.0] — NeoForge

The six things a pack maker runs into within an hour of the 2.3.0 offsets: patterns needing nine
entries, a set that only half-happens, no way to gate on the surroundings, and no way to move a
block rather than replace it.

### Added
- **Tooltips you can actually read, in two lengths.** An interaction can carry a dozen facts, and
  the viewer used to end with *+1 more transformation(s)* — telling the player something was
  hidden and then hiding it. The summary now names the block each transformation writes
  (*Places Torch*, not *Places a block*); holding a key expands it into every transformation,
  numbered and in order, each with its destination, its condition and what it drops. A region
  says how many blocks it covers, and a set-level chance is stated before the entries it gates.
- **`pta-client.toml`**, a client-side config file. `detail_key` picks which key expands a
  tooltip and `max_drop_rows` how tall a recipe box gets before its drops scroll. A separate
  file because a server has no business deciding which key a player holds to read a tooltip.
- **A troubleshooting guide**, [troubleshooting.md](docs/troubleshooting.md): why a file did not
  load, why a click did nothing, why the drops or the block are not what you wrote, and a table
  of every message the mod logs with what it means.
- **The wiki is generated from the documentation**, by `tools/sync-wiki.sh`. It had drifted to
  2.0.0 while the mod shipped 2.4.0, and nothing could notice; `--check` now exits non-zero when
  it falls behind.
- **`LICENSE-ASSETS`**, covering the logo and the JEI artwork, and both licence files now travel
  inside the jar. A jar found in a modpack could not say what you were allowed to do with it.
- **A permissions section** in the README and on the store page: modpacks yes, servers yes, forks
  yes, reuploading as your own mod please no. Credit appreciated, never required.
- **Four in-world tests for the protection guard.** A listener that cancels the vanilla break and
  place events is, from this mod side, exactly what a claim mod is — so the veto is now tested
  rather than asserted, including that it stops only the blocks it protects.
- **Two new documents, and thirteen worked examples.** [capabilities.md](docs/capabilities.md)
  says what the mod can and cannot do, the things that look possible and are not, and every
  hard limit — so an impossible idea is cheap to rule out rather than expensive to discover.
  [cookbook.md](docs/cookbook.md) is whole interactions organised by what you are building,
  each combining several fields; every one of them ships as a runnable `combo_*.json`.
- **`conditions.neighbours` — gate an interaction on the blocks around it.** `require` asks about a
  block a transformation is going to change; this asks whether the recipe applies at all, which is
  what multi-block setups are made of:

  ```json
  "conditions": { "neighbours": [
    { "at": { "y": -1 }, "block": { "match": "minecraft:obsidian" } },
    { "at": { "y": 1 }, "block": { "match": "#minecraft:logs" }, "invert": true }
  ] }
  ```

  The `block` half is a `target`, so states and tags work exactly as they do there. All of them must
  hold; `invert` flips one. A neighbour in an unloaded chunk or outside the world counts as not
  matching, so a recipe never fires on evidence nobody could see.
- **Regions.** An offset can name a second corner with `to`, and then covers the box between the two,
  corners included — a 3x3 is one entry rather than nine. Both corners read in the same frame, so a
  box in a rotating frame turns as a whole and a shape drawn one way lands that way. Every block
  counts against `max_transformations_per_interaction`, and each is checked on its own: a `place`
  over a region fills the gaps and leaves the rest alone rather than failing outright.
- **A chance for a whole set.** `transformation` accepts `{ "chance": 0.7, "all": [ ... ] }`. Each
  entry still rolls its own chance; this one decides whether the set is attempted. Without it, "a
  seven-in-ten chance the pattern appears" was only expressible as "each block of it, independently",
  which for a pattern means a different half-built shape every time.
- **`into: { "kind": "copy" }` — write the block that is already somewhere rather than naming one.**
  Paired with a break of its own source, that moves a block:

  ```json
  "transformation": [
    { "chance": 1.0, "at": { "y": 1 }, "into": { "kind": "copy" } },
    { "chance": 1.0, "op": "break", "drops": false }
  ]
  ```

  The copy is read while planning, before anything is written, so the break cannot empty the source
  first. `from` reads somewhere other than the interacted block.
- **`rewards.at` — where the drops land.** They appeared at the interacted block and nowhere else,
  which reads oddly once the interaction is really acting three blocks away.
- **`drops: "tool"` on `op: break`** — the block's loot as broken by the held item, Fortune and Silk
  Touch included. `true`/`false` still mean what they meant (`"vanilla"` / `"none"`) and still
  round-trip as booleans. Off by default on purpose: mining at a distance with an enchanted tool is a
  fine thing for a pack to choose and a poor one to inherit by accident.
- **Four examples sized to the drop grid** (9, 12, 18 and 27 drops), so the layout above is
  something you can look at rather than reason about.
- **A test that every feature of the format has a runnable example.** Fifty-seven of them, matched
  against the example pack as text — a field documented but never demonstrated is invisible by
  inspection once there are sixty files, and the gap opens every time a feature is added.
- **Eight more in-world tests**, covering regions, copying, tool drops and neighbour conditions.
  Twenty-four in all, still about two seconds.

### Changed
- **A recipe no longer inherits the height of the widest one in the category.** JEI sizes a
  category rather than a recipe, so a single interaction with thirty drops made every other one
  three rows tall and the list became a column of mostly empty boxes. The box is now capped by
  `max_drop_rows` (three by default), and only a recipe with more than that gets a scrolling
  grid — so a scrollbar appears where it does something and nowhere else. A pack whose
  interactions all fit in fewer rows is untouched: the cap is a maximum, not a constant.
- **`max_transformations_per_interaction` now defaults to 64**, up from 8, and counts every block a
  region covers. Eight was chosen when a transformation meant one block.

  > **Upgrading:** config files keep the values already written in them. An existing
  > `pta-common.toml` still says `8`, and regions will quietly stop at the eighth block — with a line
  > in the log under `Debug.log_skipped_interactions`. Raise it by hand, or delete the file and let it
  > regenerate.

### Fixed
- **Regions never worked.** `at.to` parsed, the box maths was right, and the resolver between
  them never read the second corner — so every region shipped as a single block.
  `combo_excavator_3x3` advertised nine and broke one. Every layer was individually correct and
  all three test suites were green, because none of them crossed that seam: the codec test
  stopped at the spec, the offset test built a box by hand, and the game tests did too. There is
  now a test that resolves each shipped example and fails if a `to` corner does not survive.
- **JEI and EMI crashed on a `copy` transformation.** A copy is neither air nor a named block,
  so both viewers fell through to the fluid branch and dereferenced a null — JEI showed *This
  recipe crashed* in place of the recipe. It now reads *The block that was there*, which is what
  a copy writes.
- **The sneak requirement was shown in two places that could disagree.** The icon came from the
  click type, a tooltip line came from `conditions.requires_sneaking`, and nothing tied them
  together: an interaction using the type showed the sneaking figure and no tooltip, while one
  using the condition showed the standing figure and a tooltip saying sneaking was required.
  Sneaking is now folded into the type at load, so there is one source of truth, the icon is
  always right, and the duplicate tooltip line is gone.
- **An interaction whose `requires_sneaking` disagreed with its type could never fire**, and was
  listed in the viewers as a working recipe anyway. The condition is now taken as the intent and
  the type adjusted to match, with a warning naming the type to write instead.
- **Removed a Minecraft texture that had been sitting in the mod resources.** A copy of the
  vanilla grass side was shipping in every jar, referenced by nothing. Mojang assets are not
  redistributable, so this was a licensing problem rather than dead weight — though it was also
  dead weight.
- **Removed `CREDITS.txt`.** It was the Minecraft Forge 1.12 credits file, inherited from the old
  MDK: it thanked the Forge authors, described MCP redistribution terms, and had nothing to do
  with this mod. Attribution that names the wrong project is worse than none.
- A neighbour or copy source that cannot be read — unloaded, out of the world — is treated as absent
  rather than as air, so nothing fires or writes on the strength of a chunk that was not there.

---

## [1.21.1-2.3.0] — NeoForge

### Added
- **Transformations can act on a block other than the one you clicked, and can break or place
  rather than only overwrite.** `transformation` gains four fields:

  ```json
  "transformation": {
    "chance": 1.0,
    "op": "place",
    "at": { "y": 1 },
    "require": { "match": "minecraft:air" },
    "into": { "id": "minecraft:torch" }
  }
  ```

  `op` is `replace` (the previous and still default behaviour), `break` (destroys what is there,
  with its particles, sound and — unless `"drops": false` — its loot) or `place` (writes only where
  there is room). `at` moves the destination: `x` is right, `y` is up, `z` is forward, read in the
  world axes by default or against the player's facing (`relative_to: "player"`) or the clicked face
  (`relative_to: "face"`). `require` is the same shape as `target`, asked of the destination.
- **`transformation` also accepts a list**, applied in declaration order, each entry rolling its own
  `chance`. Every destination is resolved and checked against the world as it was when the click
  happened, before any of them is written, so declaration order does not change what `require` sees.
- **Air-target interactions can now carry transformations**, as long as they are offset — their
  origin is the player, so "place a block above me" is expressible. A zero-offset transformation on
  an air target is still dropped, as it was before: there is no block there to change.
- **Four config keys**, under `PunchThemAll.Interactions`: `allow_offset_transformations`,
  `max_transformation_offset` (default 8), `max_transformations_per_interaction` (default 8) and
  `fire_protection_events` (default true).
- **In-world automated tests.** `./gradlew runGameTestServer` runs the transformation pipeline
  against a real `ServerLevel` on a headless server — no client, no clicking. Sixteen tests cover
  breaking, placing and replacing, offsets, `require`, the offset cap, and blocks being claimed once
  per click; they take about two seconds. `./gradlew verify` runs them after the unit suite. This is
  the first automated coverage of anything that needs a live world.

### Changed
- **Transformations post block break/place events**, so claim and protection mods can veto them, and
  they respect `Level.mayInteract`. This matters more than it used to: an offset transformation can
  reach a block the player never pointed at, including across a claim boundary. Turn it off with
  `fire_protection_events` only on a single-player world.
- **A click now transforms any one block at most once, rather than performing at most one
  transformation.** Two interactions matching the same click still cannot both act on the same
  block, but they no longer block each other when they act on different ones.
- **JEI and EMI say what a transformation does and where.** Both drew it as one output slot next to
  the target, which reads as "this becomes that" — untrue for a break, an offset, or a list. The
  slot is unchanged; the tooltip now carries the operation, the offset with its frame, the
  requirement and a count of any further transformations. JEI's placeholder for a transformation
  that writes nothing now reads *Broken* for `op: "break"` rather than *Air*, since a break leaves
  the block's drops behind and air does not.
- **`op: "place"` refuses a destination the block cannot survive on**, instead of placing it and
  letting the next block update pop it a tick later. A floating torch that vanishes reads as a mod
  bug; a skipped placement with a line in the debug log reads as a rule.

### Fixed
- A transformation reaching outside the world height, or into an unloaded chunk, is skipped instead
  of acting. Chunks are never force-loaded from a click.
- Two entries of one transformation list that resolve to the same block no longer both apply; the
  second is skipped, the same way two interactions on one click already were.
- A transformation whose write is refused by the world no longer counts as having happened, so its
  sound does not play and the block is not marked as changed for the rest of the click.
- Every reason a transformation declines to act is reported under `Debug.log_skipped_interactions`,
  naming the block and the reason. A transformation that quietly does nothing was previously
  indistinguishable from a mod bug.

---

## [1.21.1-2.2.0] — NeoForge

A correctness pass over 2.1.0, two new authoring fields, and the first automated test suite.

### Added
- **`hand.consume.count`.** How much a successful interaction spends, on top of the existing
  `chance`, which only ever decides *whether* anything is spent. The two are independent and
  compose:

  ```json
  "consume": { "mode": "shrink", "chance": 0.33, "count": { "min": 3, "max": 5 } }
  ```

  = one click in three costs 3–5 items. It takes the same shapes as every other count (`3`,
  `{ "count": 3 }`, `{ "min": 3, "max": 5 }`), defaults to `1`, and has a floor of `1` — spending
  nothing is what `chance` and `mode: "none"` are for. Under `mode: "durability"` it is durability
  points rather than items, and a tool that runs out still breaks exactly once. Holding fewer items
  than the roll asks for never blocks the interaction; it just costs what is there. JEI shows an
  *Amount* line on the hand slot whenever the value is not `1`, and EMI now carries the held-item
  chance and amount on its arrow summary (it showed neither before). Example:
  `pta_examples:hand_consume_count`.
- **`hidden`** on an interaction. `"hidden": true` keeps it out of JEI and EMI while it loads, syncs
  and fires exactly as before — for secrets, and for the intermediate steps of a multi-stage recipe.
  Distinct from `enabled: false`, which is the one that turns an interaction *off*. Hidden
  interactions also stop stretching the category to fit their drop rows. Example:
  `pta_examples:hidden_from_viewers`.
- **A test suite**: 244 tests over the NBT matchers, the count/weight arithmetic, the codec, the
  resolver, the registry and the sync batching. `./gradlew test`. It boots the game's registries in
  process, so it covers real `ItemStack` and NBT behaviour rather than stand-ins.
- **A warning when `type` and `conditions.requires_sneaking` contradict each other.** Pairing
  `shift_left_click` with `requires_sneaking: false` yields an interaction that can never match; PTA
  now names the file instead of leaving you to work it out. The warning immediately found one in our
  own example datapack — see below. `requires_sneaking` is redundant with `type` in every case and is
  now documented as such; prefer the `type`.
- **Unresolvable `sound`, `particles` and biome entries are reported.** They were dropped silently,
  which is indistinguishable from never having written them.

### Fixed
- **A malformed id no longer takes down the whole datapack load.** `sound`, `particles`,
  `effects[].id` and `rewards.fortune.enchant` used to throw on an id that was not a valid resource
  location — inside the reload, so a single typo cost the pack *every* interaction. Bad ids are now
  reported per file and skipped, and one interaction failing to resolve no longer aborts the rest.
  The same applied to a malformed biome `#tag`, which threw on every click for as long as the pack
  was installed.
- **`count: { "min": 0, "max": 3 }` on a drop now means "nothing to three".** It silently meant
  "never anything": the entry produced no items, vanished from the viewers, and still consumed its
  weight in the pool — while the schema and docs both advertised it as valid.
- **A shape mismatch in `nbt.whitelist` / `nbt.blacklist` no longer crashes mid-click.** Requiring a
  list where the item or block entity holds something else threw a `ClassCastException` from inside
  the interaction filter. It now simply does not match.
- **Which interaction wins is now deterministic.** Matches were collected into a hash set, so with
  `max_matches_per_click`, or with several interactions competing to transform one block, the winner
  varied between runs. Candidates are ordered by id.
- **Fortune can no longer produce an over-sized stack.** The bonus is clamped to the item's maximum
  stack size; the surplus used to disappear the moment the drop entered an inventory.
- **The recipe viewers no longer churn on every reload.** An interaction now compares equal to itself
  across a reload, so JEI hides and re-adds only what actually changed. Previously every entry looked
  new, and JEI's permanent hidden-recipe set grew each time.
- **A client leaving a server forgets its interactions**, instead of showing the previous server's set
  in JEI on the main menu and into the next world.
- **Cooldown bookkeeping no longer grows without bound.** Fake players never log out, so every machine
  that clicked left an entry behind for the life of the server. Stale entries are pruned, and
  everything is dropped when the server stops.
- **The interaction registry is published as one immutable snapshot.** In singleplayer the client and
  the server rebuild the same registry from two threads while a third reads it on every click; the
  old clear-then-refill could be observed half-empty.
- **A cost's maximum is clamped against its floored minimum**, so `amount: 0` no longer produces an
  inverted range.
- **Slot counting matches what the viewers lay out**, so a drop grid can no longer overflow the
  category background.
- **The `conditions_sneaking` example never worked.** It shipped `type: right_click` with
  `requires_sneaking: true` — the same axis, contradicting — so it could not fire since 2.1.0. It is
  now a plain `shift_right_click`, and the catalogue entry explains the trap instead of demonstrating
  it.
- **Four examples never showed their particles.** `hand_off_hand`, `left_click_air`,
  `target_block_entity_nbt` and `transformation_break` passed particle-*type* ids
  (`minecraft:happy_villager`, `minecraft:cloud`, …) where `particles` wants a **block** id. The docs
  always said so; the examples did not follow, and the failure was silent. Both halves are fixed —
  the ids, and the silence.

### Changed
- **A client without PunchThemAll can now join a PunchThemAll server.** The network channel is
  optional: every gameplay decision is server-side, and the sync only feeds JEI/EMI. Such a client
  simply sees no PTA entries. *(Protocol version bumped to `2`; a 2.1.0 client and a 2.2.0 server will
  not talk to each other.)*
- **The interaction sync is sent in batches.** The specs travel as NBT, which vanilla caps at 2 MiB
  per tag — a large pack in one payload would have dropped clients at join with a decode error.
- **Registry lookups answer an unknown id with `null`** rather than the registry default, so a typo
  can no longer read as `minecraft:air`.
- **Debug logging can no longer break loading.** Reading a config flag before the config file is
  attached now falls back to its default instead of throwing.
- `kind: "any"` on `minecraft:water` / `minecraft:lava` resolves to the **block**, since those ids
  exist in both registries and a target cannot mix the two. Documented; use `kind: "fluid"` if you
  want the fluid.

---

## [1.21.1-2.1.0] — NeoForge

The **NeoForge 1.21.1** release. The JSON you write is unchanged (`schema_version: 2`), but *where*
you put it changed: interactions are now **datapack** data.

### ⚠️ Migrating from 1.20.1 (2.0.x)
- **Interactions move to a datapack.** Copy your files from `config/punchthemall/interactions/` into
  `data/<namespace>/pta/interaction/` inside a datapack. Ids come from the path
  (`data/mypack/pta/interaction/early/flint.json` → `mypack:early/flint`).
- **Only `schema_version: 2` is accepted.** The legacy (schema 1) format is gone — convert first,
  using the mapping table in [docs/interaction-format.md](docs/interaction-format.md).
- **The `Loader` config section is gone** (`recursive_discovery`, `lowercase_generated_ids`,
  `load_from_datapacks`). All other config sections are unchanged.

### Added
- **Interactions are datapack data**, at `data/<namespace>/pta/interaction/*.json`. They reload with
  `/reload`, can be **overridden by pack order**, and support `neoforge:conditions`. The server syncs
  its loaded set to clients on join and after every reload, so JEI/EMI are correct on dedicated
  servers with no setup.
- **Native EMI support**, alongside JEI (now JEI 19).
- **Biome `#tags`** in `conditions.biomes`, in addition to exact biome/dimension ids.
- **JSON schema** (`docs/interaction.schema.json`) for editor autocomplete and validation.
- **Example datapack** at `examples/punchthemall-examples` — **40** interactions covering every field
  of the format, each catalogued with what it shows and how to trigger it in game.
- **Item conditions are spelled out in JEI and EMI.** The hand/target tooltips used to print the tag
  structure back at you (`Enchantments[].lvl : [1 - 5] where {id:"minecraft:efficiency"}`). They now
  read as two plain blocks — *The item must have* / *The item must NOT have* — with enchantments
  named and levelled ("Efficiency I - V"). EMI shows them too, where it previously showed none.
- **`left_click` on `air` now works.** It never could before, on either loader: the event behind it
  is client-only, so the server never saw the swing. The client now reports it, and only when a pack
  actually defines such an interaction.

### Changed
- Ported to **NeoForge 21.1.x / Java 21** (ModDevGradle).
- **Item data now uses Data Components** internally (1.20.5+ removed item NBT). PTA matches items
  against a **version-stable view** (`Damage`, `Enchantments:[{id,lvl}]`, `custom`), so your
  `nbt`/`nbt_predicates` expressions keep working unchanged across mod versions.
- Interactions are synced by a small server-to-client payload sent on join and after each `/reload`.
  (A datapack *registry* would sync for free, but `/reload` cannot re-read one — see
  [docs/porting/neoforge-1.21.1-plan.md](docs/porting/neoforge-1.21.1-plan.md).)

### Fixed
- **NBT range checks compare numerically.** A `RangeTag` was matched by exact tag type, so a file
  mixing widths — `{Damage:{RangeTag:[0,500]}}` (ints) against a short value, or `[2s,7s]` against an
  int — could silently never match, or throw. Whitelists and blacklists now both compare as numbers.
- **`target.kind: "any"` no longer reports a phantom error.** Trying both a block and a fluid lookup
  is the whole point of `any`, but the failing side logged `Unknown block/fluid` even when the other
  succeeded, sending pack makers after a problem that did not exist.
- JEI drop tooltips no longer carry a "Recipe By: PunchThemAll" line.
- Weighted drop selection no longer has an off-by-one bias (it slightly over-weighted the first entry
  and under-weighted the last — noticeable with `weight: 1` fillers).
- Random rolls now use the world's random source consistently, instead of a global RNG.

---

## [1.20.1-2.0.0]

A large, **fully backward-compatible** update. Every existing interaction file keeps working; the
new features are opt-in. If you author interactions, this is the release to read.

> **Why 2.0.0?** This release introduces a new interaction format (`schema_version: 2`), a rebuilt
> parser, multiplayer synchronisation, and a big set of new capabilities — a major step up for the
> mod. It is still 100% backward compatible: old files continue to load unchanged.

### TL;DR for pack makers
- A new, cleaner **`schema_version: 2`** JSON format (strictly valid JSON, better error messages).
- Interactions can now do a lot more: **guaranteed drops, multiple rolls, Fortune bonuses, potion
  effects, sounds/particles, and conditions** (time, weather, altitude, light, sneaking, food, XP).
- **JEI now works correctly on dedicated servers**, and shows the new features.
- You can optionally load interactions from **datapacks** as well as the config folder.

### Added
- **New interaction format (`schema_version: 2`).** Opt in by adding `"schema_version": 2` to a
  file. Benefits:
  - Strictly valid JSON — no more editor/linter complaints. NBT is written as a normal SNBT
    **string** (e.g. `"{Damage:0}"`).
  - Unified `match` selectors everywhere: a single id, a list of ids, or a `#tag`.
  - Precise, path-based error messages when a file is malformed.
- **Advanced rewards** (`rewards` block):
  - `guaranteed` — items always dropped, in addition to the weighted pool.
  - `rolls` — draw the weighted pool multiple times.
  - `fortune` — add a Fortune/Looting-style bonus to weighted drops based on the held tool.
- **Player effects & feedback**:
  - `effects` — apply potion effects to the player on success (with per-effect chance).
  - `sound` / `particles` — play feedback on the interaction itself (not only on transformations).
- **Conditions** (`conditions` block): gate an interaction by `time` (day/night), `weather`,
  `y_range`, `light` level, `requires_sneaking`, and `player_state` (`min_food`, `min_xp_levels`),
  in addition to the existing biome/dimension filters.
- **Typed NBT predicates** (`nbt_predicates` on `hand` and `target`): match item/block-entity data
  with a clean `path` + `int_range` + optional `where` filter — a validated replacement for the
  old embedded `RangeTag` convention.
- **Multiplayer JEI sync.** The server now sends its interaction registry to clients, so the JEI
  category is correct on dedicated servers (previously it could be empty or wrong).
- **Optional datapack loading.** With `Loader.load_from_datapacks = true`, interactions are also
  read from `data/<namespace>/pta/interaction/*.json`, layered on top of the config folder and
  synchronised to clients by vanilla. New config key: `Loader.load_from_datapacks` (default `false`).
- **Expanded JEI display.** The category now shows guaranteed drops as extra output slots, typed
  `nbt_predicates` in the hand/target tooltips, and a summary tooltip on the arrow listing rolls,
  Fortune, effects, conditions and whether the interaction plays a sound/particles.
- **Documentation & examples.** A full v2 reference, a CurseForge overview, and 17 focused,
  copy-paste example files under `configExamples/interactions/v2/`.

### Fixed
- Unknown ids/tags in a file are now reported clearly instead of silently becoming `air`/`empty`.
- NBT **blacklist** range checks (`RangeTag` in a blacklist) now work correctly and support short
  values (enchantment levels), instead of never matching.
- Interaction **cooldown** now uses world time, so it no longer wrongly blocks a player after a
  respawn or dimension change.
- Right-click interactions no longer risk double-processing (once per hand); one click is handled
  once.
- An invalid NBT snippet no longer discards the whole interaction file — it is skipped with a log.
- Assorted null-safety fixes (biome lookups, entity reach attribute on fake players).

### Changed / Deprecated
- The original JSON format (no `schema_version`, or `schema_version: 1`) is now **deprecated**. It
  still loads exactly as before, but logs a one-time warning per file suggesting migration to v2.
  A migration table is in [`docs/interaction-format.md`](docs/interaction-format.md).
- Filtering is now indexed by click type and target, so packs with hundreds of interactions
  evaluate a click faster. Behaviour is unchanged.
- Internal parsing was rebuilt on Mojang serialization `Codec`s for the v2 path (better validation
  and error reporting). No gameplay change.

### Notes
- Nothing about existing (v1) interactions changes in-game. You can migrate file-by-file at your
  own pace, or not at all.
- The new JEI display has been built and validated to compile; if you spot a rendering glitch,
  please report it with the interaction id shown in the click tooltip.

---

## [1.20.1-1.1.0]

- Modular common config (`Interactions`, `Players`, `Drops`, `Loader`, `Debug`) with per-section
  keys and presets.
- Recursive interaction discovery and deterministic, path-based interaction IDs.
- Fake-player/automation support with dedicated config gates.
- Updated particle handling and general stability fixes.

## [1.20.1-1.0.0]

- Initial release: JSON-defined interactions (click a block, fluid, or the air with an optional
  hand item), weighted drop pools, block/fluid transformations, player damage and hunger costs,
  biome and block-state/NBT filters, and a JEI category to browse them.
