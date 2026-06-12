# Dark Mode — Manual QA Checklist

Dark mode is mostly verified by the Robolectric suites
(`app/src/test/java/com/manuelmaly/hn/util/ThemeHelperTest.java`,
`app/src/test/java/com/manuelmaly/hn/NightResourcesTest.java`), which cover the
preference→mode mapping, persistence, night detection, comment recoloring, and the
per-page `values-night` overrides. The visual behavior below still needs a device or
emulator, since theming ultimately renders pixels.

## How to run the automated tests
```
./gradlew :app:testDebugUnitTest      # logic + resource regression
./gradlew :app:assembleDebug          # confirms resources/theme compile
```
(Requires the standard Android setup: SDK installed, JDK 11+, and a dummy
`release.properties` in the repo root as described in the README.)

## Setup
1. Build & install the debug APK on a device/emulator (API 21+ ideally; algorithmic
   WebView darkening needs an up-to-date WebView, API 29+).
2. Open the app and have at least a few stories loaded.

---

## Scenario A — Follow system (default)
1. Fresh install, **do not** touch the dark-mode setting (defaults to *Follow system*).
2. Put the **OS** in light mode → app is light on all screens.
3. Put the **OS** in dark mode (Quick Settings / Display settings) while the app is open.
   - [ ] Article list re-themes to dark **without** restarting the app.
   - [ ] Open Comments and Article reader — both dark.
4. Toggle the OS back to light → app returns to light.

## Scenario B — Manual switch (Settings → Dark Mode)
1. Overflow menu → **Settings**. A new **Dark Mode** entry shows the current choice.
2. Select **Dark**:
   - [ ] Settings screen itself turns dark immediately (no manual back/forward needed).
   - [ ] Back out to the list / comments / article — all dark.
   - [ ] This overrides the OS setting (works even if the OS is in light mode).
3. Select **Light**: everything returns to light, overriding an OS dark setting.
4. Select **Follow system**: app matches the current OS mode again.
5. Set **Dark**, fully kill the app, relaunch:
   - [ ] App starts in dark with **no white flash** on launch (preference persisted).

---

## Per-page checks (verify in dark mode)

### Article list (`MainActivity`)
- [ ] List background is dark; row dividers visible but subtle.
- [ ] Unread post titles are light/legible; **read** titles are dimmer (still readable).
- [ ] Points badge and URL/meta text legible.
- [ ] Pull-to-refresh / "More" (load more) row is dark with legible text.
- [ ] "Loading …" empty state is dark with legible text.
- [ ] Action bar stays **orange** (intended — brand color), title legible.

### Comments (`CommentsActivity`)
- [ ] Background dark; author/time metadata legible.
- [ ] **Comment text is legible at every nesting depth** (top-level bright, deep replies
      dimmer but never invisible) — this is the key regression: previously the parser
      forced black/dark-gray text.
- [ ] Indentation bars on the left are faint **light** overlays (visible on dark), not
      invisible black ones.
- [ ] "Ask HN" header text + divider legible.
- [ ] Action bar orange, back button works.

### Article reader (`ArticleReaderActivity`)
- [ ] Action bar is **dark** (not the light/white bar), title legible.
- [ ] Page content is darkened (algorithmic darkening) on supported WebViews.
- [ ] No bright white flash while a page loads.
- [ ] Tapping the title still navigates to Comments.

### Settings (`SettingsActivity`)
- [ ] List background dark; all preference titles/summaries legible.
- [ ] Dark Mode summary reflects the selected option.

---

## Edge cases
- [ ] Rotate the device on each page in dark mode — stays dark, no crash.
- [ ] Scroll position is retained after a system dark/light toggle (activity recreation).
- [ ] Long-press menus / dialogs are readable in dark mode.
- [ ] Day mode is unchanged from before this feature (same palette as the original app).

## Known minor cosmetics (acceptable)
- The points badge (`points_roundedrect.9.png`) and the comment-count bubble
  (`bubble_patch*.9.png`) are raster 9-patches and stay light peach in dark mode; their
  text stays dark for legibility. These are small, distinct pill elements and read fine
  on a dark row. Converting them to themeable shapes is a possible future polish.
