# ShaRemove

[![Build ShaRemove](https://github.com/thejaustin/sharemove/actions/workflows/build.yml/badge.svg)](https://github.com/thejaustin/sharemove/actions/workflows/build.yml)

Hide apps from Android's intent chooser sheets — the share sheet, the "Open with" link
picker, the APK installer prompt — using **Shizuku** or **root**. No ads, no analytics,
no network access.

## Why

Android's chooser sheets show every app that claims an intent, and OEMs give you no way
to trim them. One stale file manager or a never-used browser and every share action
means scrolling past clutter. ShaRemove lets you pick exactly which apps appear.

## Categories

| Tab | Intent |
|---|---|
| App Installer | `VIEW` · `application/vnd.android.package-archive` |
| Browser | `VIEW` · `https` links |
| Share: Text | `SEND` · `text/plain` |
| Share: Images | `SEND` · `image/*` |
| Share: Files | `SEND` · `*/*` |

## How hiding works

Two modes, selectable in Settings. Both run `pm` commands through Shizuku (shell uid,
no root needed) or root when available:

- **Suspend** (default) — `pm suspend` pauses the whole package. App data stays intact;
  the launcher icon is greyed out until you unhide it.
- **Component** — `pm disable-user` on the specific activity that handles the intent.
  Surgical: the rest of the app keeps working. Caveat: some apps (notably browsers)
  route their launcher entry through the same activity, which would disable that too.

There is also a per-app **Disable** switch that fully disables a package
(`pm disable-user` on the whole package). Disabled and hidden apps stay in the list so
they can always be restored.

## Requirements

- Android 12+ (API 31)
- [Shizuku](https://github.com/RikkaApps/Shizuku) running, **or** a rooted device

## Install

Grab the latest APK from [Releases](https://github.com/thejaustin/sharemove/releases)
(built automatically from `main`), or from the artifacts of any
[Actions run](https://github.com/thejaustin/sharemove/actions).

## Build

Built exclusively via GitHub Actions (`.github/workflows/build.yml`) — push to `main`
produces a versioned debug APK and a prerelease. Locally: `gradle assembleDebug` with
JDK 17 and Gradle 8.10.

## Stack

Single-module Kotlin app: Jetpack Compose + Material 3 (dynamic color), MVVM with
`StateFlow`, DataStore preferences, Shizuku API 13. Min SDK 31, target 35.

## Acknowledgements

ShaRemove wouldn't exist without these projects:

- **[Shizuku](https://shizuku.rikka.app)** and the
  **[Shizuku-API](https://github.com/RikkaApps/Shizuku-API)** by
  [RikkaApps](https://github.com/RikkaApps) — the privileged-access layer that makes
  non-root hiding possible (Apache-2.0 / MIT)
- **[Jetpack Compose, Material 3 & AndroidX](https://developer.android.com/jetpack/compose)**
  by Google/AOSP — UI toolkit, design system, DataStore, lifecycle (Apache-2.0)
- **[Kotlin & kotlinx.coroutines](https://kotlinlang.org)** by JetBrains (Apache-2.0)

## License

[GPL-3.0](LICENSE) — free software; forks and derivatives must remain open source.
