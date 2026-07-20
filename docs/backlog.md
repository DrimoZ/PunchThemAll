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
  explicit `{ "empty": true, "weight": n }` would say what it means.

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
  entirely.
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
- **Payload protocol discipline.** `PtaNetwork.PROTOCOL_VERSION` is `"1"` and now covers two
  payloads. Nothing is published yet, so it has never mattered — but after the first release any
  payload change must bump it, or a mismatched client and server will believe they agree and fail at
  decode time.
- **Cooldown map keyed by UUID.** Fake players often reuse one UUID per machine type, so
  `apply_cooldown_to_fake_players` can bleed cooldowns between unrelated machines.

## Tooling

- **SPDX headers** (`SPDX-License-Identifier: MIT`) on source files.
- **Schema/codec agreement check.** `docs/interaction.schema.json` and `InteractionSpec` are kept in
  sync by hand; nothing catches a drift.

---

## Done

- **`left_click` + `target: air`** — impossible on both loaders until 2.1.0 (the event is
  client-only); now reported to the server by a payload. *(1.21.1-2.1.0)*
- **Native EMI plugin**, alongside JEI 19. *(1.21.1-2.1.0)*
- **Biome `#tags`** in `conditions.biomes`. *(1.21.1-2.1.0)*
- **JSON schema** for editor autocomplete. *(1.21.1-2.1.0)*
- **Plain-language item conditions** in the viewers, replacing the raw tag dump. *(1.21.1-2.1.0)*

## Dropped

- **JUnit / GameTest suite.** Considered during the port and dropped: heavy scaffolding for the
  return, given how much of the risk sits in in-world behaviour that unit tests would not reach
  anyway. Revisit if the logic layer grows or a regression escapes twice.
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
