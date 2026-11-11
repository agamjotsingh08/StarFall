# Validation record

## Current 0.3.0 checks — 2026-11-11

### Fixed during review

- Particle effects previously consumed the same random sequence as enemy formations.
  Separate effect randomness now keeps combat identical across graphics presets.
- Initial taps outside the fixed-aspect arena could activate Dash, Nova or Pause on
  wide/tall viewports. Those taps are now ignored; an existing drag can still move
  outside the arena and is clamped safely.

- Reactor cooldown caps previously erased ship perks at high upgrade ranks. Applying
  those caps before the ship multipliers now preserves Vanguard and Falcon bonuses.
- Manual play exposed misleading pickup messages when weapons or hull were already
  full. Those pickups now say `SALVAGE RECOVERED` and describe their score bonus;
  actual upgrades and repairs retain their respective messages. A regression test passes.

### Passed

- 29 JVM gameplay tests, including Casual balance, all ship perks, supply attraction,
  hull limits, deterministic simulation, graphics-independent combat, collision,
  upgrades, pause, victory, restart and particle/gameplay pool isolation.
- 36 seeded complete campaign simulations across all upgrade paths, difficulties and
  ships. Every run completed 12 missions, defeated four bosses and restarted.
  The driver is invulnerable to exercise progression; this does not prove human balance.
- Android debug and instrumentation APKs compiled. The debug package reports
  `com.starfall.game.debug`, version `0.3.0` / code `3`, min API 26, target API 35.
- The APK signature verified successfully using APK Signature Scheme v2.
- Android lint: zero errors, four warnings (one API-33-only manifest attribute and
  three newer test dependency versions available).
- PowerShell build helper syntax and local documentation links checked.
- Git remote verified. No files staged, committed or pushed.
- 432 independent level configurations and 288 damage-enabled input-stress runs passed.
  See [LEVEL-AUDIT.md](LEVEL-AUDIT.md) for every-level results and reproduction steps.

### Difficulty probe

Stationary pilot, Vanguard, mission 1, 20 fixed seeds per mode. No steering or
abilities. No seed cleared the first mission by remaining idle.

| Difficulty | Median survival | Range |
| --- | ---: | ---: |
| Casual | 19.6 seconds | 17.6–22.1 seconds |
| Standard | 12.3 seconds | 11.3–12.5 seconds |
| Veteran | 8.3 seconds | 8.1–8.5 seconds |
| Nightmare | 7.5 seconds | 7.4–7.7 seconds |

These measurements show more reaction time in Casual; real playtesting is still
needed to judge how enjoyable the difficulty feels.

### Native testing and screenshots

Nine instrumentation tests passed together on Android 10 / API 29, AOSP x86_64,
720 × 1600 at 320 dpi. Coverage: launch, lifecycle pause, four software-Canvas
viewport sizes, a live final-boss encounter, upgrades, multi-touch dash, saved
ship/mode selection, taps outside the arena and normal Casual combat.

Ten original PNGs are saved in [screenshots/v0.3.0](screenshots/v0.3.0/README.md),
including all three ships, normal combat, boss combat, pause, upgrades, victory and
the separate manual-play result. The README includes a curated gallery. Images
were visually checked for legibility, clipping and correct interface state.
Boss, debrief and victory fixtures are explicitly labeled; they do not represent
an unassisted campaign completion.

The final harness run passed all nine tests in 33.309 seconds; the preceding passing
run took 194.022 seconds with idle-queue waits. In the final run, short normal-combat
and boss samples reported 58.0 and 60.0 FPS respectively, with 16.68 ms p95 frame
intervals (381 and 536 samples respectively). These measure
Choreographer callback intervals, not GPU presentation time or sustained phone
performance. A cold-launch attempt had only 12 samples and a 600 ms p95 interval;
that attempt paused unexpectedly and failed its combat assertion. The isolated
combat rerun and subsequent complete suite passed. The precise source of that
first-launch focus interruption was not established.

Manual playtesting on Android 15 / API 35, 720×1600: completed the first Casual
mission with steering, auto-fire, Nova, weapon/repair pickups and normal damage.
Chose hull reinforcement, advanced to mission 2, used Dash, paused and resumed.
Saved the actual result as `docs/screenshots/v0.3.0/manual-debrief.png`.
An additional manual check on Android 10 on 2026-11-11 exercised deployment,
relative steering, Nova, normal damage, restart, pause and return to the hangar.

Android 15 batch attempts were interrupted by host memory exhaustion and a desktop
emulator display-buffer error. They are not counted as passed tests. Android 10
provided the completed native suite and new screenshot gallery.
Earlier version 0.2.0 screenshots are retained locally and excluded from Git.

The harness now waits for window focus and captures the rendered window with
PixelCopy and a ten-second completion timeout. Thread-stack inspection also found
ActivityScenario waiting for an idle UI queue while the game continuously scheduled
frames. Bounded checks posted directly to the UI thread and stopping frames before
test teardown address that test-only wait; production gameplay is unchanged.
Local raw logs live under ignored `test-results/` and `.tools/`.

### Build environment

Earlier attempts failed because Windows could not commit enough native memory for
Java. The current APK compilation and lint succeeded using separate runs with a
384 MB Gradle heap, Serial GC, one worker and limited JIT compilation. The JVM tests
and simulation drivers ran separately with a 64 MB heap. No system settings were
changed and no unrelated applications were closed by the agent.

APK: `dist/starfall-debug.apk` (also under `app/build/outputs/apk/debug/`).
SHA-256: `F7BA0DFBDCFB8D0B4A73F69CCCF2D4B0DA84581BA2CBBD979EF1E47D074C0CE8`.
This is a debug build signed with the local development key, not a store release.

## Reproduce

With JDK 21 and SDK 35 configured:

```powershell
.\scripts\build.ps1
.\gradlew.bat assembleDebugAndroidTest connectedDebugAndroidTest
```

Device testing requires a running API 29+ emulator or phone. On macOS/Linux use
`sh ./gradlew assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest`.
These commands have not been run on macOS/Linux in this validation session.

For the dependency-free simulation drivers, compile `Campaign.java`, `Fleet.java`,
`GameWorld.java`, `scripts/SimulationSmoke.java` and `scripts/DifficultyProbe.java`
with javac into a temporary output directory, then run the two main classes in
package `com.starfall.game`.

## Earlier 0.2.0 baseline

- 16 gameplay unit tests passed; five native tests passed together on a Pixel 7
  profile / Android 15 API 35 emulator. The two-finger dash test passed separately.
- An isolated boss fixture reported about 59 FPS / 16.67 ms p95 frame interval over
  the last 600 samples. With a concurrent Gradle build it reported 49 FPS / 33.33 ms.
- Software Canvas checks covered compact, tall, portrait-tablet and landscape
  viewports. An actual tablet-emulator run was not completed in the recorded baseline.

## Remaining release coverage

- Real-device playtesting of all ships, difficulties, graphics presets and missions.
- Visual inspection of the maximum seven-segment hull HUD on physical screens.
- Upgrade from 0.2.0: mission unlocks retained, Casual selected initially, and later
  difficulty/ship selections retained across relaunch.
- Physical low/mid/high-end phones, tablets, Android 8+ coverage, sustained frame timing,
  temperature, battery, process termination and accessibility review.
- A clean-copy build and fresh-computer installation of the published source.

This record describes checks performed, not a guarantee of zero defects. Emulator
results do not establish performance on physical phones. An interrupted run returns
to the hangar after process termination; settings, high score and unlocks persist.
