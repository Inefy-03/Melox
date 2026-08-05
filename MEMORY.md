# Melox Project Memory

Last updated: 2026-08-05

## Stable Decisions

- Routine work stays on `main` and commits directly there. Create and mention
  a separate branch only when a new feature or bug fix needs isolated testing.
- Release APK filenames use `Melox_<versionName>_<yyMMddHHmm>.apk`, with the date
  resolved in the `Asia/Shanghai` time zone at build configuration time.
- Routine verification avoids emulator clicking, screen recording, and
  screenshots. Prefer compilation, unit tests, Lint, and static inspection;
  use the emulator only for a critical runtime behavior that cannot be
  validated meaningfully another way.
- Melox is an independent offline Android local-music player with application ID `com.melox.player`.
- The baseline is Android 9+, `minSdk 28`, `compileSdk 37`, `targetSdk 36`, Miuix `0.9.3`, and Media3 `1.10.1`.
- The project may directly reuse maintainer-owned UixPlayer music-library and playback code and maintainer-owned IconEditor settings/About behavior.
- Visible UI stays within the Miuix component family. The full player is the only strongly atmospheric screen; the library and settings remain quiet and information-first.
- The main destinations are Home, Songs, Library, and Settings. Library owns a
  Miuix `TabRow` and nested pager for Albums, Artists, and
  Folders; each tab retains its own query, list position, and top-bar state.
  Phones use bottom navigation; widths of at least 600 dp use a navigation
  rail. Home, Songs, or Library can be persisted as the default next-start
  destination without navigating away from the current Settings page. Home is
  the first root item/page and uses `首页` in Simplified Chinese. The persisted
  default initializes the root pager directly, so non-Home startup never first
  renders Home and then scrolls away.
- Home follows Flamingo's local recommendation hierarchy without copying its
  GPL implementation: a Miuix large-title page resolves five real-artwork
  tracks from one fresh random seed per application composition, then appends
  one previously unseen card after each forward pager swipe. When all eligible
  artwork tracks have been shown, the carousel ends without repeats. The
  horizontal pager alone owns 16 dp start/end screen padding; 256 dp fixed-width
  cards retain the carousel geometry and render two-line metadata on a darkened
  artwork-derived surface with a white title and semi-transparent white artist.
  A card tap immediately plays the selected recommendation, keeps the other
  loaded recommendations after it in carousel order, and then appends the
  remaining library in the current playback-mode order. Any later playback-mode
  change restores full-queue mode ordering without the recommendation prefix. The root
  composition, not the disposable Home pager page, retains the seed, selected
  track IDs, request flag, and completion state. First activation uses ordered
  batches of eight artwork probes; leaving and returning in the same app
  session neither restarts loading nor rerandomizes the result. Home also shows
  Recently added beneath recommendations: scan-detected additions lead the
  list, retain all entries above 20, and otherwise fill to 20 from newest file
  modification time.
- API 28-32 use opaque visual fallbacks. Blur and liquid glass are runtime-gated on API 33+.
- Playback is owned by one service-side ExoPlayer and MediaSession. UI clients use a MediaController.
- Tapping a library row snapshots the current page playback list and starts at
  that row; Songs-page searches resolve the complete sorted list as documented
  below.
- Songs-page playback retains the current sorted, unfiltered list alongside
  the filtered presentation. Any non-empty search starts the clicked song at
  its position in the full Songs list, regardless of whether one or many
  results are visible; the active playback mode then orders that complete
  queue.
- The last physical queue, stable source order, current item, position, and
  three-state playback mode are restored after leaving/reopening or process
  restart without autoplay. Queue/current-item/mode changes write immediately,
  while position-only updates remain debounced; task removal performs a final
  atomic snapshot write.
- Startup queue restoration is staged: a bounded 16-item window around the
  current song is persisted with the mini-player identity and preloaded before
  the service exposes its session. The service then decodes and publishes the
  complete checksummed queue before the slower per-file readability pass. Queue
  therefore opens with at least a visible page immediately after launch;
  background validation later removes
  unreadable items while preserving current item, position, playback state, and
  mode when the user has not mutated the queue. A concurrent queue mutation wins.
- Settings include language, theme mode, dynamic colors, blur, floating navigation, liquid glass, predictive back, manual library scan, and About.
- Missing audio access is requested once when the app enters. A grant from this
  startup request changes the library to an unscanned idle state without
  querying MediaStore; a grant triggered by the Settings scan preference scans
  immediately. Denial never redirects to app settings, and the Settings action
  may launch the system permission request again. Home and Songs show the same
  action-free `No music found` state while unscanned, permission-blocked, or
  successfully scanned with zero songs.
- Root Settings-page preference rows intentionally omit leading option icons;
  secondary pages may still use icons for navigation and actions.
- Blur and predictive back default to enabled. Dynamic colors remain disabled
  by default and appear last in the theme page's Appearance group.
- Secondary pages keep one Miuix Navigation3 `NavDisplay`, scene state, and
  default transition-effects host. The setting switches between the official
  predictive `NavigationBackHandler` and ordinary back handling without
  replacing that host.
- `/Users/bocchi/Code/miuix` tag `v0.9.3` is the primary implementation
  reference for Miuix page motion and visual effects. Melox follows the
  official example patterns for `NavDisplay`, `FloatingNavigationBar`,
  `layerBackdrop`, `textureBlur`, and Monet `ThemeController` modes.
- Floating navigation always uses the Miuix example's iOS-like geometry and
  moving selection pill. Blur uses the official backdrop-blur API, liquid glass
  uses the official example's refractive path, and disabling both keeps the
  same iOS-like geometry with an opaque Miuix surface.
- `MainActivity` resolves and passes its Activity-owned `MeloxViewModel` before
  `setContent`. ViewModel initialization reads the first real DataStore settings
  value on `Dispatchers.IO`, so the first Compose frame uses the persisted
  normal, floating, or liquid-glass bottom bar without briefly drawing the
  normal fallback. This small settings read is the only pre-Compose storage
  prerequisite; library, playback-snapshot, and artwork work remain
  asynchronous.
- Theme-page switches and the theme-mode dropdown publish a screen-local value
  before their DataStore write completes, so both the preference summary and
  popup selected row update without waiting for persistence.
- `MainActivity` handles `uiMode` together with locale/layout-direction changes.
  System light/dark changes therefore preserve the Compose hierarchy, expanded
  player state, mini-player click handling, and MediaController connection.
  Artwork source sampling is keyed only to the cover; theme changes remap HCT
  tone while retaining the output bitmap and painter.
- Language uses AndroidX per-app locales and is not duplicated in DataStore.
  `MainActivity` handles locale/layout-direction changes in place, and the
  language preference keeps immediate local selection state so an override
  equal to the system locale does not appear stuck.
- API 33+ language switching calls the platform `LocaleManager` directly,
  matching IconEditor's in-place path and avoiding AppCompat's first-call
  locale synchronization flash. API 28-32 retain the AndroidX locale path.
  Locale application starts directly from the selection callback without a
  popup-dismiss delay, so all visible resource strings change in place.
- About ports the official Miuix example: animated OS3 effect background on
  supported devices, the monochrome vector icon layer from
  `drawable/ic_launcher_monochrome.xml`, large app name/version identity,
  and sequential scroll-driven fade/scale into the collapsed top bar. Its
  page-level backdrop records all scrolled content so the collapsed bar can
  blur it, while a separate background-level backdrop supplies header texture
  blur. Never let a `textureBlur` descendant sample the same `LayerBackdrop`
  that records its ancestor; this creates a RenderThread recursion and native
  stack overflow. The Developer card uses the official About-page transparent
  texture-blur background and light/dark blend tokens, with a
  `surfaceContainer` fallback when blur is unavailable. The About content
  appears before the heavy OS3/background texture layers mount, and version
  text reads from `BuildConfig`, reducing first-entry jank without changing the
  final layout. The lower introduction cards are replaced by one Developer
  preference whose `Github` action opens `https://github.com/Inefy-03`.
  API 28-32 keep the same complete layout on an opaque Miuix surface.
- Theme settings and About share the official Miuix page-backdrop and
  texture-blur path for their secondary top bars. The Theme settings bar reacts
  to the screen-local Blur switch immediately; About follows the persisted app
  Blur setting.
- Full player identity stays at the safe-area top without a visible back
  control or "Now playing" label. Its slightly smaller artwork is centered
  between that identity and the lower progress area, carries a restrained
  shadow, uses 100% of its layout bound
  while playing, and scales to 90% while paused. Pausing shrinks with
  `LinearOutSlowInEasing`; resuming reaches 102% and then returns to 100% with
  the same fast-to-slow easing. The Miuix
  `LinearProgressIndicator` matches the artwork width, accepts taps and drags,
  and scales uniformly in both axes while pressed so its round ends and inner
  progress remain valid. Header metadata, lyrics, progress, timestamps, and
  both control rows remain white in dark mode and use a contrast-adjusted
  artwork color in light mode. Light mode deepens that color for the song title,
  current lyric, progress, and primary transport controls while retaining the
  softened deep tier for artist and the normal tier for inactive lyrics,
  timestamps, and secondary controls.
  Lyrics match the playing-artwork/progress width; primary text is slightly
  enlarged and same-timestamp translation lines use 80% of the primary size.
  UI state and controller toggling follow `playWhenReady`, preventing seek
  buffering from flashing the Play icon. Opening and dismissing retain a dark
  underlay instead of exposing an unrelated page color. Mini/full Play and
  Pause plus full-player Previous/Next use the maintainer-supplied rounded SVG
  paths as tintable Android vector resources; control sizes, semantics, and
  icon transitions remain shared. Mini/full Play/Pause uses the same 180 ms in
  and 140 ms out fade-scale `AnimatedContent` transition.
- Root alphabet-index section letters use the same Miuix action color as the
  scroll-to-top icon while idle. The actively dragged letter changes to solid
  black or white according to surface luminance; the floating selection
  indicator uses a semi-transparent Miuix gray (`secondaryContainer`) surface
  with Miuix primary blue text and icon. One expanded-title
  position is captured and reused, keeping the index 4 dp below it without
  responding to collapse frames. Only SearchBar visibility changes its top.
  Its bottom stays 12 dp above the mini player. The scroll-to-top action fades
  in only after collapse; dragging to the top still selects section `0` without
  expanding the title.
- Songs puts Search immediately before Sort. Music library provides one Search
  action and a tab-specific Sort action; the official Miuix `SearchBar` appears
  below its `TabRow`, remains open across tab switches, and swaps the query,
  hint, projection, and sort state with the selected Albums/Artists/Folders
  tab. Albums, Artists, and Folders switch only through TabRow taps; the nested
  pager disables user swiping so it does not conflict with the root pager. The
  Library TabRow retains the default Miuix outlined-tab visual while using
  20 dp side padding, 12 dp item spacing, 38 dp height, and 13 sp text. Tab taps
  switch selection directly without animating through intermediate tabs. Its
  background becomes transparent while Blur is active so the owning top bar's
  blur remains visible, while opaque fallback keeps the normal surface. Songs
  and Library search regions keep 12 dp under expanded large titles and 0 dp
  under collapsed small titles. Library keeps 6 dp below the TabRow and gives
  Search another 6 dp top padding, preserving a 12 dp total gap.
  Songs retains its title/file-name alphabet index in descending order by
  reversing the same section sequence used by Library.
- Root Album, Artist, and Folder searches match only their current-page primary
  titles: album name, artist name, or folder name. Album artists, counts, paths,
  and all metadata from songs inside those groups never participate.
- Albums are derived into 2-column or 3-column grids with album-name,
  album-artist, song-count, and year sorting. The 2-column layout uses a small
  square cover beside two text lines inside a horizontal rounded card; 3-column
  removes the outer card and places its rounded square cover and two labels
  directly on the page. Grid text is always the album name above only the
  localized song count. Album root grids use 20 dp side padding and every album
  grid style uses 12 dp row/column gaps. The 2-column style uses a 56 dp cover
  with 6 dp top/bottom/start padding, 12 dp end padding, and a 10 dp
  cover-to-label gap; 3-column uses 14 dp cover corners and a 6 dp start inset
  shared by its title and count. Both styles share `body2` title typography and
  explicit 12 sp count typography because Miuix has no matching text token.
  The style is persisted, with legacy 2/3-column values and the removed large-2
  option migrated to the remaining 2-column/3-column styles.
  Artists expose name, song-count, and album-count sorting.
  Both have a right index; artist root and detail layouts retain Lyrico-aligned
  16 dp margins. Album membership follows
  Lyrico's normalized album-name plus explicit album-artist key; MediaStore
  album IDs and track artists do not split one logical album. Artist membership
  splits track artists on `，`, `,`, `、`, `/`, and `&`, trims/collapses whitespace,
  de-duplicates per track by normalized name, and shows split artist names
  joined with ` / ` in track and album-header display contexts.
- Album and artist detail pages use empty-title `SmallTopAppBar`s because their
  fixed headers already show album or artist identity. Those headers are hosted
  in `bottomContent`, with the artist selector on the same fixed surface, so
  artwork and information share the outer Miuix blur/opaque fallback while the
  app bar itself stays transparent. Detail lists fill the page behind the bar
  with top content padding so scrolled content remains available to the blur
  backdrop. Album-detail song rows show artist-only descriptions,
  artist-detail song rows show album-only descriptions, and the artist-detail
  album tab follows the persisted root Album grid style.
- Folders group songs by normalized direct-parent path, not only by folder
  name. Rows show the folder name and localized song count followed by a
  shared-storage-relative display path. Folder rows use a colored 32 dp file
  icon with 30 dp start padding, 20 dp spacing before text, `body1` titles, and
  explicit 12 sp descriptions because Miuix has no matching text token. Song
  and Artist root rows use `headline2` titles and `footnote1` descriptions, and their
  trailing navigation indicator uses the same `MiuixIcons.Basic.ArrowRight`
  glyph as Settings `ArrowPreference`: 10 dp wide, 16 dp high,
  `onSurfaceVariantActions`, with its right edge 28 dp from the screen edge.
  Folder roots
  support name/song-count sorting and search; folder detail reuses the Songs
  page behavior for only that folder's direct songs. Library snapshot version 6 persists
  `MusicTrack.folderPath`, `MusicTrack.albumId`, and audio-property metadata.
- Local metadata follows Lyrico's descriptor-based TagLib path. A read-only
  MediaStore descriptor is duplicated and detached before TagLib reads actual
  title, artist, album, album artist, year/date, track number, disc number,
  duration, bitrate, sample rate, and channel count. Android 12+ separately
  reads the platform bits-per-sample value; unsupported devices or containers
  keep bit depth unavailable instead of guessing. MediaStore is only a
  field-level fallback when the embedded tag is absent. Startup reuses completed
  reads when ID, URI, modified time, and file size are unchanged; an explicit
  Scan action refreshes every file. Snapshot version 7 persists the added tag
  and bit-depth fields, while v6 snapshots force one complete refresh. Quality badges classify recognized
  lossless files at 88.2 kHz or 1,500 kbps as `HR`, other recognized lossless
  files as `SQ`, and non-lossless files at 256 kbps or above as `HQ`; unreadable,
  lower, or indeterminate files have no badge. `HR` uses `#FFD54F` in both light
  and dark modes.
- Album groups and detail headers use only the explicit album-artist field;
  they never inherit a track artist. A missing value displays the localized
  `Unknown album artist`. The detail header contains album name, album artist,
  an optional positive year, and song count. Year appears only when available;
  year and song count use 12 sp theme foreground text.
- Song-row descriptions place the 16 dp `ProjectBadge` before artist/album
  text. The 36 dp trailing More target has no pressed indication. All song
  surfaces share `TrackActionsOverlay`, which retains its selected song until
  Miuix reports that the bottom-sheet exit animation finished.
  Track-action summaries use the same `secondaryContainer` card background as
  the option group, 56 dp artwork with 12 dp left/top/bottom artwork-side
  padding, and a 20 dp Add-to-queue icon shifted 1 dp right with 2 dp more text
  gap. Album and Artist actions ellipsize after one line. A single artist opens
  its detail page; multiple artists open a titled artist-list sheet with real
  artist groups. Those artist rows keep 12 dp around their artwork and omit the
  trailing navigation indicator. The separate titled song-information sheet has
  a leading Close action, two metadata cards, and a localized unavailable value
  for unreadable bit depth; the untitled track-actions sheet does not add one.
  More and participating-artist sheets retain Miuix content overscroll while
  keeping nested-scroll dismissal enabled for downward sheet drags.
- The playback queue sheet uses the official `OverlayBottomSheet` directly.
  Its list viewport extends behind the transparent navigation bar while a
  scrollable bottom content inset keeps the final row reachable. Its content height follows
  the queue count and remains capped by the sheet maximum. It has a leading
  Close action and a Delete action whose visible glyph ends 28 dp from the
  screen edge. Queue rows are direct full-width `BasicComponent` options with
  no wrapping card, 44 dp artwork, 24 dp start padding, 16 dp end padding,
  12 dp top/bottom padding, no extra inter-item spacer, and only a circle-minus
  remove action on the trailing side. Its glyph shares the header Delete icon's
  28 dp visual right edge. The current row uses
  `BasicComponent.holdDownState` for the persistent Miuix indication instead of
  a custom colored rounded background, while its title stays the normal
  `onSurface` color. The lazy list opens at the current queue
  item. Navigation-bar inset plus 12 dp belongs to the lazy list's scrollable
  content rather than a separate fixed footer background. Playback artwork displayed with fit scaling clips
  and shadows the actual rectangular image bounds when embedded art is not square.
- Album, artist, and folder projections are built once per immutable track
  snapshot on `Dispatchers.Default` and reused by root/detail pages. Pager
  synchronization observes `currentPage` through `snapshotFlow` instead of
  invalidating the whole root composition. The pager retains one adjacent page
  only with Blur disabled; the default blurred path avoids an extra offscreen
  full-screen backdrop.
- Artwork-disk-cache eviction snapshots every candidate's timestamp and size
  before sorting. Never compare `File.lastModified()` live: fast alphabet-index
  traversal loads and touches many covers concurrently, which can otherwise
  violate TimSort's comparator contract and crash the app.
- The mini player is always present. Its empty state uses placeholder artwork,
  `Melox`, and a localized no-music message and cannot open the full player.
  It has no progress track and no outer press-scale feedback. Normal navigation
  uses the same surface and blur treatment as the bar, with 6 dp horizontal
  margins, a 6 dp navigation gap, a subdued matching outline, and 10 dp artwork
  top/bottom/start padding. Floating mini and navigation pills share one width,
  64 dp height, an 8 dp gap, captured backdrop, and gravity-following highlight.
  The mini result is clipped to its larger pill radius. The navigation uses the
  official Miuix 10 dp black drop shadow (20% dark / 10% light), which the
  floating mini player also uses; the 44 dp
  artwork keeps its additional 6 dp start inset.
- The floating mini-player queue action uses the Miuix default 24 dp playlist
  glyph and is shifted 2 dp toward the play/pause control; the normal bar keeps
  its existing alignment.
- Mini metadata supports horizontal previous/next swipes under edge alpha
  masks. Previous/Next remains 12 sp behind the translated metadata boundary.
  The normal artwork-side mask starts 4 dp farther left without moving resting
  metadata; the floating mask starts 8 dp farther left and its resting title
  and artist move 4 dp left. A vertical drag updates the bounded shared-player
  progress directly from distance divided by full-player height. Upward release
  settles open and non-zero downward release settles closed with the same
  critically damped spring. The stable host accepts only vertical-dominant
  drags so horizontal metadata swipes remain
  track changes. Entering an eligible Previous or Next commit region for a
  different queued track triggers a system threshold haptic. Returning below
  the threshold rearms that direction, and reversing into the opposite commit
  region triggers its own haptic. The trailing action
  opens the queue. The player page remains over the retained root, and one
  reversible shared-player progress expands a squircle container between the
  recorded mini/full layers while crossfading their content early in the path.
  Container center/height interpolate linearly while width uses `EaseInCubic`.
  A separate artwork overlay travels between measured root-coordinate bounds. The cover grows
  uniformly while moving right and upward into the full-player position;
  rotation, skew, and narrow-top/wide-bottom deformation are excluded. The
  recorded mini layer retains Melox's `MiniPlayerChrome` and `miniPlayerSurface`
  Miuix blur/liquid-glass parameters rather than adopting VMusic's backdrop.
  Back and rejected gestures reverse the active path instead of jumping through
  the other endpoint.
- The floating mini-player shadow is drawn outside the shared transition clip,
  with its Miuix dark/light opacity multiplied by the mini-layer handoff alpha.
  Expand and collapse therefore fade the shadow continuously instead of
  restoring it only when the mini player reaches its endpoint.
- Mini-player horizontal metadata swipes commit only after the Previous/Next
  label fully clears the edge mask. Committed swipes return from the release
  offset to rest using the same path as uncommitted swipes, crossfading old and
  new metadata during that return. Mini-player artwork uses the same 320 ms
  FastOutSlowIn track-artwork crossfade timing as the full player.
- Full-player background reuses the cached artwork bitmap and builds an 8-by-8
  HCT color field off the main thread. Pixel hue is retained, realized chroma is
  capped at 32, and tone is fixed to 48 in light theme or 24 in dark theme. The
  field drives one bilinearly filtered 4-by-4 background seeded from the center
  4-by-4 source region. Matching pixels in its four 2-by-2 quadrants orbit
  through center, side, outer-corner, and vertical-side regions: top-left and
  bottom-right clockwise at 24 then 18 seconds, top-right and bottom-left
  counterclockwise at 18 then 24 seconds. A separate 18-second phase rotates the
  complete 4-by-4 field through clockwise quarter-turn pixel mappings, including
  each pixel's local coordinate, so cardinal endpoints preserve adjacency rather
  than forming a center cross. ARGB interpolation and the VMusic rendering path (`BitmapPainter` with
  `FilterQuality.Low`, then `Image` with `ContentScale.Crop`) keep adjacent
  spatial and temporal color transitions smooth. The
  bitmap itself stays fixed and fills the viewport without geometric rotation,
  animated scale, translation, a second layer, or perspective deformation. Both
  phases advance only while the settled player is expanded, resumed, and the
  current song is playing; pause preserves their current progress.
  Missing artwork
  falls back to fixed `#242424` in both themes; no bitmap blur, `AndroidView`, or local
  AAR is part of this path.
- The persistent mini player's recorded blur layer is explicitly invalidated
  from root-pager offsets and a bounded route-transition draw signal. This
  keeps its backdrop current without recomposing the full page tree. Retained
  navigation entries read the latest bottom inset, and root alphabet indexes
  use that live inset so their bottom stays 12 dp above the mini player while
  navigation enters or exits.
- Playback mode cycles `ORDER -> REPEAT_ONE -> RANDOM` with direct icon
  replacement and no icon transition animation. Order and Repeat-one
  horizontally mirror the Miuix loop glyph so its arrows read clockwise, while
  the separate Repeat-one `1` badge remains unmirrored. Order and Random use Media3 repeat-all,
  Repeat one uses repeat-one, and Random physically shuffles once before
  walking that displayed queue. Returning to Order restores stable source
  order. Switching modes dynamically moves queue items and changes repeat
  state without replacing, rebuilding, preparing, or audibly interrupting the
  current media item. New
  visible queues inherit the current mode instead of resetting to Order, and
  snapshots restore that mode after app restart. Previous and Next controls
  always jump directly to the adjacent queued item instead of restarting the
  current song first. Play-next inserts a stable fractional source position so
  it remains next after later mode changes.
- The playback summary snapshot is loaded by both the controller and service.
  The service preloads its last current item before exposing the media session,
  so the mini player is not replaced by a transient empty queue while full
  snapshot validation completes asynchronously.
- The app is fully offline and does not declare `INTERNET` or
  `ACCESS_NETWORK_STATE`.
- AppCompat `1.7.1` is required with the current Miuix stack. AppCompat `1.6.1`
  does not install the NavigationEvent view-tree owner expected by Miuix
  `SearchBar` and causes an activity-start crash.
- Versioned library and playback snapshots live in `noBackupFilesDir`.
  Playback restore preserves the current item's position when invalid entries
  before it are pruned, and resets the position when the current item is gone.
- The first deliverable is a verified Debug APK at `artifacts/Melox-debug.apk`.

## Known Constraints

- `/Users/bocchi/Downloads/AGENTS.md` and `/Users/bocchi/Downloads/CLAUDE.md` were unavailable during planning; `/Users/bocchi/Code/AGENTS.md` and `/Users/bocchi/Code/CLAUDE.md` are the accepted substitutes.
- `https://github.com/Inefy-03/Melox` was not publicly accessible during planning, so the first About page must not expose a dead source link.
- The Miuix blur artifact declares an Android 13 baseline. Supporting Android 9 requires manifest override plus strict runtime gating and an actual API 28 validation pass.
- Preserve Apache-2.0 headers and attribution in migrated custom liquid-glass code. Do not migrate GPL-only IconEditor navigation code.
- After changing the application ID to `com.melox.player`, Android Studio run state must not retain the old `com.inefy.melox` package. If Run `app` starts
  `{com.inefy.melox/com.melox.player.MainActivity}`, refresh `.idea/workspace.xml`
  package-state entries, Sync Gradle, and reinstall the app.

## Verification Record

- `:app:testDebugUnitTest`, `:app:lintDebug`,
  `:app:connectedDebugAndroidTest`, and `:app:assembleDebug` passed after the
  Navigation3, floating-bar, defaults, and locale fixes on 2026-07-26.
- A final source audit against the local official Miuix `v0.9.3` tag confirmed
  that secondary transitions, predictive back, floating navigation, backdrop
  blur, glass highlight, and Monet modes use the official APIs and example
  patterns.
- Four instrumented tests passed on API 26 and API 37. API 26 used opaque
  blur/liquid-glass fallbacks; API 37 covered the modern visual path.
- Runtime regression on API 37 toggled predictive back off, on, and off while
  Theme settings stayed visible; the process remained alive and the crash
  buffer stayed empty. The enabled handler also completed an edge-back gesture.
- The pre-fix crash was reproduced as `Key THEME_SETTINGS was used multiple
  times`: swapping two `NavDisplay` hosts registered one visible entry in two
  saveable-state providers. Retaining one host fixes the cause.
- API 26 rendered the opaque iOS-like floating bar after enabling it and
  returned to Settings without a shader-path crash. API 37 visually confirmed
  the same iOS-like structure with Blur disabled.
- API 37 UI-tree verification confirmed Blur and Predictive back are selected
  on a fresh install, Dynamic colors is last in Appearance, predictive back
  returns to the root Settings page, and Simplified Chinese remains selected
  when the language popup is reopened without a page refresh.
- API 37 visually confirmed the Miuix About background, monochrome icon,
  app-name/version identity, and initial options layout. A following scroll
  moved the options under the collapsed About bar while the header exited.
- On 2026-07-30, the About page crash was reproduced on API 37 as a native
  RenderThread `SIGSEGV` after 512 recursively repeated render frames. The
  page-level recording backdrop had also been supplied to descendant
  `textureBlur` consumers. Restoring the official two-backdrop hierarchy
  eliminated the cycle; entering About, collapsing it twice, and expanding it
  twice kept the same process alive with an empty crash buffer. Visual
  inspection confirmed the official texture-blurred Developer card and the
  drawable monochrome vector icon.
- API 37 measured the mini-player phone bounds at 12 dp from both horizontal
  edges and 12 dp above navigation. The first process-local language switch
  updated Chinese to English in place with the same PID (`13241`) and no crash.
- API 26 opened About through the opaque fallback with PID `10592` and an empty
  crash buffer.
- API 37 visually confirmed the full-player header, centered 12 dp artwork,
  lowered white progress indicator, and two-row control hierarchy. A progress
  tap moved playback to `1:48`, and a following drag moved it to `2:44`; the
  process stayed alive with an empty crash buffer.
- API 37 visually confirmed the Theme settings secondary bar with Blur enabled.
  The latest four instrumented tests passed again on both API 26 and API 37
  after the secondary-bar and full-player changes.
- The API 37 Pixel 10 Pro image reports a 16,384-byte page size.
- External `audio/*` input, system media-session metadata, media-button
  handling, queue/position restore without autoplay, and screen-off playback
  were exercised. During the screen-off check, playback advanced while the
  device reported `Asleep`.
- The final APK is v2-signed, archive-valid, and 16KB zip-aligned. Its
  SHA-256 is
  `0718ea0473f6c0c8b0b190f2ed4704de3803861e8c2da88facc62fee446d582e`.
- On 2026-07-27, unit tests, Lint, Debug assembly, and all four instrumented
  tests passed on both API 26 and the API 37 Pixel 10 Pro 16KB image after the
  five-destination folder library, fixed indexes, bottom-bar geometry, and
  player-gesture work.
- API 37 UI-tree inspection confirmed all five navigation items, Search before
  Sort, a Miuix SearchBar without Cancel, and fixed index bounds. At 3x density,
  revealing SearchBar moved the index start down by 48 dp; collapsing the title
  then left the index at exactly the same vertical position and faded in the
  scroll-to-top action.
- API 37 grouped the five test songs as `Music`, displayed
  `5 songs - /Music`, and opened a folder detail page containing exactly those
  five songs with its own Search and Sort actions.
- API 37 measured normal mini-player margins and navigation gap at 6 dp.
  Floating mini player and navigation both measured 64 dp high and 1136 px
  wide at 3x density, with a 6 dp gap and no navigation drop shadow.
- API 37 visually confirmed the updated full-player hierarchy, smaller
  transport icons, white interactive progress track, artwork/background, and
  three secondary actions. The progress track measured 6 dp idle and 10 dp
  while held without changing width. Gesture-frame capture confirmed a dimmed
  root during dismissal, and both API 26 and API 37 crash buffers stayed empty.
- API 37 UI-tree and screenshot verification confirmed solid white root-index
  letters in dark mode, one `Music` album card for two tracks, and a `Music`
  detail page containing `Melox-QA-Hero` and `Melox-QA-Ping`.
- Focused API 37 root traversal with Blur disabled improved janky frames from
  93.85% to 85.07%, median frame time from 53 ms to 48 ms, P90 from 101 ms to
  77 ms, and P95 from 121 ms to 81 ms. With default Blur enabled, P90 improved
  from 150 ms to 117 ms, P95 from 200 ms to 150 ms, and slow UI-thread frames
  from 32 to 26; the debug-emulator jank rate remained 100%.
- After the album-identity, index-color, and root-pager optimization, unit
  tests, Lint, Debug assembly, and all four instrumented tests passed on both
  API 26 and API 37. API 26 and API 37 runtime crash buffers remained empty.
- On 2026-07-27, `compileDebugKotlin`, unit tests, Lint, and Debug assembly
  passed after the four-root Home/Songs/Music-library/Settings navigation,
  full-bound player sharing, retained Lyrics page, `playWhenReady` controls,
  proportional Miuix progress press feedback, live mini-player backdrop
  refresh, and live index-bottom inset changes.
- No emulator was already running for that focused verification, so no new
  screenshot or frame-by-frame claim was made and no emulator was started.
  `artifacts/Melox-debug.apk` is archive-valid, v2-signed, 16KB zip-aligned, and
  has SHA-256
  `73d2b5e4484493769ee21e9c2869b38327b1343a14138df5ba5083404f044a6a`.
- On 2026-07-27, `compileDebugKotlin`, 36 unit tests, Lint, and Debug assembly
  passed after Lyrico-style TagLib property reads, snapshot reuse, HR/SQ/HQ
  badges, mini-player mask/label geometry, indication-free song More targets,
  and retained action-sheet exit content. Per the requested focused-validation
  boundary, no emulator was started; the bottom-sheet motion is compile/static
  verified rather than frame-by-frame runtime verified. The final APK contains
  TagLib for arm64-v8a, armeabi-v7a, x86, and x86_64; arm64/x86_64 ELF LOAD
  segments and APK entries pass 16KB alignment checks. The APK is archive-valid,
  v2-signed, declares no network permission, and has SHA-256
  `ec9a1cdc95d57564a6c66f64e546e8e4c469402da2c5b74353c08dd706fff9a8`.
- On 2026-07-28, visible search state was separated from Miuix `InputField`
  focus state. A search left open retains its query across root-page switches
  without requesting focus or briefly opening the keyboard; only an explicit
  Search action or field tap focuses it.
- The custom Miuix search cleanup icon writes directly to the visible query
  state. This preserves unfocused-query retention while keeping the trailing
  cleanup action effective after root-page focus is cleared.
- On 2026-08-03, opening an album, artist, or folder from filtered Library
  clears the root search focus and hides the IME before the secondary route
  transition. The query and filtered results remain intact, but returning no
  longer reopens the keyboard or competes with the page animation.
- Albums, Artists, and Folders now share one stationary root index overlay.
  Its top gap is 4 dp, its bottom gap is 12 dp for normal navigation and 6 dp
  for floating navigation, and the retained root inset prevents position jumps
  while the navigation bar enters or exits.
- Mini/full-player visibility uses one reversible shared-player progress while
  the root remains composed, preserving root list positions. Mini/full roots
  publish measured bounds and record independent layers for one expanding
  squircle container; their artwork publishes separate bounds for one overlay
  image. Bar/page layers crossfade on the same progress. The 420dp decode bucket
  remains a cache size, not an animation duration.
- Artwork requests use bounded pixel-size buckets with in-flight deduplication,
  retain the previous bitmap during a track change, and throttle disk-cache
  access timestamp writes. A prefetch effect outside the mini/full visibility
  scopes keeps current and adjacent full-player buckets warm in both states.
  Playback artwork keeps display size separate from requested decode size.
  Cached bitmaps preserve source aspect ratio and never upscale; playback
  artwork uses `Fit`, while library cards retain `Crop`. The mini player uses
  its bar-sized request and the full player uses the full-player request; the
  requests are independent of opening and closing. Full artwork keeps its
  320 ms old/new bitmap blend, while the atmosphere retains its last completed
  HCT field until the replacement is ready and then uses an independent 640 ms
  color interpolation. That blend is not a mini/full transition.
- Non-root Navigation3 scene backgrounds render full-screen behind the retained
  mini player and system-bar inset. Only their interactive content consumes the
  live player bottom inset. The scaffold backdrop therefore samples the current
  secondary or tertiary page rather than an empty black/white surface or a
  retained root frame. Theme settings delays bottom-scaffold replacement until
  the official Miuix dependent-option visibility motion completes.
- Release signing reads the ignored project-root `local.properties` keys
  `melox.keystore.path`, `melox.store.password`, `melox.key.password`, and
  `melox.key.alias`. The ignored local release keystore is `InefyKey.jks` with
  alias `InefyKey`; password values may stay blank until the maintainer fills
  them locally. Credentials are not recorded here. Release tasks fail during
  configuration if the keystore file, properties file, or any value is missing,
  so `assembleRelease` cannot silently produce an unsigned release APK. The
  root `assembleRelease` task wraps `:app:assembleRelease` and prints the
  signed APK path for Android Studio Terminal use.
- Focused verification on 2026-07-28 was intentionally limited to
  `compileDebugKotlin`, Debug assembly, keystore alias inspection, and final
  archive/signature/alignment checks. No emulator or screenshot validation was
  run. `artifacts/Melox-debug.apk` is archive-valid, v2-signed, 16KB
  zip-aligned, and has SHA-256
  `5d544a511cd7d281917c6166ae2eb391a78fed02e87f2f371df550b855758b43`.
- On 2026-07-28, the environment-only `InefyKey` signing configuration
  produced `artifacts/Melox-release.apk`. Release compilation, signing
  validation, vital lint, and assembly passed. The APK is archive-valid,
  v2-signed by the expected `CN=Inefy, OU=Melox, O=Inefy, C=CN` certificate,
  16KB zip-aligned, and has SHA-256
  `cd150f350a62e33ab2d9064cb65fa8b61779494679625626b1bcb2a08c49fdb3`.
- On 2026-07-28, Release switched from disabled optimization to the stable AGP
  9.2 legacy R8 configuration: `isMinifyEnabled = true`,
  `isShrinkResources = true`, and `proguard-android-optimize.txt`. Do not use
  `optimization.enable = true` on this AGP version unless intentionally opting
  into the experimental `android.r8.gradual.support` path.
- App keep rules preserve line positions and normalize source names for retracing. Android
  component/native/enum/Parcelable/annotation rules come from the optimized
  defaults, and TagLib's AAR supplies its own JNI consumer rule. The final R8
  configuration contains no global `-dontshrink`, `-dontoptimize`, or
  `-dontobfuscate` and no package-wide Melox/Miuix keep rule.
- The optimized DEX retains manifest entry points `MainActivity` and
  `PlaybackService`, plus the TagLib JNI bridge. R8 produced a non-empty
  59 MB mapping and reduced the signed APK from about 17 MB to 8,065,907 bytes.
  `artifacts/Melox-release.apk` is archive-valid, v2-signed, 16KB zip-aligned,
  and has SHA-256
  `ca6837a9f54809320764f22bb5492d38a108abca018da4b016759ad651a56d68`.
  The matching retrace map is `artifacts/Melox-release-mapping.txt` with
  SHA-256
  `2641bd83ef662af30a6d7d80e24ea79e514e8818f0486d71149a2c95aa55baf7`.
  No emulator was started, so runtime flows remain static/build-verified rather
  than device-smoke-tested.
- On 2026-07-28, `compileDebugKotlin` passed after the serialized player gesture
  seeking, spring settle/reverse path, deferred artwork shadow, 80% paused
  artwork, synchronized cover/background crossfade, persistent adjacent-cover
  prefetch, retained root bottom-strip layer, Miuix liquid-glass option motion,
  persisted default root destination, and English `Library` label changes. Per
  the requested focused-validation boundary, no emulator, screenshot, full
  test suite, or APK rebuild was run; motion remains compile/static verified.
- The retained root bottom-strip layer from that pass was incorrect: it clipped
  secondary routes at the mini-player top and exposed root/empty surfaces below.
  On 2026-07-28 it was removed, non-root routes were restored to full-screen
  background rendering with content-only bottom insets. A follow-up found that
  outgoing-content retention regressed the mini title/artist shared animation,
  so player dragging moved to the stable outer shared-player host instead.
  `compileDebugKotlin` passed; no emulator or frame-by-frame runtime claim was
  made.
- On 2026-07-28, the root player scaffold was made persistent beneath a
  seekable full-player overlay so Songs list state remains composed across
  player open/close. Release follows the final vertical direction. The
  experimental reference-derived artwork/surface easing changes were reverted;
  text retains only the requested color interpolation and aligned whole-length
  crossfade.
  `compileDebugKotlin` passed after this focused rollback; no emulator,
  screenshot, full test suite, or APK rebuild was run.
- On 2026-07-29, the mini title returned to Miuix `onBackground` and the artist
  to `onSurfaceVariantSummary`; shared progress resolves them to the full-player
  header colors. The shortened/full text transition again crossfades the whole
  length, with height-based scaling and a common Start anchor keeping the
  shared prefix aligned. `compileDebugKotlin` passed; no emulator, screenshot,
  full test suite, or APK rebuild was run.
- On 2026-07-29, the full player adopted normal and 20%-deeper artwork-derived
  light-mode color tiers for header, lyrics, progress, and controls. Lyrics now
  match the playing-artwork/progress width, enlarge primary lines slightly, and
  render additional same-timestamp lines at 80% as translations. Artwork uses
  100% while playing, 90% while paused, and a 102%-to-100% resume rebound.
  `compileDebugKotlin` passed; no emulator, screenshot, full test suite, or APK
  rebuild was run.
- On 2026-07-29, Theme settings gained an immediate local theme-mode selection
  shared by its preference value and popup row. Language changes now apply
  directly from selection without the former 150 ms popup-exit delay. Root and
  folder-detail indexes capture one expanded-title top anchor, add SearchBar
  height separately, use the scroll-top action color while idle, and switch the
  dragged letter to luminance-aware black/white. `compileDebugKotlin` passed;
  no emulator, screenshot, full test suite, or APK rebuild was run.
- On 2026-07-29, API 37 frame capture confirmed continuous full-player
  dismissal into the mini player with one artwork image scaling and moving
  throughout, without the former instant exit or top-left thumbnail frame.
  Short upward mini drags returned closed, drags beyond one mini height opened,
  horizontal metadata swipes changed tracks without opening, and a downward
  drag reversed upward before release reopened from its current progress.
  Compile, unit tests, Debug assembly, APK archive validation, and final device
  smoke passed; the process remained alive and the crash buffer stayed empty.
- On 2026-07-29, the maintainer rejected the transition precomposition,
  partial-seek manual reversal, one-frame full-surface enter, settled full-bleed
  underlay, lyric-row clipping, deep secondary controls, and golden light `HR`
  changes because opening the mini player covered the root background and
  enlarged/cropped artwork. The previous `animateTo`, `verticalDrag`, 180 ms
  surface enter, transparent full-player scaffold, lyric padding, secondary
  control color, and `0xFFB87800` light `HR` color were restored. The
  first-complete-measurement guard for the library index remains.
  `compileDebugKotlin` passed; device motion was not rerun because additional
  tool permission remained unavailable.
- On 2026-07-29, the player shared transition kept the accepted background
  handoff unchanged while moving all shared geometry onto one 560 ms timeline.
  The surface and text use front-loaded curves with zero endpoint velocity;
  artwork uses a slow-fast-slow curve with the same zero-velocity finish. All
  surface edges share one bounds curve. Rejected drag outcomes now reverse the
  active seek fraction toward its origin instead of retargeting through the
  opposite endpoint, and pointer settlement waits for release so a stationary
  held drag remains in place. Compile, unit tests, Debug assembly, and APK
  archive validation passed. API 37 emulator checks confirmed short mini drags
  return closed, longer drags open, a two-second held full-player drag stays at
  its seek position, and an upward final direction reopens directly. The app
  process remained alive and the cleared crash buffer stayed empty.
- On 2026-07-29, the shared-player master timeline changed from 560 ms to
  360 ms. One `PlayerTransitionVisualState` now derives root dim alpha,
  surface/artwork/text progress, both corner interpolations, artwork
  scale/shadow progress, controls alpha, and full-bleed background alpha. Unit
  tests lock every previous normalized formula in both directions. The former
  100 ms surface handoff and 180 ms full-surface entry scale to 64 ms and
  116 ms, retaining their normalized positions; the endpoint visibility
  handoff remains one millisecond. Compile, the focused
  transition test, and the complete Debug unit-test suite passed. API 37
  runtime checks confirmed tap open/back close, short-drag rejection, long-drag
  opening, a live process, and an empty crash buffer.
- On 2026-07-29, focused unit tests passed for descending alphabet fallback and
  compact-2/large-2/3-column album-grid persistence migration; the same Gradle
  task compiled Debug production and test Kotlin. No emulator, screenshot, full
  test suite, or APK rebuild was run.
- On 2026-07-29, Home recommendation cards returned to a fixed-width carousel
  with 16 dp applied only to the pager's outer start/end padding. Candidate
  tracks now resolve through the shared artwork cache and missing-artwork
  tracks are excluded before the random queue is shown. Album compact-2 follows
  the horizontal card reference, while 3-column uses a borderless cover and
  label stack. The focused Debug unit-test task compiled production/test Kotlin
  and passed; no emulator, runtime screenshot, full suite, or APK build ran.
- On 2026-07-29, Album root grids moved to 24 dp side padding. Compact-2 reduced
  its cover to 48 dp. The borderless 3-column layout uses 12 dp row/column
  spacing and cover corners, an 8 dp title-only horizontal inset, and smaller
  count text. A forced Debug Kotlin compilation passed; no emulator, runtime
  screenshot, test suite, or APK build ran.
- On 2026-07-29, all Album grid styles unified song-count typography on
  `footnote2`. Light-mode `HR` shifted from `#B87800` to the more yellow
  `#B88600`, based on a darkened version of the official Hi-Res AUDIO logo's
  visible gold hue; dark mode stayed unchanged. A forced Debug Kotlin
  compilation passed; no emulator, runtime screenshot, tests, or APK build ran.
- On 2026-07-29, light-mode `HR` was explicitly unified with dark mode at
  `#FFD54F`. Compact-2 changed to a 56 dp cover with 4 dp
  top/bottom/start padding, while 3-column title typography moved to `body2`
  and its song count adopted the same 8 dp start inset. A forced Debug Kotlin
  compilation passed; no emulator, runtime screenshot, tests, or APK build ran.
- On 2026-07-30, Album root grids moved to 20 dp side padding. Compact-2 kept a
  56 dp cover with 6 dp top/bottom/start padding. Compact-2, large-2, and
  3-column all use 12 dp row/column gaps. Album title weight was reduced from
  bold to semibold, and the 3-column title/count inset changed from 8 dp to
  6 dp. The 3-column cover corner radius is 14 dp.
- On 2026-07-30, the Album style menu was reduced to `2列` and `3列`.
  The former `2列（小）` was renamed to `2列`; `2列（大）` was removed from the
  menu and render branch. Legacy large-2 persisted values migrate to `2列`,
  while stored 3-column values remain `3列`. Album sort field label changed
  from `专辑` to `专辑名`, and the 2-column card end padding changed from 8 dp
  to 12 dp.
- On 2026-07-30, the first root navigation item/page changed its Simplified
  Chinese label from `主页` to `首页`. The root pager now initializes directly
  from the synchronously loaded persisted `DefaultHomePage`; the former
  post-composition scroll from Home was removed, so Songs/Library startup has
  no visible selection movement. A forced Debug Kotlin compilation passed; no
  emulator, runtime screenshot, tests, or APK build ran.
- On 2026-07-30, Home recommendation ownership moved from the disposable Home
  pager page to the retained root composition. A saveable session seed,
  selected IDs, request flag, and completion flag prevent page revisits from
  restarting artwork checks or rerandomizing results. Artwork eligibility now
  probes ordered batches of eight concurrently and keeps the seeded result
  order. The focused recommendation test and Debug production/test Kotlin
  compilation passed; no emulator, runtime screenshot, full suite, or APK build
  ran.
- On 2026-07-30, full-player drag release adopted Flamingo's velocity-direction
  destination rule above Android's scaled minimum fling threshold. Slow and
  near-zero releases retain Melox's last-direction fallback. Debug Kotlin
  compilation, focused and complete Debug unit tests, and Android Lint passed.
  API 37 emulator checks confirmed fast downward close, held near-zero
  downward close, fast upward reversal reopen, and held near-zero upward-tail
  reopen. The app process remained alive, the crash buffer stayed empty, and
  the final full-player screenshot showed no overlap.
- On 2026-07-30, shared playback artwork separated its display and decode
  sizes. Every non-zero transition fraction now uses the prefetched 420 dp
  bucket; only the fully collapsed playback bar uses its 44/48 dp bucket.
  Debug Kotlin compilation, focused and complete Debug unit tests, and Android
  Lint passed. API 37 held-frame captures confirmed the opening upgrade occurs
  while the cover is still playback-bar sized and the same partially dismissed
  frame remains sharp where the previous build was visibly pixelated. The app
  process remained alive and the crash buffer stayed empty. The verified Debug
  APK was copied to `artifacts/Melox-debug.apk`; archive validation passed with
  SHA-256
  `d331202a0f50e0cf7aa68a8b8b64230cfe4018f97aff700a625cc82877e50d69`.
- On 2026-07-30, the maintainer-supplied Play, Pause, Previous Track, and Next
  Track SVG paths were converted without geometry changes into tintable Android
  vector resources. Full-player transport controls use all four; the mini
  player uses Play/Pause. Existing sizes, touch targets, descriptions, colors,
  and icon transitions remain intact. Forced Debug resource/Kotlin compilation
  passed; no emulator, runtime screenshot, tests, or APK build ran.
- On 2026-07-30, Previous Track and Next Track vectors were refreshed from the
  latest supplied SVGs; their separator bars now use the updated capsule
  geometry while retaining the 56 by 49 viewport.
- On 2026-07-30, full-player Play/Pause was set to a 40 dp icon inside a 64 by
  64 dp Miuix touch target, while Previous/Next was set to 32 dp icons inside
  56 by 56 dp targets. Mini-player Play/Pause was set to a 20 dp icon inside a
  40 by 40 dp normal-bar target or a 36 by 36 dp floating/liquid target, with
  the target and icon shifted 6 dp left without moving the queue control.
  Forced Debug resource/Kotlin compilation passed; no emulator, runtime
  screenshot, tests, or APK build ran.
- On 2026-07-30, artwork cache schema 3 replaced square center-cropped
  thumbnails with bounded aspect-preserving decodes. Mini/full playback covers
  use `Fit`; existing library cards keep `Crop`. Opening targets the prefetched
  full bucket at zero progress and the mini cover crossfades low/high for 96 ms;
  closing crossfades high/low only at the collapsed playback bar. Debug Kotlin
  compilation, focused and complete Debug unit tests, and Android Lint passed.
  API 37 held-frame checks covered resting mini, early opening, and partial
  dismissal without placeholder, geometry jump, or sharpness loss. Available
  device covers were square, so landscape/portrait no-crop behavior was covered
  by pure dimension tests. The process remained alive and crash logs were
  empty. The verified Debug APK was copied to `artifacts/Melox-debug.apk`;
  archive validation passed with SHA-256
  `582fe472b9de037c9a510905c6650ec47d9810600d7086f3fcdc587b34b7e457`.
- On 2026-07-30, the playback artwork's standard two-sided `Crossfade` was
  replaced with an opaque-base stacked fade. The outgoing resolution now stays
  fully opaque while the incoming resolution fades above it for the existing
  96 ms, eliminating the high-to-low opacity dip at the collapsed endpoint
  without changing request timing or the 360 ms player transition. Debug Kotlin
  compilation, focused and complete Debug unit tests, Android Lint, and API 37
  held/immediate-release frame checks passed; the process remained alive and
  crash logs were empty. The rebuilt `artifacts/Melox-debug.apk` passed archive
  validation with SHA-256
  `0b6517edf5bb63ebe42f2dccdf90fce55e9419017972baef3b9ebe92da165003`.
- On 2026-07-30, mini-player Play/Pause adopted the full player's shared
  fade-scale icon transition without changing icon size, touch target, or
  offset. Full-player artwork pause scaling and both resume segments now use
  `LinearOutSlowInEasing`, preserving 90% paused scale, 102% resume overshoot,
  and the return to 100%.
- On 2026-07-30, the mini-player upward-opening distance threshold was removed.
  Any accepted vertical-dominant upward drag with positive travel now opens on
  release; horizontal metadata swipes remain isolated from player expansion.
  Debug compilation, unit tests, Debug assembly, and APK archive validation
  passed. API 37 confirmed a 60 px upward drag opens, a horizontal swipe changes
  tracks while staying collapsed, the process remains alive, and the cleared
  crash buffer stays empty. The verified `artifacts/Melox-debug.apk` has
  SHA-256
  `4444190849e18df9f5ce80bbd25a13df0e38ece83ba040277debb4b5939c36b6`.
- On 2026-07-30, the shared-player surface changed to linear bounds so its top
  edge follows complete pointer displacement from the original down position.
  Artwork now uses the symmetric near-linear
  `CubicBezierEasing(0.25f, 0.15f, 0.75f, 0.85f)` curve; title/artist bounds use
  a small bounded lead while their fade/color remains delayed. Focused and
  complete Debug unit tests and Android Lint passed. API 37 held-frame checks
  at approximately 25% and 50% in both opening and closing directions confirmed
  the background top stays above fully visible title/artist text and artwork.
  Release settled to both endpoints, the process remained alive, and the crash
  buffer stayed empty. The verified `artifacts/Melox-debug.apk` passed archive
  validation with SHA-256
  `5098aa69f2406fe9d4aaa0dfdbcb5ffe50605644fc5519beb048a4b6109fa013`.
- On 2026-07-30, the shared-player surface adopted a reversible
  distance-balanced bounds path. At 180 ms its side/bottom/top edges reach
  50%/54%/58%; every edge has 3 dp remaining at 300 ms and 0.5 dp at 330 ms,
  then all four reach the screen at 340 ms while the corner radius reaches the
  physical screen radius. During 340-360 ms, the existing shared-surface
  artwork backdrop overscans beyond the viewport; it does not create a second
  bitmap or solid background, and player content remains unscaled. Focused and
  complete Debug unit tests and Android Lint passed. API 37 held-frame checks
  covered 50%, both equal-distance keyframes, edge arrival, settled overscan,
  and the reverse order; the process remained alive and crash logs were empty.
  The verified `artifacts/Melox-debug.apk` passed archive validation with
  SHA-256
  `a4835f6fcbdef28f20d9619f69caf09d5848be10cf7eba19fc98878b890d2e0e`.
- On 2026-07-30, the final shared-surface phase was corrected to keep viewport
  bounds fixed and resolve the physical screen corner radius to zero instead
  of scaling the artwork backdrop outside the viewport. Artwork, title, and
  artist now finish at 340 ms and hold during that 20 ms corner-only phase.
  Closing first restores the physical corner while those elements remain at
  their full endpoints, then starts their exact reverse movement with the
  surface edges. The background remains one shared-surface source for future
  live transition blur. Debug Kotlin compilation and the focused shared-player
  test passed. API 37 opening/closing held frames confirmed fixed content
  geometry during the corner phase, synchronized movement after the top edge
  leaves, a live process, and an empty crash buffer. A subsequent complete
  Debug test/Lint run was blocked by the Codex tool usage limit. The verified
  `artifacts/Melox-debug.apk` passed archive validation with SHA-256
  `b89fe305907fed48a3dd1d5422f6141766dba8bd7a78efa1e2db3aeb83cd4e08`.
- On 2026-08-01, queue and track-action sheets standardized on the Miuix 0.9.3
  sheet-owned header and 24 dp inside margin. Queue is always maximum height,
  uses a centered official title plus trailing Delete action, persists the
  current row's whole-surface selection, extends its background through the
  navigation-bar region, and confirms Clear in an `OverlayDialog`. Track actions
  remain untitled, reuse the song-row summary, group all
  actions into one preference-style card, and route Album/Artist actions through
  the existing detail navigation. Library list title/summary typography now
  follows `BasicComponent` (`headline1` Medium and `body2` default weight).
  API 37 verification confirmed the queue background covers the gesture-
  navigation inset, Clear exposes the expected `OverlayDialog` semantics, the
  untitled track-action sheet exposes all five actions, and Album opens the
  matching detail page with no crash-buffer entries. Debug unit tests, Lint,
  and assembly passed. The final `artifacts/Melox-debug.apk` archive passed
  validation with SHA-256
  `ea52490d68e6881df4014944ac30ce379c56c294b0ad2545141c45cc3a7e26bf`.
- On 2026-08-01, the track-action sheet's lower action group adopted the Miuix
  bottom-sheet example's explicit `secondaryContainer` card background so the
  options read as lighter item surfaces instead of the sheet's plain card
  surface. The Add-to-queue action uses the maintainer-supplied
  `ic_add_list` tintable vector resource.
- On 2026-08-01, mini/full playback was replaced with a Flamingo-style player
  sheet: one direct drag progress drives a bottom-origin 93%-to-full sheet and
  settles with `spring(stiffness = 400f, dampingRatio = 1f)`. Artwork now uses
  a direct `FastOutSlowInEasing` shared-element path, and title/artist expand
  from the top edge into the centered safe-inset-plus-16-dp header.
  Floating mini and navigation pills share a clipped backdrop, the same
  gravity-following highlight, and the official Miuix 10 dp drop shadow on the
  navigation pill. Debug Kotlin
  compilation, all Debug unit tests, and Debug Lint passed. Per the requested
  scope, no emulator was started and no runtime visual claim was recorded.
- On 2026-08-05, the full-player background replaced the former experimental
  backdrop implementation with the requested VMusic-style
  8-by-8 HCT field with hue retention, realized chroma capped at 32, tone
  48/24, cropped full-screen presentation, fixed `#242424` missing-artwork fallback,
  and a single center-seeded 4-by-4 field. Matching pixels in its four 2-by-2
  quadrants use the clockwise/counterclockwise 24/18-second orbit pairs, while
  the complete grid rotates through quarter-turn pixel mappings every 18
  seconds without rotating the bitmap geometry. The VMusic `BitmapPainter` /
  `FilterQuality.Low` / `ContentScale.Crop` path provides bilinear spatial
  filtering, while continuous
  ARGB interpolation prevents temporal hard cuts or cardinal-angle center
  crosses. Theme changes retain the sampled source pixels, output bitmap, and
  painter, remapping only HCT tone. Both phases advance only on
  the settled, resumed full player while
  the current song is playing; pause preserves their current progress.
  Full-player foreground no
  longer samples artwork: title and transport controls are solid white, while
  artist, secondary actions, progress, and time labels are 80% white. The
  obsolete artwork-motion AAR, dependency, notice, and local-AAR documentation
  were removed. `MainActivity` now handles `uiMode` in place, preserving the
  expanded player and mini-player click path during system theme changes. Debug
  Kotlin compilation, 85 unit tests, Debug Lint, and Debug assembly passed. The
  merged Debug manifest retains `locale|layoutDirection|uiMode`. The final APK
  passed archive, v2 signature, and 16 KB ZIP
  alignment checks with SHA-256
  `1b9fae76aacc5e7c006ccc3910422c07bb58fc6ce59e09ed8b32a8f119e68c8d`.
  Per maintainer direction, no emulator, screenshot, or interaction test ran.
- On 2026-08-05, the playback artwork placeholder keeps its original
  `secondaryContainer` color but no longer draws a centered Music icon. The
  fixed `#242424` fallback applies only to the no-artwork playback backdrop.
- On 2026-08-05, full-player track changes keep the last completed HCT field
  visible while the replacement is computed. The background no longer reuses
  the cover's 320 ms progress; once the replacement is ready, it independently
  interpolates the current 8-by-8 field into the next field over 640 ms. Rapid
  track changes snapshot the in-flight interpolated field before continuing,
  and missing artwork remains the fixed `#242424` endpoint. The field cache now
  resets on a new artwork/theme key so an old field cannot be mistaken for a
  ready replacement. Debug Kotlin compilation, 89 unit tests, Debug Lint, Debug
  assembly, APK archive, v2 signature, and 16 KB alignment checks passed. The
  verified `artifacts/Melox-debug.apk` has SHA-256
  `2d4bfe5065b0d86ba49930957661c1e9c5b24610ea6a9e8781dd9a1b99d55967`.
  Per maintainer direction, no emulator, screenshot, or interaction test ran.
- On 2026-08-03, song rows moved their trailing duration to Miuix `footnote2`,
  artist parsing added `&`, and track actions gained one-line Album/Artist
  ellipsis plus single-artist direct navigation and a multi-artist Miuix sheet.
  Song information now uses separate identity and technical cards for title,
  artist, album, album artist, duration, format, file size, bitrate, sample
  rate, unavailable bit depth, and a `/storage/emulated/0/...` location.
  Mini-player metadata swipes trigger one system
  `GestureThresholdActivate` haptic after crossing the existing commit
  threshold for a different target, and floating mini players use the same
  official 10 dp shadow as the floating navigation pill. Debug Kotlin
  compilation, all Debug unit tests, Debug Lint, `git diff --check`, Debug
  assembly, and APK archive validation passed. Per the maintainer's request,
  no emulator or screenshot verification ran. The verified
  `artifacts/Melox-debug.apk` has SHA-256
  `447c7b64de871d3c9ea2d6d5565d0561ffaaf54f31f85d4b47c402fd34917146`.
- On 2026-08-04, Home recommendations were extended from a fixed five-card
  sample to a forward-swipe-growing carousel. It appends only previously unseen
  artwork tracks and ends after the eligible source is exhausted. A recommendation
  click now plays the selected track first, keeps the remaining loaded cards in
  carousel order, and lets only the remaining library use the current playback
  mode; switching modes restores mode ordering for the full queue. Recommendation
  cards are now 256 dp. When the recommendation carousel is past its first card,
  root pager user scrolling is disabled until it returns to page zero, preventing
  nested horizontal gestures from switching Home pages. Recently added sorts by
  file modification time, prioritizes
  IDs found newly during a successful scan, fills fewer than 20 additions with
  newer existing tracks, and retains every detected addition above 20. The
  localized recommendation heading is Random recommendations / 随机推荐; both
  Home section headings use `title4` at default weight with a 28 dp start inset.
  Recently added uses Lyrico's two-column square-cover Album-grid geometry with
  16 dp outer padding and 12 dp gaps, while its labels retain the Melox Albums
  three-column scales and positions; its current track title also stays
  `onSurface` rather than switching to primary blue. Debug Kotlin compilation, focused
  `UiLogicTest`, Debug Lint (0 errors), Debug assembly, and APK archive validation
  passed. API 37 UI-tree checks without screenshots or recordings confirmed two
  visible recommendations, the 256 dp card width, selected-track-first queue
  entry, the next loaded recommendation, a live process, and an empty crash
  buffer. The verified `artifacts/Melox-debug.apk` has SHA-256
  `8e741e9c28bf9128a799258214facb0ad278d035853a7e828235e9fe814f7258`.
- On 2026-08-04, Album and Artist detail fixed identity regions moved into the
  empty-title `SmallTopAppBar.bottomContent`; the Artist tab selector shares the
  same top-bar surface. Their lists now fill the page behind the bar and retain
  the original first-row placement through top content padding, so scrolled
  artwork and rows can feed the existing Miuix backdrop blur. Blur-disabled and
  unsupported devices keep the existing opaque fallback. Debug Kotlin
  compilation, complete Debug unit tests, Debug Lint, Debug assembly, and APK
  archive validation passed. Per maintainer direction, no emulator or
  screenshot verification ran. The verified `artifacts/Melox-debug.apk` has
  SHA-256
  `401537e3eec94026b3a3750177749873bf2b041367cfa77017c863cd1c3172f7`.
- On 2026-08-03, Albums and Artists reset their retained lazy-list anchor when
  the shared Library query changes. This keeps a newly filtered result set at
  its first matching result instead of preserving an apparent position in the
  unfiltered library.
- On 2026-08-03, the Queue current item uses Miuix's tertiary selection
  container with an 18 dp squircle, so its selected state remains visible in
  both light and dark themes. Queue artwork matches song-row sizing at 48 dp
  with 6 dp corners. Track action, song information, and participating-artist
  sheets use Miuix vertical overscroll; the artist sheet groups its Miuix rows
  inside the same secondary-container card treatment as More actions.
- On 2026-08-04, the queue sheet anchors its lazy list at the current item on
  open and uses the same one-line `headline2` title and `footnote1` artist
  hierarchy as song rows. Playback-mode queue moves are serialized until their
  expected order is visible, and randomization identifies repeated queue slots
  by stable source order instead of dropping duplicate tracks.
- On 2026-08-04, root lazy states are composed above the Navigation3 entry so
  Album and Artist positions survive detail navigation. Their screens no longer
  reset to item zero merely because the retained root entry is recomposed.
- On 2026-08-04, the More sheet no longer offsets content into Miuix's header,
  preventing its summary card from clipping. More and participating-artist
  content handle edge pulls with their own Miuix vertical overscroll.
- On 2026-08-04, queue options were grouped into one Miuix
  `secondaryContainer` card of full-row `BasicComponent` items. Their artwork
  was reduced from the song list's 48 dp to 44 dp, with 12 dp start/top/bottom
  padding; the rows retain only title, artist, and per-item removal. The
  participating-artist sheet now keeps 12 dp on every side of its artwork and
  omits the trailing navigation indicator without changing the artist library.
- On 2026-08-04, the queue sheet content height became count-driven within the
  official Miuix maximum. The current queue row now uses
  `BasicComponent.holdDownState` and Miuix's indication for its persistent
  selection effect; the custom light-blue rounded background was removed.
- On 2026-08-04, opening a large restored queue no longer multiplies every row
  into one unbounded fixed height. `QueueSheet` caps the item count against the
  actual remaining Miuix sheet height before multiplication, preserving compact
  short queues while long queues scroll internally and cannot exceed Compose's
  representable `Constraints` range. Debug Kotlin compilation, 64 unit tests,
  Lint, Debug assembly, ZIP/v2-signature/16 KB alignment checks, and an API 37
  no-image queue open passed. The queue actions appeared in the UI tree, the
  process stayed alive, and the crash buffer remained empty. The verified
  `artifacts/Melox-debug.apk` has SHA-256
  `cde69dedfcf2c5a9ab647b66bfca29720dc106fee0552be6d5d0ca536db5934c`.
- On 2026-08-04, playback-mode icons began switching directly without the shared
  Play/Pause fade-scale transition; Play/Pause animation remains unchanged.
- On 2026-08-04, Random-to-Order queue normalization stopped scanning and moving
  every Media3 item individually on the main thread. `PlaybackController` keeps
  the current media item untouched and restores only the ranges before and after
  it with at most two bulk replacements, preserving playback and position while
  preventing large queues from causing quadratic work and timeline-event storms.
  Debug Kotlin compilation, 66 unit tests including a 100,000-item queue and
  repeated-slot coverage, Lint, Debug assembly, ZIP/v2-signature/16 KB alignment
  checks passed. API 37 no-image verification completed Random-to-Order while
  the control remained Pause, the process stayed alive, and crash/ANR checks
  stayed empty. The verified `artifacts/Melox-debug.apk` has SHA-256
  `ad3c2aaafc9485fe026b69932d1061165e2fccf7dd058a7226f7c294dd2cb00c`.
- On 2026-08-04, queue bottom spacing moved outside the option card. The card now
  ends at the last row, while navigation-bar inset plus 12 dp remains below it,
  matching the More and participating-artist sheets without leaving an empty
  item-colored tail.
- On 2026-08-04, the More and participating-artist sheets restored Miuix nested
  scrolling, allowing a downward content drag to dismiss them like the queue
  sheet while retaining their explicit vertical overscroll effect.
- On 2026-08-04, Order and Repeat-one horizontally mirrored only their shared
  Miuix loop glyph, reversing the arrows from counterclockwise to clockwise while
  leaving the separate Repeat-one `1` badge unchanged.
- On 2026-08-03, the 16 KB/R8 release follow-up kept the artwork background free
  of bundled native libraries. Release R8 now repackages
  obfuscated classes into a short package, normalizes source-file names, limits
  packaged locales to English and Simplified Chinese, and keeps only
  `armeabi-v7a` plus `arm64-v8a`; Debug still retains emulator ABIs. The signed
  Release APK fell from the 10,766,578-byte baseline to 7,863,189 bytes
  (26.97% smaller). `artifacts/Melox-release.apk` is archive-valid, v2-signed,
  16 KB ZIP-aligned, and its arm64 native LOAD segments are 16 KB aligned. Its
  SHA-256 is
  `16d03ed2daa451b468ba61eeb193e4f3c501b7104fc8dd48022cc40b1dd94384`.
  The matching 64,387,511-byte mapping has SHA-256
  `aefdb1300effefd1b29b62fd440fdc4b32264f32951c1af5c1b17f8dcb9efe90`.
  Debug unit tests, Debug Lint, Release vital lint, Release assembly, APK
  signature/archive checks, and a fresh-signature cold start on the API 37
  16 KB Pixel 10 Pro image passed; the Melox process remained alive with an
  empty crash buffer and the Home UI tree was present.
- On 2026-08-04, the mini/full transition adopted VMusic's bounded shared
  progress structure: vertical distance updates progress directly, release
  direction selects the endpoint, and normalized release velocity feeds the
  existing critically damped `spring(dampingRatio = 1f, stiffness = 300f)`.
  Container center/height stay linear while width uses `EaseInCubic`; artwork
  uses uniform scale with `EaseOutCubic` horizontal and `EaseOut` vertical
  movement, so it grows rightward/upward without rotation, skew, perspective,
  or deformation. Recorded mini/full `GraphicsLayer` content completes its
  early handoff at progress 0.4. The recorded mini layer still uses Melox's
  original Miuix blur/liquid-glass/highlight/fallback parameters. `420 dp`
  remains only the full-artwork decode bucket, never a duration. Debug Kotlin
  compilation, 58 unit tests, Lint, Debug assembly, ZIP/v2-signature/16 KB
  alignment checks, and API 37 no-image endpoint interactions passed. Tap,
  downward drag, upward drag, and Back reached the expected endpoints; the
  process stayed alive and the crash buffer remained empty. Per maintainer
  direction, no screenshot or recording was used. The verified
  `artifacts/Melox-debug.apk` has SHA-256
  `12e992c3579692ce21491dc1e3d7329cba39c52f1ad3faf81c6b937fc39da056`.
- On 2026-08-04, the full-player artwork now reports its current visible
  center-scaled bound to the shared transition instead of leaving the target at
  the unscaled layout bound. Opening and dismissing therefore reach the 90%
  paused cover, the 100% playing cover, and the existing 102% resume peak
  without changing the pause/resume animation or the VMusic container/layer
  structure. Melox's mini-player Miuix blur, liquid-glass, highlight, and
  fallback parameters remain unchanged. The focused regression test, complete
  Debug unit-test task, Debug Lint, Debug assembly, ZIP/v2-signature/16 KB
  alignment checks passed. Per maintainer direction, no emulator, screenshot,
  or recording verification ran. The verified `artifacts/Melox-debug.apk` has
  SHA-256
  `8e741e9c28bf9128a799258214facb0ad278d035853a7e828235e9fe814f7258`.
- On 2026-08-04, the shared artwork overlay began interpolating the fitted
  bitmap rectangle for non-square covers instead of scaling the surrounding
  square frame as the image geometry. The full player reports the actual
  centered `Image` layout bound, and the overlay uses that target as its native
  layer size so progress 1 has scale 1 and zero translation. This prevents a
  rectangular cover from shrinking toward the upper-right after the shared
  handoff. Focused fitted-rectangle tests, Kotlin compilation, Debug Lint,
  Debug assembly, ZIP/v2-signature/16 KB alignment checks passed. The complete
  Debug unit-test task still has one unrelated existing queue assertion failure
  in `stagedPlaybackValidationPreservesTheActiveQueueSlotAndPosition`. Per
  maintainer direction, no emulator, screenshot, or recording verification
  ran. The verified `artifacts/Melox-debug.apk` has SHA-256
  `097a6bfa30c5761d9182ebadfe71fa90b60cd8c265804cb83a810d7cff54c64b`.
- On 2026-08-04, the playback queue sheet changed from one shared
  `secondaryContainer` card to direct full-width Miuix rows. Queue artwork now
  uses 24 dp start, 16 dp end, and 12 dp top/bottom padding, while the header
  Delete glyph ends 28 dp from the screen edge. Its lazy viewport now extends
  through the transparent navigation-bar region and keeps the safe bottom inset
  as scrollable content, removing the separate blocking footer appearance.
  The row remove glyph now shares the header Delete icon's 28 dp visual right
  edge. The Add-to-queue action icon moved 1 dp left without changing its footprint.
  Debug Kotlin compilation, complete Debug unit tests, Lint, Debug assembly, and
  APK archive validation passed without emulator, screenshot, or recording use.
  The verified `artifacts/Melox-debug.apk` has SHA-256
  `04679030636050e1d8e83fcd2b8b21b6b466f74b2f349c02a42c3d8972ae7abe`.
- On 2026-08-05, the shared artwork path was tuned to match the requested VMusic
  visual trajectory: horizontal center movement remains front-loaded with
  `EaseOutCubic`. Vertical center movement is one continuous blend of 35%
  linear progress and 65% `EaseInCubic`; the linear contribution gives the
  curve a non-zero initial upward velocity, while the cubic contribution makes
  the latter half vertically dominant. This is not a two-stage or six-stage
  animation. Source and target coordinates still come from Melox's measured
  mini/full artwork bounds, so VMusic's cover height and bottom-bar location are
  not copied. The cover moves right and up from the first frame, remains
  uniformly scaled, and reaches the measured full-player endpoint exactly;
  rectangular-cover fitting, Miuix blur/liquid-glass parameters, and the
  no-rotation/no-skew constraint are unchanged. The staged-axis trajectory
  regression, complete Debug unit tests, Debug Lint, and Debug assembly passed.
  Per maintainer direction, no emulator, screenshot, or recording verification
  ran. The verified `artifacts/Melox-debug.apk` has SHA-256
  `33da2df2533b117639a0101b147b83552c70bd57deb280a446c524620c5f27f4`.
- On 2026-08-05, the shared-player background corner phase was hardened against
  interrupted or reversed transitions. Any stored final corner-expansion value
  is ignored while the main container progress is below 1, cleared when main
  geometry motion starts, and reset after closing. The physical screen corner
  can therefore affect only the fully expanded endpoint before the final
  straight-corner handoff, preventing partial stale values from producing
  apparently random corners during opening. Physical screen corners are now
  used only when the Activity occupies the display's maximum window bounds and
  is outside multi-window and picture-in-picture modes. Split screen, freeform
  small windows, picture-in-picture, and other undersized window scenes use a
  `0.dp` expanded target radius. Miuix still supplies the physical radius and
  squircle clipping for eligible full-screen windows. Focused corner/window
  tests, complete Debug unit tests, Debug Lint, Debug assembly, and APK archive
  validation passed without emulator screenshots or recordings. The verified
  `artifacts/Melox-debug.apk` has SHA-256
  `5d9a1ea4cd5391ff5a3b02127dcf7005e135d6ca1e7532ff2ec68a14c0f3633b`.

- On 2026-08-05, Songs-page searches now keep the complete current sorted Songs
  list as the playback queue for both single-result and multi-result searches,
  while seeking to the selected track's matching index. The active playback
  mode still orders that queue through the playback controller. The pure
  selection helper is covered by unit tests for both branches;
  Debug unit tests, Debug Lint, Debug assembly, and APK archive validation
  passed without emulator, screenshot, or recording verification. The verified
  `artifacts/Melox-debug.apk` has SHA-256
  `8e79922197ac596b01fb4aad6c08953246da79c1b37e078c30374b79a7e903f8`.
- On 2026-08-05, mini-player horizontal metadata swipes replaced the single
  per-drag haptic flag with an active commit-direction state. Entering an
  eligible Next or Previous threshold triggers
  `GestureThresholdActivate`; staying inside the same threshold does not repeat
  it, while returning below the threshold or reversing into the other direction
  rearms the haptic. The focused threshold-direction regression, Debug Kotlin
  compilation, complete Debug unit tests, Debug Lint, forced Debug assembly,
  ZIP/v2-signature/16 KB alignment checks passed. Per maintainer direction, no
  emulator, screenshot, or recording verification ran. The verified
  `artifacts/Melox-debug.apk` has SHA-256
  `8e79922197ac596b01fb4aad6c08953246da79c1b37e078c30374b79a7e903f8`.

- On 2026-08-05, Songs-page playback was corrected for multi-result searches:
  any non-empty Songs search now uses the complete current sorted Songs list as
  the queue, while the clicked track remains the starting item. The existing
  playback controller then applies Order, Repeat-one, or Random queue ordering
  to that complete list. The focused selection regression, Debug unit tests,
  Debug Lint, Debug assembly, and APK archive validation passed without
  emulator, screenshot, or recording verification. The verified
  `artifacts/Melox-debug.apk` has SHA-256
  `eea7da0326b258e9582994b8f0235400c7ed719a968b83dcfe921f58d8248b28`.
- On 2026-08-05, the shared Miuix search wrapper observes the IME visibility
  transition. A focused field now clears focus when the already-visible
  keyboard is dismissed by Back, so one Back press both hides the keyboard and
  cancels focus while retaining the query. The focused search-state regression,
  Debug Kotlin compilation, complete Debug unit tests, Debug Lint, Debug
  assembly, and APK archive validation passed without emulator, screenshot, or
  recording verification. The verified `artifacts/Melox-debug.apk` has SHA-256
  `78bc4840adace89dbcc2874fc8e49b37587786334903fae504154f6bc02d4bcc`.
- On 2026-08-05, all Miuix search fields now use 16 dp horizontal outer spacing
  and the generic Search / 搜索 placeholder. The focused search-state regression,
  Debug Kotlin compilation, complete Debug unit tests, Debug Lint, forced Debug
  assembly, and ZIP/v2-signature/16 KB alignment checks passed without emulator,
  screenshot, or recording verification. The verified
  `artifacts/Melox-debug.apk` has SHA-256
  `c9a0a480df334b080974287ac2a8fc9e9587656e0f06870f0a741a79a2815ec4`.
- On 2026-08-05, the closing shared-player host stops intercepting the mini
  player before the critically damped spring's invisible tail finishes. Once
  the recorded mini layer reaches 80% opacity, the full-screen host moves
  behind the root page and disables its Back/down-drag handlers; a vertical
  drag that starts on the mini player retains ownership for the entire gesture.
  Click and upward-drag therefore work as soon as the restored bar is visibly
  available. The artwork vertical path remains one continuous curve but now
  blends 40% linear progress with 60% `EaseInCubic`, slightly increasing upward
  travel during the first 35% so the growing cover's bottom edge also rises
  immediately. Miuix blur, glass, corner, and shared-layer rendering remain
  unchanged. Focused interaction/trajectory tests, complete Debug unit tests,
  Debug Lint, Debug assembly, and APK archive validation passed without emulator
  screenshots or recordings. The verified `artifacts/Melox-debug.apk` has
  SHA-256
  `d3b7f87dbb548e6d753e8d967be6d47299481e107f6ab8b294cdebde94b56a03`.
- On 2026-08-05, the full-player host returned to on-demand mounting after
  constant composition was found too expensive: hidden full-player progress
  recomposition and GraphicsLayer recording are no longer retained while the
  mini player is idle. The existing current/adjacent artwork prefetch now also
  warms the small 8-by-8 HCT field through a weak bitmap-keyed cache, preserving
  the expansion path without keeping the full page resident. Status-bar icons
  now follow the rendered playback background's top-row luminance in real time;
  only the shared handoff selects the playback status-bar mode, and animation or
  artwork-field changes can switch between light and dark icons. Navigation-bar
  behavior is unchanged. Debug unit tests, Debug Lint, Debug assembly, APK
  archive validation, and `git diff --check` passed without emulator screenshots
  or recordings. The verified `artifacts/Melox-debug.apk` has SHA-256
  `b0c9cb8c5a6b6460a126d702b49b787fc862d0c9590f43cbc134ba5cd319428e`.
