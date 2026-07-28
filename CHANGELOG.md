# Changelog

All notable changes to **PunchThemAll** are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and the project aims to follow [Semantic Versioning](https://semver.org/).

Version tags use the form `MC-version - mod-version`, e.g. `1.20.1-2.0.0`.

---

## [1.21.1-2.2.0] — NeoForge

A correctness pass over 2.1.0, one new authoring field, and the first automated test suite.

### Added
- **`hidden`** on an interaction. `"hidden": true` keeps it out of JEI and EMI while it loads, syncs
  and fires exactly as before — for secrets, and for the intermediate steps of a multi-stage recipe.
  Distinct from `enabled: false`, which is the one that turns an interaction *off*. Hidden
  interactions also stop stretching the category to fit their drop rows. Example:
  `pta_examples:hidden_from_viewers`.
- **A test suite**: 241 tests over the NBT matchers, the count/weight arithmetic, the codec, the
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
