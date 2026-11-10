# Miuix Glass source integration

- Upstream: https://github.com/compose-miuix-ui/miuix
- Pinned commit: b7a5f08f (2026-11-07, OS4 glass material, PR #423).
- License: Apache-2.0; see ../LICENSE. Original source headers are retained.
- Scope: Glass material, top bars, buttons, dropdowns, menus, dialogs and bottom sheets.
- Sources are copied from miuix-glass/src/commonMain/kotlin at the pinned commit.
  android-api-guards.patch adds explicit Android API 33 guards to the four
  shader drawing functions so lint can verify the API 28 fallback path.
  Melox adds 2 dp padding inside each original 28 dp icon slot for 24 dp
  visible icons, preserving navigation height and interaction geometry.

The app compiles the Glass sources against the pinned 0.9.4.3-melox UI backport.
The rail inset patch is retained. The backport is compiled with Melox's Compose
1.12.0, Kotlin 2.4.20 and AGP 9.4.1 versions. Other Miuix modules remain 0.9.4. The existing theme/palette implementation
and material-color-utilities 5.0.1 are retained.
GlassDropdownPreference adds per-item enablement to retain disabled app choices.
GlassPopup and GlassIconDropdownMenu expose animation-completion callbacks so
the app popup host stays mounted through the actual exit animation.
The upstream Android artifact requires API 33. Melox retains minSdk 28 by
disabling OS4 and iOS-like below API 33 and resolving persisted unsupported
choices to OS3. On supported versions, disabling
blur passes a null backdrop and uses the upstream opaque navigation fallback.
Runtime shaders are only created behind upstream capability checks.
Device validation is required for older Android releases and glass rendering.
The upstream checkout is not required for normal Melox builds.
