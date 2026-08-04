# Melox Project Guide

Last updated: 2026-08-01

## Project

- Melox is an offline Android local-music player.
- Application ID and namespace: `com.melox.player`.
- Stack: Kotlin, Jetpack Compose, Miuix `0.9.3`, AndroidX Media3 `1.10.1`.
- SDK baseline: `minSdk 28`, `compileSdk 37`, `targetSdk 36`.
- The app must not request network access.

## Required References

Use this precedence for visible UI and Miuix API decisions:

1. Current user requirements and live Melox behavior.
2. This file, `MEMORY.md`, `PRD.md`, and `Tech-Spec.md`.
3. The local official Miuix `v0.9.3` tag at
   `/Users/bocchi/Code/miuix`, including its component source, example app,
   and matching guides.
4. `/Users/bocchi/Code/Miuix-UI-Development-Guide.md`.
5. `/Users/bocchi/Code/AGENTS.md` and `/Users/bocchi/Code/CLAUDE.md` as upstream UI guidance.

The upstream files may describe another application or a newer library revision. Reuse general UI rules, not product-specific routes, icons, thresholds, or business behavior.

## Authorized Reuse

- The maintainer owns `/Users/bocchi/Code/UixPlayer`; its music-home UI, MediaStore scan, artwork cache, sorting, index, playback, and external-audio behavior may be migrated directly.
- The maintainer owns `/Users/bocchi/Code/IconEditor`; its settings hierarchy, theme settings, and About-page interaction may be adapted for Melox.
- Preserve Apache-2.0 headers and `THIRD_PARTY_NOTICES.md` attribution. Do not copy GPL-only IconEditor floating-navigation code.

## Architecture

- Keep one Android application module.
- Keep concrete repositories in `data/repository`.
- Keep immutable app models in `model`.
- Keep Media3 player/session ownership in `playback/PlaybackService`.
- The UI talks to playback through a `MediaController` wrapper; an Activity or ViewModel must never own the `ExoPlayer`.
- Keep feature screens in `ui/screen/<feature>` and feature-only components in `ui/component/<feature>`.
- Do not add dependency injection, repository interfaces, use-case layers, or extra Gradle modules without more than one real implementation.
- Persist small settings in DataStore and versioned library/playback snapshots with `AtomicFile`.

## UI Rules

- All visible controls use Miuix. Do not add Material or Material3 composables.
- Reuse the official Miuix `v0.9.3` implementation or example pattern for
  animations and visual effects. Add app-side glue only when the library has no
  equivalent reusable API.
- Use `MiuixTheme.colorScheme` and `MiuixTheme.textStyles`.
- Use Miuix squircle modifiers for custom shapes:
  - `squircleBackground` for flat non-clipping backgrounds.
  - `squircleClip` for artwork or other clipped content.
  - `squircleSurface` for clickable custom surfaces.
- Each `MiuixScrollBehavior` must be connected to its scrolling container.
- Use `scrollEndHaptic()` and Miuix overscroll for page lists; disable the scrollable's own overscroll when applying an explicit Miuix effect.
- Keep every `Overlay*` inside the owning Miuix `Scaffold`.
- Use Miuix `miuix-navigation3-ui-android` for secondary-page enter, exit, and
  predictive-back transitions. Keep one stable official `NavDisplay` and scene
  state when the user changes the predictive-back setting at runtime; toggle
  the official `NavigationBackHandler` instead of replacing the navigation
  host. Keep the default transition effects and do not add a parallel
  hand-written page translation state machine.
- Use a bottom navigation bar below 600 dp and a navigation rail at 600 dp and above.
- Keep the music list visually aligned with UixPlayer. The full player is the only strongly atmospheric surface.
- Blur and liquid glass are optional. API 28-32 must render a complete opaque fallback; API 33+ may enable runtime-shader effects when supported.
- All visible strings live in Android resources. Maintain Simplified Chinese and English together.
- Reusable composables expose `modifier: Modifier = Modifier` as the first optional parameter and apply it to their root.
- Collect Flow in Compose with `collectAsStateWithLifecycle()`.
- Locale changes are handled in place through
  `android:configChanges="locale|layoutDirection"`; language controls must
  update their selected override immediately even when it matches the effective
  system locale.

## Product Boundaries

Version 1 includes:

- MediaStore music scan and cached startup library.
- Song, album, and artist search, deterministic sorting, and contextual
  alphabet indexes.
- Album and artist library pages plus album and artist detail pages.
- Background playback, system media controls, queue management, and playback restoration.
- Mini player, full player, settings, theme settings, and About.
- Android open/share handling for one `audio/*` URI.

Version 1 excludes:

- Lyrics, favorites, user playlists, equalizer, tag editing, file deletion,
  network artwork, casting, cloud services, and downloads.

## Commands

- Compile: `./gradlew :app:compileDebugKotlin`
- Unit tests: `./gradlew :app:testDebugUnitTest`
- Lint: `./gradlew :app:lintDebug`
- Build APK: `./gradlew :app:assembleDebug`
- Instrumented tests: `./gradlew :app:connectedDebugAndroidTest`

Run the narrowest meaningful check first. Do not report success unless the command exits cleanly.

For final delivery, copy the verified APK to `artifacts/Melox-debug.apk`, verify that it exists and is a valid ZIP/APK, and report its SHA-256.

## Worktree and Submission

- Inspect status before editing and preserve unrelated user changes.
- Use focused edits and remove only code made obsolete by the current change.
- Do not commit, push, or publish unless explicitly requested.
- Use English for code, comments, identifiers, technical documents, commit messages, and PR content.
