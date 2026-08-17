# Melox Version 1 Technical Specification

## Build Baseline

- Single `:app` Android module.
- Namespace/application ID: `com.melox.player`.
- `minSdk 28`, `compileSdk 37`, `targetSdk 36`.
- Kotlin/Compose compiler `2.4.0`, AGP `9.2.1`, Gradle `9.4.1`.
- Miuix `0.9.3`, Media3 `1.11.0`.

## Layers

### Data

- `MusicRepository` queries MediaStore on `Dispatchers.IO`, reads/writes a versioned `AtomicFile` library snapshot, and never blocks composition.
- `AudioPropertiesReader` follows Lyrico's descriptor-based metadata path:
  open the MediaStore URI read-only, duplicate and detach its file descriptor,
  then use TagLib's average read style to obtain real duration, bitrate, sample
  rate, and channel count without loading artwork. The same scan reads the
  TagLib property map and resolves title, artist, album artist, album, year/date,
  track number, and disc number through Lyrico-compatible aliases. Android 12+
  also reads the platform-reported bits per sample without format guessing. New or
  source-revision-changed files are read sequentially on `Dispatchers.IO`;
  unchanged files reuse snapshot properties.
- `SettingsRepository` exposes `Flow<AppSettings>` backed by Preferences DataStore.
- `SettingsRepository` also persists the default root destination, selected
  Music-library tab, Songs and per-tab sort field/direction, and the Albums
  2-column/3-column style, dynamic-color source, scan-on-launch toggle,
  sub-60-second exclusion toggle, and persisted custom-folder tree URIs.
  Dynamic color source defaults to `PLAYBACK_ARTWORK`; the dropdown lists it
  before `DESKTOP`, while missing artwork falls back to the system palette.
  `PLAYBACK_ARTWORK` is labeled as playing song artwork, uses the cached
  current-cover sample, and falls back to the platform palette when artwork is
  unavailable.
  Legacy stored column `2` and the removed large
  2-column style map to 2-column; stored column `3` maps to 3-column.
  ViewModel initialization resolves the
  first small DataStore value on `Dispatchers.IO` before `setContent`, then
  seeds the settings state with that exact value. Compose keeps immediate
  screen-local values for later edits.
- `PlaybackSnapshotStore` atomically stores the physical queue, source order,
  current index, position, and `PlaybackMode`. Writes are debounced and occur
  on transitions, queue changes, and lifecycle-safe checkpoints.
- Playback mode changes update Media3 repeat state and normalize the queue with
  at most two bulk replacements around the untouched current item, without
  rebuilding or preparing that item. This avoids the quadratic main-thread work
  and event burst of moving every song individually. The service retains the
  selected mode independently while queue
  changes are applied, so mode persistence is not tied to a metadata
  replacement.
  New visible queues inherit the current mode, and Previous/Next uses explicit
  adjacent queue indexes instead of Media3's restart-current-song previous
  behavior.
- Artwork loading shares one size-aware memory/disk cache. Requested pixels are
  normalized into bounded display-size buckets before cache identity is built,
  allowing list, mini-player, full-player, and backdrop consumers to reuse a
  decode when their requested sizes are visually equivalent. Disk-entry access
  timestamps are updated at a throttled cadence rather than on every read.
  Eviction snapshots each file's timestamp and size once before sorting, so
  concurrent cache hits cannot mutate a TimSort comparison key mid-sort.
- Cached artwork thumbnails scale their longest edge into the requested bucket
  without upscaling or changing aspect ratio. Cache schema versioning prevents
  previously center-cropped entries from being reused. Callers choose `Crop`
  or `Fit`; mini/full playback artwork uses `Fit`, while existing library card
  surfaces retain `Crop`.
- Artwork rendering retains the last bitmap while a changed cache key is
  loading. The current mini-player cover may synchronously consult only the
  already-generated small disk-cache entry; extraction remains asynchronous.
  An application-level prefetch effect warms current and adjacent queue covers
  at the full-player bucket. The mini player requests its own bar-sized bucket,
  while the directly composed full player requests the full-player bucket.
  Full-player cover and background consume the same cached bitmap source, but
  only the cover uses the 320 ms track-change progress. The background retains
  the last completed HCT field until the next field is ready, then independently
  interpolates its current color-field state toward the replacement over 640 ms.
  Missing artwork uses fixed `#242424` as either endpoint. Opening and dismissing use a separate
  shared-player overlay that reuses the resolved current bitmap and interpolates
  the measured mini artwork bound and current visible full-artwork content
  bound, including its animated playing/paused internal inset and the
  bitmap's fitted aspect ratio.
- `LyricsRepository` performs bounded local reads on `Dispatchers.IO`. It
  inspects Media3 container metadata for embedded lyric text and queries or
  opens same-name sidecar LRC/TTML candidates without requesting network or
  broad storage access. Parsed documents are cached by content URI, modified
  time, and file size.

### Playback

- `PlaybackService : MediaSessionService` owns the only `ExoPlayer` and `MediaSession`.
- `PlaybackController` owns only a `MediaController` connection and exposes `StateFlow<PlaybackUiState>`.
- Local playback uses Media3 audio focus/noisy-route handling and a playback-scoped local wake lock so screen-off playback remains reliable.
- The service starts decoding the checksummed playback snapshot before exposing
  its media session, publishes the decoded queue without waiting for per-item
  descriptor checks, and then validates URI readability on `Dispatchers.IO`.
  Validation removes invalid/unreadable items while preserving an unchanged
  queue's active item, position, playback state, and mode. A concurrent queue
  mutation wins over validation; an empty validated queue clears the snapshot.
- Media item metadata contains title, artist, album, source URI, and a local track ID when applicable. The app extracts embedded artwork through its shared bounded cache.
- Position persistence is throttled; UI position updates may tick more frequently without writing to disk every frame. Queue shape, current-item, and playback-mode changes bypass the debounce, and task removal performs one final atomic write so reopening cannot fall back to an older queue.
- Alongside the full queue snapshot, the service writes a tiny atomic
  16-item window beginning at the current item when enough following items
  remain, or ending at the queue tail otherwise. `PlaybackController` may
  synchronously read only this bounded preview for first-frame mini-player and
  Queue identity, and `PlaybackService` preloads the same window before opening
  its media session. Full snapshot decoding
  and queue publication remain asynchronous, but they are no longer blocked by
  the slower per-file readability pass.
- Playback-mode changes serialize bulk playlist updates until the expected queue order
  is reported by Media3. This prevents repeated mode taps from interleaving
  queue mutations, while preserving the current media item and position.
- Audio renderer selection follows the automatic decoder policy: the
  platform Media3 audio renderer remains first, and a locally built Media3
  FFmpeg extension renderer matched to the pinned Media3 version is enabled as
  a fallback. Decoder fallback stays enabled for alternate platform decoders.
  Playback errors remain available in `PlaybackUiState` for controller/system
  diagnostics. A newly reported error shows the localized unsupported-audio
  message once as a Toast; the full-player header renders no error text.

### UI

- `MeloxViewModel` publishes library/settings/scan state separately from the
  high-frequency playback state. Album, artist, and folder projections are
  created once per immutable track snapshot on `Dispatchers.Default`; filtered
  and sorted presentation lists are also shared and kept warm outside page
  composition.
- The root composition launches the current platform audio-permission request
  once on entry when access is missing. Its callback records `ScanStatus.Idle`
  after a startup grant and does not query MediaStore. The Settings scan action
  uses the same launcher with a manual-scan source flag, so a grant from that
  path immediately calls `scanMusic()`. Denial records
  `ScanStatus.PermissionRequired`; no lifecycle-resume hook or app-settings
  redirect starts a scan implicitly.
- `ScanStatus.Idle`, `PermissionRequired`, and `Success(0)` all render the
  action-free `No music found` state on Home and Songs. Only `Scanning` renders
  progress; scan controls remain in Settings.
- The root Scan music preference derives its summary from the current library:
  zero tracks use `No songs`, while a populated library uses the localized song
  count. The scan page uses a Miuix `TopAppBar` and connects its `MiuixScrollBehavior`
  to the list so the title transitions between large and small states. Persisted
  switches and custom-folder rows keep screen-local values for immediate Miuix
  feedback, then write through `SettingsRepository`. The folder picker result
  and delete confirmation update that local list before DataStore completes.
  Each stored folder uses a Miuix `BasicComponent` row with the default 16 dp
  card inset, a 24 dp file glyph, and a 16 dp visual gap before its text.
- Root UI is one Miuix theme and a Home/Songs/Music-library/Settings pager.
  Home is index `0`; the root pager's `initialPage` is resolved from the
  synchronously loaded `DefaultHomePage` before the first composition. There is
  no post-composition startup scroll used to apply this preference.
  Home owns a retained recommendation pager over a per-application random
  sample of local tracks whose shared artwork loader resolves a real bitmap.
  It probes and publishes two visible cards first, then resumes background
  selection to its initial five cards and raises the requested count by one
  on each forward page change. The shared selector keeps the seeded artwork
  order and returns only each eligible track's first occurrence; when it has no
  unseen artwork track left, the pager's finite page count ends the carousel.
  The pager uses fixed-width 240 dp cards and
  applies 16 dp only as its outer start/end content padding. The metadata
  presentation is selected directly from the persisted Blur setting. With Blur
  disabled it retains the existing darkened shared-artwork-color bar. With Blur
  enabled it requests the shared 512 px artwork, center-crops and reduces a copy
  to 96 px, applies 1.5x saturation through a color matrix, draws a 5-by-5 mesh
  deformation plus dark overlays, then calls the bundled RenderScript
  replacement Toolkit blur once with radius 25. The derived bitmap uses a
  versioned cache key containing the artwork URI, modified time, and file size,
  and shares the bounded memory/disk cache with ordinary artwork without key
  collisions. The 240 by 310 dp card draws the clear cover to 240 dp, vertically
  mirrors and crops the derived bitmap below it, then uses a
  transparent-to-opaque black gradient as a `DstIn` alpha mask from 160 to
  240 dp. This progressively reveals the darkened reflection over the still
  visible clear cover instead of painting an opaque blur band in the opposite
  direction. Compose scrolling and recomposition perform only cached bitmap
  composition; this path does not use the live
  `BarBlurEffect`. With Blur enabled, the card stays absent until both the shared
  artwork and cached reflection are available; it never renders the color-bar
  fallback during reflection generation. With Blur disabled, it waits only for
  artwork and uses the existing artwork-color bar. Both presentations share the
  color bar's original 70 dp metadata height and bottom-anchored layout: 18 dp
  from the card's left edge and 14 dp from its bottom edge. The pager allocates
  the complete 240-by-310-dp card before applying its separate 14 dp section spacing. Both
  presentations retain enough height for title and artist. The localized Random
  recommendations and Recently
  added headers use `title4` at the default weight with a 28 dp start inset.
  Recommendation playback starts with the selected identity, then the other
  de-duplicated loaded recommendation identities in carousel order, followed by
  remaining scanned tracks ordered for the active playback mode. Any later
  playback-mode change applies that mode to the entire queue and removes this
  temporary recommendation prefix. The root composition retains the random
  seed, resolved track IDs, request flag, completion flag, and recommendation
  page index and whether the current pointer gesture began inside the
  recommendation pager. Root user scrolling is disabled only for such a
  gesture on an interior recommendation page. The first and final recommendation
  pages leave root paging enabled, and gestures outside the recommendation
  bounds never disable it. The recommendation pager disables its own overscroll
  so unconsumed outward movement at the final card is handed to the root pager
  instead of translating the complete Home page.
  These values remain retained even when the root pager disposes the Home page.
  Selection begins on the first
  Home activation, continues if the user leaves mid-load, and probes artwork in
  ordered batches of eight; result order remains the seeded shuffle order.
  Home also derives a Recently added list from the immutable library snapshot:
  it sorts by file modification time descending, normally limits to 20, and
  gives scan-detected IDs priority. Detected additions beyond 20 remain visible;
  fewer additions are followed by the newest non-added tracks to reach 20. The
  section renders static two-item rows using the borderless root Albums
  three-column geometry: 16 dp outer padding, 12 dp gaps, and a 14 dp rounded
  square cover above labels. Its title and artist use matching 6 dp horizontal
  insets with the root Albums three-column text scales and default weight.
  Music library owns a nested three-page Albums/Artists/Folders pager controlled
  by Miuix `TabRow`. Search visibility and query are shared by the three library
  tabs, so changing tabs keeps the same filter while swapping hint, projection,
  and sort controls without closing Search. The nested pager disables user
  swiping so only TabRow clicks switch Albums, Artists, and Folders; root pager
  horizontal swiping stays unambiguous. The TabRow keeps the default Miuix
  outlined-tab visual treatment, with 20 dp side padding, 12 dp item spacing,
  38 dp height, and 13 sp text. Taps use the official Miuix example's
  `animateScrollToPage` pattern, so content transitions to the target page
  while direct pager swiping remains disabled. Its own background becomes
  transparent when Blur is active so the parent top-bar blur remains visible;
  opaque fallback keeps the normal surface. Its top gap is 12 dp when the large title is
  expanded and 0 dp when the small title is collapsed. Library reserves 6 dp
  below the TabRow and gives an open SearchBar another 6 dp top padding, keeping
  the total TabRow-to-Search spacing at 12 dp. Songs SearchBar uses the same
  12 dp expanded-title gap and 0 dp small-title gap. All visible search fields
  use 16 dp horizontal inside margin and the shared generic Search / 搜索 hint.
  The shared search wrapper separates visibility from focus: the first Back
  press hides the IME and clears focus while keeping the field open and
  retaining its query; the second Back press closes it. The wrapper never feeds
  Miuix's collapsed-field empty callback back into the retained query state,
  preventing a focus-loss flash.
- The Album root grid uses 20 dp horizontal content padding. The 2-column style
  uses a 56 dp cover with 6 dp top/bottom/start, 12 dp end card padding, and a
  10 dp cover-to-label gap. The borderless 3-column layout uses 12 dp
  row/column spacing, 14 dp cover corners, and a 6 dp inset shared by title and
  song count. Both Album styles use `body2` title text and explicit 12 sp song
  count text because Miuix has no matching text token.
  Its sort popup uses Miuix dropdown positioning and measures the option set as
  one complete column, keeping `Descending` visible without an initial scroll
  while still placing the popup above the action when the lower viewport is
  too short for all choices. Pass every `DropdownImpl` directly to
  `ListPopupColumn`; an intermediate wrap-content `Column` would shrink the
  selectable area to the text and pull the selected `Ok` icon away from the
  popup's trailing edge.
- Albums, Artists, and Folders attach their lists to one
  `MiuixScrollBehavior`, giving the shared top bar one collapse state across
  pages. One index overlay is owned above the nested pager and dynamically
  targets the selected tab's retained list state. Its top padding is the
  expanded bar plus the measured TabRow/search region and a 4 dp gap exactly
  once. The expanded-bar baseline is captured only after the bottom-content
  region has produced its first measurement, preventing initial double
  counting.
- Artist rows use `headline2` titles and `footnote1` descriptions, with a
  16 dp leading inset and a 12 dp artwork-to-text gap. Folder rows
  use `body1` titles and explicit 12 sp descriptions because Miuix has no
  matching text token, with a 32 dp file glyph, 30 dp start padding, and 20 dp
  gap between glyph and text.
- The root pager disables Compose overscroll and applies Miuix horizontal edge
  spring behavior. The outer root Scaffold paints `MiuixTheme.colorScheme.surface`
  behind that translated pager, preventing the Home and Settings edge springs
  from exposing the transparent window as black in light mode. It retains one
  adjacent page on each side only while Blur is disabled; the blurred path
  avoids recording an extra offscreen full-screen backdrop. Pager-page
  synchronization observes `currentPage` through
  `snapshotFlow` so intermediate page changes do not invalidate the complete
  application composition.
- Theme settings, About, Album detail, and Artist detail use
  the official Miuix `v0.9.3` `NavDisplay` defaults for enter, exit, corner
  clipping, dimming, and predictive back instead of a hand-written translation
  layer.
- Album and Artist detail pages use `SmallTopAppBar` with an empty title; their
  fixed artwork and metadata own the visible album or artist identity inside
  the app bar's `bottomContent`. Album Detail uses the root three-column cover
  width and 14 dp corner radius with a `title3` album name. Artist Detail uses
  a 72 dp cover with a `title3` artist name, then separate 12 sp song-count and
  album-count rows in that order. Both detail pages use the Miuix
  `TabRowWithContour` plus a two-page `HorizontalPager` in the same fixed top-bar
  surface; their Cards and unselected contour backgrounds are transparent while
  Blur is available so the bar backdrop remains visible. Album pages use Songs
  and Participating artists. The latter derives every distinct split artist from
  all album tracks in first-occurrence order, renders the standard artist row,
  and navigates with the matching global artist key. Both detail pagers enable
  direct horizontal swiping as well as TabRow taps because no root pager is
  present on those routes. An Artist Detail opened from an Album Detail stores
  the source album key as its parent context and Back restores that exact album.
  Its Albums tab performs the same Back action, restoring the source Album
  Detail with its prior state instead of replacing it or adding another route.
  Symmetrically, a Participating artists tap from an Album Detail that is a
  child of Artist Detail restores the source Artist Detail with its prior state
  instead of adding another route.
  Album-detail song rows use artist-only descriptions, and artist-detail song
  rows use album-only descriptions. Artist-detail Albums uses the persisted
  Album grid style and its column count.
- Full player is intentionally outside `NavDisplay`. The root remains composed
  while one reversible shared-player progress mounts the full-player overlay.
  Mini-player and full-player roots publish their root-coordinate bounds and
  record independent `GraphicsLayer` content. The transition host draws those
  layers inside one interpolated squircle container and crossfades them on the
  same progress. Their artwork nodes separately publish root-coordinate bounds
  for one shared bitmap. Vertical drag distance and spring settlement publish
  through the same observable progress; release cannot leave the artwork layer
  subscribed only to the last pointer-driven value. Release velocity is
  normalized by the full-player height before a critically damped spring
  settles to the direction-selected endpoint. Back and cancelled gestures
  reverse the active path without swapping the root tree.
  A vertical gesture records whether it began on the mini-player or full-player
  host and keeps that host interactive until pointer release, even when the
  reversible progress crosses the content-handoff threshold. This prevents a
  mid-gesture z-order change from cancelling the pointer stream and freezing
  the shared background or artwork at an intermediate frame.
- During an active shared-player transition, mini-player recording excludes
  the surface itself and retains only the bar content. The shared container
  re-applies the existing `miniPlayerSurface` with the current
  `MiniPlayerChrome` backdrop, blur-active, liquid-glass-active, dark/light,
  outline, and fallback values at the interpolated container size. A highlight
  is supplied only by the liquid-glass path; the ordinary floating blur path
  passes `null` to the Miuix blur highlight parameter.
  This keeps Miuix's live backdrop sampling while the bar height changes.
  Content handoff reaches the opaque full-player layer at `p = 0.25`; at that
  same boundary the mini-player surface, backdrop blur, and refraction stop
  drawing. No external blur or refraction parameters are introduced. This is a
  rendering-cost optimization only, so the liquid-glass highlight, floating
  shadow, and visible handoff remain unchanged.
- The shared artwork path uses uniform scale and translation only. Its opening
  direction is rightward and upward from the mini-player cover into the page
  cover, with interpolated rounded corners and no rotation, skew, or
  narrow-top/wide-bottom deformation. Its `GraphicsLayer` reads the same shared
  progress used by the container so both pointer movement and the release spring
  update the cover continuously. The cover-page path remains fully visible.
  Lyrics has no shared cover element: the selected pager page, not the pending
  open/close target, disables the shared overlay for the complete downward drag
  and spring settlement. The measured offscreen artwork bound remains a fallback
  guard and applies no partial visibility fade. Below the `p = 0.25` handoff, the
  mini-layer keeps its own cover with the title and buttons at their original
  local offsets from the mini-player's top edge; it does not scale those elements
  with the expanding container and restores the complete bar together during
  close. The selected artwork/Lyrics page is retained by the outer player host
  while the full-player composition is unmounted and while tracks change, but it
  is not persisted across a new application process. Reopening on Lyrics reverses
  this same complete-bar transition and never creates the independent shared-cover
  trajectory.
- `PlaybackBackgroundStyle` persists `BLURRED_ARTWORK` or `FLOWING_COLORS`, with
  blurred artwork as the default. The Theme settings screen updates the value
  immediately. The preference is hosted in its own card immediately below the
  Appearance card without a second `SmallTitle`.
- Blurred playback backgrounds load one 128 px center-cropped ARGB_8888 source. It is
  processed before blur: saturation is
  raised to `1.5f`, a `0x4D000000` black layer is drawn with `OVERLAY`, and a
  second `0x4D000000` black layer is drawn normally. No additional uniform or
  vertical-gradient black layer is composited by the Compose renderer. It is
  blurred once off the main thread with a three-pass box approximation calibrated
  to RenderScript's radius-25 sigma mapping and stored through the existing
  bounded artwork derivative cache; the
  playback path does not call the legacy native Toolkit AAR. The default renderer
  applies one fitted frame before animation and interpolates between overscanned
  random crop states over 12 seconds with AccelerateDecelerate timing. Animation
  advances only while the settled player is resumed and playing, so pause keeps
  the current frame and can never expose an identity-matrix top-left image. The
  current item is prefetched before adjacent queue items, including its 128 px
  source and cached blur derivative. Completed layers also enter a
  bounded memory cache, allowing a prefetched blur to be selected synchronously
  on the first composed frame. The ordinary blurred renderer never substitutes
  its clear foreground artwork while a derivative resolves. Rapid track changes retain
  the completed background until the next pair is ready, then crossfade without
  exposing a placeholder. The foreground cover retains the shared mini/full
  artwork overlay. At `p = 1`, the shared container uses the exact measured
  full-player bounds for its surface, clip, and recorded content during the
  screen-corner settlement phase, so the handoff cannot expose a 1 dp bottom gap
  before the in-place layer takes over. Lyrics uses the same background
  state and does not add another scale transform.
- Flowing-color full-player processing runs on `Dispatchers.Default` against the
  already cached artwork bitmap. The bitmap is bilinearly reduced to 8 by 8;
  each pixel is converted through MaterialKolor HCT, keeps its source hue, caps
  realized chroma at 32, and fixes tone to 64 for light theme or 32 for dark
  theme. The resulting ARGB_8888 field supplies four quadrant paths. One
  reusable 4-by-4 output bitmap starts as source coordinates `x=2..5,
  y=2..5`. Each output pixel follows the matching local position through the
  center region, horizontal 2-pixel offset, outer-corner 2-pixel offset, and
  vertical 2-pixel offset. Top-left and bottom-right use clockwise 24-second
  then 18-second laps; top-right and bottom-left use counterclockwise 18-second
  then 24-second laps. Colors interpolate between each adjacent pixel center,
  and the two-lap timing pair repeats every 42 seconds. The bitmap is updated in
  place and wrapped once by a Compose `BitmapPainter` with `FilterQuality.Low`.
  A standard `Image` draws it with `ContentScale.Crop`, using
  Compose/Skia bilinear sampling instead of stretching it through a native
  Canvas destination rectangle. Continuous ARGB interpolation handles temporal
  transitions while painter filtering smooths adjacent output pixels. A separate
  18-second phase rotates the complete 4-by-4 field through clockwise
  quarter-turn source-coordinate mappings. Both the field and each pixel's local
  coordinate rotate together; consecutive quarter-turn fields interpolate per
  pixel. This preserves spatial adjacency at 0/90/180/270/360 degrees and
  removes the cross-shaped seam created by moving unrotated quadrant blocks;
  it never transforms the bitmap geometry. There is no `graphicsLayer` rotation,
  animated scale, translation, second layer, or X/Y perspective transform. Both
  phases run only for the settled resumed player while `PlaybackUiState.isPlaying`
  is true. Pausing preserves the current phases for the next resume. Missing artwork uses
  fixed `#242424` in both themes; the last completed non-null field
  remains visible while a replacement is computed. Once ready, the replacement
  uses an independent 640 ms `FastOutSlowInEasing` per-pixel color interpolation.
  An interrupted transition snapshots its current interpolated 8-by-8 field as
  the next starting point, so rapid track changes do not jump or restart from a
  stale cover field.
- Secondary navigation keeps one stable Miuix scene state and `NavDisplay`.
  The persisted predictive-back setting only enables either the official
  `NavigationBackHandler` or the ordinary back handler; it never swaps two
  navigation hosts that could register the same saveable entry twice.
- The player scaffold stays outside `NavDisplay`. Its mini player remains
  composed for root, secondary, and tertiary routes; only the root navigation
  bar uses vertical Miuix enter/exit motion. Route content consumes the current
  mini-player bottom inset so no list or control is covered. Retained
  navigation entries read the latest inset through stable state instead of
  capturing the value from their first composition. Non-root route hosts remain
  full-screen so each route's visual background and Navigation3 transition
  continue behind the mini player and system-bar inset. Only scrollable or
  interactive content receives the live bottom inset. The player scaffold's
  `layerBackdrop` records that full route frame, so the mini player samples the
  active secondary or tertiary page instead of a retained root frame or an
  empty black/white theme surface.
- Predictive back follows the persisted setting and defaults to enabled.
- Overlays for track actions, details, and queue stay below an owning Miuix `Scaffold`.
- Queue uses the official `OverlayBottomSheet` directly and sizes its content
  from the queue count, capped by the sheet's remaining maximum height before
  row-height multiplication so a large restored queue cannot create an
  unrepresentable Compose constraint. It has the
  library-owned title row, plus a leading Close action and trailing Clear action
  whose visible glyph ends 28 dp from the screen edge. Queue entries are direct
  full-width `BasicComponent` rows with no wrapping card, 44 dp artwork, 24 dp
  start padding, 16 dp end padding, and 12 dp top/bottom padding without an extra inter-item
  spacer. Their only trailing control is the per-item remove action, whose
  circle-minus glyph shares the Clear icon's 28 dp visual right edge; quality
  badges, duration, and More are excluded. The current row sets
  `BasicComponent.holdDownState` so Miuix's indication supplies the persistent
  selection effect; no custom colored rounded selection surface is used.
  Its lazy state anchors the current item when the sheet opens. Custom row text
  uses the song-list `headline2` title and `footnote1` artist styles, both with
  one-line ellipsis.
  The active entry relies on the persistent Miuix hold-down indication rather
  than layering a custom selection surface.
  Clear confirmation is an `OverlayDialog`. The list viewport draws through the
  transparent navigation-bar region. Navigation-bar inset plus 12 dp is applied
  as `LazyColumn` content padding, so the last row remains reachable without a
  separate fixed footer background. The item remove action uses a circle-minus
  icon derived from AddCircle.
- `TrackActionsOverlay` omits a sheet title, uses the default Miuix sheet margin,
  reuses the song-row metadata composition without
  trailing duration/actions, gives the summary item the same `secondaryContainer`
  background as the option card, enlarges the summary artwork to 56 dp with
  12 dp left/top/bottom artwork-side padding, and renders all actions as one
  `Card` of `BasicComponent` rows. Add to queue uses a 20 dp icon shifted
  1 dp right with 2 dp more text gap. Album and Artist labels use a one-line
  ellipsis. A single artist resolves directly to its `ArtistGroup`; multiple
  artists open a titled Miuix sheet with the same rows as the artist library,
  12 dp spacing on every side of the artwork, and no trailing navigation icon,
  then navigate from the selected real group. Song-information rows copy their
  rendered trailing value to the platform
  clipboard on click. Two Edit-icon actions before Song information use the
  Halcyon-compatible external-editor contract. The More-sheet labels remain
  visible, but the old Melox-specific candidate implementation is removed. For
  local files, derive a FileProvider URI when the indexed file is readable and
  otherwise use the MediaStore URI. Lyrico receives one
  `com.lonx.lyrico.action.EDIT_TAG` intent targeted to `com.lonx.lyrico`, with
  the selected URI as data and stream plus Halcyon's metadata aliases. Music Tag
  Editor receives Halcyon's ordered candidates: explicit path-based
  `ACTION_EDIT` to `com.xjcheng.musictageditor.SongDetailActivity`, then
  explicit/package `ACTION_VIEW`, then package `ACTION_SEND`; the URI, path, and
  displayed metadata extras match that implementation. Each candidate carries
  read/write flags and `ClipData`. Before direct `Context.startActivity`, grant
  both data and stream URIs to the resolved package and add Halcyon's
  `NEW_TASK|EXCLUDE_FROM_RECENTS` flags. After returning to Melox, refresh the
  selected track without rebuilding or interrupting playback.
  Manifest package visibility covers both applications and the Lyrico action.
  If no candidate resolves, Melox shows the application-specific localized
  not-found Toast without dismissing the action sheet.
  Resuming Melox after a successful external launch triggers one targeted
  repository refresh and a lyrics revision signal. Library and UI playback
  metadata are updated without
  replacing Media3 items, changing the queue, calling `prepare()`, or seeking.
  Song information uses a separate titled official `OverlayBottomSheet` with a
  leading Close action. Its identity
  and technical metadata are separate option cards, including a localized
  unavailable value only when Android does not expose the file's bit depth. The
  More summary stays directly after the standard Miuix header rather than
  offset over it. The More and participating-artists scroll regions retain
  Miuix vertical overscroll and keep the sheet's nested-scroll connection
  enabled so a downward drag can dismiss the overlay.
- Song rows use `headline2` titles and `footnote1` descriptions. They implement
  the More target as a 36 dp semantic button with no indication.
  `TrackActionsOverlay` is the single action-sheet implementation; it retains
  the last non-null track and song-info mode while `show` becomes false, then
  clears both from `OverlayBottomSheet.onDismissFinished`.
- Theme settings and collapsed About bars record page content with
  `layerBackdrop` and render the bar through the shared Miuix `textureBlur`
  wrapper when Blur is enabled. Unsupported or disabled blur resolves to an
  opaque Miuix surface without changing layout.
- Floating navigation always renders the Miuix example's iOS-like bar. It
  records the page backdrop only for enabled, supported blur or liquid-glass
  effects; otherwise the same bar renders an opaque `surfaceContainer`.
  Root-pager offsets and a bounded route-transition refresh signal invalidate
  the recorded layer while child graphics layers move, so the persistent mini
  player samples the current page rather than a stale transition frame.
- About ports the official Miuix example's OS3 effect-background and
  scroll-header composition. API 33+ animates the background while API 28-32
  renders the same complete layout on the Miuix surface without constructing a
  runtime shader. To keep the first navigation frame responsive, the OS3
  background and decorative texture-blur layers mount one short deferred beat
  after the page content; version text reads from `BuildConfig` instead of a
  first-entry package-manager query. Version, app name, and monochrome icon use
  the same sequential scroll-progress fade/scale intervals as the example.
  Matching the official implementation, one page-level `LayerBackdrop` records
  content for the collapsed bar while a separate background-level backdrop
  supplies the decorative header and option-card texture blur. The Developer
  card uses the official About-page light/dark blend tokens with a transparent
  container; unsupported or disabled blur falls back to `surfaceContainer`. A
  backdrop must never sample itself. A single Developer preference opens
  `https://github.com/Inefy-03` through the platform URI handler.
- Mini player is always composed and never shows a progress track. Empty state
  uses a non-clickable placeholder. The normal shape is the existing rounded
  card; floating styles use a pill whose backdrop is selected by the same
  blur/liquid-glass capability resolver as the navigation bar. Metadata owns a
  clipped horizontal drag layer with edge alpha masks. Previous/Next text uses
  a density conversion from 12 sp and remains that distance behind the
  translated metadata boundary throughout a swipe. A system
  `GestureThresholdActivate` haptic fires whenever the drag enters an eligible
  Previous or Next commit region and a different queue item is available. The
  active threshold direction resets below the commit boundary, so re-entering
  the same direction or reversing into the opposite direction fires again.
- The normal mini player consumes the same surface color and recorded backdrop
  as the navigation bar. It omits glass highlights, uses the navigation
  divider's reduced monochrome stroke, and has 6 dp outer side/gap spacing with
  10 dp artwork padding. Reducing the artwork-to-metadata viewport spacer from
  10 dp to 6 dp moves its mask boundary 4 dp left; a 4 dp internal metadata
  inset preserves the resting title and artist position.
- Floating navigation computes one shared pill width for both the 64 dp mini
  player and 64 dp navigation bar. Their gap is 8 dp. Both consume the exact
  same backdrop and shared Miuix-style shadow modifier. In ordinary floating
  mode, `textureBlur` receives no highlight and Blur changes only the backdrop
  treatment. Liquid glass additionally supplies the shared lens parameters and
  gravity-following highlight object;
  the mini-player draw result is finally clipped to its larger pill shape so
  blur cannot square off its corners. The navigation container uses the local
  Miuix example's `dropShadow`: black, 10 dp radius, 20% alpha in dark mode and
  10% alpha in light mode. The 44 dp mini
  artwork keeps its additional start
  inset. Compact trailing controls retain reduced spacing.
- Mini-player upward and full-player downward drags update the shared-player
  progress in real time. Upward release settles open and non-zero downward
  release settles closed; cancelling returns to the gesture origin. Horizontal
  mini-player metadata swipes remain independent.
- Full player has no visible top app bar. Its centered title/artist header sits
  at the safe status-bar inset plus 16 dp above a slightly smaller centered
  artwork surface with a 12 dp Miuix squircle crop. Its black shadow
  uses black at 20% alpha, Overlay blending, a 16 dp blur radius, a 90% shadow
  shape, no horizontal offset, and a 10% vertical offset. The shared artwork
  transition multiplies that shadow alpha by sheet progress, so it fades in on
  expansion and fades out on collapse without changing artwork opacity. Its fixed outer frame
  retains the existing position while the visible artwork is measured from
  an outer container expanded by 12 dp in both dimensions and an animated
  four-edge inset: 8 dp while playing and 32 dp while paused. Both
  directions use `spring(dampingRatio = 0.6f, stiffness = 200f)`, allowing the
  resumed artwork to rebound naturally around the playing inset. The progress-layout
  width follows the fixed artwork bound.
  The Miuix
  `LinearProgressIndicator` uses the artwork width; an app-side pointer layer
  maps down/drag positions to duration, provides tap-to-seek, and retains
  accessible progress semantics. Press feedback remeasures the complete
  indicator to a slightly wider width and roughly double height instead of
  scaling only its rendered vertical axis; foreground, background, progress,
  and rounded ends are redrawn together. The 26 dp gesture target remains
  centered on the indicator, but timestamp placement is calculated from the
  idle 6 dp indicator's actual lower edge; the timestamp row begins 6 dp below
  that edge instead of below the gesture target.
  Header metadata, lyrics, progress, timestamps, and both control rows use
  white in dark mode and a contrast-adjusted artwork accent in light mode.
  Light mode derives a second, 20%-deeper accent. Song title, current primary
  lyric, progress, and primary previous/play/next controls use the deep accent;
  artist uses its slightly softened current-translation variant, while inactive
  lyrics, timestamps, and secondary controls retain the normal accent.
  Header colors use their final full-player values immediately; no collapsed
  mini-player color interpolation runs on opening. Play/pause
  presentation and controller toggling follow `playWhenReady`, so a
  seek-induced buffering event cannot flash the Play icon while playback
  intent remains active.
  Primary previous/play/next controls retain explicit Miuix touch targets:
  Play/Pause is a 42 dp glyph inside 80 by 80 dp, while Previous/Next are 26 dp
  glyphs inside 64 by 64 dp. Playback mode, Lyrics settings, queue, and track
  actions make the secondary row. In portrait, that row consumes the complete
  available screen width and uses `Arrangement.SpaceEvenly`, making the four
  inter-item/edge intervals equal. The idle progress indicator's actual lower
  edge to the primary row is 32 dp, while the primary row to the secondary row
  is 16 dp; the timestamp row is overlaid into the first interval and does
  not increase it. The portrait control panel intentionally does not apply the system
  navigation-bar bottom inset; its own 32 dp bottom padding and control height
  provide separation while the background extends like the hidden-controls
  Lyrics page. Play/Pause uses the shared 180 ms in / 140 ms out
  fade-scale motion, while playback-mode icons switch directly without a
  transition. Order and Repeat-one horizontally mirror only the shared Miuix
  loop glyph to make its direction clockwise; the independent Repeat-one `1`
  badge is not transformed. Mini-player Play/Pause uses that same transition and remains 20 dp
  inside a 40 by 40 dp normal-bar target or
  a 36 by 36 dp floating/liquid target, with its target offset 6 dp to the left
  without moving the queue control.
- `ic_player_play`, `ic_player_pause`, `ic_player_previous_track`, and
  `ic_player_next_track` are Android VectorDrawable conversions of the supplied
  SVG paths. Miuix `Icon` loads them through `painterResource`, preserving
  semantic descriptions, tint, and `AnimatedContent` behavior.
- With Lyrics control hiding disabled, the portrait full-player middle region is
  a two-page horizontal pager: artwork is page zero and timestamped lyrics is
  page one, while one control panel remains outside and below the pager. Its
  gesture viewport spans the screen width while the artwork and progress retain
  their shared narrower width. Only this bounded vertical region moves. The
  lyric viewport is inset 16 dp below the identity header and 16 dp above the
  progress indicator, and uses equal 100 dp fade masks at both edges. With
  Lyrics control hiding enabled, the same retained `PagerState` instead spans
  the complete content region: page zero contains artwork plus the complete
  control panel, and page one contains only Lyrics. The Lyrics page keeps its
  16 dp top inset and top fade, reaches the bottom content edge, and disables the
  bottom fade. Its active line uses the complete playback-page center rather
  than the remaining viewport center below the identity header. Full-player
  foreground content applies top and horizontal safe
  drawing insets globally, then applies the bottom inset only to artwork and
  control layouts. Hidden-controls Lyrics therefore continues through the
  transparent, still-visible system navigation region to the physical screen
  bottom without adding another navigation-bar background. No shared-panel translate,
  fade, collapse, or lift animation is retained. Both portrait branches reuse one layout skeleton whose weighted
  artwork viewport begins below the identity header and ends above the progress
  indicator. Page-zero artwork is centered in that viewport, while controls
  below the progress indicator cannot change its center. The pager page is not
  reset by a current-item or setting change, so
  Lyrics remains visible across track changes and layout-mode switches. The
  identity title and artist use fixed single-line slots with explicit centered
  32 sp and 24 sp line heights respectively, preventing Latin, Japanese, or
  fallback font metrics from changing header height and shifting either text or
  artwork. The artist uses 16 sp text within its 24 sp line-height slot. A
  current-item
  switch crossfades both identity rows together with a 140 ms fade out and
  180 ms fade in. Artist metadata is normalized into names joined by ` / `, and
  the slash alone uses thin font weight.
  The active line is found by binary search from `PlaybackUiState.positionMs`
  and is scrolled to the measured center of the lyric viewport. A newly mounted
  lyric document performs its first center placement with direct `scrollToItem`
  and `scrollBy` calls while the list is hidden, then reveals the already
  centered result without an upward entry animation. Later playback-driven
  focus changes retain a spring-based coordinated visual transition.
  A retained `LazyListState` performs one direct focus placement. Visible targets
  use their measured center delta; off-screen targets resolve the requested index
  and final measured correction without traversing intermediate rows. A root
  `Animatable` graphics translation preserves the pre-placement visual position
  and springs the complete lyric surface back to zero. Lyric rows have no
  `LookaheadScope`, `ApproachLayoutModifierNode`, deferred offset animation, or
  distance-sensitive placement spring. The LazyListState is not left inside a
  programmatic scroll animation, so rows cannot move sequentially or lose clicks
  while centering.
  Lyrics compute horizontal content padding from the same layout width as the
  playing artwork and progress indicator without a second line-level horizontal
  inset. Additional newline-separated content at one timestamp is rendered with
  the secondary lyric style. At scale `1f`, primary lyrics use `24.sp` /
  `28.sp` line height and translation uses `16.sp` / `22.sp`; the persisted
  percentage scale remains `0.7f..1.3f`, so primary text spans `16.8.sp..31.2.sp`.

## Core Models

```kotlin
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = false,
    val dynamicColorSource: DynamicColorSource = DynamicColorSource.DESKTOP,
    val playbackBackgroundStyle: PlaybackBackgroundStyle =
        PlaybackBackgroundStyle.BLURRED_ARTWORK,
    val lyricFontScale: Float = 1.0f,
    val lyricFontWeight: Int = 400,
    val forceWordByWordLyrics: Boolean = false,
    val lyricBlurEnabled: Boolean = false,
    val centerLyrics: Boolean = false,
    val hideControlsOnLyrics: Boolean = false,
    val showLyricsTranslation: Boolean = true,
    val blurEnabled: Boolean = true,
    val floatingBottomBar: Boolean = false,
    val liquidGlass: Boolean = false,
    val predictiveBackEnabled: Boolean = true,
    val libraryTabIndex: Int = 0,
    val musicSortFieldOrdinal: Int = 0,
    val musicSortDescending: Boolean = false,
    val albumSortFieldOrdinal: Int = 0,
    val albumSortDescending: Boolean = false,
    val albumGridStyleOrdinal: Int = 0,
    val artistSortFieldOrdinal: Int = 0,
    val artistSortDescending: Boolean = false,
    val folderSortFieldOrdinal: Int = 0,
    val folderSortDescending: Boolean = false,
    val defaultHomePage: DefaultHomePage = DefaultHomePage.HOME,
)

data class PlaybackUiState(
    val queue: List<PlaybackQueueItem> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val playWhenReady: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val bufferedPositionMs: Long = 0,
    val playbackMode: PlaybackMode = PlaybackMode.ORDER,
    val errorMessage: String? = null,
)
```

`PlaybackController` supports replace-and-play, play/pause, seek, previous,
next, playback-mode cycling, play-next, append, jump, remove, and clear.
`PlaybackMode` is `ORDER`, `REPEAT_ONE`, or `RANDOM`. Queue items carry their
stable source order. Entering Random physically shuffles once while retaining
the current item and position; returning to Order restores source order.
ExoPlayer uses repeat-all for Order/Random and repeat-one for Repeat one.

## Library Behavior

- MediaStore selection always includes `IS_MUSIC != 0` and conditionally adds
  `DURATION >= 60000`. When custom folder tree URIs exist, normalized
  `RELATIVE_PATH`/legacy data paths must match one selected folder or one of its
  descendants; otherwise the scan covers all shared-storage music.
- Missing canonical metadata remains null and is localized only in UI.
- Search matches normalized title, artist, album, or file name.
- Root Album, Artist, and Folder presentation searches match only their primary
  item title: `AlbumGroup.name`, `ArtistGroup.name`, or `FolderGroup.name`.
  Album artists, counts, paths, and all fields from grouped tracks are excluded.
- Stable sort ties end with title sort key and MediaStore ID.
- Song index keys come from title or file name according to the active sort.
  The section index is `0`, `A`-`Z`, `#` for ascending and the reversed sequence
  for descending; Chinese uses Pinyin initials.
- Album and artist projections are derived from the immutable track snapshot.
  Albums key by whitespace-normalized, case-folded album name plus the explicit
  album-artist tag, matching Lyrico. MediaStore album IDs and track artists do
  not split a logical album; an absent album artist remains one empty key and
  never inherits the track artist.
  Album year is the first positive year in the group.
- Artist projection splits `MusicTrack.artist` on `，`, `,`, `、`, `/`, and `&`,
  trims/collapses whitespace, de-duplicates names per track by normalized key,
  and inserts the track into each resulting artist group. Display contexts that
  show a track's artist join split names with ` / `.
- Tapping a row passes the current page playback list and row index to
  playback.
  Songs presentation also retains the current sorted, unfiltered list. If a
  non-empty Songs search has exactly one visible result, row playback resolves
  that track's index in the retained full list; multi-result searches use the
  same retained full list and resolve the clicked track's index there. The
  playback controller applies the active order, repeat-one, or pseudo-random
  mode to that complete queue.
- `MusicTrack.albumId` stores MediaStore's stable album identity and
  `MusicTrack.folderPath` stores a normalized direct-parent path. API 29+ reads
  `MediaStore.RELATIVE_PATH`; API 28 reads the legacy media path and removes
  the file name. `MusicTrack` also stores TagLib-derived title, artist, album,
  album artist, year, track/disc numbers, MIME type, bitrate, sample rate,
  channel count, platform-reported bit depth, and a completed-read marker. Startup waits for the small
  cached snapshot, publishes it, and reuses these properties when ID, modified
  time, and file size are unchanged. Explicit and launch scans keep those
  unchanged properties instead of re-reading the same song, while a single-file
  external-edit refresh still forces that file's metadata read.
  Snapshot version 7 persists these additions while earlier snapshots decode
  unavailable tag/bit-depth fields as unread until the next scan.
- Audio-quality classification is pure and deterministic. A recognized
  lossless format is `HR` when sample rate is at least 88,200 Hz or the TagLib
  bitrate is at least 1,500,000 bps. Other recognized lossless files are `SQ`.
  Non-lossless files at 256,000 bps or above are `HQ`; lower or unknown values
  return no badge. No file-size estimate is used. The song-row
  `ProjectBadge` uses a 16 dp minimum height, 3 dp rounded corners, 4 dp
  horizontal padding, and theme-aware colors. `HR` uses `#FFD54F` in both light
  and dark modes.
- Folder projection keys by the normalized full parent path so equal folder
  names at different locations remain distinct. Display paths remove common
  shared-storage roots such as `/storage/emulated/0`, keep a leading slash, and
  omit a trailing slash. Folder search matches only the folder name; stable
  sorting supports name and direct song count in both directions.
- Every root alphabet index captures one expanded top-bar padding after Miuix
  publishes its collapse range and reuses that value instead of recalculating
  it from live height offsets on every frame. The measured SearchBar region is
  added separately, so only SearchBar visibility motion moves the index. A 4 dp
  top gap is applied once. Bottom padding uses a stable retained root inset
  while navigation collapses, leaving 12 dp for normal navigation or 6 dp for
  floating navigation without moving relative to the mini-player strip.
  Idle section labels share the scroll-top action color. The actively dragged
  label resolves to pure black/white based on surface luminance, while the
  floating indicator uses a semi-transparent Miuix gray surface with Miuix
  primary blue text/icon. The scroll-top cell
  retains its hit-space but fades its icon and disables the action while the
  large title is expanded.
- Root lazy states and Miuix top-bar states are hoisted above the navigation
  entry. Sort actions explicitly reset the owning state only after the
  ViewModel has emitted the matching query/sort projection and that projection
  reaches a Compose frame; screen composition does not run an initial
  sort-reset effect. This prevents stable lazy-item keys from restoring the
  pre-sort anchor. Alphabet scroll jobs are cancelled when their map changes
  and validate the latest item count before scrolling.
- Album detail keeps its root-three-column-sized, 14 dp-cornered artwork plus
  title3 name/artist/optional-year/count header outside the song `LazyColumn`.
  A positive year appears directly above the song count; both use 12 sp
  `onSurface` text, and a missing year adds no row. Album Detail and Artist
  Detail each use `TabRowWithContour` backed by a two-page `HorizontalPager`,
  matching the local Miuix interaction instead of a crossfade. Album pages
  contain Songs and Participating artists, where every distinct split artist
  from the album's tracks appears once in first-occurrence order. Both detail
  pagers accept TabRow taps and horizontal swipes because their routes do not
  compete with root-pager navigation. A participating-artist route saves the
  source album key, making Artist Detail a third-level child whose Back restores
  that Album Detail. If the Artist Detail opens an album through its Albums tab,
  it pops back to that source Album Detail with its prior state instead of
  replacing it or adding a route. The inverse child Album Detail similarly pops
  to its source Artist Detail when a Participating artists row is tapped. Both
  detail screens use an empty-title Miuix
  `SmallTopAppBar`; its `bottomContent` owns the fixed metadata header and
  selector so the complete fixed region shares the outer Miuix blur/fallback
  surface. With Blur active, the selector Card and contour background stay
  transparent. The app bar itself stays transparent. Detail lists fill the page
  behind that surface and use top content padding, allowing scrolled rows to
  supply the backdrop without changing their initial placement.

## Lyrics

- The previous `TimedLyricLine` implementation is replaced by a local
  local model. `LyricLine` stores agent, non-negative begin/end times, optional
  plain text, timed `LyricWord` spans, translation, and optional paired lines.
  A `LyricsDocument` contains ordered stable lines plus its local source and
  `LRC` or `TTML` format.
- LRC accepts multiple line timestamps, centisecond or millisecond fractions,
  enhanced inline timestamps, and `[offset:]`. Enhanced spans retain trailing
  spacing and receive bounded end times. TTML accepts `<p>` begin/end/dur clock
  values, `ttm:agent`, word-timed spans, translation roles, and background
  paired lines while disabling external entity resolution.
- Text decoding recognizes UTF-8/UTF-16 byte-order marks, validates UTF-8, and
  falls back to GB18030 for common legacy Chinese sidecars. Reads are capped at
  2 MiB and parsed output is capped at 10,000 timed lines.
- Embedded candidates include Media3 `USLT`/lyrics text frames, Vorbis
  `LYRICS`/`SYNCEDLYRICS` comments, and equivalent MP4 lyric metadata. Only
  embedded text that parses as timestamped LRC or TTML is displayed. Embedded
  local lyrics are attempted before same-name `.ttml` and `.lrc` sidecars.
- The active line is resolved by binary search. Enhanced LRC lines use the last
  timed word's end, including a 500 ms fallback for a final word with
  no explicit end, instead of extending that line to the next lyric timestamp.
  Parsing precomputes a stable `LyricTransition` before the first line or
  between adjacent lines whenever the measured gap is at least 5000 ms. The
  transition is inserted into the document's stable render-item sequence, and
  transition lookup uses binary search rather than scanning every gap per
  display frame. During a long gap the transition becomes the scroll target and
  no lyric line remains focused; shorter gaps continue to target the next line
  without falsely marking it as sung. True
  word timing is rendered through measured Canvas glyphs. Every wrapped row owns
  its own 100 px horizontal progress edge. Words at least 1000 ms long with more
  than 200 ms per character use staggered character animation: start
  times span the first 20% of the word, while the remaining 80% drives rise,
  scale, and glow. Pure CJK plus Arabic/Devanagari content and faster words use
  the 700 ms simple float with a maximum 4 px offset; the offset is pixels, not
  density-independent pixels. While playback is active, a display-frame clock
  advances from the latest Media3 position sample and snaps only for a real seek
  or playback discontinuity, so both native and forced word-by-word progress are
  continuous between the controller's 500 ms state publications. When Force
  word-by-word lyrics is enabled, a line without spans uses its own begin/end
  interval as one continuous soft linear reveal. Wrapped visual rows divide that
  interval in reading order and complete one row before the next begins instead
  of sharing one full-width mask. Disabling the option renders the ordinary
  current line without synthetic progress. TTML span `end` or `dur` values are
  retained rather than extending a word to the next span. Focus alpha/scale and
  inactive-line blur animate through the same offscreen graphics layer. Automatic
  line changes place the list target directly and animate one root translation,
  preventing distance-dependent springs from causing lyric overlap or holding
  the list's scroll mutex. The line-item vertical padding is reduced by 2 dp.
- A transition renders three circles whose 11 dp base diameter
  and 7 dp spacing scale with the 70%-130% lyric-size setting. A leading
  transition uses 5 dp scaled vertical padding; a transition between two lyric
  rows uses 10 dp scaled vertical padding so its distance to both neighbors
  follows the ordinary lyric rhythm. The circles advance their alpha in three
  equal progress segments.
  The transition item remains part of the precomputed lazy-list structure even
  when its content is not active, so playback never inserts or removes a row at
  the gap boundary.
- `LyricsUiState.Loading` has no visible loading label. The Lyrics host retains
  the outgoing `LyricsDocument`, fades it to zero over 180 ms, replaces it only
  after the new document is available, and fades the incoming document in over
  240 ms. `LazyColumn` continues to compose only the visible lyric viewport and
  its prefetch area; parsing and file reads remain on `Dispatchers.IO`.
- Beginning a real `LazyColumn` drag enters a retained browsing state when a
  parent pointer observer sees dominant vertical displacement greater than
  `LocalViewConfiguration.current.touchSlop` in the final pointer pass. The
  observer never consumes the event, so lyric-row clicks remain in the normal
  chain. Taps, horizontal movement, and diagonal movement do not enter browsing.
  Programmatic centering never starts a LazyList scroll animation. Inactive-line
  blur remains disabled after drag or fling completion, and
  automatic line centering remains suspended while the list is moving. When
  playback is active, drag or fling completion starts a 5000 ms idle timeout;
  another manual scroll cancels and restarts it. Expiry clears browsing state
  and centers the current playback lyric. Progress seeks derive the off-screen
  `scrollOffset` from both `viewportStartOffset` and `viewportEndOffset`, including
  the large lyric content padding, then apply any measured-size correction
  directly before the root translation animates to zero. Lyric rows keep
  scale, alpha, and `BlurEffect` followed by the indication-free `clickable`
  target in one full-width modifier chain, preserving the expected ordering. A
  blurred inactive line therefore performs seek and centering on the first tap
  without changing the blur state. Another row remains clickable while the
  previous root translation is active and retargets it immediately. Pressing
  Play also resumes following immediately, and no ripple is clipped into a
  rounded rectangle.
- Lyric font scale is clamped to `0.7f..1.3f` and applies to primary, inactive,
  and translation styles. At 100%, primary text is 24 sp / 28 sp and translation
  is 16 sp / 22 sp; primary text therefore spans 16.8 sp through 31.2 sp over the
  percentage slider, which remains percentage-based rather than direct-sp. A legacy
  stored value is divided by 0.8 and clamped during migration, so old 80% becomes
  new 100%. The lyric content width equals the progress width; line renderers do
  not add another horizontal 16 dp inset. Lyric blur leaves the current line
  sharp and blurs inactive lines, with a readable non-blur fallback when off.
- Lyrics use start alignment by default. The persisted `centerLyrics` setting
  changes both primary and translation text to centered alignment without
  rewriting parsed content. Forced word-by-word Canvas measurement fixes both
  width constraints to the available lyric width so `TextAlign.Center` remains
  effective before, during, and after the reveal.
- A lyric tap seeks to the tapped row's start time and then uses the same
  `LyricsDocument.currentLineIndex` and focus-time parsing as normal playback.
  It does not retain a parallel requested-row focus state or suppress the
  current-line transition while the seek is pending.
- Full-player progress dragging publishes a transient lyric preview position on
  every pointer update and commits one Media3 seek only on release; a progress
  tap uses the same release path directly. Full-player seek callbacks publish a monotonically increasing request key and
  requested position to the lyric renderer before forwarding the Media3 seek.
  The display clock and list focus use that requested position immediately and
  keep the request unapplied until the smooth clock is within 250 ms of the
  target. Controller publications that remain closer to the pre-seek position
  are rejected, so progress-bar seeks cannot first center earlier rows. After
  the lyric document completes its first hidden placement, both progress-bar
  seeks and lyric taps directly place the retained list using measured item and
  viewport geometry, then spring one root visual translation to zero. An
  off-screen target uses a viewport-derived initial offset, one measured
  correction, and a viewport-edge directional translation with a non-bouncing
  spring rather than an animated
  list-entry traversal. A programmatic lyric tap remains outside manual-browsing
  state while it centers, and another tap can retarget the active translation
  immediately. The same root translation keeps `stiffness = 80` for ordinary
  gaps and raises stiffness from the next timestamp interval for dense lyrics,
  shortening the movement without discrete row jumps or changes to manual
  browsing and blur behavior. The translated list and its fixed edge fade are
  composed in one explicit offscreen container, so the top and bottom mask stays
  at the viewport edge during the root translation. The smooth clock tracks whether
  a seek request is new: pausing without a new seek preserves its current frame
  position instead of reusing the last seek target, while a new paused seek uses
  the new target. A current-item change creates a document-scoped clock and
  resets the seek request, preventing old lyric or word-progress state reuse.
- A row without a visible translation receives enlarged primary-only vertical
  padding. This depends only on whether translation is rendered, so a document
  with no translation and a document whose translation setting is disabled
  have identical primary-line spacing for the same lyric renderer. Native
  timed-word gradients and forced word-by-word base text use the same inactive
  alpha as non-focused lyric rows for content not yet revealed.
- `lyricFontWeight` is persisted as one of `100, 200, ..., 900`, defaults to
  `400`, and is applied to both primary and translation styles. The renderer
  disables synthesized weight; fonts without a usable real weight or variable
  `wght` axis can therefore remain visually unchanged.
- The full-player secondary row opens an owning-Scaffold `OverlayBottomSheet`
  through the Miuix `ConvertFile` icon between playback mode and queue, using the
  same icon size as the adjacent actions. The first
  sheet item is an icon-free, description-free, default-on Lyrics translation
  `SwitchPreference`; it changes only
  translation visibility, not the parsed document or original lyric line. The
  remaining controls appear exactly as Lyrics size, Lyrics weight, Center
  lyrics, Lyric blur, Hide controls on Lyrics, and Force word-by-word lyrics.
  The two official Miuix `SliderPreference`s persist size and the discrete
  100–900 weight; the remaining `SwitchPreference`s persist centering, blur,
  control hiding, and forced reveal. The control-hiding item has no summary.
  When hiding is off,
  player details remain one shared panel outside the bounded artwork/Lyrics
  pager. When hiding is on, player details are composed only inside the artwork
  page of the full-content pager, while the Lyrics page owns no controls.
- Lyrics remain fully local. Do not add Lyricon, notification/MediaSession
  lyric publishing, Lyric Getter, or Super Lyric code, permissions, queries,
  services, receivers, or dependencies.

## Settings and Locale

- Resolve the Activity-owned `MeloxViewModel` before `setContent` and pass that
  instance into `MeloxApp`. ViewModel initialization performs one bounded
  `runBlocking(Dispatchers.IO)` read of the first DataStore settings value and
  seeds both `loadedSettings` and the initial `AppUiState` with it. The first
  Compose frame therefore constructs the persisted normal, floating, or
  liquid-glass navigation style directly.
- Keep the settings value as the only pre-Compose storage prerequisite.
  Playback/library snapshots, MediaStore scanning, projections, artwork
  extraction, and cache work remain asynchronous.
- Theme settings keep an immediate screen-local value for each switch and the
  theme-mode dropdown, then reconcile it with the DataStore-backed setting
  after persistence. The preference summary and popup selected row read that
  same local theme-mode value.
- `MeloxTheme` leaves Miuix `ThemeController.keyColor` unset for the desktop
  wallpaper source so the controller resolves the complete platform Monet
  palette.
  Artwork source supplies the existing cached cover sample as `keyColor`; an
  unavailable cover leaves it unset and therefore falls back to the platform
  palette. During a track change, generate the new Monet palette once, retain
  the currently displayed Miuix palette, and interpolate every color role over
  600 ms with fast-out-slow-in timing. This avoids repeatedly remapping an
  intermediate seed through Monet hue ranges and lets rapid changes retarget
  from the currently displayed frame. The outer controller continues to
  provide the Monet mode while a nested direct-colors theme supplies the
  interpolated palette, preserving Miuix dynamic-color component styling.
  Theme-mode and color-source changes still apply directly.
  Changing the floating-bottom-bar preference persists liquid glass as disabled.
  Liquid glass must be enabled separately after floating mode is active, while
  unsupported devices still resolve to the opaque floating fallback.
- Use the platform `LocaleManager` directly on API 33+ so the first language
  change follows the same in-place path as later changes. Use
  `AppCompatDelegate` and `LocaleListCompat` only on API 28-32.
- Handle `locale|layoutDirection|uiMode` configuration changes in `MainActivity`
  so locale and system light/dark changes update Compose without recreating the
  page, player transition state, or MediaController connection. The artwork
  source is reduced to 8-by-8 only when the cover changes; theme changes reuse
  those source samples, remap only HCT tone, and retain the same 4-by-4 output
  bitmap and painter. Keep
  immediate dropdown selection state separate from the effective resource
  locale so choosing an override equal to the system locale still updates.
  Apply the locale directly from the selection callback instead of delaying it
  until the popup exit animation, keeping the existing composition visible
  while every `stringResource` updates.
- Enable generated locale configuration from the maintained resources.
- Disable Android cloud backup and device transfer; caches, settings, and playback snapshots remain on the originating device.

## Compatibility

- The manifest overrides the Miuix blur library minimum SDK only because every shader/render-effect call is runtime guarded.
- API 28-32 never instantiate shader-only code paths.
- API 33+ also checks runtime shader/render-effect support before enabling blur or liquid glass.
- Unsupported stored liquid glass falls back to a floating opaque bar without deleting the user's preference.

## Signing

- Generate the local ignored release keystore as `InefyKey.jks` with alias
  `InefyKey`.
- Gradle release signing reads the ignored project-root `local.properties`
  entries `melox.keystore.path`, `melox.store.password`,
  `melox.key.password`, and `melox.key.alias`. The local keystore path defaults
  to `InefyKey.jks`; password values may stay blank until the maintainer fills
  them locally.
- Release tasks fail during configuration when any signing value is missing.
  They must never silently produce `app-release-unsigned.apk` as the release
  deliverable.
- The root `assembleRelease` task wraps `:app:assembleRelease`, allows Gradle's
  configuration cache to be reused, and prints the generated
  `app/build/outputs/apk/release/Melox_<versionName>_<yyMMddHHmm>.apk` path.
  The timestamp is computed by the APK timestamp task during execution, so
  Android Studio Terminal output points directly to the signed artifact without
  freezing the build time in the configuration cache.
- Release uses the stable AGP 9.2 Release DSL with code minification, optimized
  resource shrinking, and `proguard-android-optimize.txt`, which runs R8 code
  shrinking, optimization, and obfuscation without the experimental gradual-R8
  flag. Project rules stay in `app/src/main/keepRules/*.keep`; dependency
  consumer rules remain authoritative for libraries such as TagLib's JNI
  bridge.
- Keep rules must be narrow. The optimized Android defaults retain framework
  entry points, native method names, enum APIs, Parcelable creators, and
  runtime annotation/signature metadata. The app keeps line numbers for
  retraceable production crashes, normalizes source-file names, and repackages
  obfuscated classes to reduce DEX name overhead; it does not disable shrinking,
  optimization, or obfuscation globally.
- Package only default/English and Simplified Chinese resources. Debug and
  Release native packaging retain only `arm64-v8a`.
- Each optimized delivery retains `mapping.txt`, checks that the manifest
  Activity and MediaSessionService implementations plus TagLib JNI bridge are
  present in the optimized DEX, and verifies APK signing, 16KB ZIP alignment,
  and 16KB alignment for every packaged arm64 native ELF LOAD segment. The
  full-player artwork background must not add a bundled native library.

## Failure Handling

- Permission denial closes the system dialog. The Settings scan preference can
  launch the runtime request again without redirecting to app settings.
- A scan failure retains the last successful rows; Settings remains the retry
  entry point.
- An explicit scan compares the resulting immutable track list with the current
  library. When it changes, a one-shot UI event shows the localized completed
  song count; when it is equal, the full-width button keeps the `Start scan`
  label and shows `No music file changes`. Launch refreshes do not emit either
  Toast.
- Playback errors remain available through controller state and system
  diagnostics. A newly reported error shows one localized Toast; the full-
  player header does not show an unsupported-audio message. Unavailable
  restored items are removed.
- Corrupt or incompatible snapshots are ignored without deleting user audio or settings.

## Verification

- Unit-test pure sorting, search, section mapping, settings fallback, permission mapping, snapshot codecs, queue operations, and visual capability resolution.
- Unit-test Home recommendation exhaustion without repeats, selected-track
  recommendation queue ordering across playback modes and subsequent full-queue
  mode normalization, and Recently added priority/fill behavior including more
  than 20 scan-detected additions.
- Unit-test that the priority two-card recommendation pass preserves its seeded
  order and reuses published artwork IDs while expanding to five cards.
- Emulator-check Home's early two-card recommendation render, non-clipped
  recommendation artist text, borderless Recently added grid, and Artist list
  insets.
- Unit-test independent mini-player and full-player artwork request sizes,
  shared-player endpoint/easing calculations, and the full-player per-track
  bitmap crossfade.
- Verify that opening and dismissing use one bitmap overlay, reach the measured
  mini bound and current visible full-artwork bound, reverse from the current
  progress, and never rotate or skew the cover.
- Unit-test aspect-preserving thumbnail dimensions for square, landscape, and
  portrait artwork without upscaling.
- Unit-test folder path normalization, grouping, filtering, both sort fields,
  duplicate-name separation, direct-folder membership, and snapshot migration.
- Unit-test album-ID grouping across different track artists, equal-title
  separation across different album IDs, and snapshot migration.
- Unit-test `HR`/`SQ`/`HQ` classification, low/unknown omission, source-revision
  property reuse, TagLib value normalization, and snapshot persistence of
  audio-quality metadata.
- Unit-test LRC/TTML timestamps, offsets, multiple timestamps, malformed input,
  text decoding, embedded metadata extraction, and sidecar candidate naming.
- Unit-test alphabet target clamping/cancellation helpers, descending section
  order, album-grid style column counts, and legacy style migration.
- Unit-test persisted library-presentation normalization and tiny playback
  summary migration/validation.
- Unit-test direct full-player visibility behavior and HCT pixel conversion for
  the light/dark tone targets and chroma cap.
- Unit-test bar-sized and full-player-sized artwork requests and statically
  review that the processed field retains its previous non-null result while a
  replacement is prepared. Unit-test the field-level transition endpoints and
  midpoint interpolation.
- Run Compose/instrumented tests for localization, settings persistence, navigation, and overlay presentation.
- Validate API 28 opaque fallback and API 37 liquid glass on emulators.
- Compare focused API 37 root-pager `gfxinfo` frame statistics before and after
  the optimization using the same destination traversal. The recorded
  default-Blur baseline is 51 rendered frames, P50 81 ms, P90 121 ms, and 39
  slow UI-thread frames.
- Package and install the final Debug APK, exercise foreground/background playback, inspect UI trees and screenshots, check crash logcat, then copy the artifact to `artifacts/Melox-debug.apk`.
- Inspect every packaged arm64 native ELF LOAD segment and verify each has
  16KB alignment in addition to the APK ZIP page-alignment check.
- For the focused implementation pass requested on 2026-07-27, limit routine
  verification to Kotlin compilation and final APK/signature/archive checks;
  do not claim frame-by-frame interaction proof without a runtime capture.

### Version 1 Result

- Unit tests, Lint, API 28/API 37 instrumented tests, and Debug assembly pass.
- The packaged manifest contains no network permission.
- API 37 validation runs on a 16KB-page Pixel 10 Pro image.
- Background, screen-off, external-audio, system media-control, locale,
  responsive-layout, capability-fallback, and paused-restore flows pass.
- Miuix Navigation3 secondary transitions, in-place locale updates, immediate
  language and theme-switch selection, runtime predictive-back toggling, the
  iOS-like floating bar, and fresh-install Blur/Predictive-back defaults pass
  on API 26 and API 37 in the historical pre-minSdk-28 validation record.
- Delivered APK:
  `artifacts/Melox-debug.apk`
- SHA-256:
  `5d544a511cd7d281917c6166ae2eb391a78fed02e87f2f371df550b855758b43`
- Optimized Release:
  `artifacts/Melox-release.apk` and
  `artifacts/Melox-release-mapping.txt`. R8 code shrinking, optimization,
  obfuscation, and optimized resource shrinking completed; the optimized APK
  is 7,863,189 bytes, archive-valid, v2-signed, 16KB zip-aligned, and has
  SHA-256
  `16d03ed2daa451b468ba61eeb193e4f3c501b7104fc8dd48022cc40b1dd94384`.
  The matching retrace map is 64,387,511 bytes with SHA-256
  `aefdb1300effefd1b29b62fd440fdc4b32264f32951c1af5c1b17f8dcb9efe90`.
