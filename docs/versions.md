# Which version has what

PunchThemAll ships on two lines. **The JSON format is the same on both** — field for field, including
everything added in 2.3.0 and 2.4.0 — so a datapack written for one loads on the other without an
edit. What differs is the platform around it.

| | Forge 1.20.1 | NeoForge 1.21.1 |
| --- | --- | --- |
| Mod version | 2.4.0 | 2.4.0 |
| Loader | Forge 47.x | NeoForge 21.1.x |

---

## The format: identical

Everything below works the same on both, and is documented once in
[interaction-format.md](interaction-format.md).

| | |
| --- | --- |
| Click types, hand matching, `consume` with `chance` and `count` | both |
| Weighted and `guaranteed` drops, `rolls`, Fortune bonus, `rewards.at` | both |
| Transformations: `op` (`replace` / `break` / `place`), `at`, `require`, `drops` | both |
| Offsets in `world` / `player` / `face` frames, and regions via `at.to` | both |
| `into: { "kind": "copy" }` and `from` | both |
| A list of transformations, and `{ "chance": …, "all": [ … ] }` | both |
| `conditions.neighbours`, with `invert` | both |
| Every other condition: biomes and biome `#tags`, dimension, time, weather, Y, light, sneaking, food, XP | both |
| `nbt.whitelist` / `nbt.blacklist` / `nbt_predicates`, `hidden`, `enabled` | both |
| `left_click` on air | both |

The 69-interaction [example datapack](../examples/punchthemall-examples) is the same on both lines.

---

## What only NeoForge 1.21.1 has

| Feature | Why |
| --- | --- |
| **EMI support** | The EMI plugin exists on the 1.21.1 line only. Both lines support JEI. |
| **Load conditions** (`neoforge:conditions`) | 1.21.1 reads interactions through the datapack machinery that evaluates conditions. The 1.20.1 loader reads the files itself, so a conditions block is **ignored and the file always loads**. To ship mod-dependent content on 1.20.1, put it in a separate datapack the player installs alongside that mod. |

---

## What only Forge 1.20.1 has

Both are things the 1.21.1 line deliberately dropped.

| Feature | Why |
| --- | --- |
| **Interactions in the config folder** | `config/punchthemall/interactions/*.json` is the default source on 1.20.1. Datapacks work too, but are opt-in: set `load_from_datapacks = true`. On 1.21.1 datapacks are the only source. |
| **The legacy (schema 1) format** | Still read on 1.20.1, with a deprecation warning, and it gains no new field — `op`, `at`, `require` and `neighbours` are `schema_version: 2` only. 1.21.1 removed it. |
| **The `PunchThemAll.Loader` config section** | `recursive_discovery`, `fail_fast`, `lowercase_generated_ids`, `load_from_datapacks` — all about the config folder, so all gone on 1.21.1. |

---

## Moving a pack between the lines

Going **1.20.1 → 1.21.1**:

1. Move your files from `config/punchthemall/interactions/` into a datapack at
   `data/<namespace>/pta/interaction/`. The path becomes the id.
2. Convert anything still on the legacy format to `schema_version: 2`.
3. Drop the `Loader` section from your config; the rest of the file carries over.

Going **1.21.1 → 1.20.1**:

1. Your datapack works as-is — set `load_from_datapacks = true`.
2. Remove any `neoforge:conditions` blocks: they are ignored rather than honoured, so a file meant to
   load conditionally will load always.
3. Tags in the `c:` convention namespace are `forge:` here — `#c:seeds` becomes `#forge:seeds`.

---

## What is the same under the hood

Worth knowing if you are reporting a bug: both lines run the same transformation pipeline, plan
every transformation before writing any, honour the same offset and per-click caps, and post block
break and place events so claim mods can veto them. Both are covered by the same 28 in-world tests.
