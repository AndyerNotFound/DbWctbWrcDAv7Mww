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

## Build

```bash
./gradlew :app:assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

CI (`.github/workflows/android.yml`) validates the Gradle wrapper, assembles the debug
APK on GitHub Actions, prints its SHA-256 and uploads it as an artifact.

- `compileSdk` / `targetSdk` 36, `minSdk` 26
- Java 17, AGP 8.10.1, Gradle 8.11.1
- No Compose, no Kotlin, no custom Material copies.
