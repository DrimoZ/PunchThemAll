# Interaction files and JEI display

PunchThemAll interactions are JSON files placed in:

```text
config/punchthemall/interactions
```

Example interaction files are available in:

```text
configExamples/interactions
```

## Loading behavior

Interaction loading is controlled by `PunchThemAll.Loader` in the common config.
By default, the loader:

1. Creates `config/punchthemall/interactions` if it is missing.
2. Recursively scans for `*.json` files.
3. Sorts files deterministically by relative path.
4. Generates a resource ID from each relative path.
5. Lowercases generated IDs so they remain valid Minecraft resource locations.
6. Logs errors per invalid file and continues loading the rest unless
   `fail_fast = true`.

Example:

```text
config/punchthemall/interactions/crushing/gravel.json
```

becomes:

```text
pta:crushing/gravel
```

If `recursive_discovery = false`, only JSON files directly inside
`config/punchthemall/interactions` are loaded.

## Reloading

Interactions are loaded when the mod initializes/reloads its data listener. For
pack development, use `/reload` after changing JSON files. Loader config changes
such as recursive discovery or fail-fast behavior also require interactions to be
loaded again.

## Runtime gates

Even when a JSON file is valid and loaded, it can be disabled at runtime by the
common config. The most important gates are:

* `PunchThemAll.Interactions.enabled`
* `PunchThemAll.Interactions.allow_left_click`
* `PunchThemAll.Interactions.allow_right_click`
* `PunchThemAll.Interactions.allow_block_interactions`
* `PunchThemAll.Interactions.allow_air_interactions`
* `PunchThemAll.Interactions.allow_fluid_interactions`
* `PunchThemAll.Interactions.allow_transformations`
* `PunchThemAll.Players.allow_fake_players`
* `PunchThemAll.Players.allow_player_damage`
* `PunchThemAll.Players.allow_food_consumption`

Enable `PunchThemAll.Debug.log_skipped_interactions` when debugging why a loaded
interaction does not run.

## JEI category

PunchThemAll registers a JEI interaction category. The category is intended to be
a pack-author and player-facing overview of loaded interactions.

The JEI display includes:

* click type icon;
* sneak/regular state icon;
* hand requirement and hand mode icon;
* target block, fluid, or air marker;
* transformation output when present;
* weighted result pool rows;
* output chance and count ranges;
* biome whitelist/blacklist tooltip;
* player damage and hunger cost tooltips;
* block-state whitelist/blacklist details;
* NBT whitelist/blacklist details;
* interaction ID in the click tooltip.

The interaction ID shown in JEI is useful when a user reports a recipe issue: it
maps directly to the generated ID of the JSON file loaded from the config folder.

## Pack organization tips

For small packs, keeping all files directly in `interactions` is simple:

```text
interactions/flint_from_gravel.json
interactions/clay_from_water.json
```

For larger packs, use folders and recursive discovery:

```text
interactions/early_game/flint_from_gravel.json
interactions/create/crushing_dust.json
interactions/automation/click_machine_only.json
```

Keep filenames lowercase with underscores to avoid resource-location issues and
to make generated IDs stable across operating systems.

## JSON authoring robustness

The interaction reader validates common reusable shapes before creating runtime
objects. This keeps the format extensible while giving pack authors clearer logs.
Top-level metadata can be added without changing gameplay; `enabled: false` skips
a file without deleting it, and `schema_version` is reserved for future migrations:


```json
{
  "schema_version": 1,
  "enabled": true,
  "type": "shift_left_click"
}
```

* `hunger`, `damage`, and pool entries all use the same count range pattern:
  either `count` or `min`/`max`.
* `pool` is optional so transformation-only interactions remain possible; when
  present, it must be an array of objects and invalid entries are skipped
  independently so one bad drop does not discard the whole file unless
  `fail_fast = true`.
* Pool `chance` values must be positive weights.
* `biome.whitelist` and `biome.blacklist` are parsed as string arrays. If both
  are present, the loader logs the conflict so the file can be cleaned up.
* Transformation state is read from `transformation.state`, matching the JSON
  format documented for block/fluid state filters.

For future features, prefer adding new optional objects or sections rather than
changing the meaning of existing keys. This lets older interaction files keep
loading while newer packs opt in to expanded behavior.

### Selector formats

Selectors now accept both the historical single-value keys and plural list keys.
This makes small files stay concise while larger packs can group related targets
without duplicating whole interaction files.

Hand item selectors support any combination of:

```json
"hand": {
  "hand": "main_hand",
  "item": {
    "item": "minecraft:stick",
    "items": ["minecraft:bone", "minecraft:blaze_rod"],
    "tag": "forge:tools/hammers",
    "tags": ["forge:tools/wrenches"]
  }
}
```

Block target selectors support block, fluid, and tag lists:

```json
"block": {
  "blocks": ["minecraft:cobblestone", "minecraft:stone"],
  "state": {
    "blacklist": {
      "waterlogged": "true"
    }
  }
}
```

Pool entries support item and tag lists too:

```json
"pool": [
  {
    "items": ["minecraft:flint", "minecraft:gravel"],
    "chance": 10,
    "min": 1,
    "max": 2
  }
]
```

Avoid mixing block and fluid targets in the same `block` selector. If both are
present, the loader logs the conflict and keeps the block targets because the
runtime model treats block and fluid targets as different interaction kinds.
