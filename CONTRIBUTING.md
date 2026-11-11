# Working on Starfall

Use JDK 21, SDK 35 and the checked-in Gradle wrapper. Follow the README to run the app.

- Keep gameplay rules in the pure-Java simulation (`GameWorld`, `Campaign`, `Fleet`).
- Keep Android drawing/input in `GameView` and cached artwork in `GameArt`.
- Avoid allocation in per-frame paths. Respect the separate gameplay/particle pools.
- Add meaningful tests for gameplay changes; use fixed seeds for reproducibility.
- Update controls, ship stats and installation docs when the behavior changes.
- Keep APKs, SDKs, JDKs, signing keys and personal paths out of source control.

Before submitting a gameplay change, run:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

Use `sh ./gradlew` instead on macOS/Linux. Device tests require a running API 29+
device. Check the hangar, combat, debrief, pause, multi-touch controls and persistence
after relaunch. Describe what changed, how it was tested and any remaining device gaps.

Do not infer performance on physical phones from emulator results. Record measured
frame timing with its device, quality preset and workload in `docs/TESTING.md`.
