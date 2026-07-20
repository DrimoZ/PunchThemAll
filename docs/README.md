# PunchThemAll documentation

Start here, then dig in as needed.

| Doc | Read it when you want to… |
| --- | --- |
| [getting-started.md](getting-started.md) | **Learn by doing** — build an interaction step by step, with a cookbook and troubleshooting. |
| [interaction-format.md](interaction-format.md) | Look up **every field** of the `schema_version: 2` format, plus the legacy → v2 migration table. |
| [interactions.md](interactions.md) | Understand **datapacks, loading, IDs, multiplayer sync**, and the JEI/EMI category. |
| [configuration.md](configuration.md) | Tune the **`pta-common.toml`** config (cooldowns, gates, drops, debug). |
| [interaction.schema.json](interaction.schema.json) | Wire up **editor autocomplete/validation** for interaction files. |
| [curseforge.md](curseforge.md) | A short **project overview** (used for the CurseForge page). |

Related:

- Changelog: [../CHANGELOG.md](../CHANGELOG.md)
- Ready-to-copy example datapack: [`../examples/punchthemall-examples`](../examples/punchthemall-examples)

## The 30-second version

An interaction is one JSON file in a **datapack**, at `data/<namespace>/pta/interaction/`. It says:
*on this click, with this optional item in hand, on this target, do these things* — drop items,
transform the block, cost the player, grant effects, gated by conditions. Edit a file, run `/reload`,
done. Everything shows up in JEI and EMI, on servers too.

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
