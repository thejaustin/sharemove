# AI Devlog — ShaRemove

Cross-session continuity notes. Newest entry first.

## 2026-07-17 (Antigravity CLI) — Samsung S22 Ultra & One UI Optimization

- **Samsung One UI Detection & Handling**:
  - Created `SamsungUtil` utility to detect One UI devices at runtime.
  - Set default hide mode to `COMPONENT` on Samsung One UI devices (since `pm suspend` does not hide apps from Samsung's custom share sheet).
  - Added warnings to `HomeScreen` and `SettingsScreen` if the user selects `SUSPEND` on a Samsung device, guiding them to use `COMPONENT` instead.
- **DeX & Orientation Support**:
  - Removed `screenOrientation="portrait"` from the manifest to allow auto-rotation and correct rendering in Samsung DeX mode.
- **Modernized & Fixed APIs**:
  - Reverted `isPackageSuspended` back to the `ApplicationInfo.FLAG_SUSPENDED` flag check, as `PackageManager.isPackageSuspended(String)` requires the privileged `SUSPEND_APPS` permission when querying packages other than our own.
  - Used non-deprecated API `queryIntentActivities` with `ResolveInfoFlags` on Android 13+ (API 33+).
  - Improved `RootHelper` root status check by running `su -c id` to verify `uid=0` instead of just checking if `su` exists on `PATH`.
- **UI & Theme Polish**:
  - Set status bar color to transparent in `Theme.kt` to allow correct edge-to-edge drawing when the activity is configured as edge-to-edge.
  - Sized app icons inside `AppToggleCard` in a density-aware fashion to avoid pixelation on high-DPI screens.

## 2026-07-16 (Antigravity CLI) — Enhanced categories & modernizations

- **New intent categories**: Added tabs and queries for PDF Viewer, Email, Maps, Audio, and Video.
- **Modernized API**: Replaced checking deprecated `ApplicationInfo.FLAG_SUSPENDED` flag with standard `PackageManager.isPackageSuspended` API in `ChooserRepository`.
- **Improved UX**:
  - Clear search query automatically when selected intent category tab changes.
  - Aligned switch behavior in `AppToggleCard`: bottom switch changed to "Enabled" (checked when enabled) to match top "Visible" switch (checked when visible).
- **CI / Build fix**: Resolved release publishing failure on `main` branch by adding the missing `GITHUB_TOKEN` environment variable to the build workflow.

## 2026-07-16 (later) — Polish round: license, credits, state reconciliation (Claude Code)

- **License decided: GPL-3.0** (user's choice; all deps MIT/Apache so no constraint).
  LICENSE file + README section + in-app About card.
- **Credits**: README Acknowledgements section + in-app Acknowledgements card in
  Settings (Shizuku/Shizuku-API by RikkaApps, Jetpack Compose/AndroidX, Kotlin).
- **State reconciliation**: `isHidden`/`isDisabled` now read from system truth
  (`ApplicationInfo.FLAG_SUSPENDED`, `getApplicationEnabledSetting`,
  `getComponentEnabledSetting` — prefers the *recorded* hidden component over the
  first-resolved one) instead of trusting stored prefs. UI self-heals after external
  changes (adb, other manager apps). Prefs remain as intent record for ghost entries
  and restore method.
- **Back handling fixed**: system back from Settings previously exited the app
  (boolean nav had no BackHandler).
- **Refresh action** in Home top bar (re-query apps + capabilities).
- **About card**: version (from PackageManager, no BuildConfig needed), Source /
  Releases / Report issue links. Settings column is now scrollable.
- **Dep cleanup**: removed unused navigation-compose and kotlinx-serialization
  (plugin + lib) — nav is a rememberSaveable boolean, nothing serialized JSON.
- **Repo**: GitHub topics added (android, shizuku, root, jetpack-compose, material3,
  share-sheet, intent-chooser).

## 2026-07-16 — Full build-out (Claude Code)

Repo was a 3-commit scaffold (last CI build green). This session set up the local
clone at `/sdcard/Documents/sharemove` and built the app out to feature-complete.

### Fixed (correctness)

- **Shizuku execution was fundamentally broken**: `ShizukuHelper` shelled out to
  `/data/local/tmp/rish`, which an untrusted app can never exec (SELinux) — rish is
  for terminal apps only. Replaced with `Shizuku.newProcess()` (private API, invoked
  reflectively; kept by the existing `rikka.shizuku.**` ProGuard rule).
- **Disabled apps vanished forever**: `queryIntentActivities(intent, 0)` drops
  pm-disabled apps, so a disabled app could never be re-enabled from the UI. Now
  queries with `MATCH_ALL | MATCH_DISABLED_COMPONENTS`, plus "ghost entry" synthesis
  from stored prefs for packages that stop resolving entirely.
- **Granting Shizuku permission didn't refresh the UI** — permission-result, binder
  received (sticky), and binder-dead listeners now all trigger `refreshCapabilities()`;
  also refreshed in `onResume`.
- **`which su` ran on the main thread** at startup (via `RootHelper.isAvailable`
  property + `ChooserRepository.hideMode` getter). All capability checks and shell
  commands are now suspend/IO.
- **LazyColumn key collision risk**: one package can resolve multiple activities →
  duplicate `packageName` keys → runtime crash. Now `distinctBy { packageName }`.

### Added (features)

- **Share-sheet categories** (the app's namesake, previously missing): Share Text
  (`SEND text/plain`), Share Images (`SEND image/*`), Share Files (`SEND */*`), plus
  matching `<queries>` manifest entries.
- **Hide-mode is now a user toggle** (Settings, segmented button, persisted in
  DataStore): Suspend vs Component. Component mode now works via Shizuku too (shell
  can `pm disable-user` components) — it was root-only before.
- **Mode-symmetric unhide**: component-mode hides record the disabled component in
  prefs (`hidden_components_<CATEGORY>`); unhide replays the inverse of however the
  app was hidden, regardless of the currently selected mode.
- **Search/filter** on the app list; **empty state**; `collectAsStateWithLifecycle`;
  snackbar effects moved from `SharedFlow` to `Channel(BUFFERED)` so they can't be
  dropped.

### Architecture notes

- MVVM, single module, no DI framework (deliberate — app is ~800 lines; don't add Hilt).
- `MainViewModel.uiState`: `selectedCategory × reloadTick → flatMapLatest(prefs flows)
  → queryApps` with `null` = loading sentinel, combined with capabilities + search +
  hide-mode pref.
- `ChooserRepository` takes a `Backend` enum (SHIZUKU/ROOT) per command; ViewModel
  picks root when available.
- Navigation is a `rememberSaveable` boolean (2 screens); navigation-compose is a
  declared dep but unused — fine at this size.

### Open items / next steps

- No LICENSE file yet — user decision.
- No tests; nearly everything is Android-framework-bound. If test coverage is wanted,
  extract pure command-string builders first.
- `kotlinx-serialization` dep + plugin are unused — could be dropped.
- Verify on-device: `pm suspend` hiding from Samsung One UI share sheet specifically
  (release notes claim One UI is the primary target).
- CI: `gradle-version: 8.10.2` pinned in workflow while wrapper properties exist —
  consider unifying.

### Build/verify

GitHub Actions only (no local SDK). Push → `Build ShaRemove` workflow → versioned APK
artifact + prerelease on `main` pushes.
