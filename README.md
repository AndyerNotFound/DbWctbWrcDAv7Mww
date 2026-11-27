# Neko Chat — Material 3 XML demo

A standalone, visual-only Android demo written in **Java + XML**, built entirely on
`com.google.android.material:material:1.14.0` (Material 3 Expressive).

The app is a phone-shaped chat shell: a tabbed top app bar, a live conversation, a
conversation map, mission/workspace status, session usage, and a docked composer with a
context budget bar.

## What is inside

- **Top bar** — expandable sidebar (side sheet), session-folder split button, session title
  (text button with a live status icon), session preset, new chat, overflow menu.
- **Tabs** — Chat / Map / Mission / Usage, fill gravity, elastic animation, full-width
  rounded line indicator.
- **Composer** — wavy determinate/indeterminate context progress, filled multi-line field
  (6-line cap, internal scrolling), fullscreen composer, attachments, quick message,
  model + reasoning split button, interpolation, recording toggle, send/stop tonal button.
- **Attachments sheet** — multi-section list, shared-axis X page switching, system back
  returns to the root page; drafts, web search, workspace binding, features, compression.
- **Reasoning sheet** — effort slider with nine ticks (Off → Ultra), custom
  `reasoning_effort` / `thinking_budget` dialog, and a two-column model picker with
  foldable providers and rich model cards.
- **Sidebar** — `SideSheetDialog` session browser.
- **Custom views** — branching conversation map (animated live head) and animated
  throughput bar chart.
- Light and dark schemes, edge-to-edge insets, portrait demo.

## Poke at it

- Tap the four tabs; swipe the pager.
- Tap the folder split button, the session title (cycles live status), the preset, new chat
  and the overflow menu.
- Tap the attachment clip for the multi-section sheet; enter Drafts / Web search /
  Workspace / Features / Compress and use the system back key.
- Tap the model split button for the reasoning sheet; open the model page, switch providers
  (the left rail folds), pick a model, drag the effort slider, open the custom
  `reasoning_effort` / `thinking_budget` dialog.
- Tap the sidebar icon for the session side sheet.
- Type in the field (the fullscreen glyph appears), send, watch the bar turn wavy and the
  tonal send button flip to stop. Toggle the mic for the REC state.

## Build

```bash
./gradlew :app:assembleDebug :app:assembleRelease
```

- debug: `app/build/outputs/apk/debug/app-debug.apk`
- release (demo-signed with the debug keystore): `app/build/outputs/apk/release/app-release.apk`

CI (`.github/workflows/android.yml`) validates the Gradle wrapper, assembles both APKs on
GitHub Actions, prints their SHA-256 and uploads them as the `neko-chat-1.0.0` artifact.

- `compileSdk` / `targetSdk` 36, `minSdk` 26
- Java 17, AGP 8.10.1, Gradle 8.11.1
- No Compose, no Kotlin, no custom Material copies.
