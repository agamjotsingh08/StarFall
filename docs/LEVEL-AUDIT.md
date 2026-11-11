# Level audit — 0.3.0

Simulation checks completed 2026-11-09; Android rebuild and 29-test regression suite completed 2026-11-10.
These results establish tested behavior, not a guarantee of zero defects or device FPS.

Nine native tests and the interface screenshot gallery were completed on Android 10
on 2026-11-11. See [TESTING.md](TESTING.md) for device coverage, performance samples
and limitations; the per-level simulation results below are unchanged.

## Issue reproduced and fixed

Reactor upgrades applied a shared cooldown minimum after ship bonuses. This erased
Vanguard's Nova advantage and Falcon's dash advantage at high upgrade ranks.
For example, Casual Vanguard with three reactor upgrades took 14 seconds instead of
the promised 13.6 seconds. The new test failed before the fix and passed afterward.
The minimum now applies before ship bonuses, preserving them through all upgrade ranks.

The previous review also fixed graphics effects changing combat randomness and
taps outside the play area activating controls.

The native playtest additionally caught misleading pickup banners at maximum weapon
strength/full hull. Those pickups now report their 100-point salvage reward instead
of claiming an upgrade or repair that did not occur; a regression test covers both.

## Coverage

- **29 regression tests passed**, including both phases of every boss, Nova finishing
  bosses exactly once, boss-spawn retry after a full pool, pause/defeat/restart on
  every level, and ship perks through all 12 possible reactor ranks.
- **432 independent level runs passed:** 12 levels × 4 difficulties × 3 ships ×
  3 seeds/graphics presets. Every run completed, cleared its objects, updated its
  unlocks/high score, accepted its next upgrade and restarted from mission selection.
- **36 complete campaigns passed:** all combinations of difficulty, ship and
  weapons-only / hull-only / reactor-only upgrade choices. This exercises 432 more
  sequential level completions and 144 boss defeats.
- **288 randomized input runs passed:** damage enabled, 24 runs on every level.
  Outcomes: 238 defeats, 50 clears, zero still running at the 90-second limit.
  All preserved valid health, position and charges; pause and restart checks passed.

Progression drivers use invulnerability to test completion independently of player
skill. Randomized steering is a robustness test, not a skilled player or a human
difficulty study. Its inability to beat a boss does not demonstrate an unwinnable boss.

## Every-level results

Each row includes all four difficulties and all three ships. Clear times are simulated
game time under the automated progression driver, not rendering performance.

| Level | Operation | Progression runs | Both boss phases observed | Longest clear | Peak gameplay objects / 320 |
| --- | --- | ---: | --- | ---: | ---: |
| 1 | Dead Signal | 36/36 | — | 32.01 s | 88 |
| 2 | Debris Field | 36/36 | — | 35.01 s | 107 |
| 3 | The Warden | 36/36 | 36/36 | 57.48 s | 113 |
| 4 | Blackout | 36/36 | — | 35.01 s | 121 |
| 5 | Kill Corridor | 36/36 | — | 33.61 s | 139 |
| 6 | Revenant | 36/36 | 36/36 | 40.05 s | 133 |
| 7 | Ash Front | 36/36 | — | 35.30 s | 152 |
| 8 | Iron Veil | 36/36 | — | 36.33 s | 163 |
| 9 | Dominion | 36/36 | 36/36 | 18.55 s | 133 |
| 10 | No Return | 36/36 | — | 39.38 s | 167 |
| 11 | Event Horizon | 36/36 | — | 40.08 s | 172 |
| 12 | Singularity | 36/36 | 36/36 | 19.98 s | 182 |

Object counts are sampled every 30 simulation steps, so these are sampled peaks,
not an assertion about every intermediate instant. No stalled run or capacity-related
failure occurred. Machine-readable results: [level-audit.csv](level-audit.csv).

## Reproduce without Android

With JDK 21 on PATH, from the project root:

```sh
javac -d test-results/logic app/src/main/java/com/starfall/game/Campaign.java app/src/main/java/com/starfall/game/Fleet.java app/src/main/java/com/starfall/game/GameWorld.java scripts/LevelAudit.java scripts/InputStressAudit.java scripts/SimulationSmoke.java
java -Xmx64m -cp test-results/logic com.starfall.game.LevelAudit
java -Xmx64m -cp test-results/logic com.starfall.game.InputStressAudit
java -Xmx64m -cp test-results/logic com.starfall.game.SimulationSmoke
```

Regression tests run via `gradlew.bat testDebugUnitTest` (Windows) or
`sh ./gradlew testDebugUnitTest` (macOS/Linux). See [TESTING.md](TESTING.md) for native
device coverage, screenshots and remaining limitations.
