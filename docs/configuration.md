# PunchThemAll configuration

PunchThemAll uses a NeoForge common config file located at:

```text
config/punchthemall/pta-common.toml
```

The configuration is intentionally modular. Pack makers can tune gameplay,
automation, drops, loader behavior, and diagnostics independently.

Most runtime options are read directly when an interaction is processed, so they
can be changed while testing without rebuilding the mod. Loader options affect
JSON discovery and therefore apply when interactions are loaded again, usually on
world load or `/reload`.

## Section overview

| Section | Purpose |
| --- | --- |
| `PunchThemAll.Interactions` | Global interaction pipeline, click gates, target gates, transformations, cooldowns. |
| `PunchThemAll.Players` | Real-player effects, fake-player automation, player damage, food costs. |
| `PunchThemAll.Drops` | Inventory insertion and world drop spawn physics. |
| `PunchThemAll.Loader` | JSON file discovery, generated IDs, development fail-fast mode. |
| `PunchThemAll.Debug` | Optional logs for loaded and skipped interactions. |

## `PunchThemAll.Interactions`

| Key | Default | Description |
| --- | ---: | --- |
| `enabled` | `true` | Master switch for all PunchThemAll interactions. Set to `false` to disable the runtime handler without removing JSON files. |
| `cooldown_ticks` | `1` | Minimum delay between two successful interactions for the same player. `20` ticks = 1 second. Set to `0` to disable cooldowns. |
| `max_matches_per_click` | `64` | Maximum number of matching interactions processed per click. Use `1` for strict recipe-like behavior; increase for intentional multi-output/chained packs. |
| `cancel_vanilla_interaction` | `true` | Cancels the original Minecraft click event after PunchThemAll succeeds. Keep enabled unless another mod must also process the same click. |
| `allow_left_click` | `true` | Enables JSON interactions declared as left-click interactions. |
| `allow_right_click` | `true` | Enables JSON interactions declared as right-click interactions. |
| `allow_block_interactions` | `true` | Enables interactions targeting blocks. |
| `allow_air_interactions` | `true` | Enables interactions configured with an air target. |
| `allow_fluid_interactions` | `true` | Enables ray-traced source-fluid interactions. |
| `allow_transformations` | `true` | Enables block/fluid transformations after a successful interaction. Drops can still happen when this is disabled. |

### Recommended presets

Strict recipe mode:

```toml
[PunchThemAll.Interactions]
max_matches_per_click = 1
cancel_vanilla_interaction = true
```

Compatibility mode with other click-handling mods:

```toml
[PunchThemAll.Interactions]
cancel_vanilla_interaction = false
cooldown_ticks = 2
```

Disable world changes while keeping drops:

```toml
[PunchThemAll.Interactions]
allow_transformations = false
```

## `PunchThemAll.Players`

| Key | Default | Description |
| --- | ---: | --- |
| `allow_fake_players` | `true` | Allows machines and fake players to trigger interactions. Disable to prevent automation. |
| `apply_cooldown_to_fake_players` | `false` | Applies `cooldown_ticks` to fake players too. Useful for throttling fast automation. |
| `apply_player_effects_to_fake_players` | `false` | Applies player-only side effects such as swing animation, damage, and food costs to fake players. Most packs should keep this disabled. |
| `allow_player_damage` | `true` | Allows interaction JSON entries to hurt real players. |
| `allow_food_consumption` | `true` | Allows interaction JSON entries to consume real-player saturation/food. |

Automation-safe preset:

```toml
[PunchThemAll.Players]
allow_fake_players = true
apply_cooldown_to_fake_players = true
apply_player_effects_to_fake_players = false
```

Player-safe preset:

```toml
[PunchThemAll.Players]
allow_player_damage = false
allow_food_consumption = false
```

## `PunchThemAll.Drops`

| Key | Default | Description |
| --- | ---: | --- |
| `place_in_inventory` | `true` | Attempts to insert drops into real-player inventory before spawning them in the world. Remaining items still drop if the inventory is full. |
| `place_fake_player_drops_in_inventory` | `true` | Attempts to insert drops into fake-player/machine inventory. Disable when automation should eject items into the world. |
| `world_drop_offset` | `0.75` | Distance from the clicked face where world drops spawn. |
| `world_drop_velocity` | `0.10` | Velocity multiplier applied to world drops in the clicked face direction. |

World-drop-only automation preset:

```toml
[PunchThemAll.Drops]
place_fake_player_drops_in_inventory = false
world_drop_offset = 0.75
world_drop_velocity = 0.10
```

## `PunchThemAll.Loader`

| Key | Default | Description |
| --- | ---: | --- |
| `recursive_discovery` | `true` | Loads `*.json` interaction files recursively inside `config/punchthemall/interactions`. Disable to load only files directly inside that folder. |
| `fail_fast` | `false` | Stops loading remaining interactions after the first invalid JSON file. Enable while developing packs; disable for released packs. |
| `lowercase_generated_ids` | `true` | Lowercases IDs generated from file paths. Minecraft resource locations require lowercase paths, so this should normally stay enabled. |

When recursive discovery is enabled, a file at:

```text
config/punchthemall/interactions/early_game/flint.json
```

is loaded as:

```text
pta:early_game/flint
```

## `PunchThemAll.Debug`

| Key | Default | Description |
| --- | ---: | --- |
| `log_loaded_interactions` | `false` | Logs every interaction ID loaded from config. |
| `log_skipped_interactions` | `false` | Logs when runtime interactions are skipped by global config gates. |

Debug preset for pack development:

```toml
[PunchThemAll.Loader]
fail_fast = true

[PunchThemAll.Debug]
log_loaded_interactions = true
log_skipped_interactions = true
```

## Migration notes

Older configs used a flatter layout with keys such as `min_interaction_delay`.
The current config uses grouped sections and clearer names such as
`cooldown_ticks`. If a world already has an older generated config, regenerate or
manually migrate the file to the grouped layout above.
