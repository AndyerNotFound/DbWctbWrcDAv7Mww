# Neko Chat — Material 3 XML demo

A standalone, visual-only Android demo written in **Java + XML**, built entirely on
`com.google.android.material:material:1.14.0` (Material 3 Expressive).

The app is a phone-shaped chat shell: a tabbed top app bar, a live conversation, a
conversation map, mission/workspace status, session usage, and a docked composer with a
context budget bar.

## What is inside

- **Top bar** — sidebar entry (shared axis X to the panel), session-folder split button, session
  title (text button with a live status icon), session preset, new chat, overflow menu.
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
- **Sidebar** — a full page session panel: a collapse chevron plus the Neko Chat title on top,
  three pinned quick actions stacked as one full width column, and the chat history scrolling
  on its own underneath. Each day group is a card that grows into its own page with a
  `MaterialContainerTransform` fragment transition, and the bottom left FAB opens Settings.
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
- Tap the sidebar icon for the session panel; the collapse chevron on its leading edge closes it
  with the shared axis X return, and a day group card grows into its own page.
- Tap the FAB at the bottom left for Settings; the settings pages are pushed with shared axis X.
- Type in the field (the fullscreen glyph appears), send, watch the bar turn wavy and the
  tonal send button flip to stop. Toggle the mic for the REC state.

## Build

```bash
./gradlew :app:assembleDebug :app:assembleRelease
```

- debug: `app/build/outputs/apk/debug/app-debug.apk`
- release (demo-signed with the debug keystore): `app/build/outputs/apk/release/app-release.apk`

CI (`.github/workflows/android.yml`) validates the Gradle wrapper, assembles both APKs on
GitHub Actions, prints their SHA-256 and uploads them as the `neko-chat-<version>` artifact.
Pushing a `v*` tag (or dispatching the workflow with *publish_release*) then publishes the
release APK and `sha256.txt` to a GitHub Release, with notes built from the same run.

- `compileSdk` / `targetSdk` 36, `minSdk` 26
- Java 17, AGP 8.10.1, Gradle 8.11.1
- No Compose, no Kotlin, no custom Material copies.
