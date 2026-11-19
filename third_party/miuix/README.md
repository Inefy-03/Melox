# Miuix Android Integration

Melox follows [Miuix main at `08d52d45`](https://github.com/compose-miuix-ui/miuix/commit/08d52d455bf1d62c45e979ec75b47f6b5dc377f3),
covering [the 28 commits after `v0.9.5-beta01`](https://github.com/compose-miuix-ui/miuix/compare/v0.9.5-beta01...08d52d455bf1d62c45e979ec75b47f6b5dc377f3).
UI, Blur and Preference use the bundled Android AARs at `0.9.5-beta01` from the
local Maven repository.
Other Miuix Android modules retain the Beta 01 release artifacts.

The current snapshot includes the upstream top-bar title alignment and collapsed
Glass rail avoidance, plus horizontal cutout and navigation-bar insets for the
bottom bars. Adaptive layout uses the measured root constraints so the window
size updates on Android 10 rotation.

- License [Apache-2.0](LICENSE)
- Source patch [navigation-rail-insets.patch](navigation-rail-insets.patch)
- Vendored Glass source hashes [portrait-glass-sources.json](portrait-glass-sources.json)

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

## Upstream Source Audit (2026-11-18)

Audited upstream [`6d2554a8`](https://github.com/compose-miuix-ui/miuix/commit/6d2554a850b8960e2616c968263ff64beb5ffb03)
after the existing `bf8281e2` pin. This commit updates BuildConfig release metadata:
library version 0.9.4 to 0.9.5, example version 1.0.9 to 1.1.0, and compile SDK
minor version 0 to 2. All 50 selected upstream source hashes and all 50 transformed
local hashes were verified against this commit. The manifest records this audit.


## Rebuild

Requirements: Python 3, Git, JDK 21 and the Android SDK/toolchain configured for
Melox. Use an upstream clone containing the pinned commit:

```sh
python3 third_party/miuix/rebuild.py /path/to/miuix
```

The script reads that clone without changing it, exports the pinned source to
a temporary directory, verifies the original rail source hash, applies the
patch, builds the three Android AARs and updates this directory's local Maven artifacts.
It copies only `sdk.dir` from Melox's local configuration when present.
The isolated build omits demo and baseline-profile generation projects.
The packaged AARs carry the corresponding Beta 01 dependency metadata.
Ordinary Melox builds resolve the bundled modules exclusively from the local
repository. Rebuilds use the upstream version in the Gradle version catalog,
replace existing artifacts at that version, and remove superseded versions
after all active AAR/POM pairs are installed.

## Compatibility

Miuix Beta 01 requires Android compile SDK 37.2. The Melox app targets API 37
with minimum API 28. The original public Beta 01 UI entry points are retained.
`verify_binary_compat.py BASELINE_AAR CANDIDATE_AAR DEPENDENCY_AAR...`
checks dependent AAR references against the patched UI.

The Android app consumes the official Material, Nav and Icons artifacts.
The iOS-like style follows the Beta 01 example's Lens and Vibrancy sources
and uses Material's tilt sensor and Axial stroke model. The navigation body
and mini player use 4 dp blur and a 24 dp lens. The selected capsule applies
the example's press-scaled lens, depth and chromatic dispersion. Melox compiles
the selected Glass component sources for its layout and popup adapters.

The bundled Glass top bar uses statusBarsIgnoringVisibility when measuring its
progressive band. Melox reserves that same inset on foreground content, so both
background modes include the hidden status-bar region. Navigation icons render
at 25 dp within the upstream 28 dp slots. App adapters preserve custom/no-feedback
icon controls and use the upstream example's 8 dp spacing for grouped Glass controls.

The app BackIcon adapter includes the pinned miuix-glass-icons Regular
ChevronBackward path with its original license notice for the unified interface.

Glass popup anchors and submenu rows use window coordinates. Each popup surface
subtracts its host's window origin and intersects the system safe area with the
host pane. App menu anchors mark only the icon as transform content.

The historical blur artifact `miuix-blur-android:0.9.4.1-melox` was built from
the original v0.9.4 blur sources. `progressive-blur-downsample.patch` caps both progressive
sampling branches at exponent 3 (1/8 resolution) without reducing blur radii or
changing ordinary blur. The current rebuild generates the pinned UI, Blur and Preference AARs.
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

## Official Portrait Glass Navigation

OS4 portrait navigation uses the official GlassNavigationBar, layout, indicator,
press/motion helpers, GlassIconButton, and their material/blur dependency chain
from commit `ac0af4671bc9e4489d0960685a46a5b1ffd557da` (2026-11-16).
The 50 selected sources are audited against main at
`189ef10a624d47fc82d68c718f2f53a92cbda11d`, checked on 2026-11-17.
Source and transformed SHA-256 hashes are recorded in
[portrait-glass-sources.json](portrait-glass-sources.json).

These files live under `top.yukonga.miuix.kmp.upstream` to keep portrait dependencies
separate from the existing landscape, toolbar, popup and mini-player backports.
Complete sources use package relocation and bind common expect functions to their
Android actual implementations. Three file-scoped lint annotations preserve the
upstream API-33 effect boundary inside Melox's API-28 module and the default static
shader-cache lifetime. The existing runtime support checks remain unchanged. Supporting top-bar
button defaults/context and popup anchor/surface data are extracted from official
files; those selections are recorded in the manifest. Navigation, rendering and
material algorithms are unchanged.

Os4PortraitPlayerScaffold follows the example's GlassPage integration: native
visibility animation, 54 dp height, 25 dp destination icons, 12 dp action spacing,
and a 54 dp search button with its 28 dp Regular vector and Float shadow.
The page uses 20 dp top spacing and safe-bottom spacing of at least 24 dp
(system bottom inset plus 8 dp when larger). The mini player and the bar share
16 dp horizontal margins inside the horizontal safe area.
The mini player, destination callbacks, search state and full-player translation
remain application-owned. The original 8 dp mini-player bottom padding supplies
the player-to-navigation gap, with no additional Column spacing. Search keeps hidden chrome from accepting input.

Portrait tap/drag previews the indicator and commits once on release; cancellation
restores selection. Native visibility replaces Melox's slide/expand wrapper.
Landscape continues using the existing leading-action and variable-width navigation
with press/drag selection (`selectOnRelease = false`); its sources and styling are
unchanged by the portrait integration.

## Glass Top Controls and Menus

Toolbar controls and the popup chain follow the Miuix Glass demo at commit
`ac0af4671bc9e4489d0960685a46a5b1ffd557da` (2026-11-16). GlassIconButton is
extracted into its upstream source file. Material opacity affects the colour
layers and stroke independently of visibility; unsupported runtime shaders use
the opaque fallback path. Popup anchors retain material opacity separately from
live visibility, and intrinsic panel measurements are cached during transitions.

The app uses the demo's 28 dp Regular toolbar vectors inside the existing 44 dp
buttons and keeps the 8 dp action spacing. Back, Search, Tune, SettingsOutline, More, Add, SelectAll,
Close and Ok vectors are copied from that commit with their original headers.
The sort entry uses Glass Tune; the landscape settings button uses Glass
SettingsOutline at 28 dp inside its 44 dp button. Glass sidebar destination icons
keep the user-requested 28 dp size, and the upper-left toggle stays
24 dp. Row horizontal content padding is 12 dp, aligning the 28 dp row icons
with the upper-left control at a 38 dp center within the panel. The icon-to-label
gap remains 12 dp. Sidebar shortcuts keep RecentTasks,
Item, Contacts and FolderFill. Music, Album, Contacts and FolderFill navigation
vectors are copied from the Regular weight at `3a980f7c` into NavigationIcons.kt,
retaining the official 28 dp canvas and inset. Music and Contacts retain Melox's
solid silhouettes by omitting the upstream interior cutouts. Other screen icon variants remain
unchanged; the sidebar no longer compensates for legacy 24 dp canvases.

All three navigation styles share Glass HomeFill, Music, Album and SettingsFill
vectors for their four primary destinations. OS3 uses the official non-floating
NavigationBar and NavigationRail sizes of 26 dp and 28 dp. Both orientations of
iOS-like navigation keep the demo's 22 dp icons. Glass bottom items use the
25 dp GlassNavigationBar default; Glass sidebar items keep 28 dp by user requirement. The iOS-like
navigation renders the supplied vectors directly. Navigation choices do not
change toolbar icons.

The menu's tick and submenu arrow use the
upstream Basic.Check and Basic.ArrowRight directly, bypassing the app's Glass
icon substitutions. Leading menu symbols retain app-owned icon selection.
Popup sizing remains 200–288 dp wide, with a 24 dp radius and 44 dp minimum rows.

The backport retains the existing material packages, Int backdrop downscale API,
disabled controls, preference-item availability, and close-completion callbacks
for Miuix PopupLayout. Shared preference dropdown callers also report window
bounds, keeping their anchors compatible with the updated popup surface.
The landscape search adapter retains its prior opacity contract through the
existing transform helper. Landscape sidebar/top navigation hosts, selection and
motion are unchanged; the sidebar icon slot follows its icon size plus the 12 dp gap.

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

## Unified application interface (2026-11-17)

Application Glass adapters now live in ui/component without a theme switch.
Navigation retains Glass, Original, and iOS-like as three separate choices.
The app targets API 37 with minimum API 28. Glass layout stays active without
runtime shaders, using the component opaque fallback and resting segmented tabs.
The bundled portrait source references match upstream commit 189ef10a;
existing UI/blur/nav patches and palette behavior are retained.

## Glass Source Synchronization (2026-11-17)

Audited all six commits after `3a980f7c`, through
[`63d51cdf`](https://github.com/compose-miuix-ui/miuix/commit/63d51cdfe18488b18027b791a2e09799fcdd6665).
Used implementations receive the segmented-tab drag/morph interaction, title blur,
fallback band gradient and icon-button material-state optimizations. Both the
legacy material adapter and the relocated portrait IconButton are synchronized.
The native Glass rail badge changes, new floating components and iOS Scaffold fix
do not enter Melox's active Android implementation.

`top-app-bar-glass-sync.patch` updates the basic title layout required by the
Glass top bar. The original public BlurTopAppBar signature is retained for binary
callers. UI patch version is `0.9.4.4-melox`. Theme and rail patches still apply.
The fallback band overhang changes from 28 to 36 dp, peak alpha from 1 to 0.9,
and title blur from 12 to 8 dp, with compact-title blur capped at 4 dp. The
inner fade follows the official 36 dp value, giving a total fade height of 72 dp.

## Additional Upstream Blur Sync (2026-11-17)

The relocated upstream blur dependency chain is synchronized through
`d45ca1813089b38ba8d6b4daf4a2949eefa52dbe`, covering nine additional commits
published during the sync. It includes the two-level progressive stack, normalized
gradient uniforms, Android ramp blend/unpremultiplication fusion, sharp-band and
downscale optimizations, center/color/noise sampling changes and the 100 dp maximum
blur radius. Android actual functions remain bound directly in the app namespace.
The retained 0.9.4 compatibility blur artifact has its separate pinned source patch.

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
