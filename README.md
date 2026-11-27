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
  rounded line indicator. The pager underneath is tap-only: horizontal drags and the edge
  stretch are both off, so a page never moves unless its tab is tapped.
- **Composer** — wavy determinate/indeterminate context progress, filled multi-line field
  (6-line cap, internal scrolling), fullscreen composer, attachments, quick message,
  model + reasoning split button, interpolation, recording toggle, send/stop tonal button.
- **Attachments sheet** — multi-section list, shared-axis X page switching, system back
  returns to the root page; drafts, web search, workspace binding, features, compression.
- **Reasoning sheet** — effort slider with nine ticks (Off → Ultra), custom
  `reasoning_effort` / `thinking_budget` dialog, and a two-column model picker with
  foldable providers and rich model cards.
- **Sidebar** — a full page session panel with a collapsing title: a collapse chevron, three
  pinned quick actions stacked as one full width column, and the chat history scrolling on its
  own underneath. Every day group is an accordion card: tapping its header opens the indented
  session list inside that same card, the groups below slide down to make room, and the rows
  settle in from the indent. One group is open at a time, and nothing ever leaves the panel —
  no activity, no child page, no page swap. The bottom left FAB grows into Settings the same
  way.
- **Chrome** — every titled scrolling page (settings, preferences, providers, settings detail,
  sidebar, Mission, Usage) uses a collapsing title over a pinned toolbar. Toolbar navigation
  icons are tinted with `?attr/colorOnSurface`, and every vector in `drawable/` fills with a
  theme attribute, so the leading glyphs and the in-content icons follow light and dark instead
  of staying white.
- **Conversation** — an assistant reply has no bubble. The avatar and the author sit above the
  content. Folding follows RikkaHub's chain of thought, with two levels and no containers around
  either fold line:

  * one line above the card collapses *everything before the latest paragraph* — the steps card
    and the earlier paragraphs together — reading *Collapse previous N steps* while they are
    shown and *Expand previous N steps* while they are not;
  * the card's own first line folds its **leading** steps, so a folded card keeps its last two —
    the ones that led to the reply — and reads *Show N more steps* / *Collapse*. The line stays
    in place and only its chevron turns over; it does not disappear when the card opens.

  Each step is its own row: icon, a two-line label, a trailing chevron when it opens something,
  and a thin meter underneath whose length is that step's share of the time, with the duration
  printed at its end. The body is text-selectable, so a long press starts a selection. Below it,
  a bare icon-only `MaterialButtonGroup` — no pill around it — carries copy, regenerate, share and
  more, with the turn's stats closing the row on the end side.
- **User message** — a button shaped surface (`BubbleButton`): filled container, 10% pressed
  state layer, ripple clipped to the outline, `Button` accessibility role, and a `message_media`
  slot so a thumbnail or any other rich child sits above the caption. Pressing it morphs the
  corners from the bubble shape towards the small pressed shape and springs back on release, the
  way an M3 button does; there is no haptic tick.
- **Message editing** — tapping a user bubble pulls that message back into the field and turns
  the bubble to `colorTertiaryContainer`. A removable input chip above the field names what is
  being edited; removing it clears the draft and leaves the edit state, and sending writes the
  text back into the same row.
- **Message actions** — holding a user bubble opens a floating card under it. Options are
  stacked as their own rounded rows carrying an icon and a label only — no icon container, no
  outline, no trailing chevron — and the destructive row switches to the error container.
- **Settings** — a multi-section list: each block is named by a section header, and its rows are
  `ListItemLayout` segments sharing one continuous card, so only the first row rounds its top
  corners and only the last rounds its bottom.
- **Custom views** — branching conversation map (animated live head) and animated
  throughput bar chart.
- Light and dark schemes, edge-to-edge insets, portrait demo.

## Poke at it

- Tap the four tabs; the pager only follows the tab, it does not swipe.
- Tap the folder split button, the session title (cycles live status), the preset, new chat
  and the overflow menu.
- Tap the attachment clip for the multi-section sheet; enter Drafts / Web search /
  Workspace / Features / Compress and use the system back key.
- Tap the model split button for the reasoning sheet; open the model page, switch providers
  (the left rail folds), pick a model, drag the effort slider, open the custom
  `reasoning_effort` / `thinking_budget` dialog.
- Tap the sidebar icon for the session panel; the collapse chevron on its leading edge closes it
  with the shared axis X return, and a day group card opens its indented session list in place.
  Tap the same header again (or press back) to fold it, and the groups below close the gap.
- Tap the FAB at the bottom left for Settings: it grows into the page and shrinks back into the
  FAB. Settings rows do the same, and a back press during any entrance retraces it instead of
  cutting.
- Type in the field (the fullscreen glyph appears), send, watch the bar turn wavy and the
  tonal send button flip to stop. Toggle the mic for the REC state.
- Tap a user bubble: it comes back into the field with a removable chip above it, and the bubble
  turns tertiary while it is being edited. Remove the chip to drop the draft, or send to write the
  text back into the same row.
- Hold a user bubble: a card grows out of it with stacked icon-and-label options — select and
  copy, preview, edit, share, create branch, delete.
- Tap *Show N more steps* inside the reasoning card to list every step; the line stays put and
  turns into *Collapse*. Tap *Collapse previous N steps* above it to fold the card and the earlier
  paragraphs away together. Hold the reply body to select text, and use the icon row under it for
  copy, regenerate, share and more.

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
