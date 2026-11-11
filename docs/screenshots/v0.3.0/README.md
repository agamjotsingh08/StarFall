# STARFALL 0.3.0 interface captures

Original 720 × 1600 PNG captures, inspected for readable text, complete controls,
correct ship selection, and unobstructed gameplay. They have not been composited,
retouched or generated as interface mockups.

| Files | Source and scenario |
| --- | --- |
| [Vanguard](ship-vanguard.png), [Falcon](ship-falcon.png), [Bastion](ship-bastion.png), [hangar](hangar.png) | Android 10 / API 29 emulator; native hangar with Casual defaults. |
| [Casual combat](casual-combat.png) | Normal mission-one simulation with Bastion, damage enabled and no invulnerability override. |
| [Boss combat](boss-combat.png), [pause](pause.png), [victory](victory.png) | Mission 12 test fixture: upgraded Vanguard and invulnerability to exercise the live boss renderer, pause overlay and completion screen. Not a full unassisted campaign. |
| [Upgrade screen](debrief.png) | Stage completion invoked by the native test to check upgrade controls. The zero-kill S grade belongs to that fixture. |
| [Manual mission result](manual-debrief.png) | Android 15 / API 35 manual play on 2026-11-10: mission one completed with steering, Nova and normal damage, followed by a hull upgrade and mission two. Captured before the pickup-message correction; the debrief UI is unchanged. |

The API 29 images were captured on 2026-11-11 directly from the application's
rendered window with Android PixelCopy. The manual result used ADB screencap.
The README displays a selection at reduced width; these files retain full resolution.

To regenerate, build both debug APKs, start an API 29+ emulator and run
`scripts/capture-screenshots.ps1` from the project root. On a fresh emulator, open
the game once and dismiss any Android onboarding overlay before running the suite.
Inspect the new images before publishing them. The script preserves the separate
manual-play capture.

See [validation record](../../TESTING.md) and [every-level audit](../../LEVEL-AUDIT.md).
