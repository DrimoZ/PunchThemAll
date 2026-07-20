# Changelog

All notable changes to **PunchThemAll** are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and the project aims to follow [Semantic Versioning](https://semver.org/).

Version tags use the form `MC-version - mod-version`, e.g. `1.20.1-2.0.0`.

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
