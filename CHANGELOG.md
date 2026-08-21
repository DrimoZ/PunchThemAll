# Changelog

All notable changes to **PunchThemAll** are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and the project aims to follow [Semantic Versioning](https://semver.org/).

Version tags use the form `MC-version - mod-version`, e.g. `1.20.1-2.0.0`.

---

## [1.20.1-2.4.0]

Brings this line level with the NeoForge 1.21.1 one for everything a transformation can
do, so a pack can move between them without editing files. Interactions written for
2.1.0 are unaffected: without `op`, `at` or `all`, a transformation does exactly what it
did before.

### Added
- **Transformations can act on a block other than the one you clicked, and can `break` or
  `place` rather than only overwrite.** `at` gives an offset read in world axes
  (`relative_to: "world"`), against the player's facing (`"player"`), or out of the clicked
  face (`"face"`). `transformation` also accepts a list, applied in declaration order, each
  entry rolling its own chance. Server owners get a distance cap and a per-click budget, and
  claim mods can veto every one of them.
- **Regions.** An offset can name a second corner with `to`, covering the whole box between
  the two, corners included — a 3×3 excavator is one entry rather than nine.
- **`require` on a transformation.** Same shape as `target`, but asked of the destination
  rather than of the block you clicked. Nothing is written unless it matches.
- **`into: { "kind": "copy" }`** — write whatever block is already somewhere, instead of
  naming one. With `from` to say where to read it. A `copy` plus a `break` is a block that
  moves.
- **A chance for a whole set.** `transformation: { "chance": 0.7, "all": [ ... ] }` rolls
  once for the set, so a pattern either appears or does not, rather than appearing half-built.
- **`conditions.neighbours`** — gate an interaction on the blocks *around* the target, with
  `invert` for "and not".
- **`rewards.at`** — where the drops land, when the interaction is really acting elsewhere.
- **`drops: "tool"` on `op: "break"`** — the block's loot as broken by the held item,
  Fortune and Silk Touch included.
- **Air interactions can carry transformations**, measured from the player.
- **Tooltips in two lengths.** The transformation slot says what it does and where; the full
  breakdown sits behind a key you choose. New `pta-client.toml` holds `detail_key`
  (shift/control/alt/always/never) and `max_drop_rows`.
- **A test suite, where this branch had none.** 294 unit tests with the game booted in
  process, and 28 in-world tests covering the transformation pipeline. `./gradlew verify`
  runs both.
- **The 69-interaction example datapack** from the 1.21.1 line, with three tests that keep it
  honest: every example parses, every id resolves against the game registries, and every
  feature of the format has an example. Set `load_from_datapacks = true` to use it.

### Changed
- **A click transforms any one block at most once**, rather than performing at most one
  transformation. Two interactions matching the same click can both act, as long as they act
  somewhere different.
- **A recipe no longer inherits the height of the widest one in the category.** The box is
  capped by `max_drop_rows`, and the drops past the cap share a slot, which JEI cycles through
  on its own. Guaranteed and weighted drops are laid out separately, guaranteed first, so a
  square never alternates between what you always get and a one-in-four chance.
- **Item requirements read as sentences** — "The item must have: Efficiency I - V" — instead
  of the authored tag structure printed back at you.
- **Transformations post block break/place events**, so claim and protection mods can veto
  them, and `op: "place"` refuses a destination the block could not survive on.

### Fixed
- **Every tag selector resolved to nothing.** Reload listeners run before tags are bound, so
  `#minecraft:hoes` and every other tag loaded as an empty set: the interaction drew as a
  barrier in JEI and could never fire. Interactions are now resolved again once tags exist,
  and the first pass loads quietly instead of reporting problems that are not real.
- **Biome tags never matched.** `conditions.biomes` compared entries as plain strings, so a
  `#minecraft:is_forest` entry matched no biome while the resolver validated it as a
  well-formed tag and said nothing.
- **`left_click` on air could never fire.** The event behind it is posted on the client only,
  and the handler returns on the client, so the type existed and did nothing. The client now
  tells the server, which re-derives every gate itself.
- **A malformed id took down the whole datapack load.** Every registry lookup built its
  `ResourceLocation` with the constructor, which throws on authored text like `"a b c"`; one
  typo in one file threw out of the reload listener and cost the pack every interaction. An
  unknown id also answers `null` now rather than the registry default, which used to hand back
  `AIR` and read as a real block.
- **An empty row of slots** hung below a recipe whose drop grid was capped.
- **The sneak requirement was shown in two places that could disagree.** It belongs to the
  type; the icon already shows it.
- Drop rolls take the level's random source instead of `Math.random`, and an interaction
  carries a hash of its source file so a reload of an unchanged pack stops churning JEI.

---

## [1.20.1-2.1.0]

### Added
- **`hand.consume.count`** (`schema_version: 2`). How much a successful interaction spends, on top of
  the existing `chance`, which only ever decides *whether* anything is spent. The two are
  independent and compose:

  ```json
  "consume": { "mode": "shrink", "chance": 0.33, "count": { "min": 3, "max": 5 } }
  ```

  = one click in three costs 3–5 items. It takes the same shapes as every other count (`3`,
  `{ "count": 3 }`, `{ "min": 3, "max": 5 }`), defaults to `1`, and has a floor of `1` — spending
  nothing is what `chance` and `mode: "none"` are for. Under `mode: "durability"` it is durability
  points rather than items, and a tool that runs out still breaks exactly once. Holding fewer items
  than the roll asks for never blocks the interaction; it just costs what is there. JEI shows an
  *Amount* line on the hand slot whenever the value is not `1`. Example:
  `configExamples/interactions/v2/19_consume_count.json`.

  The legacy (schema 1) loader ignores it, as it already ignores `enabled` and `hidden`.
- **`hidden`** on an interaction (`schema_version: 2`). `"hidden": true` keeps it out of JEI while it
  loads, syncs and fires exactly as before — for secrets, and for the intermediate steps of a
  multi-stage recipe. Distinct from `enabled: false`, which is the one that turns an interaction
  *off*. Hidden interactions also stop stretching the category to fit their drop rows. Example:
  `configExamples/interactions/v2/18_hidden_from_jei.json`.

  Back-ported from the NeoForge 1.21.1 line so a pack can move between them without editing files.
  The legacy (schema 1) loader ignores it, as it already ignores `enabled`.

### Fixed

Also back-ported from the 1.21.1 audit. All six affect both schema versions.

- **`count: { "min": 0, "max": 3 }` on a drop now means "nothing to three".** It silently meant
  "never anything": the entry produced no items, vanished from JEI, and still consumed its weight in
  the pool — while the docs advertised it as valid.
- **A shape mismatch in `nbt.whitelist` / `nbt.blacklist` no longer crashes mid-click.** Requiring a
  list where the item or block entity holds something else threw a `ClassCastException` from inside
  the interaction filter.
- **`RangeTag` in a whitelist now compares numerically.** It matched on the exact tag type, so a
  range written as `[1,5]` (ints) against an enchantment level (a short) threw a
  `ClassCastException`, and valid files silently failed to match. A malformed `RangeTag` is now
  rejected rather than running off the end of the list — the old size check was an `assert`, which
  is disabled at runtime. The blacklist path already did this; the two now agree, and both accept any
  numeric width.
- **Which interaction wins is now deterministic.** Matches were collected into a hash set, so with
  `max_matches_per_click`, or with several interactions competing to transform one block, the winner
  varied between runs and between machines. Candidates are ordered by id.
- **Fortune can no longer produce an over-sized stack.** The bonus is clamped to the item's maximum
  stack size; the surplus used to disappear the moment the drop entered an inventory.
- **A cost's maximum is clamped against its floored minimum**, so `amount: 0` no longer produces an
  inverted range. Slot counting also matches what JEI lays out, so a drop grid can no longer overflow
  the category background.

> These were found by an audit on the 1.21.1 branch, which has a unit suite covering this logic. This
> branch has none, so the fixes here are verified by review and a build only — see `docs/backlog.md`.

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
