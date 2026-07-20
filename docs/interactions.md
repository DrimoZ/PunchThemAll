# Datapacks, loading & the JEI/EMI display

PunchThemAll interactions are **datapack** data. Files live at:

```text
data/<namespace>/pta/interaction/**/*.json
```

The mod ships **no** interactions by default — packs provide them. See the ready-made
[example datapack](../examples/punchthemall-examples), and the format reference in
[interaction-format.md](interaction-format.md).

## Loading behaviour

- Every `*.json` under `data/<namespace>/pta/interaction/` becomes one registry entry.
- The **id is the resource path**: `data/mypack/pta/interaction/crushing/gravel.json` → the id
  `mypack:crushing/gravel`.
- Only **`schema_version: 2`** is accepted; files declaring an older version are rejected with a clear
  error in the log (`Incorrect Json format - <id> - schema_version … is not supported`).
- Add `"enabled": false` to skip a file without deleting it.
- Datapacks **override** each other by pack order (a later pack wins for the same id), and you can gate
  a file with `neoforge:conditions` (e.g. only load it when another mod is present).

## Reloading

Run **`/reload`** after changing files. Interactions are loaded by a datapack reload listener, so a
reload re-reads every file and resolves it into the runtime form — no restart, no separate discovery
option. Note this only works because interactions are ordinary datapack data: `/reload` does not
re-read datapack *registries* (worldgen and the like), which is why they are not one.

## Multiplayer

The server pushes its loaded set to clients over a small sync payload, on join and after every
`/reload`:

- gameplay always uses the **server's** interactions;
- clients receive the same set on join and after `/reload`, so **JEI and EMI show exactly the
  server's interactions** — nothing to configure per client;
- in single-player and on a LAN host it's the same shared data.

## Runtime gates

Even when a file is valid and loaded, the common config (`config/punchthemall/pta-common.toml`) can
disable interactions at runtime. The most important gates:

* `Interactions.enabled`
* `Interactions.allow_left_click` / `allow_right_click`
* `Interactions.allow_block_interactions` / `allow_air_interactions` / `allow_fluid_interactions`
* `Interactions.allow_transformations`
* `Players.allow_fake_players`
* `Players.allow_player_damage` / `allow_food_consumption`

Enable `Debug.log_skipped_interactions` to log why a loaded interaction does not run. See
[configuration.md](configuration.md) for every key.

## JEI / EMI category

PunchThemAll registers an **Interaction** category in both **JEI** and **EMI** — a player-facing
overview of every loaded interaction. It shows:

* click type + sneak/regular icons;
* the hand requirement (item/tag, hand slot, consume mode) with its NBT / `nbt_predicates` in the
  tooltip;
* the target block, fluid, or air marker, with state / NBT details;
* the transformation output when present;
* **weighted** drop slots with chance and count, and **guaranteed** drops as extra output slots;
* a summary (rolls, Fortune bonus, potion effects, conditions, sound/particles) — on the arrow tooltip
  in JEI, and in EMI's recipe display;
* the interaction id (handy when reporting an issue — it maps straight to the datapack file).

Both viewers refresh whenever the synced set arrives (join / `/reload`), so the display always
matches the server.

## Organising a pack

- **One interaction per file**, lowercase names with underscores, folders for grouping:

  ```text
  data/mypack/pta/interaction/early_game/flint_from_gravel.json   → mypack:early_game/flint_from_gravel
  data/mypack/pta/interaction/create/crushing_dust.json           → mypack:create/crushing_dust
  data/mypack/pta/interaction/automation/click_machine_only.json  → mypack:automation/click_machine_only
  ```

- Ship interactions inside your modpack's datapack, or as a standalone datapack players enable in the
  **Data Packs** screen / drop into a world's `datapacks/` folder.
