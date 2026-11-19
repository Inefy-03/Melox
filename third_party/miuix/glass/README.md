# Miuix Glass source integration

## Post-Beta 01 synchronization (2026-11-19)

Glass navigation uses the pinned upstream `GlassNavigationBar` in portrait and
`GlassNavigationRail` in landscape. The rail owns both the 250 dp sidebar and
its 44 dp collapsed top capsule. Melox provides four primary destinations,
including Settings with the shared SettingsFill icon, and its existing library
and playlist shortcuts below the divider. An optional parent index identifies
the primary destination to highlight while a shortcut is selected and the rail
is collapsed.

Both orientations use the upstream `GlassSearchOverlay`. The original page
and its toolbar follow the Glass Component demo's fade and transition. Scrolling
the results drives the search header band, as in IconPage. Wide search clears
the expanded sidebar or the collapsed capsule using the rail's morph progress.
The shell bottom navigation and mini player remain visible above search.
The search field and cancel button follow the Icon page's scroll-triggered
transition from solid fill to glass. Melox supplies query and result state,
localized action descriptions, and the page header gradient and blur settings.

The relocated source manifest records the 52 selected files at `08d52d45`,
including the latest progressive blur renderer and dynamic-color navigation
materials. The existing legacy Glass adapters also receive the upstream
dynamic colors and lazy dropdown anchor tracking.

## Shared blur synchronization (2026-11-17)

The prior upstream audit was pinned to `6d2554a850b8960e2616c968263ff64beb5ffb03` (2026-11-18).
All 50 selected upstream source hashes match this revision. Blur and material
implementation sources are identical to the previously audited `189ef10a` pin.

Legacy Glass buttons, top bars, popup menus, dialogs and bottom sheets now render
through the same relocated blur pipeline as navigation and the mini player.
BackdropBridge preserves existing capture ownership and forwards fractional
downsampling between the official Beta blur API and the vendored Glass renderer. Glass uses value-equal pipelines and the
current ordered-color shader. Progressive top bars and page/card texture blur
also use the current TextureEffect implementation, including sharp coverage,
premultiplied intermediate passes, normalized sampling and noise handling.

Locate-current-track buttons use the navigation/mini-player material and stroke,
including its 20 dp blur. Their list capture includes the fixed wallpaper with
explicit coordinates. Exit hit interception and page-bound placement remain
application-owned.

- Upstream: https://github.com/compose-miuix-ui/miuix
- Pinned commit: b7a5f08f (2026-11-07, OS4 glass material, PR #423).
- License: Apache-2.0; see ../LICENSE. Original source headers are retained.
- Scope: Glass material, top bars, buttons, dropdowns, menus, dialogs and bottom sheets.
- Sources are copied from miuix-glass/src/commonMain/kotlin at the pinned commit.
  android-api-guards.patch adds explicit Android API 33 guards to the four
  shader drawing functions so lint can verify the API 28 fallback path.
  Melox adds 2 dp padding inside each original 28 dp icon slot for 24 dp
  visible icons, preserving navigation height and interaction geometry.

The app compiles the Glass sources alongside Miuix `0.9.5-beta01` and uses
the UI patch `0.9.5-beta01-melox` for native rail insets and observable top-bar
collapse limits. The official Blur and Material packages expose float recording
scales and shader support. The backdrop adapter forwards the scale directly.
The iOS-like bottom bar follows the Beta 01 example's Lens and Vibrancy
sources. Its mini player shares the example's navigation body material.
GlassDropdownPreference adds per-item enablement to retain disabled app choices.
GlassPopup and GlassIconDropdownMenu expose animation-completion callbacks so
the app popup host stays mounted through the actual exit animation.
Melox uses the Glass interface on every supported Android version with minSdk 28.
Glass layout and interaction remain active when runtime shaders are unavailable.
Null backdrops select opaque surfaces. glassPanel also guards runtime support,
and segmented tabs accept null backdrops and retain their resting track without
shader capture. iOS-like navigation retains its API 33 gate and Original fallback.
Runtime shaders are only created behind upstream capability checks.
Device validation is required for older Android releases and glass rendering.
The upstream checkout is not required for normal Melox builds.

The non-progressive top-app-bar band is adapted from upstream commit
`63d51cdfe18488b18027b791a2e09799fcdd6665`: the quintic gradient with
0.9 peak alpha, 36 dp overhang and the official 36 dp inner fade replace
the legacy scaled gradient.
The official foreground-first Layout also measures the bottom slot before the
progressive band, whose height no longer follows the expanded title.
Scroll/background coupling follows current upstream: list canScrollBackward
controls floating; visualContentOffset controls normal/progressive alpha and
elastic material. Melox forwards the native overscroll offset to visualContentOffset alongside
its lazy list position.

Custom wallpaper rendering follows the [GlassPage Wallpaper example](https://github.com/compose-miuix-ui/miuix/blob/3a980f7c509c62c6d315f71ac28b40ecc8d31f3b/example/shared/src/commonMain/kotlin/GlassPage.kt):
Compose Image with ContentScale.Crop, captured separately with layerBackdrop.
Melox retains its dim overlay. Each page records wallpaper plus controls for its
top bar, while cards sample the separate wallpaper-only layer. The top-bar slot
does not repaint wallpaper. Root pages additionally sample the existing fixed-window wallpaper while their
content pages and rebounds. Secondary routes keep page-local wallpaper sampling.

The used segmented tab row includes upstream horizontal dragging, spring-driven
indicator morphing, press feedback and keyboard/accessibility activation as of
`63d51cdf`. IconButton shares the upstream material-state and shadow-lambda fixes.
Title rendering uses the synchronized basic layout from the local UI artifact.

## Progressive Blur Follow-up (2026-11-17)

Audited three commits after `d45ca181` through
[`189ef10a`](https://github.com/compose-miuix-ui/miuix/commit/189ef10a624d47fc82d68c718f2f53a92cbda11d).
The used relocated blur chain receives `76504ab0`: native-resolution sharp
coverage, a 50/50 progressive level split, premultiplied intermediate passes,
normalized Gaussian sampling and specialized shader variants for the two axes.
The source manifest records all 50 source and transformed hashes at this pin.
About follows `7188729e` with an always-collapsed top bar. Its header artwork
and title-opacity animation remain page-owned. GlassPage popup hosting is already
represented by Melox's root-Scaffold popup adapters.

The landscape navigation adapter retains its destination-width and leading-action
layout extensions while using the same relocated blur, material, edge stroke and
glassPanel renderer as portrait bottom navigation. Both expanded panel and collapsed
top capsule sample the existing upstream glassBackdrop outside their own surfaces.
Legacy material/stroke Defaults helpers remain available to existing callers.

CopyEditPoint and EditSubtitle retain the upstream Glass Regular paths from
miuix-glass-icons. They use the existing Melox GlassIcons receiver and shared
HyperOS icon builder, with their displayed size owned by the consuming control.

All local Glass vectors follow the upstream 28 dp canvas convention. Legacy
1142.9-unit viewports are normalized to 1333.3833 with 95.2417-unit padding on
each axis; existing paths and fill choices are retained. The HyperOS builder
retains its upstream 1333.3333-unit precision. Display size belongs to callers.
Search, close and navigation-back helpers reuse the shared vector definitions.
