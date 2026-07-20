# Porting plan — PunchThemAll → NeoForge 1.21.1

Status: **done and verified in game** (1.21.1-2.1.0, branch `neoforge_1.21.1`). Kept as a record of
the reasoning, not as a live task list — everything below has been carried out, or moved to
[../backlog.md](../backlog.md) with a reason.

Where the plan turned out to be wrong, the correction is inline (see §4d) rather than a rewrite, so
the mistake stays visible.

**Audited against the code, section by section. Not carried out, and why:**

| Plan item | Outcome |
| --- | --- |
| §4b finer `TriState` cancelling | Backlog — `setCanceled(true)` is enough until a pack asks for more |
| §4b null guard on the reach attribute | Dropped — `BLOCK_INTERACTION_RANGE` is vanilla and always present |
| §4b document fake-player limits | Backlog — the detection is still heuristic, the docs still oversell it |
| §5 drops carrying components / enchantments | Backlog — `ItemView.applyTo` covers `Damage` and `custom_data` only |
| §5 `components."ns:id"` escape hatch | Backlog — niche, and deliberately version-specific |
| §5 back-port `ItemView` to 1.20.1 | Dropped — no development left on that branch |
| §12 source hint (config vs datapack) | Dropped — datapacks are the only source since 2.1.0 |
| §13 `chance` vs `weight` naming | Backlog — internal, and it touches the display code |
| §13 renames (`PtaBlock`/`PtaInteractionRecord`) | Backlog — wide, low-value diff |
| §13 SPDX headers | Backlog |
| §13 CI (GitHub Actions) | Dropped — the workflow was removed from the repo; builds are run locally |
| §14.2 test suite | Dropped — heavy scaffolding for the return; revisit if a regression escapes twice |

Everything else in §1–§15 is implemented, including the two items the plan left open as questions:
`left_click` on `air` (§4b, §13) now works via a client payload, and §14.1's JSON schema shipped.

## 0. Goals & scope

- **Platform:** Forge 1.20.1 (Java 17) → **NeoForge 1.21.1 (Java 21)**.
- **JEI:** 15.12 → **JEI 19.x** (19.x is the 1.21.1 line).
- **Architecture decision (locked in):** this version accepts **only the `schema_version: 2`
  architecture**. The legacy v1 parser is **removed entirely** — `InteractionCreator`,
  `InteractionJsonReader`, the regex/lenient SNBT path, and the v1 half of `RegistryConstants` all
  go. `InteractionParser` becomes a thin v2-only entry point.
- Behaviour of v2 interactions must stay equivalent, except where 1.21 semantics force a redesign
  (item data → Data Components — see §5).
- **Loading is datapack-first (decided):** interactions load only from
  `data/<ns>/pta/interaction/*.json`; the config folder **and** the custom S2C networking are removed
  (vanilla syncs datapacks). `pta-common.toml` keeps only global settings. See §4d.
- **Recipe viewers (decided):** port JEI (→ 19), then add a **native EMI** plugin as a fast-follow.
  REI optional; NEI is dead. See §12.
- Versioning going forward: `2.1.0+1.21.1` style (mod+mc), per NeoForge convention.

## 1. What ports essentially unchanged ✅

These are vanilla / Mojang-serialization based and carry over with at most import tweaks:

- **The whole codec layer** `core/codec/` — `PtaCodecs`, `CountSpec`, `InteractionSpec` (+ nested
  specs), most of `InteractionSpecResolver`. `com.mojang.serialization.*`, `RecordCodecBuilder`,
  `JsonOps`, `DataResult`, `Codec.either/listOf`, `TagParser` are all unchanged in 1.21.
- **Pure model/logic**: `PtaConditions`, `PtaRewards` (weighting/rolls math), `PtaPool`,
  `PtaDropRecord` (count math), `PtaNbtPredicate` (the path walker), `PtaEffect`/`PtaExtras`
  containers, `PtaStateRecord`, enums. Logic is loader-agnostic.
- **Filtering** in `InteractionRegistry` (the index + filter pipeline) — pure logic; only the
  registry/tag lookups and item-NBT access change.
- **Config *shape*** `PTAConfig` — same structure; only the spec class name and registration change
  (§4).
- **Lang/JSON assets** and all example files.

## 2. Cross-cutting API renames (mechanical, everywhere)

| Forge 1.20.1 | NeoForge 1.21.1 |
| --- | --- |
| `net.minecraftforge.*` | `net.neoforged.neoforge.*` (and some moved to vanilla) |
| `new ResourceLocation(s)` / `(ns, path)` | `ResourceLocation.parse(s)` / `ResourceLocation.fromNamespaceAndPath(ns, path)` |
| `MinecraftForge.EVENT_BUS` | `NeoForge.EVENT_BUS` |
| `@Mod` + `FMLJavaModLoadingContext.get().getModEventBus()` | `@Mod(id)` constructor receives `IEventBus modBus, ModContainer container` |
| `ForgeRegistries.ITEMS/BLOCKS/FLUIDS/SOUND_EVENTS/MOB_EFFECTS` | `BuiltInRegistries.ITEM/BLOCK/FLUID/SOUND_EVENT/MOB_EFFECT` |
| `net.minecraftforge.common.util.FakePlayer` | `net.neoforged.neoforge.common.util.FakePlayer` |
| `mods.toml` | `META-INF/neoforge.mods.toml` |

`ResourceLocation.parse` alone touches the checkers, resolver, network, JEI plugin, and effect/sound
resolution — do it as one sweep.

## 3. Build & metadata (Phase 0)

- Replace ForgeGradle with **ModDevGradle (NeoGradle)**; `build.gradle` + `settings.gradle` rewrite.
- `gradle.properties`: `minecraft_version=1.21.1`, `neo_version=21.1.x`, Java 21 toolchain, drop
  `forge_version`, update `jei_version` to a 19.x build, bump `mod_version`.
- `mods.toml` → `META-INF/neoforge.mods.toml` (loader `javafml`, `loaderVersion="[21,)"`, deps on
  `neoforge`/`minecraft`, `license="MIT"`).
- `gradle-wrapper` bump if needed.
- ⚠️ Verify current `neo_version` and JEI 19 build against live docs at implementation time.

## 4. Loader, events, config, networking (Phase 1–2)

### Main class `PunchThemAll`
- Constructor takes `(IEventBus modBus, ModContainer container)`.
- Config registration: `container.registerConfig(ModConfig.Type.COMMON, PTAConfig.COMMON_CONFIG, ...)`.
- Game-bus registrations move to `NeoForge.EVENT_BUS`; common/client setup via `modBus.addListener`.
- Network registration moves to a `RegisterPayloadHandlersEvent` listener on the mod bus (§ below).

### Config `PTAConfig`
- `ForgeConfigSpec` → `net.neoforged.neoforge.common.ModConfigSpec` (same builder API, renamed).
  Keep every key including `load_from_datapacks`.

### Events
- `PlayerInteractionHandler`: `PlayerInteractEvent.{LeftClickBlock,LeftClickEmpty,RightClickBlock,
  RightClickItem}` and `PlayerEvent.PlayerLoggedOutEvent` move to `net.neoforged.neoforge.event.*`
  (names unchanged). Register on `NeoForge.EVENT_BUS`.
  - **Reach:** `ForgeMod.BLOCK_REACH.get()` → vanilla `player.blockInteractionRange()` /
    `Attributes.BLOCK_INTERACTION_RANGE` (added in 1.20.5). Simplifies `getBlockReach`/`rayTrace`.
  - **Durability:** `itemStack.hurt(1, random, null)` → `itemStack.hurtAndBreak(1, serverLevel,
    serverPlayer, item -> {})` (1.21 signature — needs a `ServerLevel`). ⚠️ verify signature.
  - **Damage/food:** `player.hurt(src, amt)`, `FoodData` setters — unchanged.
- `DataReloadListener`: `AddReloadListenerEvent` and `ResourceManagerReloadListener` still exist
  (`net.neoforged.neoforge.event.AddReloadListenerEvent`). `ServerLifecycleHooks` →
  `net.neoforged.neoforge.server.ServerLifecycleHooks`.
- `PtaSyncEvents`: `PlayerLoggedInEvent` → NeoForge package; send via new payload API.

### Networking `core/network` — **REMOVED** (decision)
Going **datapack-first** (see §4d) makes the entire custom S2C sync redundant: vanilla synchronises
datapacks server→client automatically, so the client registry is populated on join/reload for free.
Therefore **delete `core/network/` wholesale** — `PtaNetwork`, `SyncInteractionsPacket`,
`ClientPacketHandler`, `PtaSyncEvents`. No `CustomPacketPayload`/`StreamCodec`/`PayloadRegistrar`
work is needed. (This also erases the whole "networking" risk row from §11.)

The only remaining client concern is refreshing JEI/EMI after the client receives updated data — use
a client hook such as `RecipesUpdatedEvent` (or the reload listener's own client-side pass) to call
`refreshFromRegistry()`, instead of the old "sync packet triggers refresh".

## 4b. Fake players & interaction-event nuances (subtle — easy to get wrong)

PTA leans heavily on fake-player detection and the interact events, and several of these are
genuinely *different* on NeoForge 1.21.1 (not just renames). All of this lives in
`PlayerInteractionHandler`.

- **FakePlayer moved package** (mechanical): `net.minecraftforge.common.util.FakePlayer` →
  `net.neoforged.neoforge.common.util.FakePlayer` (factory: `FakePlayerFactory.get(ServerLevel,
  GameProfile)`). PTA only does `instanceof FakePlayer`, so just the import changes. Note: NeoForge
  warns that *creating* fake players with arbitrary UUIDs leaks memory — irrelevant to us since PTA
  only detects, never creates them.
- **Detection stays heuristic.** Not every automation "player" extends NeoForge's `FakePlayer`
  (some mods use their own `ServerPlayer` subclasses, or drive interactions with no player at all).
  So `instanceof FakePlayer` catches the common case but never all of them — same as on Forge. The
  port is a good moment to *optionally* widen detection (e.g. a config/tag, or also flagging players
  with a synthetic connection), but keep the current check for behaviour parity by default.
- **Whether PTA even sees a fake-player action is mod-dependent.** Many machines call lower-level
  methods (`useItemOn`, the server game-mode) that do **not** post `PlayerInteractEvent`. Those never
  reach PTA on either loader. "Fake-player support" therefore only covers automation that goes
  through the standard interact events (e.g. Click Machine right-click). Unchanged by the port —
  worth documenting so expectations are right.
- **Reach for fake players.** After switching to `player.blockInteractionRange()`
  (`Attributes.BLOCK_INTERACTION_RANGE`), fake players (being `ServerPlayer`) have the attribute, but
  their eye position / look angles are usually meaningless, so the ray trace can miss or hit the
  wrong block — PTA already mitigates by using the event's `getPos()` for block clicks. **Keep a
  null/presence guard** on the attribute; some non-standard fake players may not have a full
  attribute map. Re-verify the guard translates to the vanilla accessor.
- **Off-hand heuristic stays valid.** Fake players commonly report an empty off-hand, so PTA's
  `offHandItem = mainHandItem` workaround for `FakePlayer` should remain.
- **Cooldown map keyed by UUID.** Fake players often reuse a fixed UUID per machine type, so
  `apply_cooldown_to_fake_players` can bleed cooldowns across machines. Pre-existing; note it.

Interaction-event changes (affect real *and* fake players):

- **Cancellation model changed.** `LeftClickBlock`/`RightClickBlock` now expose
  `setUseBlock(TriState)` / `setUseItem(TriState)` instead of Forge's `Event.Result`.
  `setCanceled(boolean)` still exists, so PTA's `event.setCanceled(true)` for
  `cancel_vanilla_interaction` still works — but re-evaluate whether finer `TriState` control is
  wanted (e.g. cancel the block-break but allow item use).
- **`LeftClickEmpty` is client-only.** It fires only on the physical client, and PTA returns early on
  `isClientSide()`. So **`type: left_click` + `target: air` cannot be handled from this event
  server-side** — a latent limitation on Forge too. The port is the right time to fix it properly:
  send a small C2S payload (we already have the payload system after §4) when the client sees a
  left-click-empty that could match. Flag as a design item, not a mechanical port.
- **`RightClickBlock` still fires once per hand** — keep the existing `getHand() != MAIN_HAND` guard.
- **`LeftClickBlock` still fires start/abort/stop** — keep the existing `getAction()` handling
  (client fires start + held; server fires start + end).

## 4c. Note: PTA registers no blocks/items

PTA adds **no** content (no blocks, items, entities, block entities, menus). It is purely
events + config + networking + JEI. So the whole `DeferredRegister` / `BuiltInRegistries.register`
migration story does **not** apply — there is nothing to register, no creative tab, no data
generation (`runData` is a no-op). This meaningfully shrinks the port surface: the only registry
interaction is *reading* existing registries/tags in the checkers (§7).

## 4d. Loading model — **datapack-first** (decision)

Interactions **leave the config folder**. The single source of truth becomes datapacks:
`data/<namespace>/pta/interaction/*.json`, loaded by one `SimpleJsonResourceReloadListener` that
parses each file through `InteractionSpec.CODEC` (§9) and fills `InteractionRegistry`.

Consequences:
- **Delete** `InteractionLoader` (config-dir `Files.walk`) and `DatapackInteractionLoader` (the
  layered merge). One listener replaces both.
- **Delete** the custom networking (§4 above) — vanilla syncs datapacks to clients for free, which is
  the main reason for this choice. Dedicated-server JEI/EMI becomes correct with no packet code.
- **Free wins:** `/reload` support, datapack override/priority by pack order, and
  `neoforge:conditions` load conditions (gate an interaction on another mod being present, etc.).

> ⚠️ **Correction (found during implementation).** "`/reload` support" and "vanilla syncs it" are
> **mutually exclusive**, and this section was wrong to claim both. A datapack *registry* is read
> once by `WorldLoader`; `MinecraftServer.reloadResources` passes its existing `this.registries`
> straight through, so `/reload` can never re-read it — verified empirically (0 interactions after
> two `/reload`s, 21 after a rejoin, same files on disk) and in the 1.21.1 source. The shipped
> implementation therefore uses an ordinary `SimpleJsonResourceReloadListener` (reloadable) plus a
> small S2C payload sent on `OnDatapackSyncEvent` (which fires on join *and* after a reload). That
> reinstates a trimmed `core/network/`, contrary to §4/§11 below.
- **IDs** come from the resource path exactly like other datapack data:
  `data/mypack/pta/interaction/early/flint.json` → `mypack:early/flint`.
- PTA still ships **no** interactions by default; packs provide a datapack (a `pack.mcmeta` +
  `data/<ns>/pta/interaction/…`). Examples move from `configExamples/` to a sample datapack layout in
  the docs.
- **`pta-common.toml` stays** for global settings; only the `Loader` section is affected (below).

### Config impact (`PTAConfig`)
- Keep `Interactions`, `Players`, `Drops`, `Debug`.
- **Drop the `Loader` section almost entirely** — `recursive_discovery`, `lowercase_generated_ids`,
  and `load_from_datapacks` were all about the config-folder scan and no longer apply. `fail_fast`
  can stay as a dev toggle (throw on the first bad JSON during datapack load) or be dropped too.

## 5. Item data: NBT → Data Components (Phase 3 — the crux) ⚠️

This is the biggest semantic change and the main design decision of the port.

**Problem.** In 1.20.5+, items no longer expose a single `getTag()` CompoundTag. Data lives in typed
**Data Components** (`ItemStack.getComponents()`), and enchantments are in
`DataComponents.ENCHANTMENTS` (`ItemEnchantments`), *not* an `Enchantments` NBT list. Every place PTA
reads item NBT breaks:

- `InteractionRegistry.matchesNBTWhitelist/Blacklist` and `matchesPredicates(mainHandItem.getTag()…)`
- `TagHelper` (item NBT range matching)
- `nbt_predicates` on the **hand** (`Enchantments[].lvl`, `Damage`, …)
- hand `nbt.whitelist/blacklist` SNBT strings (item-shaped)
- `PtaDropRecord.getItemStack()` → `new ItemStack(item, count, nbt)` (constructor removed)

**Block-entity NBT is mostly fine** — block entities still serialize to a CompoundTag, via
`blockEntity.saveWithoutMetadata(level.registryAccess())` (was `serializeNBT()`), and transformation
NBT applies via `loadWithComponents`. So `target.nbt` / target `nbt_predicates` survive with the new
call + a `registries` argument.

**Hard constraint (decided):** the **JSON authoring format must stay the same across mod versions**
(1.20.1 Forge and 1.21.1 NeoForge). Packmakers must not rewrite their `nbt_predicates` / `hand.nbt`
when moving versions. This rules out exposing either raw NBT (1.20.1) or serialized components (1.21)
directly, since those two structures genuinely differ.

**Chosen approach — a PTA-owned "normalised item view":**
PTA defines its **own stable pseudo-NBT shape** for an item and populates it per loader; all item
matching runs against that view, never against the real item structure.

- Stable shape (the 1.20.1-vanilla-ish names authors already use):
  `{ "Damage": <int>, "Enchantments": [ { "id": "...", "lvl": <int> } ], "custom": { …modded NBT… } }`.
- **1.20.1**: the view ≈ the raw `getTag()` — existing files already match, no change.
- **1.21.1**: PTA **synthesises** the same shape from Data Components
  (`minecraft:damage` → `Damage`, `minecraft:enchantments.levels` → `Enchantments:[{id,lvl}]`,
  `minecraft:custom_data` → `custom`). One small adapter, e.g. `ItemView.of(stack)` → `CompoundTag`.
- The existing generic `PtaNbtPredicate` path-walker **and** the SNBT `hand.nbt` whitelist/blacklist
  run unchanged against this view. So `{ "path": "Damage", "int_range": [0,500] }` and
  `{ "path": "Enchantments[].lvl", "where": "{id:'minecraft:unbreaking'}" }` work **identically on
  both versions** — zero format change for packmakers.
- Fragility is **centralised in one PTA adapter** (updated once if Mojang changes component layout),
  not spread across every pack's JSON.
- **Escape hatch (1.21-only, rare):** expose exotic components under `components."minecraft:xxx"` for
  power users. Only this niche path is version-specific; the common cases stay consistent.

To keep the two branches truly aligned, the same `ItemView` abstraction should be **back-ported to the
1.20.1 branch** (trivial there — it's ~identity over `getTag()`), so both implement one contract.

**Still required on 1.21 regardless of the above:**
- **Drops with data:** `new ItemStack(item, count, nbt)` is gone. Apply a `DataComponentPatch` parsed
  from the drop's SNBT to a base `new ItemStack(item, count)` (keeps `match` = item id). The drop SNBT
  authoring can stay the same if we likewise map the stable shape → a component patch.
- **Block-entity NBT** (`target.nbt` / target `nbt_predicates`) already stays as real CompoundTag —
  just swap `serializeNBT()` → `saveWithoutMetadata(level.registryAccess())` and `load` →
  `loadWithComponents`. No view needed there.
- **Enchantment resolution is now level-dependent.** `ForgeRegistries.ENCHANTMENTS` is gone;
  enchantments are a **datapack registry** (`Registries.ENCHANTMENT`). `PtaRewards.fortuneEnchant`
  stores a `ResourceKey<Enchantment>`/id and resolves a `Holder<Enchantment>` at drop time via
  `level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)`, then
  `EnchantmentHelper.getItemEnchantmentLevel(holder, stack)`. (The `ItemView` synthesis of
  `Enchantments` also goes through this registry to map holder → id.)

## 6. Effects, sounds, particles, enchantments (Phase 3, smaller)

- `PtaEffect` stores `MobEffect` → store `Holder<MobEffect>`; `MobEffectInstance(Holder, dur, amp)`.
  Resolve via `BuiltInRegistries.MOB_EFFECT.get(rl)` → `.wrapAsHolder()` (mob effects stay in
  `BuiltInRegistries`).
- Sounds: `BuiltInRegistries.SOUND_EVENT.get(rl)`; `level.playSound` unchanged.
- Particles: `BlockParticleOption(ParticleTypes.BLOCK, state)` and `ServerLevel.sendParticles`
  unchanged.

## 7. Registry checkers & tags (Phase 1)

`ItemChecker`/`BlockChecker`/`FluidChecker`:
- `ForgeRegistries.X.containsKey(rl)` → `BuiltInRegistries.X.containsKey(rl)`.
- `getValue(rl)` → `get(rl)` (still returns the default AIR/EMPTY, so the existing `doesXExist`
  guards remain correct).
- Tags: `ForgeRegistries.X.tags()` + `ITagManager` → `BuiltInRegistries.X.getTag(TagKey)` →
  `Optional<HolderSet.Named<T>>`; flatten with `.stream().flatMap(HolderSet::stream).map(Holder::value)`.
  Tag existence = `getTag(key).isPresent()`.

## 8. JEI 15 → 19 (Phase 4) ⚠️ verify heavily

JEI 19 keeps the overall shape (`IModPlugin`, `IRecipeCategory<T>`, `RecipeType.create`,
`IGuiHelper`, `IDrawable`, `IJeiRuntime`, `onRuntimeAvailable`, `IRecipeManager.addRecipes/hideRecipes`)
but several signatures changed across 15→19:
- Slot ingredient API (`addItemStacks` vs `addIngredients`/`add`), fluid ingredients (NeoForge
  `FluidStack`), and tooltip callbacks (`ITooltipBuilder`) need checking against the JEI 19 javadocs.
- `IRecipeCategory` background/size handling and the `draw`/tooltip hooks changed slightly.
- Keep the runtime-sync design (`refreshFromRegistry` via `IRecipeManager`) — the concept is intact.

Treat the JEI plugin/category as a **rewrite-and-verify** unit, not a mechanical port.

## 9. Remove the legacy path (Phase 5)

- Delete `InteractionCreator.java`, `InteractionJsonReader.java`.
- `InteractionParser`: drop the `schema_version` routing; parse **every** file via
  `InteractionSpec.CODEC`. Require `schema_version == 2` (or absent → assume 2); reject anything else
  with a clear error. Remove the deprecation-warning branch.
- Trim `RegistryConstants` to what the codec/resolver still use (`SAME_STATE`, `INCORRECT_FORMAT`,
  `STRING_SCHEMA_VERSION`); delete the ~90 v1 key constants.
- `TagHelper`: keep only if still used by block-entity SNBT matching; otherwise fold its range logic
  into `PtaNbtPredicate` and delete. (v2 already prefers `nbt_predicates`.)
- Update `configExamples/` — the legacy (non-`_v2`) samples become dead; either delete or convert.

## 10. Suggested phase order & checkpoints

1. **Phase 0 — build**: NeoGradle, `neoforge.mods.toml`, gradle.properties; empty `@Mod` compiles.
2. **Phase 1 — core APIs**: ResourceLocation sweep, registries/checkers, config (drop `Loader`),
   events (no JEI).
3. **Phase 2 — datapack loading**: `SimpleJsonResourceReloadListener` for
   `data/<ns>/pta/interaction/*.json`; delete `InteractionLoader`/`DatapackInteractionLoader` **and
   all of `core/network/`** (§4d). Client JEI/EMI refresh via `RecipesUpdatedEvent`.
4. **Phase 3 — item data**: the Data Components redesign (§5) + effects/enchant holders.
5. **Phase 4 — JEI 19**: rewrite plugin/category, verify against javadocs. *(EMI native = fast-follow
   after a shared render helper is extracted — §12.)*
6. **Phase 5 — legacy removal + cleanup** (§9).
7. **Phase 6 — verify**: `./gradlew build`, `runClient`, `runServer`; ship the examples as a sample
   datapack, `/reload`, check drops/effects/conditions and JEI; test on a dedicated server (sync is
   now vanilla, so just confirm the datapack reaches the client).

Compile after each phase. Keep phases as separate commits on `neoforge_1.21.1`.

## 11. Risk register / verify-at-implementation

- 🔴 **Item Data Components** (§5) — largest surface; the item-NBT matching redesign and drop-NBT
  reconstruction must be re-tested end to end.
- 🔴 **JEI 19 API drift** (§8) — verify every slot/tooltip/fluid/runtime call against JEI 19 javadocs.
- 🟠 **Enchantment registry is now dynamic** — Fortune bonus + enchantment predicates need a
  `RegistryAccess` at runtime, not at load.
- ✅ **Networking** — *eliminated.* Datapack-first (§4d) removes `core/network/` entirely; vanilla
  syncs the datapack. Only a client `RecipesUpdatedEvent` hook remains to refresh JEI/EMI.
- 🟡 **Client viewer refresh timing** — confirm `RecipesUpdatedEvent` (or the reload listener's
  client pass) fires after the datapack registry is populated, so JEI/EMI show the server's set.
- 🟠 **`LeftClickEmpty` is client-only** (§4b) — `left_click` + `air` interactions can't be handled
  server-side from the event; needs a small C2S payload. Decide whether to fix in the port or keep
  the current limitation.
- 🟠 **Fake-player detection & event coverage** (§4b) — `instanceof FakePlayer` misses non-standard
  fake players, and many machines don't post interact events at all. Behaviour parity by default;
  document the limits.
- 🟡 **Interaction cancellation model** (`TriState` `setUseBlock/setUseItem`) — re-check
  `cancel_vanilla_interaction` still behaves as intended.
- 🟡 `ItemStack.hurtAndBreak` and `BlockEntity.saveWithoutMetadata/loadWithComponents` signatures —
  confirm for 1.21.1 exactly.
- 🟡 `player.blockInteractionRange()` availability + fake-player attribute presence — confirm.

## 12. Recipe viewers (JEI / EMI / REI / NEI) & category UI

### Landscape on 1.21.1
- **JEI 19.x** — Forge/NeoForge. Current integration; port the existing plugin (§8).
- **EMI 1.1.x** — NeoForge **and** Fabric; the modern, accessibility-focused viewer, and it ships a
  **JEI-compat layer** (so the ported JEI plugin already appears inside EMI). A *native* EMI plugin
  still gives a nicer, info-recipe-friendly display.
- **REI** — Architectury/Fabric-leaning; works on NeoForge via Architectury. Lower priority for a
  NeoForge-only mod (extra dependency surface).
- **NEI (Not Enough Items)** — **dead**: it's the 1.7–1.12 viewer; there is no 1.21 build. Its
  spiritual successors are exactly JEI/REI/EMI. **Not a target** — mention it only to clarify.

### Recommendation
1. **Keep JEI** (port to 19) as the baseline — widest install base, and EMI reads it via compat.
2. **Add a native EMI plugin.** EMI is increasingly the default in modern packs and is the best fit
   for "informational" recipes like ours. Small, well-documented API (`EmiPlugin` + `EmiRecipe`).
3. **REI: optional/later** — only if there's demand; it pulls in Architectury.

Architecture: keep the display data viewer-agnostic (it already is — `PtaInteraction`) and write
**thin adapters** per viewer (`jei/`, `emi/`) that share one layout/rendering helper. Avoid
duplicating the tooltip/section logic in each plugin.

**EMI-native verdict — worth it, as a fast-follow.** Our JEI plugin already shows inside EMI via
EMI's JEI-compat layer, so it isn't strictly required. But because our category is heavily
custom-drawn, the compat bridge is exactly where fidelity degrades; a native `EmiRecipe` renders
pixel-perfectly, gets first-class "info recipe" semantics, better tooltips/accessibility, and works
cleanly in EMI-only packs (now common). Do **JEI first** (baseline), then add the native EMI plugin
once the shared render helper exists — the marginal cost is small and the payoff for the growing EMI
userbase is real.

### Category UI improvements (apply to any viewer)
- **Proper `Component` styling instead of hardcoded `§` codes + `.getString()`** (audit §7) —
  makes labels translatable and resource-pack/theme friendly. Biggest polish win.
- **Clear three-zone layout**: Inputs (hand + target) → Process (arrow: rolls / Fortune / conditions
  / effects) → Outputs. Currently the arrow tooltip carries a lot; consider surfacing it visually.
- **Distinguish guaranteed vs. weighted drops visually** (a badge/frame or a separate row), not only
  via tooltip text.
- **Condition / effect icons** (a few new 18×18 textures) with tooltips, replacing the text-only
  arrow summary — much more scannable.
- **Catalyst registration** — register hand tools and target blocks as catalysts so viewing an item
  jumps to its interactions (JEI `registerRecipeCatalysts`, EMI `addWorkstation`).
- **Fluid rendering** via the NeoForge `FluidStack` ingredient (JEI 19 / EMI both support it).
- **Source hint** — optionally show whether an interaction came from config or a datapack.
- Bookmarks/search work automatically once recipes are registered in each viewer.

## 13. Improvement / rectification / clarification backlog (do during the port)

The rewrite is the cheapest moment to fix these. Grouped by type.

### Rectifications (latent correctness)
- **Weighted-pick off-by-one.** `getItemStackForChance` uses `chance <= cumulative` with `chance ∈
  [0, total-1]`, which slightly **over-weights the first entry and under-weights the last** (one slot
  each). Negligible for big weights, real for a `weight: 1` filler. Fix to the standard form:
  `int r = random.nextInt(total); … if (r < cumulative) return;`.
- **RandomSource consistency.** `PtaDropRecord` (count/pick), `PtaInteractionRecord.getValue()`,
  `PtaHand.shouldConsume`, `PtaEffect.shouldApply` still use `Math.random()`/`ThreadLocalRandom`.
  Thread the level's `RandomSource` everywhere for server-reproducible, seed-consistent results
  (`PtaRewards.roll` already takes one — finish the job).
- **Tag freshness.** Tags are flattened into concrete `Set<Item/Block/Fluid>` at config-load. If
  datapack tags reload, those sets can go stale. On 1.21 prefer storing `TagKey`/`HolderSet` and
  resolving at click-time, or rebuild the index on datapack/tag reload.
- **Biome tags.** `conditions.biomes` matches exact ids only; add `#tag` support via
  `level.getBiome(pos).is(tagKey)` (natural now that selectors elsewhere accept `#`).
- **`left_click` + `air`** can't be served from the client-only `LeftClickEmpty` (see §4b) — needs a
  C2S payload.

### Clarifications / naming (v1 is gone — clean up)
- Rename `PtaBlock` → **`PtaTarget`** (it models block/fluid/air; the v2 JSON already says `target`).
- Rename `PtaInteractionRecord` → **`PtaCost`** (it models damage/hunger; v2 says `costs`).
- Normalise internal "chance" vs "weight" terminology in the pool.
- Trim `RegistryConstants` to the handful of v2 keys still used.
- Document precisely which `pta-common.toml` keys are read live vs. require a restart.

### Architecture (enabled by the rewrite)
- Replace the empty-set "air" sentinel in the target model with a **sealed target type**
  (block / fluid / air) — clearer, and prepares a future `entity` target.
- **Datapack-first loading — DECIDED** (§4d). Interactions load only from
  `data/<ns>/pta/interaction/*.json` via one `SimpleJsonResourceReloadListener`; the config folder and
  the custom S2C networking are removed (vanilla syncs datapacks for free). `pta-common.toml` keeps
  only global settings.
- Data-Components-based item matching (§5) is itself the largest architectural change.

### Tooling / quality
- **JSON schema + test suite — two planned deliverables, detailed in §14.** Deferred (not blocking
  the port) but **important**: the schema is a big packmaker-UX win and the tests secure the riskiest
  parts of the rewrite (item-data redesign, weighted pick). Do not drop them.
- **`SPDX-License-Identifier: MIT`** headers on source files (open-source convention).
- **CI**: GitHub Actions running `./gradlew build` (+ `runGameTestServer` once tests exist).

## 14. Preparatory deliverables (deferred, but planned — important)

Two artefacts, produced **outside** the port code itself. Deferred for now, but tracked here so they
are not forgotten — both are considered important.

### 14.1 JSON schema for the v2 format — `interaction.schema.json`
A JSON Schema (draft 2020-12) describing the `schema_version: 2` format, so editors (VSCode, etc.)
give **autocomplete, live validation, and hover docs** while authoring — catching typos and wrong
shapes before the game loads. It encodes:
- `type` required, with the **enum** of the four click types.
- Optional sections (`hand`, `target`, `transformation`, `rewards`, `costs`, `conditions`, `effects`,
  `sound`, `particles`) and their fields.
- The polymorphic shapes via `oneOf`: `match` = string **or** array; `count`/`amount` = integer **or**
  `{count}` **or** `{min,max}`; `consume.mode` enum `durability|shrink|none`.
- Shapes for `nbt_predicates` (`path` + `int_range` + optional `where`), `conditions` (time/weather
  enums, `y_range`, `light`, `player_state`), and reward entries (`match`/`weight`/`count`/`nbt`).
- `description` on each field (surface as editor hover docs).

Deliverable: the `.json` file + a short doc on associating it (a `"$schema"` key in files, or a
VSCode `json.schemas` glob association). Keep it in lock-step with `InteractionSpec` — ideally add a
tiny test asserting the schema and the codec agree on the example files.

### 14.2 Test plan & suite
Grouped by what needs a running game vs not (there are **no** tests today — this is the moment).

**Pure JUnit (no Minecraft bootstrap) — business logic:**
- `PtaNbtPredicate` path-walker: dot paths, `[]` list wildcard, `where` filter, `int_range`,
  presence-only, and **absent-tag → no match**.
- Weighted pick (`PtaPool`/`PtaRewards`): statistical distribution **and a regression test for the
  off-by-one fix** (§13); `rolls`, `guaranteed`, Fortune bonus math.
- `CountSpec.resolve` (int | `{count}` | `{min,max}`, default floors); cost clamping.
- `PtaConditions.matches` for each gate (time/weather/Y/light/sneaking/food/XP) with simple inputs.
- **Codec round-trips**: `InteractionSpec.CODEC` encode↔decode over every file in the examples set —
  guards the format against regressions and keeps the examples valid.

**GameTest (`runGameTestServer`) — in-world behaviour:**
- Place a target block, drive an interaction, assert drops / transformation / effect / cooldown.
- Datapack load path populates `InteractionRegistry`; a bad file is skipped (or fails fast in dev).
- Client viewer refresh hook fires after datapack sync (JEI/EMI show the server's set).

Fixtures: reuse the example interaction files as codec/round-trip inputs. Wire both into **CI**
(`./gradlew build` + `runGameTestServer`).

## 15. Out of scope (unchanged from current mod)

Entity-target interactions and command/function outputs remain deferred gameplay features (see the
main audit) — port first, extend later. Multi-target (block **and** fluid in one interaction) and the
catalyst display are cheap enough to fold into the port if desired (catalyst lives in §12).

---

**Sources for the version-sensitive facts above:**
[JEI 19.x for NeoForge 1.21.1 (CurseForge)](https://www.curseforge.com/minecraft/mc-mods/jei/files/5745660),
[NeoForge — Items & Data Components](https://docs.neoforged.net/docs/items/),
[NeoForge — Enchantments (1.21.1)](https://docs.neoforged.net/docs/1.21.1/resources/server/enchantments/).
