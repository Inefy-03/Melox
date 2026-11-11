# Miuix Native Rail Inset Patch

Melox uses a local Android UI patch based on pinned Miuix Glass commit `b7a5f08f`.
`0.9.4.3-melox` is a Melox patch version, not an upstream release. Other Miuix
modules and the UI artifact's transitive dependencies remain at their original
versions.

- Upstream: [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix)
- Base commit: `b7a5f08f` (2026-11-07)
- License: Apache-2.0; see [LICENSE](LICENSE)
- Source changes: [navigation-rail-insets.patch](navigation-rail-insets.patch)
- Original dependency metadata: [upstream-ui-android.pom](upstream-ui-android.pom)
- Bundled AAR SHA-256:
  `8eb07acf3b58dcab63bb627ee09a73d8c91a3009fe4be2701ecafa4e5503368e`

The new stateful `NavigationRail` overload requires `statusBarInsets` and accepts
optional `contentBottomInsets`. The top input replaces the existing inner
content column's top inset. The bottom input reserves and consumes viewport
space before scrolling, avoiding duplicate native navigation-bar spacing. Both
apply when default inset handling is enabled. Existing overloads keep their
original JVM signatures and behavior. The native background, divider, expand
toggle and start-cutout handling are unchanged. Melox passes
`WindowInsets.statusBarsIgnoringVisibility` and a bottom inset ending at the
mini-player surface top; it adds no outer rail padding/background.

The source patch also documents the API in English/Chinese and exercises the
new overload in upstream demo/example code with the original inset behavior.

## Rebuild

Requirements: Python 3, Git, JDK 21 and the Android SDK/toolchain configured for
Melox. Use an upstream clone containing the pinned commit:

```sh
python3 third_party/miuix/rebuild.py /path/to/miuix
```

The script reads that clone without changing it, exports the pinned source to
a temporary directory, verifies the original rail source hash, applies the
patch, builds the Android AAR and updates this directory's local Maven artifact.
It copies only `sdk.dir` from Melox's local configuration when present. The
isolated build omits demo/baseline-profile projects and their profile-generation
dependency; it does not alter library implementation or dependency versions.
No remote publication is performed. Ordinary Melox builds consume the bundled
AAR and do not require the upstream checkout or a rebuild.

## Compatibility

The source build uses Gradle 9.9 from the upstream checkout, with Compose
1.12.0, Kotlin 2.4.20 and AGP 9.4.1 pinned to Melox's toolchain. The existing
rail source patch is applied without the obsolete upstream demo/doc hunks.
The backport adds the upstream overlay surface hooks required by Glass.
ThemeController, MiuixTheme and Android DynamicColors are retained from 0.9.4
alongside material-color-utilities 5.0.1 to preserve existing palette behavior.
Other Miuix modules remain at 0.9.4. App compilation and tests are required
after rebuilding; device checks are needed for rendering and gestures.

The original 0.9.4 OverlayDialog JVM entry point is retained through
`overlay-dialog-binary-compat.patch`, because published preference popups
still reference it. `verify_binary_compat.py BASELINE_AAR CANDIDATE_AAR
DEPENDENCY_AAR...` checks the retained modules for missing UI method calls.
Exclude the replaced UI artifact itself from the dependency arguments.

The bundled Glass top bar uses statusBarsIgnoringVisibility when measuring its
progressive band. Melox reserves that same inset on foreground content, so both
background modes include the hidden status-bar region. Navigation icons render
at 25 dp within the upstream 28 dp slots. App adapters preserve custom/no-feedback
icon controls and use the upstream example's 8 dp spacing for grouped Glass controls.

The app BackIcon adapter includes the pinned miuix-glass-icons Regular
ChevronBackward path with its original license notice; OS3 uses the existing Back icon.

GlassIconDropdownMenu and GlassTransformPopup accept an optional coordinateOrigin
(default zero) for hosting in Miuix PopupLayout. Anchor and icon bounds are converted
to host coordinates, as in upstream GlassDropdownPreference; secondary menus use
the same conversion. App menu anchors mark only the icon as transform content.

The blur artifact `miuix-blur-android:0.9.4.1-melox` is built from the original
v0.9.4 blur sources. `progressive-blur-downsample.patch` caps both progressive
sampling branches at exponent 3 (1/8 resolution) without reducing blur radii or
changing ordinary blur. `rebuild.py` reproduces both local UI and blur AARs.
GlassTransformPopup reports dismissal only after all transition channels settle;
menu hosts start the morph after PopupLayout's asynchronous mount. GlassIconButton
adds an enabled argument so the independent navigation search preserves disabled
input behavior. Search uses the pinned Regular Glass icon path.

Melox adapts the pinned example's GlassSearchOverlay header into GlassSearchHeader:
its 44 dp capsule, 400 ms search curve, cancel motion and Regular Glass search/close
vectors are retained. Query callbacks and existing result lists stay page-owned;
fullscreen mode replaces the ordinary header and bottom chrome. This example code
is not a public miuix-glass library search API. Original copyright notices remain.

GlassNavigationBarLayout animates trailing-action occupancy with GlassMotion.navShowHide,
retaining the outgoing slot through scale/fade exit and blocking its input. Capsule
width and weighted icon spacing follow that same progress. The search header uses
the upstream window-level Back handling; ordinary page navigation is suspended while
search is open. The original top bar uses the example's fade, scale and blur helpers.

NavigationBar behavior is synchronized with upstream commit
`ca674992c9645395cdb0d5109d66504ff53d018c` (2026-11-10). The navigation
icon slot is 25 dp without internal padding. Dynamic themes tint the selected
content and indicator with primary. Indicator edges animate in pixels, including
capsule width changes. Hit regions are frozen for each gesture and pointer deltas
exclude capsule placement motion. Release settles against the current geometry.
The trailing action releases retained content after exit and blocks focus and
pointer input during exit. Existing Glass material types and Melox's leading
action, variable-width destinations and shared indicator adapters are retained.

Bottom playback/navigation remain composed through search mode, as in the Glass
example. Both fade with the example's rememberGlassSearchBarAlpha curve driven by
the search-open target; input stays blocked through exit. Their state, backdrop
layers and layout no longer restart at the end of search dismissal.

Trailing-action scale/alpha and the combined bottom-chrome fade reserve the existing
popupShadowOutsets(GlassShadows.Regular) drawing halo. This prevents alpha offscreen
buffers from clipping shadows while keeping measured width and spacing unchanged.

`miuix-nav-android:0.9.4.1-melox` retains v0.9.4 source/dependencies and adds the
opt-in LocalSingleStepRootPop. A root bulk pop projects the native shared driver
onto the original top/root pair, including visibility, lifecycle and unloading;
intermediate removed entries leave immediately. The app currently leaves this
projection disabled and uses native stack transitions. OS4 landscape sidebar
switches use the zero-duration transition override; hierarchical Back uses the
configured normal transition. Rebuild runs the pure
NavPresentationMathTest suite, including multi-level pop projection and final-frame
retention; no UI test window, emulator or device is started.

The app's vertical iOS-like rail wraps only the external scene backdrop with a
90-degree inverse-sampling correction. Internal icon/selection recordings remain in
control-local coordinates. Its full-page scene capture excludes the overlaid rail
and player, includes their background footprint, and does not reduce page width.
Page content/top controls receive a safe inset instead; stable system insets keep
rail position independent of status-bar visibility.

GlassTopAppBar accepts optional overlapDistance state. Melox supplies visible-page
list/grid position gated by canScrollBackward, as in the official GlassPage, sharing it across normal material,
progressive blur and shadow ramps. The original ScrollBehavior fallback remains for
other callers. This avoids contentOffset drift/reset hiding a still-overlapped bar.

The page scroll-position provider is shared by OS3 and OS4. OS3's progressive band
uses the same rememberTopBarOverlapDistance signal as GlassTopAppBar, including
rebound and restored scroll positions; OS3's original header and normal material
remain unchanged.

OS3 and OS4 progressive bands share progressiveBandHeight: actual measured bar height
(including tab rows and inline search) plus the original collapsed-band fade reach.
Changing bottom content moves the blur edge by the same amount without stretching
the fade overhang or changing the reserved hidden-status-bar geometry.

### Compact glass top bars

`GlassTopAppBar(compact = true)` retains its glass material/background host but
renders an unclipped foreground using native small-bar dimensions. Melox uses this for wide OS4
layouts, including secondary pages, so scrolling cannot reveal a large-title
region. The default remains the existing expandable foreground.

`GlassNavigationIndicator` is the selected capsule extracted from
`GlassNavigationBar` without changing its fill, clipping or press springs. OS4
sidebar and top navigation reuse it, preserving the bottom bar's selected look.

The OS4 landscape capsule uses `GlassNavigationBar` with a leading action region
and labels-only destinations. Hit testing, drag bounds and indicator coordinates
exclude that leading region; default bottom-bar geometry remains unchanged.
`GlassNavigationIndicator(scaleOnPress = false)` lets sidebar rows retain the
same fill feedback without press scaling. The switch uses the existing Sidebar vector.

The compact foreground reserves system status space once and deliberately omits
SmallTopAppBarLayout's clipToBounds so Glass control shadows draw beyond the
compact layout and pins scrolling.

Landscape destinations may supply measured widths. Position/index conversion is
shared by drag hit testing and animated indicator edges. Indicator bounds accept
independent leading/trailing insets so an embedded switch never reserves trailing
space. Bottom navigation keeps equal slots and its original symmetric insets.
The compact Glass bar retains its original 52 dp layout and 50 dp control-center
band. Emulator-derived offsets and size adjustments were reverted.

Wide progressive top blur captures the full page before the sidebar viewport inset,
so its sampling origin agrees with the full-width bar. The top switch uses a
34 dp circular GlassIconButton, matching the selection indicator's height.

## OS4 App Icons

The app vendors the used Regular vectors from `miuix-glass-icons` at upstream
commit `797835f92a6aef3ce46a3bd1969d9d4f3a25b3bb`. Their Apache-2.0 source headers
are retained in `glass/src/com/melox/player/ui/icon`. The app-side Icon adapter
selects these vectors for OS4. Home uses the original Miuix vector, RecordingTape
represents sidebar playlists, and AiScan represents albums and album empty states.

The UI patch `top-app-bar-state-observation.patch` makes the collapse limit
snapshot-observable. Native cached title visibility now refreshes when rotation
or title measurement changes the limit without changing the current offset.
The app regression test reproduces the stale derived visibility with the prior AAR.

The navigation indicator draws the initial selected destination directly across
startup width measurements and trailing-action changes. Both edges snap to each
new startup geometry. Selection springs become active when the selected
destination changes, retaining their normal width-change and drag behavior.
