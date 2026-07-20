# PunchThemAll — example datapack

Since **2.1.0 (NeoForge 1.21.1)**, interactions are loaded from **datapacks**. This folder is a ready-to-use example datapack.

## What's inside

`data/pta_examples/pta/interaction/*.json` — one interaction per file, each demonstrating a feature
(see the file names). Every interaction id is `pta_examples:<file name>`
(e.g. `pta_examples:hand_nbt_predicates`). All files use `schema_version: 2`.

## How to install

Copy the whole `punchthemall-examples` folder (the one containing `pack.mcmeta`) into a world's
datapacks folder:

```
<your world>/datapacks/punchthemall-examples/
    pack.mcmeta
    data/pta_examples/pta/interaction/*.json
```

- **Existing world:** drop it in `saves/<world>/datapacks/`, then run `/reload` (or rejoin).
- **New world:** on the world-creation screen, open **Data Packs**, drag the folder in, and enable it.
- **Server:** put it in `<server>/world/datapacks/` and `/reload`.

It works on dedicated servers too: the server syncs its interactions to clients, so JEI/EMI show the
server's set with no extra setup.

## Authoring your own

Make your own datapack the same way — pick your own namespace and drop files in
`data/<namespace>/pta/interaction/*.json`. See the format reference:
[docs/interaction-format.md](../../docs/interaction-format.md) and the step-by-step
[getting-started guide](../../docs/getting-started.md).

> Tip: `data/mypack/pta/interaction/early/flint.json` becomes the id `mypack:early/flint`.
> Use `neoforge:conditions` in a file to gate it on another mod being present.
