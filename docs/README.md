# PunchThemAll documentation

Start here, then dig in as needed.

| Doc | Read it when you want to… |
| --- | --- |
| [getting-started.md](getting-started.md) | **Learn by doing** — build an interaction step by step, with a cookbook and troubleshooting. |
| [interaction-format.md](interaction-format.md) | Look up **every field** of the `schema_version: 2` format, plus the legacy → v2 migration table. |
| [interactions.md](interactions.md) | Understand **loading, reloading, IDs, multiplayer sync**, and the JEI category. |
| [configuration.md](configuration.md) | Tune the **`pta-common.toml`** config (cooldowns, gates, drops, loader, debug). |
| [capabilities.md](capabilities.md) | Know **what is possible and what is not** before you start, plus the traps and the hard limits. |
| [cookbook.md](cookbook.md) | Find a **whole working interaction** for what you are building. |
| [troubleshooting.md](troubleshooting.md) | Work out **why nothing happened**, in the order that finds it fastest. |
| [versions.md](versions.md) | See **what differs from the NeoForge 1.21.1 line** — the format is the same, the platform around it is not. |
| [interaction.schema.json](interaction.schema.json) | Wire up **editor autocomplete/validation** for interaction files. |
| [curseforge.md](curseforge.md) | A short **project overview** (used for the CurseForge page). |

> **The public wiki documents the NeoForge 1.21.1 line.** Most of it applies here unchanged — the
> JSON format is identical — but where it does not, [versions.md](versions.md) says so. These files
> are the reference for this branch.

Related:

- Changelog: [../CHANGELOG.md](../CHANGELOG.md)
- Ready-to-copy examples: [`../configExamples/interactions`](../configExamples/interactions)
  (single-feature v2 files in [`v2/`](../configExamples/interactions/v2)).

## The 30-second version

An interaction is one JSON file in `config/punchthemall/interactions/`. It says: *on this click,
with this optional item in hand, on this target, do these things* — drop items, transform the block,
cost the player, grant effects, gated by conditions. Edit a file, run `/reload`, done. Everything
shows up in JEI.

```json
{
  "schema_version": 2,
  "type": "shift_left_click",
  "hand": { "hand": "main", "match": "#minecraft:shovels", "consume": { "mode": "durability" } },
  "target": { "kind": "block", "match": "minecraft:gravel" },
  "rewards": { "weighted": [
    { "match": "minecraft:flint", "weight": 25 },
    { "match": "minecraft:air",   "weight": 75 }
  ] }
}
```
