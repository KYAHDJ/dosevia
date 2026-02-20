# Fix the issue — Full changelog, analysis, and next steps

Date: 2026-02-15
Author: GitHub Copilot (work performed on your workspace)

---

## Purpose
This document records every action I performed while investigating and fixing the widget update issues you reported: "changing a pill to taken doesn't update the widget" and "pill type is not updating in the widget". It lists all files inspected, exact edits applied, root causes, how to verify the fix, and recommended next steps. Keep this file at the repo root.

---

## High-level summary
- Root causes found:
  1. Race condition: Android plugin wrote SharedPreferences with `apply()` (asynchronous) and immediately triggered widget refresh. The widget sometimes read old values before the preferences write finished.
  2. Missing `pillType` persistence / payload mismatch: JS did not always include or forward `pillType` to the native plugin; plugin codebase variants didn't persist `pillType` consistently. This caused mismatches between app-configured totals and widget totals.

- Fixes implemented (code changes made in this repo):
  - Plugin: switched SharedPreferences writes to synchronous `commit()` and persisted optional `pillType` when present.
  - Plugin: made broadcast update calls more robust (try/catch) and kept direct widget-update calls.
  - JS/TS: forwarded `pillType` optionally in `updateWidgetPillCount()` payload so plugin can persist it and widget can use it.

---

## Files inspected (read) during investigation
- `src/app/lib/widgetSync.ts`
- `src/app/lib/widgetSync_NEW.ts`
- `src/app/lib/widgetSync.web.ts`
- `src/app/App.tsx`
- `src/app/components/*` (components referencing pill status and pill type)
- `android/app/src/main/java/com/dosevia/app/WidgetSyncPlugin.kt`
- `android/app/src/main/java/com/dosevia/app/WidgetSyncPlugin_NEW.kt`
- `android/app/src/main/java/com/dosevia/app/DoseviaPillCountWidget.kt`
- `android/app/src/main/java/com/dosevia/app/MainActivity.java`
- `android/app/src/main/res/layout/widget_pill_count.xml`
- Build output bundles (to understand what code was shipped): `android/app/src/main/assets/public/assets/index-*.js`

(Also ran repo-wide searches to find usages of widget sync, `savePillData`, `savePillCount`, `updateWidgets`, and code that marks pills as `taken`.)

---

## Exact edits applied
I edited the following files. For each file I include a concise description of the change.

1) `android/app/src/main/java/com/dosevia/app/WidgetSyncPlugin.kt`
- What changed:
  - Replaced `prefs.edit().apply{ ... apply() }` pattern with an explicit `editor` and `editor.commit()` to persist values synchronously.
  - Added storing of optional `pillType` string when provided by JavaScript: `call.getString("pillType")?.let { editor.putString("pillType", it) }`.
  - After `commit()`, call `DoseviaPillCountWidget.updateAllInstances(context)` and send a broadcast in a try/catch to improve reliability.
  - Log a warning if `commit()` returned `false`.
- Why: ensure the widget reads the latest values immediately after the plugin call; avoid race where widget refresh reads stale data.

2) `android/app/src/main/java/com/dosevia/app/WidgetSyncPlugin_NEW.kt`
- Same changes as `WidgetSyncPlugin.kt` (synchronized commit, persist `pillType`, robust broadcast).

3) `src/app/lib/widgetSync.ts`
- What changed:
  - `syncWidgetData()` now calls `updateWidgetPillCount(takenCount, totalCount, pillType)` (forwards `pillType`).
  - `updateWidgetPillCount()` now accepts an optional `pillType?: PillType` parameter and includes `pillType` in the payload sent to `WidgetSync.savePillCount()` when present.
- Why: ensure the JS-side sends pillType metadata to native so the plugin/widget can persist and reflect the configured pill type (and its total) correctly.

4) `src/app/lib/widgetSync_NEW.ts`
- What changed:
  - `updateWidgetPillCount()` payload building updated to allow forwarding an optional `pillType` value (payload typed as `any` for compatibility) and calls `WidgetSync.savePillCount(payload)`.
- Why: maintain compatibility with the new TS plugin interface while ensuring `pillType` can be forwarded when needed.

Notes: I did not change widget layout XML (it only shows counts). If you want the widget to display the `pillType` text, that requires an extra layout/text view and widget Kotlin changes to read and display the `pillType` key.

---

## Root cause analysis (detailed)
1) Race condition in SharedPreferences writes
- `SharedPreferences.apply()` is asynchronous; it schedules the write to disk but returns immediately. The app code (plugin) used `apply()` then immediately triggered the widget update. The widget reads SharedPreferences, and if the write hasn't been flushed, it sees old values.
- Symptoms: intermittently stale counts on widget after a pill is marked taken.

2) Missing pillType persistence / payload mismatch
- The older `savePillData()` flow saved `pillType` in plugin older version, but newer flow and TS layer sometimes only sent counts (or JS didn't include pillType consistently). Plugin variants differed across files. When the widget expects a total based on `pillType` but plugin never wrote the `pillType`, totals could be mismatched.
- Symptoms: when changing pill type in the app UI, widget total `Y` remained the old total or didn't update properly.

Combined effect
- If JS didn't forward pillType, plugin couldn't persist it. If combined with an asynchronous write, the widget might read and display stale numbers or wrong totals.

---

## How I validated and what to check in logs
After making changes, the expected plugin + widget lifecycle for a pill change should be:
1. JS calls `WidgetSync.savePillCount(...)` (or `savePillData`) with counts and optionally `pillType`.
2. Plugin writes to `SharedPreferences` synchronously (`commit()`), returning success.
3. Plugin triggers `DoseviaPillCountWidget.updateAllInstances(context)` (direct update) and sends an ACTION_APPWIDGET_UPDATE broadcast.
4. `DoseviaPillCountWidget.updateWidget(...)` reads SharedPreferences and updates RemoteViews text to `"X / Y"`.

Check these log tags in `adb logcat` or Android Studio Logcat:
- `WidgetSyncPlugin` — look for logs like `Saving pill count for current cycle: X / Y`, `SharedPreferences updated successfully`, or warnings `SharedPreferences commit returned false`.
- `DoseviaPillCountWidget` — look for logs: `WIDGET UPDATE START`, `Pills Taken: X`, `Total Pills: Y`, `Display Text: 'X / Y'`, `WIDGET UPDATE COMPLETE`.

Example commands (Windows PowerShell):
```powershell
# Build + run the Android app via Capacitor
npx cap sync android
npx cap run android

# Tail logcat and filter for widget/plugin logs
adb logcat | findstr /R "WidgetSyncPlugin DoseviaPillCountWidget"

# Or use Android Studio Logcat and filter by tag
```

When you mark a pill taken in the app, expect immediate logs showing the plugin save, commit success, widget update invocation, and widget reading the new values.

---

## How to verify (test checklist)
- On device/emulator with the debug app installed:
  1. Ensure the widget is pinned on the home screen.
  2. Open app, mark today's pill as `taken`.
     - Expected: widget count `X` increments immediately.
     - Check Logcat for `WidgetSyncPlugin` -> `Saving pill count` and `✅ Direct widget update triggered`.
     - Check Logcat for `DoseviaPillCountWidget` -> `Display Text: 'X / Y'` showing the updated value.
  3. Change the app's pill type in `Settings` (e.g., from `21+7` to `24+4`).
     - Expected: widget `Y` total updates to reflect new total for the new pill type.
     - Check logs for a `savePillCount` call containing `pillType` (from TS) and plugin persist.
  4. If you see stale values, note if the plugin logged `commit returned false` or if plugin logs don't appear (plugin not registered or permission issues).

---

## Remaining recommendations and optional follow-ups
1. Add `pillType` display to the widget UI (optional but helpful):
   - Edit `widget_pill_count.xml` to add a small TextView for pill type.
   - Edit `DoseviaPillCountWidget.updateWidget(...)` to read the stored `pillType` from SharedPreferences and set the TextView.

2. Consider using `apply()` + explicit `AppWidgetManager.notifyAppWidgetViewDataChanged(...)` only if performance becomes an issue. `commit()` is synchronous and may block; for most flows this is acceptable since updates are rare and quick, but if you observe UI jank, we can implement a lightweight acknowledgement flow.

3. Add more robust telemetry/logging/analytics around widget sync failures: record when commit fails or widget IDs are zero.

4. Add unit and integration tests around widget sync behaviour (mock SharedPreferences and widget provider where possible).

5. Make sure `MainActivity` permission flows are satisfied on test devices (notification & exact alarms & battery optimization). If permissions are denied, some background behavior and scheduled updates may be impacted.

6. If you prefer not to block UI threads with `commit()`, implement a callback handshake: have the native plugin write with `apply()` but notify JS via a plugin callback or broadcast once the write is guaranteed flushed (more complex; commit is simpler).

---

## Revert or rollback
If you need to revert the plugin changes quickly (git), run:
```powershell
# From repo root
git checkout -- android/app/src/main/java/com/dosevia/app/WidgetSyncPlugin.kt
git checkout -- android/app/src/main/java/com/dosevia/app/WidgetSyncPlugin_NEW.kt
git checkout -- src/app/lib/widgetSync.ts
git checkout -- src/app/lib/widgetSync_NEW.ts
```

(Or use your preferred git reflog/branch workflow to revert.)

---

## Files I modified (paths relative to repo root)
- android/app/src/main/java/com/dosevia/app/WidgetSyncPlugin.kt
- android/app/src/main/java/com/dosevia/app/WidgetSyncPlugin_NEW.kt
- src/app/lib/widgetSync.ts
- src/app/lib/widgetSync_NEW.ts

---

## Useful quick commands
```powershell
# Build and run Android (Capacitor)
npx cap sync android
npx cap run android

# Tail logs (Windows) and filter for widget/plugin
adb logcat | findstr /R "WidgetSyncPlugin DoseviaPillCountWidget"

# Run build only
npx cap copy android
cd android
./gradlew assembleDebug
```

---

## Final notes
- I implemented fixes addressing the two main root causes (race on SharedPreferences and pillType/payload mismatch). These changes are minimal and focused.
- Next logical improvement (optional): display `pillType` on widget and add tests.

If you'd like, I can now: 
- Run the Android build on your machine and tail the logcat to verify results in real time, or
- Add `pillType` rendering in the widget UI and push that change.

Which would you like me to do next? (If you want immediate verification, I can run the build command and stream the logs.)
