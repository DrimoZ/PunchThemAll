# Backlog — ideas & nice-to-haves

A parking lot, not a plan. Drop anything here the moment it comes to mind; pick from it when there is
room. Nothing here is committed to, and an idea sitting unpicked for a year is a fine outcome.

**Adding an entry:** one line of what, one line of why. If you know the catch, say it — a note that
saves the next person an hour of discovery is worth more than a tidy phrasing. Move an entry to
*Done* (with the version) or *Dropped* (with the reason) rather than deleting it; knowing something
was considered and rejected is useful.

---

## Authoring & format

- **Entity targets.** `target: { kind: "entity" }` — punch a mob for drops. The most-requested shape
  the format cannot express today. Needs a new event path (`AttackEntityEvent` /
  `PlayerInteractEvent.EntityInteract`) and a JEI/EMI display that shows an entity rather than a
  block.
- **Multi-target interactions.** One file matching a block *and* a fluid, instead of duplicating it.
- **Command / function outputs.** Run a function on success, for pack-level scripting.
- **`not` in predicates.** `nbt_predicates` can only require; excluding still means falling back to
  the SNBT blacklist. A negation flag would let a pack drop the SNBT form entirely.
- **Per-interaction cooldown.** Currently one global cooldown in the config.
- **Drops carrying real item data.** A drop's authored NBT only reaches `Damage` and `custom_data`
  today (`ItemView.applyTo`). An enchanted or component-bearing drop needs a `DataComponentPatch`
  built from the SNBT, and enchantments additionally need level-time registry access.
- **Component escape hatch.** Expose exotic 1.21 components as `components."minecraft:xxx"` for power
  users, alongside the version-stable view. Niche, and deliberately version-specific.
- **Weighted pool "nothing" entry.** `minecraft:air` as a filler works but reads like a trick; an
  explicit `{ "empty": true, "weight": n }` would say what it means. (`count: {min: 0}` is *not* the
  answer — that is a per-roll range on an entry that still exists.)
- **`hidden` on a whole namespace.** `hidden` is per file. A pack with thirty internal steps has to
  set it thirty times.

## Display (JEI / EMI)

- **Catalyst registration.** Register hand tools and target blocks as catalysts so looking up an item
  jumps to its interactions (JEI `registerRecipeCatalysts`, EMI `addWorkstation`).
- **Condition icons.** The arrow tooltip carries time/weather/Y/light/food/XP as text. A few 18×18
  icons would make it scannable.
- **Guaranteed vs weighted, visually.** Both are output slots today; only the tooltip distinguishes
  them. A badge or a separate row would.
- **EMI slot tooltips.** EMI builds its input slots in one loop, so item conditions ride on the arrow
  summary instead of the slot they describe. Fixing it means restructuring `addWidgets`.
- **Drop the `§` codes.** Most of the category still formats with `§` escapes and `.getString()`,
  which breaks translation and resource-pack theming. `ItemConstraintDescriber` shows the shape a
  proper `Component` version takes.

## Engine & internals

- **Tag freshness.** Tags are flattened into concrete `Set<Item/Block/Fluid>` when an interaction
  resolves. That is correct today because resolution is redone on every `TagsUpdatedEvent` — but
  storing `TagKey`/`HolderSet` and resolving at click time would remove the ordering constraint
  entirely. It would also make tags reachable from the test suite, which currently cannot cover them
  at all (they need a running server).
- **Sealed target type.** `PtaBlock` models block/fluid/air with an empty-set sentinel for air. A
  sealed type would be clearer and would prepare the entity target above.
- **Renames.** `PtaBlock` → `PtaTarget`, `PtaInteractionRecord` → `PtaCost` — the v2 JSON already
  uses the second name in both cases. Deliberately deferred: a wide, low-value diff.
- **Fake-player detection.** `instanceof FakePlayer` misses mods that use their own `ServerPlayer`
  subclass, and many machines never post interact events at all. A tag or config list could widen it.
  Either way the *limits* deserve a paragraph in the user docs: the config keys promise more than the
  detection can deliver, and packs will hit that.
- **Finer vanilla-interaction cancelling.** `cancel_vanilla_interaction` calls `setCanceled(true)`,
  which is all-or-nothing. NeoForge 1.21 also offers `setUseBlock`/`setUseItem` (`TriState`) on the
  block events, so a pack could suppress the block use but still let the item act. Worth exposing
  only if someone actually asks.
- **`chance` vs `weight` naming.** `PtaPool.getItemStackForChance` takes a *weight* roll, and the
  pool mixes both words for the same thing. Internal only; renaming touches the JEI display code.
- **Cooldown map keyed by UUID.** Fake players often reuse one UUID per machine type, so
  `apply_cooldown_to_fake_players` can bleed cooldowns between unrelated machines.
- **EMI is not pushed on sync.** JEI is refreshed explicitly when the interaction set changes; EMI is
  left to re-run `register` on its own reload. That has always been the case and nobody has reported
  a stale EMI category, but it is an asymmetry, not a decision.
- **`PtaInteraction.contentHash` is a plain `int`.** It exists so an unchanged reload compares equal.
  A collision would make an *edited* interaction look unchanged and leave the viewers showing the old
  one until the next restart. Astronomically unlikely, cheap to make impossible by keeping the spec
  itself instead of its hash.

## Known untested

Not defects — things the code does that nobody has watched happen. Listed so they are not mistaken
for verified behaviour.

Since 2.2.0 there is a unit suite (`./gradlew test`), so the *logic* below the game — NBT matching,
count and weight arithmetic, the codec, the resolver, the registry, sync batching — is covered.
What follows is what a unit test cannot reach.

For the layer just above it, `examples/dev-probe-pack` is a by-hand harness: drop it in a dev world,
`/reload`, and it exercises malformed ids, the `count: {min: 0}` roll, the sneak conflict warning and
`hidden` — each either logging something specific or visibly doing something. It is what found the
two example-datapack bugs fixed in 2.2.0.

- **The click path into transformations.** `TransformationApplier` itself now runs under game tests
  (`./gradlew runGameTestServer`), so breaking, placing, replacing, `require`, the offset cap and the
  per-block deduplication are all watched happening in a real world. What those tests do *not* cover
  is `PlayerInteractionHandler` calling it: the origin it picks for a fluid or air interaction, and
  the face it passes for the `face` frame. Those still need a client and a real click.
- **No *real* claim mod has vetoed a transformation.** The guard itself is now covered: four game
  tests register a listener that cancels the vanilla break and place events, which is exactly what
  FTB Chunks and the rest do, and check that the transformation is refused — and that a veto stops
  only the blocks it protects. What is still untested is a real claim mod install, where the
  question is whether it listens to those events at the priority we assume rather than whether we
  respect a veto.
- **The viewer tooltips.** `TransformationDescriber` has no test; the operation, offset and
  requirement lines have only been read in code, never on screen.
- **The EMI plugin has never been loaded.** `PtaEmiPlugin` compiles against the API and has never run
  once: the `runtimeOnly` line in `build.gradle` is commented out because the full EMI jar fails to
  download from TerraformersMC (the transfer truncates every time; the api and sources jars are
  fine). Worth knowing that an `EmiPlugin` throwing at load disables **EMI as a whole** for the
  player, not just our category. To test: drop the jar into `run/mods` by hand.
- **Dedicated servers.** Everything has been exercised on the integrated server. The sync payload
  does cross a real connection there, but never between two JVMs, and never with a client joining a
  remote host. The 2.2.0 batching in particular has only been proven by unit test — the split is
  correct, but nobody has watched a multi-batch series arrive over a wire.
- **A vanilla client on a PTA server.** The channel is optional as of 2.2.0, so it should just work.
  Never tried.
- **Most mechanics are verified as loading, not as firing.** Transformations, damage and hunger
  costs, biome and weather conditions, the Fortune bonus and potion effects all resolve without
  error; none has been triggered in game. The example datapack covers them all if someone plays with
  it.
- **The v1 rejection path.** A `schema_version: 1` file is refused in the unit suite, but never tried
  with a real legacy file in a real datapack.
- **Nothing renders in a test.** `JeiCategory` and `PtaEmiRecipe` are entirely unexercised — the row
  arithmetic they depend on is covered, the drawing is not.
- **The click path itself.** `PlayerInteractionHandler` needs a live world; the filtering it calls
  into is tested, the event wiring, cooldown timing and drop placement are not.

## Tooling

- **SPDX headers** (`SPDX-License-Identifier: MIT`) on source files.
- **Schema/codec agreement check.** `docs/interaction.schema.json` and `InteractionSpec` are kept in
  sync by hand; nothing catches a drift. A test that parses every example datapack file against the
  codec would catch half of it, and is easy now that a suite exists.
- **Validate the example datapack in CI.** The 41 files are the closest thing to an integration test
  the format has, and nothing currently loads them outside the game.
- **The test bootstrap reaches into FML internals.** `McBootstrap` publishes an empty
  `LoadingModList` by reflection so `FeatureFlags` can initialise. It is one method and it fails
  loudly with a pointed message, but a NeoForge update can break it.

---

## Done

- **Transformations that act somewhere else, and that break or place rather than only overwrite** —
  `op` + `at` + `require`, and `transformation` as a list. Suggested by Koynax. *(unreleased)*
- **`hidden` on an interaction** — keep it out of JEI/EMI without disabling it. *(1.21.1-2.2.0)*
- **A unit test suite** — 241 tests, booting the game's registries in process. Listed under *Dropped*
  for a while; the reasoning there was half right and half wrong, see that entry. *(1.21.1-2.2.0)*
- **Payload protocol discipline** — `PROTOCOL_VERSION` is now bumped with the payload shape, and the
  channel is `optional()` so a client without PTA can still join. *(1.21.1-2.2.0)*
- **`left_click` + `target: air`** — impossible on both loaders until 2.1.0 (the event is
  client-only); now reported to the server by a payload. *(1.21.1-2.1.0)*
- **Native EMI plugin**, alongside JEI 19. *(1.21.1-2.1.0)*
- **Biome `#tags`** in `conditions.biomes`. *(1.21.1-2.1.0)*
- **JSON schema** for editor autocomplete. *(1.21.1-2.1.0)*
- **Plain-language item conditions** in the viewers, replacing the raw tag dump. *(1.21.1-2.1.0)*

## Dropped

- ~~**JUnit / GameTest suite.**~~ **Reversed in 2.2.0.** The original reasoning — that the risk sits
  in in-world behaviour a unit test cannot reach — held for GameTest, and still does. It did not hold
  for the logic layer: the audit that preceded 2.2.0 found four real bugs (the `min: 0` drop, the
  `ClassCastException` in the NBT matcher, the inverted cost range, the non-deterministic match
  order) that a unit test would have caught the day it was written. The scaffolding turned out to be
  one class, `McBootstrap`. In-world behaviour is still uncovered and still listed under *Known
  untested*.
- **REI plugin.** EMI and JEI cover the field; REI pulls in Architectury for a NeoForge-only mod.
- **NEI.** Dead since 1.12 — listed only so nobody re-proposes it.
- **"Source hint" in the viewers** (show whether an interaction came from the config folder or a
  datapack). Moot since 2.1.0: datapacks are the only source.
- **Null guard on the reach attribute.** The 1.20.1 code guarded `ForgeMod.BLOCK_REACH` because a
  Forge-added attribute could be absent. `Attributes.BLOCK_INTERACTION_RANGE` is vanilla and sits in
  `Player.createAttributes()`, so every player — fake players included, they are `ServerPlayer` —
  has it. No guard needed.
- **Back-porting `ItemView` to the 1.20.1 branch.** The plan wanted both branches behind one
  contract. There is no 1.20.1 development left, and on that branch the view is ~identity over
  `getTag()`, so it would buy symmetry and nothing else.
