# Porting plan — PunchThemAll → NeoForge 26.1

Status: **analysis only**. Nothing has been changed. This is the survey that decides whether the
port is worth starting, and in what order.

Scope, stated up front: **no behaviour change**. Same JSON format (`schema_version: 2`), same
config keys, same gameplay, same log lines. Everything below is either a forced API migration or a
consequence of one.

Base: `master` @ `f063e3c` (`1.21.1-2.2.0`), NeoForge 21.1.241, Java 21, JEI 19, EMI 1.1.22.
Target: NeoForge **26.1.2.92** (latest release at time of writing), Minecraft 26.1.2, Java 25.

---

## 0. The shape of the problem

1.21.1 → 26.1 is **ten** intermediate versions, not one:

```
1.21.1 → 1.21.2/3 → 1.21.4 → 1.21.5 → 1.21.6 → 1.21.7 → 1.21.8 → 1.21.9 → 1.21.10 → 1.21.11 → 26.1
```

Each has its own NeoForged primer. The damage to PTA is concentrated in five of them:

| Version | What it breaks in PTA |
| --- | --- |
| **1.21.2** | `Registry#get` now returns `Optional<Holder>`; `getValue` is the direct accessor |
| **1.21.4** | `SimpleJsonResourceReloadListener` becomes codec-driven — the reload listener is rewritten |
| **1.21.5** | NBT API overhaul (`Optional` getters, `keySet`, `TagParser`); `Entity#hurt` split |
| **1.21.6** | `CompoundTag` removed from serialisation — `ValueInput`/`ValueOutput` |
| **1.21.11** | `ResourceLocation` → `Identifier`, `ResourceKey#location` → `identifier` |
| **26.1** | Java 25, deobfuscation, `Level#getDayTime` removed, `ItemStackTemplate` |

The good news, established by reading the 26.1 sources rather than guessing: **the event surface PTA
actually stands on is nearly intact**. `PlayerInteractEvent` and all four sub-events PTA listens to
still exist with the same accessors and the same `ICancellableEvent` behaviour. `TagsUpdatedEvent`,
`OnDatapackSyncEvent`, `RegisterPayloadHandlersEvent`, `ConditionalOps`/`ICondition` and the whole
payload registrar API survive. The architecture from the 1.21.1 port — reload listener, resolve on
`TagsUpdatedEvent`, explicit batched sync — carries over unchanged in *design*. It is the plumbing
underneath that has moved.

---

## 1. Toolchain and build

| Item | Now | 26.1 |
| --- | --- | --- |
| Java toolchain | 21 | **25** |
| Gradle wrapper | 8.8 | **9.1+** |
| ModDevGradle | 2.0.142 | ≥ 2.0.141 — already fine, bump anyway |
| `neo_version` | `21.1.241` | `26.1.2.92` (four components now: `MC.MC.MC.neo`) |
| `neo_version_range` | `[21.1,)` | `[26.1,)` |
| `minecraft_version_range` | `[1.21.1,1.22)` | `[26.1,27)` — **verify the convention** |
| `loader_version_range` | `[4,)` | **verify** — FML major bumped at some point |
| Parchment | 1.21.1 / 2024.11.17 | **drop it** — vanilla is deobfuscated, names are official |
| `pack_format` | 48 | **verify** the 26.1 value |

Local JDK is 26.0.1; the foojay resolver will pull a JDK 25 for the toolchain. Not a blocker.

Deobfuscation is the one piece of genuinely free good news: no more mapping layer, and parameter
names come from Mojang directly.

---

## 2. The mechanical rename: `ResourceLocation` → `Identifier`

**21 of 43 main-source files, 120 occurrences.** Plus:

- `ResourceKey#location()` → `identifier()` — 5 call sites (`InteractionRegistry`,
  `InteractionSpecResolver`, `ItemView`, `JeiCategory`, `PtaEmiRecipe`).
- `ResourceLocation.fromNamespaceAndPath` / `tryParse` / `STREAM_CODEC` all move to `Identifier`.
- `ResourceLocationException` → `IdentifierException`.

This is a find-and-replace, but it touches nearly every file, so it should be its own commit — done
first, alone, so the real migrations that follow have a readable diff.

---

## 3. Where the port actually hurts

### 3a. `InteractionReloadListener` — rewrite (highest risk)

`SimpleJsonResourceReloadListener` is no longer a Gson-and-`JsonElement` class. As of 1.21.11:

```java
public abstract class SimpleJsonResourceReloadListener<T>
        extends SimplePreparableReloadListener<Map<Identifier, T>>

protected SimpleJsonResourceReloadListener(HolderLookup.Provider registries,
                                           Codec<T> codec,
                                           ResourceKey<? extends Registry<T>> registryRef)
protected SimpleJsonResourceReloadListener(Codec<T> codec, FileToIdConverter finder)
```

Decoding now happens in `prepare()`, before `apply()` ever sees the data. Two consequences PTA
cannot ignore:

1. **The `DynamicOps` are chosen by the constructor.** PTA needs
   `new ConditionalOps<>(RegistryOps.create(JsonOps.INSTANCE, registries), conditionContext)` —
   neither public constructor lets you supply that. The ops-taking constructor is `private`.
2. **Per-file error reporting is lost.** Vanilla logs and silently drops a bad file. PTA's
   `"Incorrect Json format - <id> - <error>"` line, which the 2.2.0 audit added on purpose and
   verified in game, would disappear.

**Recommended answer:** stop extending `SimpleJsonResourceReloadListener`. Extend
`SimplePreparableReloadListener<Map<Identifier, InteractionSpec>>` directly and call the *public
static* helper in `prepare()`:

```java
public static <T> void scanDirectory(ResourceManager resourceManager, FileToIdConverter finder,
                                     DynamicOps<JsonElement> ops, Codec<T> codec,
                                     Map<Identifier, T> results)
```

That takes the ops explicitly. But it also swallows errors, so for byte-identical logging PTA should
most likely keep its own scan loop (`FileToIdConverter.json("pta/interaction")` +
`finder.listMatchingResources(resourceManager)`), decode per file, and report as today. That keeps
the current behaviour exactly and costs maybe 30 lines.

Also: the `Gson` field goes away, and the directory string becomes a `FileToIdConverter`.

### 3b. `PtaServerEvents` — the reload event was replaced

`AddReloadListenerEvent` **no longer exists**. In 26.1 it is `AddServerReloadListenersEvent`:

- `addListener(listener)` → `addRetainedListener(ListenerKey<T> key, T listener)` — PTA now needs a
  `ListenerKey`.
- `getConditionContext()` — still there. ✔
- `getRegistryAccess()` — **deprecated for removal since 26.1.2**, replaced by
  `ContextAwareReloadListener#getRegistryLookup()`. Use the replacement, not the deprecated getter.
- New: `getServerResources()`.

`TagsUpdatedEvent` (with `UpdateCause.SERVER_DATA_LOAD`) and `OnDatapackSyncEvent`
(`getRelevantPlayers()`) are unchanged. The resolve-on-tags-updated trick still works.

### 3c. NBT — the widest blast radius after the rename

From 1.21.5:

| Old | New |
| --- | --- |
| `CompoundTag#getAllKeys()` | `keySet()` — **8 call sites** (`TagHelper` ×4, `ItemView`, …) |
| `tag.getInt("k")` → `int` | `Optional<Integer>`; use `getIntOr("k", 0)` |
| `tag.contains("k", TAG_ANY_NUMERIC)` | typed `contains` gone — use the optional getter |
| `TagParser.parseTag(s)` | `TagParser.parseCompoundFully(s)` |

Affected:

- **`PtaCodecs.SNBT`** — one line, `parseTag` → `parseCompoundFully`. The codec itself is pure
  Mojang serialisation and otherwise untouched.
- **`TagHelper`** — `getAllKeys` ×4. The `NumericTag#getAsLong()` calls need checking: numeric tag
  accessors were touched in the same pass. *(unverified — check at port time)*
- **`ItemView`** — `getAllKeys`, and `view.contains("Damage", Tag.TAG_ANY_NUMERIC)` must become
  `view.getInt("Damage").ifPresent(...)`. `merge`, `put*`, `copyTag` survive.

`ItemView` is worth calling out as vindicated design: because the authoring format is PTA's own
pseudo-NBT view and not the real item structure, **no example datapack and no user JSON changes**.
The version fragility is exactly where the class docs said it would be.

### 3d. Block-entity NBT — `ValueInput` / `ValueOutput` (1.21.6)

Direct `CompoundTag` serialisation is gone.

- `InteractionRegistry.passesBlockEntityNBTFilter` —
  `blockEntity.saveWithoutMetadata(registryAccess)` must go through
  `TagValueOutput.createWithContext(...)` and read the built tag back.
- `PlayerInteractionHandler.applyNBTs` — `blockEntity.loadWithComponents(tag, registryAccess)` must
  go through `TagValueInput.create(problemReporter, registries, tag)`.

Both now want a `ProblemReporter`. PTA has no natural one; a `ProblemReporter.Collector` that logs
through `PTALoggers` is the honest choice. *(exact helper signatures unverified)*

### 3e. Registry lookups — `get` → `getValue` (1.21.2)

`Registry#get(Identifier)` returns `Optional<Holder.Reference<T>>`; the direct value accessor is
`getValue`. Affects all three checkers plus the resolver:

- `BlockChecker` / `ItemChecker` / `FluidChecker`: `BuiltInRegistries.X.get(id)` → `getValue(id)`.
  `containsKey` and `getTag(TagKey) → Optional<HolderSet.Named<T>>` are unchanged, so the
  null-instead-of-default contract these classes deliberately keep is preserved as-is. ✔
- `InteractionSpecResolver.resolveSound`: `SOUND_EVENT.get(id)` → `getValue(id)`.
- `InteractionSpecResolver.resolveExtras`: `MOB_EFFECT.getHolder(ResourceKey)` → `get(ResourceKey)`
  returning `Optional<Holder.Reference<MobEffect>>`.
- `registries.lookupOrThrow(Registries.ENCHANTMENT).get(key)` — likely unchanged. *(verify)*

### 3f. `PtaConditions` — day/night is a real semantic change

**`Level#getDayTime()` was removed in 26.1.** The fixed day-time system is replaced by datapack
`WorldClock` objects and a `ClockManager`; the nearest equivalent is
`Level#getOverworldClockTime()`, and the primer says explicitly it is **not one-to-one**.

`PtaConditions.matches` does:

```java
long dayTime = level.getDayTime() % 24000L;
boolean isDay = dayTime < 12000L;
```

This is the one place in the mod where "no behaviour change" needs actual thought rather than a
mechanical substitution. It must be verified in game (a `time: day` example fired at noon and at
midnight), not just compiled. Everything else in `PtaConditions` — weather, Y, light, sneaking,
food, XP — is expected to carry over, though `getMaxLocalRawBrightness` should be checked.

### 3g. `PlayerInteractionHandler` — mostly survives, three real edits

The event plumbing is fine. What changes:

- `player.hurt(source, amount)` → **`hurtServer(ServerLevel, DamageSource, float)`** (1.21.5 split
  into `hurtServer`/`hurtClient`). The call site is already server-only, so this is a signature fix,
  not a logic change.
- `applyNBTs` → `ValueInput` (see §3d).
- `Level#random` is now `protected` — PTA already uses `player.getRandom()` everywhere. ✔

To verify but expected intact: `ServerLevel#sendParticles`, `Level#playSound`,
`ItemStack#hurtAndBreak(int, ServerLevel, ServerPlayer, Consumer)`, `FoodData` setters
(`FoodData#tick` changed in 1.21.2 — the setters probably did not), `player.blockInteractionRange()`
(1.21.11 renamed the *predicates* `canInteractWithBlock` → `isWithinBlockInteractionRange`; the
range accessor is a different method), `ClipContext`, `FakePlayer`.

### 3h. Networking — small

- `PacketDistributor.sendToServer` → **`ClientPacketDistributor.sendToServer`** (`PtaClientEvents`).
- `PacketDistributor.sendToPlayer` — unchanged. ✔
- `RegisterPayloadHandlersEvent`, `registrar(v).optional().playToClient(...).playToServer(...)`,
  `IPayloadContext#enqueueWork/player` — all unchanged. ✔
- `ByteBufCodecs.fromCodecWithRegistries` — *verify still present*.
- Bump `PROTOCOL_VERSION` from `"2"` to `"3"`: the payload shape is the same, but the NBT encoding
  underneath it changed enough between 1.21.1 and 26.1 that letting an old client believe it agrees
  is worse than refusing it.
- Note the documented ceilings: 1 MiB clientbound, <32 KiB serverbound. `BATCH_SIZE = 64` is
  comfortably inside both; the 2 MiB-tag reasoning in the class javadoc should be updated to cite
  the payload limit instead.

### 3i. `PtaClientEvents` — one rename

`RecipesUpdatedEvent` → **`RecipesReceivedEvent`**. `ClientPlayerNetworkEvent.LoggingOut` is
unchanged. ✔

---

## 4. The viewers — the largest single work package

### JEI: 19 → 29

JEI **29.5.0.28 for NeoForge 26.1.2** exists, so the target is real. But 19 → 29 is ten major
versions and `JeiCategory` is a 529-line class built on the JEI 19 drawing model:

- `IDrawable` + `guiHelper.createDrawable(...)` for every icon and slot row.
- `draw(recipe, slotsView, GuiGraphics, mouseX, mouseY)` with manual `isMouseOver` hit-testing.
- `graphics.renderTooltip(font, lines, Optional.empty(), x, y)` — **this pattern is gone**. Since
  1.21.6, immediate-mode tooltip calls were replaced by `setTooltipForNextFrame(...)` +
  a deferred render pass, because GUI rendering became a two-phase submit/render system.
- `IRecipeCategory#getWidth/getHeight`, `getRegistryName`, `addRichTooltipCallback` — all need
  re-checking against the JEI 29 API.

The category's *content* (what is drawn, which tooltips, the arrow summary) is PTA logic and ports
directly. The *drawing layer* under it does not. Budget this as its own phase and read the JEI 29
API for real; nothing in the primers covers it.

`JEIPlugin` itself — runtime push, the `SHOWN` diff, `hideRecipes`/`addRecipes` — is small and
should survive, subject to `IJeiRuntime` still exposing the same recipe-manager methods.

### EMI: no 26.1 build exists

Modrinth's newest EMI for NeoForge is **1.1.22+1.21.1**. There is no 26.1 API to compile against.
So the 26.1 branch has three options:

1. **Ship without EMI.** Delete `emi/` on the branch, drop the `compileOnly` dependency. Lowest
   risk. PTA's EMI plugin has *never been loaded once* even on 1.21.1 (see the backlog), so nothing
   verified is being lost.
2. Keep the source, exclude it from compilation until an EMI 26.1 API appears.
3. Block the port on EMI. Not justifiable for an unverified integration.

**Recommendation: option 1**, and record it in the changelog so it is a stated decision rather than
a silent regression. Re-verify EMI's version list at port time — that page may simply be stale.

---

## 5. What ports essentially unchanged

Worth stating, because it is most of the mod's actual value:

- **The whole codec layer.** `InteractionSpec` (+ 15 nested records), `CountSpec`, `PtaCodecs`.
  `RecordCodecBuilder`, `Codec.either/listOf/optionalFieldOf`, `JsonOps`, `DataResult` are
  unchanged. Only `TagParser.parseTag` moves.
- **The runtime model.** `PtaBlock`, `PtaHand`, `PtaPool`, `PtaRewards` (weighting, rolls, the
  Fortune clamp), `PtaDropRecord` (count maths), `PtaNbtPredicate`, `PtaStateRecord`, both enums.
  Pure logic.
- **`InteractionRegistry`'s index and filter pipeline** — the `Snapshot` design, the id-ordered
  candidate lists, the waterlogged merge. Only the registry/NBT leaf calls change.
- **`PTAConfig`** — `ModConfigSpec` is expected unchanged. *(verify)*
- **`PunchThemAll`** — `@Mod(IEventBus, ModContainer)`, `registerConfig`. *(verify)*
- **Every JSON file.** The 41-file example pack, the probe pack, `interaction.schema.json`,
  `docs/interaction-format.md`. Nothing in the authoring format is version-coupled — which was the
  point of `ItemView`.

---

## 6. The test suite is a genuine unknown

`McBootstrap` publishes an empty `LoadingModList` **by reflection** before `Bootstrap.bootStrap()`,
because `FeatureFlags.<clinit>` NPEs otherwise. That is a hook into NeoForge internals across ten
versions *and* a JDK jump from 21 to 25, where reflective access into other modules has tightened
further.

Assume the harness breaks and needs re-derivation. The 241 tests themselves are mostly pure logic
and should survive once the game boots. Do not treat "the tests compile" as a signal here — get
`McBootstrap` green early, because without it every other change loses its safety net.

---

## 7. Suggested order

1. **Toolchain.** `gradle.properties`, wrapper 9.1+, Java 25, drop Parchment. Nothing compiles yet;
   that is expected.
2. **`Identifier` rename.** Alone, one commit, no other edits mixed in.
3. **Registries + NBT leaves.** Checkers, `PtaCodecs`, `TagHelper`, `ItemView`, resolver lookups.
4. **`ValueInput`/`ValueOutput`.** The two block-entity sites.
5. **Events and loading.** `AddServerReloadListenersEvent`, the reload-listener rewrite,
   `RecipesReceivedEvent`, `ClientPacketDistributor`, `hurtServer`.
6. **`getDayTime`.** Deliberately last of the logic work, because it is the only one that needs a
   behaviour decision.
7. **Get `McBootstrap` and `./gradlew test` green.**
8. **JEI 29.** Its own phase.
9. **EMI decision** (recommend: drop on this branch).
10. **In-game verification.** `runClient`, the probe pack, the same checklist as 2.2.0: load,
    `/reload`, the double-logged sync round-trip, per-file error reporting, `hidden` absent from the
    viewer, `count: {min:0}`. Plus a new one: **day/night conditions**.

Compiling clean means nothing here. Six of the 1.21.1 port's bugs compiled clean and were only found
by `runClient` — the same trap applies, more so.

---

## 8. Open questions to resolve before starting

- `loader_version_range`, `minecraft_version_range` convention, `pack_format` for 26.1.
- Does NeoForge patch `SimpleJsonResourceReloadListener` to accept custom ops, or is the
  hand-rolled scan (§3a) required?
- `ProblemReporter` — what is the intended lightweight implementation for a mod that just wants to
  log?
- `NumericTag` accessors, `ByteBufCodecs.fromCodecWithRegistries`, `FoodData` setters,
  `sendParticles`, `hurtAndBreak`, `blockInteractionRange`, `getMaxLocalRawBrightness`,
  `BlockParticleOption` after the 1.21.9 particle rework.
- The JEI 29 category API — needs reading, not guessing.
- Does `ItemStack` "requiring loaded registries" in 26.1 affect the display stacks PTA builds in
  `PtaBlock.getBlockStacks` / `PtaHand.getStacks`? Resolution runs on `TagsUpdatedEvent` with
  registries bound, so probably not — but confirm rather than assume.

---

## 9. Verdict

The port is **feasible and the architecture survives intact** — that is the important finding. No
design decision from the 1.21.1 port is invalidated by 26.1.

But it is not a weekend. Realistically three distinct efforts:

- **Core migration** (§1–§3, §5): mechanical but wide, ~20 files, one genuine semantic decision
  (day/night).
- **Test harness**: unknown, possibly nasty, and it gates everything else's safety.
- **JEI 29**: a rewrite of the drawing layer of a 529-line class.

Plus one product decision: **EMI ships or it does not**.

---

## 10. First contact — what the compiler actually said

Toolchain done (branch `neoforge_26.1`). `./gradlew compileJava` against NeoForge 26.1.2.92:
Gradle 9.2.1 runs, the JDK 25 toolchain provisions, NeoForge and JEI 29.20.0.60 both resolve, the
mod metadata expands. **100 errors, 18 files** — which is the survey above, confirmed empirically:

| Missing symbol | Count | Matches |
| --- | --- | --- |
| `ResourceLocation` | 64 | §2 |
| EMI classes (`EmiStack`, `EmiRecipe`, …) | 28 | §4 — no 26.1 build |
| `GuiGraphics` | 5 | §4 — renamed in 26.1 |
| `getTag` | 4 | §3e — the three checkers |
| `RecipesUpdatedEvent` | 2 | §3i |
| `AddReloadListenerEvent` | 2 | §3b |

Two corrections to the survey:

- **JEI 29 is gentler than feared.** `IRecipeCategory`, `RecipeType`, `getRegistryName`,
  `addFluidStack` all still exist — they compile, with `[removal]` deprecation warnings. So §4 is a
  staged migration off deprecated API, not the from-scratch rewrite assumed. The `GuiGraphics`
  errors are the real work there.
- **`Registry#get` was not flagged, `getTag` was.** The checkers' `.get(id)` still resolves; it is
  `getTag(TagKey)` that moved. §3e needs re-checking against the actual 26.1 signatures.

Nothing yet from NBT, `hurtServer`, `ValueInput`/`ValueOutput` or `getDayTime` — javac stops at the
first error per expression, so those surface only once the rename lands. Expect the count to rise
before it falls.

### 10.1 After the rename, and after the NBT/registry leaves

100 → 57 (rename, EMI out) → **16** (NBT + registry). Every remaining error is a later phase:
`GuiGraphics` ×5 (JEI), `RecipesUpdatedEvent` ×3 + `sendToServer` (§3i/§3h), `AddReloadListenerEvent`
×2 + `getRegistryAccess` (§3b), `InteractionReloadListener` ×3 (§3a), `getDayTime` (§3f). Test
sources are still unverified — Gradle skips `compileTestJava` while `compileJava` fails.

**Four corrections to §3, all found by checking the 26.1 jar rather than the primers:**

- **§3d was half wrong.** `BlockEntity#saveWithoutMetadata(HolderLookup.Provider)` still exists and
  still returns a `CompoundTag`, so `InteractionRegistry` needed **no change at all**. Only the
  *read* side moved: `loadWithComponents` now takes a `ValueInput`, so just the one site in
  `PlayerInteractionHandler` needed `TagValueInput.create(ProblemReporter.DISCARDING, …)`.
  `DISCARDING` is deliberate — the old `CompoundTag` overload reported nothing either.
- **§3g was wrong about `hurt`.** `Entity#hurt(DamageSource, float)` still exists in 26.1 as a
  `final void` that dispatches to `hurtServer`/`hurtClient`. PTA ignores the return value, so the
  call site compiles and behaves as before. No edit needed.
- **§3e had the wrong method.** `Registry#get(Identifier)` returning `Optional<Holder.Reference<T>>`
  is real, and the direct accessor is `getValue` — but what actually broke was `getTag(TagKey)`,
  which is now `get(TagKey)` (from `HolderGetter`), same `Optional<HolderSet.Named<T>>` return. The
  checkers' surrounding code was unaffected.
- **The NBT accessors were understated.** Not just `getAllKeys` → `keySet`: `NumericTag#getAsLong`
  → `longValue()`, `getAsInt` → `intValue()`, and `Tag#TAG_ANY_NUMERIC` is gone (the typed
  `contains` with it). The subtle one is **`Tag#getAsString` → `asString()` returning
  `Optional<String>`, empty for non-string tags** — where the old method fell back to the SNBT
  form. Two sites compared or rendered arbitrary tags that way, so both needed
  `asString().orElseGet(tag::toString)`; calling `asString()` alone would have made every
  non-string `where`-filter comparison trivially equal, and silently so.

### 10.2 After the events and the reload listener

16 → **7**: `GuiGraphics` ×5 (JEI) and `getDayTime` ×1. Everything else in main compiles.

**§3a was far too pessimistic — the single biggest correction in this survey.** The claim was that
the ops-taking constructor being `private` forced a hand-rolled scan. The constructor is indeed
private, but it does not matter: NeoForge patches `SimplePreparableReloadListener` to **extend
`ContextAwareReloadListener`**, which supplies

```java
protected final HolderLookup.Provider getRegistryLookup();
protected final ICondition.IContext getContext();
protected final ConditionalOps<JsonElement> makeConditionalOps();
```

`makeConditionalOps()` builds precisely the `ConditionalOps(RegistryOps(JsonOps))` stack PTA
assembled by hand on 1.21.1. So the listener keeps its own decode loop only to preserve PTA's
per-file error message — not because the ops were unreachable — and the constructor plumbing
(`registries`, `conditionContext`) **disappears entirely**, since NeoForge injects both. The listener
came out shorter than it was on 1.21.1.

Also settled, all cheap:

- `AddReloadListenerEvent` → `AddServerReloadListenersEvent`. `addListener` now takes an
  `Identifier` name (`SortedReloadListenerEvent#addListener(Identifier, listener)`), so listeners can
  be ordered against each other. `addRetainedListener(ListenerKey, …)` is for listeners that need to
  be fetched back later — PTA's does not.
- `TagsUpdatedEvent#getRegistryAccess` → `getRegistries()` / **`getLookupProvider()`**; the latter is
  the direct fit for `rebuildFrom`.
- `RecipesUpdatedEvent` → `RecipesReceivedEvent`, `PacketDistributor.sendToServer` →
  `ClientPacketDistributor.sendToServer`. Both one-liners.

### 10.3 The test suite

Main and test both compile. `./gradlew test`: **212 pass, 30 fail**, from 0 running at the start.

The old harness is gone and was replaced, not repaired. Two things killed it:

- `LoadingModList.of` gained a sixth parameter, so the reflective publish missed. `McBootstrap` now
  matches the method by name and fills each parameter by shape, which is arity-proof.
- That was not enough anyway: since 26.1 `SharedConstants.<clinit>` asks `FMLEnvironment` whether it
  is in production, which needs a current `FMLLoader`. **There is no longer a route to a usable
  Minecraft from a bare JVM.** So the build enables ModDevGradle's supported
  `neoForge { unitTest { enable(); testedMod = … } }` and the suite runs under FML. The
  `LauncherSessionListener` and its `META-INF/services` entry are deleted — MDG boots the game before
  test classes load, which is exactly what that listener existed to guarantee.

Mockito needed 5.20 (from 5.14.2): older Byte Buddy cannot instrument `Level` under Java 25 once
FML's transformer has been through it. That alone fixed 12 failures.

**The 30 remaining failures are all one cause**, in the six classes that build an `ItemStack`:
`NullPointerException: Components not bound yet`. 26.1 binds default data components lazily in
`ReloadableServerResources#loadResources`, not in `Bootstrap`, so `ItemStack`'s constructor throws.
This is the concrete form of the §8 question about `ItemStack` requiring loaded registries — it does,
and it bites tests rather than gameplay.

Doing it by hand gets close but not home:

```java
BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
        .build(VanillaRegistries.createLookup())
        .forEach(PendingComponents::apply);
```

runs the right two steps, then fails NeoForge's `CommonHooks.validateComponent` — datagen's lookup
yields lazy `HolderSet`s with no `equals`, where a real server's provider yields named ones. Reading
an item's own defaults instead is circular: `Item#components()` *is* a read of the holder being
filled.

The supported answer is `net.neoforged:testframework`'s `EphemeralTestServerProvider`, which boots a
throwaway server for the classes that need real stacks. That changes what those tests are — they stop
being pure unit tests — so it is a decision, not a detail, and it is left for its own change.

### 10.4 First run in game

`runClient`, new world, both example packs copied in, `/reload`:

| Check | Result |
| --- | --- |
| FML loads the mod (`neoforge.mods.toml` with no `modLoader`, range `[26.1.2]`) | ✅ `pta` 2.2.0, entrypoint constructed |
| `Read 45 interaction file(s)` — the rewritten reload listener | ✅ Server thread |
| `Loaded 44 interaction(s)` — `enabled: false` correctly skipped | ✅ (`enabled_false`, verified by diffing loaded ids against disk) |
| `Loaded 44` **again on the Render thread** — the client sync | ✅ |
| Per-file errors in PTA's own format | ✅ all four `bad_ids` probes, each logged twice |
| `sneak_conflict` warning | ✅ |
| Exceptions | ✅ none, whole session |
| A real interaction (flint on stone) | ✅ |

That validates the three biggest bets in one go: the hand-rolled decode loop in the reload listener,
`ContextAwareReloadListener` injection reaching a listener added via `addListener`, and the sync
payload.

**Two defects the build could not find**, both fixed:

- Both example datapacks still declared `pack_format: 48`. The single `pack_format` field was
  replaced by `min_format`/`max_format` in 1.21.9, and 26.1.2's data format is 101.1 — read from the
  jar's `version.json`, not guessed. They would have loaded as incompatible.
- **JEI tooltips rendered in the top-left corner.** `setTooltipForNextFrame` takes the same arguments
  as the old `renderTooltip`, so it looked like a like-for-like swap — but it defers to the end of
  the frame, when JEI's per-recipe transform is gone, and the `mouseX`/`mouseY` given to `draw` are
  recipe-relative. `IRecipeCategory#getTooltip(ITooltipBuilder, …)` is the right hook; JEI positions
  them. This is the §4 lesson in miniature: the API compiled, the arguments matched, and it was still
  wrong.

### 10.5 Second run — gameplay

With the tooltip fix in: tooltips render at the cursor, and a further pass covered

| Check | Path it exercises | Result |
| --- | --- | --- |
| `transformation_break` (cobweb → air, guaranteed string) | air transform + guaranteed drop | ✅ |
| `zero_min_drop` probe, repeated | the `count: {min:0}` roll | ✅ |
| `/time set day` / `night`, Overworld **and Nether** | `getOverworldClockTime()` — the deliberate change | ✅ |
| `costs_damage_and_hunger` | `hurt` + `FoodData` | ✅ |
| `transformation_block_entity_nbt` (chest → trapped chest named "Rigged") | **`TagValueInput`** | ✅ |
| Exceptions, whole session | | ✅ none |

The last row was the one that mattered, and the first attempt at it was a bad test on my part:
`transformation_break` has no `into` and no `nbt`, so it takes the `createAir` branch and never
reaches `applyNBTs`. Only a transformation carrying NBT touches `TagValueInput` — and because it is
wired with `ProblemReporter.DISCARDING` (deliberately, to match the old overload's silence), a
failure there would have produced a correctly-transformed block, no name, and **nothing in the log**.
"No exceptions" would have looked like success. The custom name is the only observable proof.

### 10.6 Dedicated server

`runServer`, with both packs staged into `run/world/datapacks/`:

- `Read 45` / `Loaded 44` — **once**, which is the point: no client, so no second pass. Two would
  have meant something was resolving twice.
- Per-file errors reported as on the client.
- **No client-only class loaded**: no `PtaClientEvents`, `JEIPlugin` or `ClientPacketDistributor`, no
  `NoClassDefFoundError`. The `Dist.CLIENT` subscriber and the `ModList.isLoaded("jei")` guard around
  `refreshViewers` both hold on a server with no viewer present.
- `Done (26.967s)!`

Note ModDevGradle accepts the EULA for dev runs, so `run/eula.txt` is never written and the server
does not stop on it.

One cosmetic warning worth remembering: `Class version 69 required is higher than the class version
supported by the current version of Mixin (JAVA_21 supports class version 65)`. Class 69 is Java 25.
Harmless here — PTA uses no mixins — but it is the kind of line that becomes a real failure with a
third-party mod that does.

**Still open**, and none of it deducible from the above:

- The 30 unit tests around the reward pipeline remain dark (§10.3). In-game checks touched drops but
  not Fortune, `rolls`, or NBT drops systematically.
- EMI is not shipped on this branch (§4).
- `hidden` was verified as loading but not visually confirmed absent from JEI.
- JEI's deprecated `RecipeType` / `getRegistryName` / `addFluidStack` still in use — works, but on
  borrowed time.

---

### Sources

Primers: [1.21.2](https://docs.neoforged.net/primer/docs/1.21.2/),
[1.21.4](https://docs.neoforged.net/primer/docs/1.21.4/),
[1.21.5](https://docs.neoforged.net/primer/docs/1.21.5/),
[1.21.6](https://docs.neoforged.net/primer/docs/1.21.6/),
[1.21.9](https://docs.neoforged.net/primer/docs/1.21.9/),
[1.21.11](https://github.com/neoforged/.github/blob/main/primers/1.21.11/index.md),
[26.1](https://docs.neoforged.net/primer/docs/26.1/).
Release notes: [NeoForge for Minecraft 26.1](https://neoforged.net/news/26.1release/).
Sources read directly: [NeoForge 26.1.x branch](https://github.com/neoforged/NeoForge/tree/26.1.x),
[payload docs](https://docs.neoforged.net/docs/networking/payload/),
[SimpleJsonResourceReloadListener @ 1.21.11](https://mappings.dev/1.21.11/net/minecraft/server/packs/resources/SimpleJsonResourceReloadListener.html).
