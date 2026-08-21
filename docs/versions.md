# Which version has what

PunchThemAll ships on three lines. **The JSON format is the same on all of them** — field for field,
checked rather than assumed: the codec accepts an identical set of fields on each. A datapack written
for one loads on the others without an edit. What differs is the platform around it.

| | Forge 1.20.1 | NeoForge 1.21.1 | NeoForge 26.1 |
| --- | --- | --- | --- |
| Minecraft | 1.20.1 | 1.21.1 | 26.1.2 |
| Loader | Forge 47.x | NeoForge 21.1.x | NeoForge 26.1.x |
| Mod version | 2.4.0 | 2.4.0 | 2.4.0 |
| Java | 17 | 21 | 25 |

---

## The format: identical everywhere

Documented once, in [interaction-format.md](interaction-format.md).

Click types and hand matching. Weighted and `guaranteed` drops, `rolls`, the Fortune bonus and
`rewards.at`. Transformations with `op`, `at`, `require` and `drops`; offsets in the `world`,
`player` and `face` frames; regions via `at.to`; `into: { "kind": "copy" }` with `from`; lists of
transformations and `{ "chance": …, "all": [ … ] }`. `conditions.neighbours` with `invert`, and every
other condition including biome `#tags`. NBT whitelists, blacklists and typed predicates. `hidden`,
`enabled`, and `left_click` on air.

The 69-interaction [example datapack](../examples/punchthemall-examples) is the same set on all three.

---

## Where they differ

| | 1.20.1 | 1.21.1 | 26.1 |
| --- | --- | --- | --- |
| **JEI** | yes | yes | yes |
| **EMI** | no | **yes** | no — no 26.1 build of EMI yet |
| **Load conditions** (`neoforge:conditions`) | **no** — see below | yes | yes |
| **Where interactions load from** | config folder always, datapacks opt-in | datapacks only | datapacks only |
| **Legacy `schema_version: 1`** | still read, deprecated | removed | removed |
| **`PunchThemAll.Loader` config section** | yes | removed | removed |
| **In-world tests** | 28 | 28 | 28 |
| **Unit suite** | full | full | partial — see below |

**Load conditions on 1.20.1.** The loader there reads interaction files itself rather than going
through the datapack machinery that evaluates conditions, so a conditions block is **ignored and the
file loads anyway**. To ship mod-dependent content on that line, put it in a separate datapack the
player installs alongside that mod.

**The unit suite on 26.1.** Thirty-one tests fail with "Components not bound yet": 26.1 binds data
components in `ReloadableServerResources` rather than in `Bootstrap`, so a bare test JVM cannot
construct an `ItemStack`. The in-world tests are unaffected — they run on a real server — and the
intended fix is recorded in that branch's `McBootstrap`.

---

## Moving a pack between the lines

**To 1.20.1**, from either NeoForge line:

1. Your datapack works as-is — set `load_from_datapacks = true`.
2. Remove any `neoforge:conditions` blocks. They are ignored rather than honoured, so a file meant to
   load conditionally will load always.
3. Tags in the `c:` convention namespace are `forge:` there — `#c:seeds` becomes `#forge:seeds`.
4. `pack.mcmeta` uses `"pack_format": 15`.

**From 1.20.1** to either NeoForge line:

1. Move your files out of `config/punchthemall/interactions/` into a datapack at
   `data/<namespace>/pta/interaction/`. The path becomes the id.
2. Convert anything still on the legacy format to `schema_version: 2`.
3. Drop the `Loader` section from your config; the rest carries over.
4. `pack.mcmeta` uses `"pack_format": 48` on 1.21.1, and `"min_format"` / `"max_format": 101` on 26.1.

**Between 1.21.1 and 26.1**: only `pack.mcmeta` changes, and EMI users lose the EMI category until
EMI ships a 26.1 build.

---

## What is the same under the hood

Worth knowing if you are reporting a bug: all three run the same transformation pipeline, plan every
transformation before writing any, honour the same offset and per-click caps, and post block break
and place events so claim mods can veto them. All three are covered by the same 28 in-world tests.
