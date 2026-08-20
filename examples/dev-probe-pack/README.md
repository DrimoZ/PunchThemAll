# Developer probe pack — **not for players**

Four deliberately awkward interactions used to verify PunchThemAll by hand, in game. Installing this
in a normal world does nothing useful and puts **five error lines in your log on purpose**. If you
came here looking for interactions to copy, you want
[`punchthemall-examples`](../punchthemall-examples) instead.

It exists because the unit suite stops at the edge of the game. Registry lookups, event timing,
`/reload`, the client sync and the recipe viewers are only observable by running, and this pack makes
each of them either log something specific or visibly do something.

## Install

Copy the folder into a dev world and `/reload`:

```
run/saves/<world>/datapacks/dev-probe-pack/
```

## What each file proves

| File | What should happen | What it caught |
| --- | --- | --- |
| `bad_ids.json` | Four `Incorrect Json format` lines — one each for `sound`, `particles`, `effects` and the biome `#tag` — naming the field. **And the interaction still loads**: left-click sand → flint. | Before 2.2.0 an unparseable id threw out of the reload listener, so one typo aborted the load of *every* interaction in *every* pack. Then, once that was fixed, three of the four failed silently — which is how four shipped examples had been passing particle-type ids where a block id is required, without anyone noticing. |
| `zero_min_drop.json` | Right-click gravel with a bowl → sometimes 0–3 wheat seeds, sometimes nothing. Shows in JEI with a 0–3 range. | `count: {min: 0, max: 3}` used to mean "never anything", while still taking up its weight in the pool — although the schema and docs both advertised it. |
| `sneak_conflict.json` | One `WARN` naming the file: the type and `requires_sneaking` contradict, so it can never match. | The warning found the same mistake in the shipped `conditions_sneaking` example, which had been unable to fire since 2.1.0. |
| `hidden_probe.json` | Right-click a bookshelf with a stick → a diamond. Absent from JEI: searching `diamond` must not surface it. | New in 2.2.0; this is the only way to check the viewers actually honour `hidden`. |

## Reading the log

`run/logs/latest.log`. Useful greps:

```
grep "com.drimoz" run/logs/latest.log | grep -iE "ERROR|WARN"
grep -E "Read [0-9]+ interaction file|Loaded [0-9]+ interaction" run/logs/latest.log
```

Every PTA line should appear **twice** after a `/reload`: once for the server's rebuild, once for the
client's rebuild from the sync payload. One occurrence means the client sync did not happen — which
is the failure this architecture is most exposed to, since a custom reload listener does not sync on
its own.
