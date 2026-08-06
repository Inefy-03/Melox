# Melox Version 1 Product Requirements

## Product Goal

Melox lets Android users find and play music already stored on their device through a native Miuix interface. It is private by design, works without an account or network connection, and continues playback outside the Activity.

## Audience

- Android 9+ users with a local audio library.
- Simplified Chinese and English speakers.
- Users who expect system media controls, reliable background playback, and MIUI/HyperOS-aligned interaction.

## Version 1 Experience

### Music library

- On entry, ask for only the platform audio permission required for the current
  Android version when it is missing. Granting this startup request does not
  start a scan.
- The Settings `Scan local music` preference starts `MediaStore.Audio`
  scanning. If permission is missing, it requests permission in place and
  scans immediately after that manual request succeeds; denial only dismisses
  the system dialog and never redirects to app settings.
- Cache the last successful result and keep cached rows visible during refresh.
- The four root destinations are Home, Songs, Library, and Settings. Home is
  labeled `首页` in Simplified Chinese and is always the first navigation item
  and first root page.
- Home follows the Flamingo-style information hierarchy with a Miuix large
  title and a horizontally paged Random recommendations section built from local
  tracks that have real artwork. Each application opening chooses a fresh
  random set while retaining that set during the session. It prioritizes and
  publishes the first two artwork cards before continuing in the background to
  its initial five cards, then appends one previously unseen card after every forward
  swipe. When no further local tracks with artwork remain, the carousel ends
  without repeating a song. The horizontal list alone keeps 16 dp start/end
  screen padding;
  fixed-width 256 dp recommendation cards retain carousel spacing rather than
  receiving their own 16 dp margins. A recommendation tap starts the selected
  song, follows it with the other distinct loaded recommendation cards in their
  carousel order, and then appends the remaining scanned songs in the current
  playback-mode order. A later playback-mode change reorders the complete queue
  for that mode and does not retain the recommendation prefix.
  Once the carousel has moved past its first card, its horizontal gesture owns
  the drag until it returns to the first card, so the root Home pager does not
  steal a continued recommendation swipe.
  The metadata area has enough height for its white title and semi-transparent
  white artist without clipping. Both Home section headers use `title4` at the
  default weight with a 28 dp left inset. Recommendation seed, resolved track IDs, and
  completion state belong to the retained root UI rather than the disposable
  Home page, so switching away and back cannot reload or rerandomize the
  section. Artwork eligibility resolves in bounded batches to reduce the first
  wait without scanning the whole library concurrently.
- Home places a Recently added two-column grid below recommendations with the
  borderless Albums three-column presentation: a rounded square cover above its
  labels, 16 dp page padding, and 12 dp grid spacing. Both title and artist
  retain matching horizontal text insets.
  Its title and artist use the Melox Albums three-column label placement,
  compact text scale, and default weight. Without scan-detected
  additions it shows the 20 newest files by modification time, descending. A
  successful scan that detects new songs puts all of those songs first; when
  fewer than 20 were detected, the remaining newest non-new songs fill the
  list, while more than 20 detected additions are all shown.
- Music library uses one Miuix `TabRow` for Albums, Artists, and Folders. Its
  default outlined-tab visual is retained while its layout uses 20 dp side
  padding, 12 dp item spacing, 38 dp height, and 13 sp text. When Blur is
  active, the tab row itself is transparent so the owning top bar's blur remains
  visible; opaque fallback keeps the normal surface. The Search action reveals
  one field below the tab row with 6 dp below the tabs and 6 dp above Search.
  Switching tabs while Search is open keeps the field visible and applies the
  same query to the selected tab's results while switching its hint and sort
  action. Returning to a page whose search
  field is already open must not focus it or open the software keyboard
  automatically. Albums, Artists, and Folders switch only through the `TabRow`;
  horizontal swipes remain reserved for the root pager. Tapping a tab changes
  the selected state directly instead of animating through intermediate tabs.
  When a focused search field receives Back, the first press hides the software
  keyboard and clears focus together; the query remains available in the open
  field.
- Albums, Artists, and Folders share one stationary alphabet-index overlay.
  Switching the `TabRow` moves only page content and updates that overlay's
  sections and target list without creating a second index.
- Music-library tab selection, Songs and per-library-tab sorting direction and
  field, and the Albums 2/3-column style survive an application restart. Album,
  Artist, and Folder content shares one large-title collapse state so switching
  tabs never changes the top-bar expansion unexpectedly.
- Album, Artist, and Folder search matches only the current page item's primary
  title: album name, artist name, or folder name. Album artists, counts, paths,
  and every field belonging to songs inside those groups are excluded.
- Personalization lets the user choose Home, Songs, or Library as the default
  root destination for the next application start. Changing this preference
  does not unexpectedly navigate away from the current Settings page. Startup
  creates the root pager directly at the persisted destination without first
  drawing or moving away from Home.
- Read title, artist, album artist, album, year, track number, and disc number
  from each file's TagLib property map, using MediaStore only when the
  corresponding embedded tag is absent. Persist these fields for grouping,
  sorting, display, and later metadata features.
- Show title, artist, album, album artist, year, duration, and cached artwork.
- Album detail shows the album year above the song count only when a positive
  year is available. Year and song count use 12 sp theme foreground text.
- Prefix each song-row description with a compact quality badge when the
  available local metadata supports one. `HR` means a lossless high-resolution
  source (at least 88.2 kHz or 1,500 kbps), `SQ` means another recognized
  lossless source, and `HQ` means a lossy source at 256 kbps or higher. Read
  bitrate, sample rate, and channel count from the audio file itself using the
  Lyrico-style file-descriptor/TagLib path. Lower, unreadable, or indeterminate
  quality has no badge rather than an inaccurate label. HR, SQ, and HQ use
  gold, purple, and blue badge families respectively. HR uses the same bright
  gold in light and dark modes.
- Search title, artist, album, and file name case-insensitively.
- Sort by title, date added, file name, file size, or duration in either direction.
- Show the song index for title or file-name order in either direction with no
  active search; descending order reverses the section rail to match the list.
  Its top stays 4 dp below the expanded large-title bar and its bottom
  stays 12 dp above normal navigation or 6 dp above floating navigation. Hiding
  root navigation must not move the index relative to the retained mini-player
  strip, and its cell height adapts to the remaining space.
- Dragging into the index top selects section `0` while retaining the collapsed
  small title. Only tapping the separate scroll-top action restores the list
  origin and large title.
- A Search action immediately precedes Sort in each library top bar. It reveals
  the Miuix SearchBar below the title with the same vertical expand/fade motion
  as dependent theme settings and leaves 6 dp below the field. Every search
  field has 16 dp of outer horizontal spacing and uses the generic Search / 搜索
  hint.
- Albums expose 2-column and 3-column grid styles and sort by album name,
  album artist, song count, or year. The 2-column style uses a rounded
  horizontal card with a square cover at the left and two text lines at the
  right. The 3-column style has no outer card: its rounded square cover, album
  name, and localized song count sit directly on the page background. Artists
  sort by name, song count, or album count. Both support search, descending
  order, and a right-side index. The Album grid uses 20 dp page margins and
  12 dp horizontal and vertical spacing. The 3-column Album grid uses 14 dp
  cover corners, a 14 sp `body2` title, a 12 sp song count, and a
  6 dp text inset shared by the title and song-count start. The 2-column style
  uses a 56 dp cover with 6 dp top/bottom/start card padding, 12 dp end
  padding, and a 10 dp cover-to-label gap. Both Album grid styles use the same
  compact title and song-count text. Songs with the same normalized album
  name and album-artist tag belong to one album even when MediaStore assigns
  different album IDs or their track artists differ.
  The Album sort popup uses Miuix dropdown positioning, measures the complete
  option column so `Descending` is visible without initial scrolling, and
  flips above the action when the lower viewport cannot show all options.
- Folders group tracks by their direct parent directory. Each row follows the
  song-row information hierarchy, uses the folder name as its title, and shows
  a localized song count followed by the shared-storage-relative path. For
  example, `/storage/emulated/0/Music/` is displayed as `/Music`.
- Song rows use 16 sp `headline2` titles and 13 sp `footnote1` descriptions.
  Artist rows use the same title and description sizes, with a 16 dp leading
  inset and a 12 dp cover-to-text gap. Folder rows use 16 sp
  `body1` titles and 12 sp descriptions, with a 32 dp file glyph,
  30 dp left padding, and a 20 dp glyph-to-text gap.
- Folders support search plus name/song-count sorting in either direction.
  Opening a folder shows only tracks directly in that folder and reuses the
  Songs page's rows, search, sorting, index, queue creation, and track actions.
- Album and artist detail pages use 16 dp horizontal margins and the same song
  rows and actions as the Songs page. Album artwork, album name, album artist,
  and song count stay fixed above the album song list. A missing album-artist
  tag displays `Unknown album artist`; it never falls back to a track artist.
  Artist summaries use the
  localized `album count - song count` order on both root and detail pages.
  Album and artist detail top bars keep an empty small title because their
  fixed metadata already shows identity. The artwork and metadata are hosted
  in the top bar's Miuix `bottomContent`, and the artist tab selector joins that
  same fixed blurred or opaque-fallback surface. Album-detail song rows omit the
  current album suffix, while artist-detail song rows omit the current artist
  prefix. The artist-detail album tab uses the same 2-column or 3-column Album
  grid style selected for the root Albums page.
- Artist names split on `，`, `,`, `、`, `/`, and `&` into separate artist entries.
  Song rows and album headers display split artist names joined with ` / `.
- Leaving Albums, Artists, or Folders for a detail page and returning restores
  the exact root list position and top-bar collapse state.
- Root search/sort presentation lists are prepared away from composition and
  stay warm across page changes. Playback position ticks do not invalidate the
  library pager.
- Home and Songs show `No music found` before the first scan and after a
  successful zero-result scan. They do not expose a scan action; scanning stays
  in Settings.

### Playback

- Tapping a row starts playback at that row using the current page's playback
  list.
  When a non-empty Songs-page search leaves exactly one visible result, the
  selected track still starts from its position in the complete current Songs
  page list, rather than creating a one-track queue. Searches with multiple
  results use the same complete Songs page queue while still starting at the
  clicked result. The active playback mode then applies its normal order,
  repeat-one, or pseudo-random queue ordering to that complete list.
- Continue through the queue in order and highlight the current local track.
- Support play/pause, previous, next, seek, order/repeat-one/pseudo-random
  playback, play next, append, remove, jump, and clear.
- Mini/full playback controls use the supplied rounded SVG artwork for Play,
  Pause, Previous track, and Next track. Full-player Play/Pause is 40 dp inside
  a 64 by 64 dp target, while Previous/Next is 32 dp inside 56 by 56 dp targets.
  Mini-player Play/Pause is 20 dp inside a 40 by 40 dp normal-bar target or a
  36 by 36 dp floating/liquid target, with the whole target shifted 6 dp left.
  Existing tint behavior, accessibility labels, and transitions remain intact.
- Playback mode cycles Order -> Repeat one -> Random. Order and Random both wrap
  after the last item; Random physically shuffles the queue once and then walks
  that displayed order. Changing modes must not rebuild or prepare the current
  audio item or interrupt normal playback. Queue normalization keeps that item
  in place and replaces only the ranges before and after it with bounded bulk
  operations, so large Random-to-Order transitions do not block the app. Starting
  a new visible queue keeps the current playback mode. Order and Repeat-one mirror the shared Miuix
  loop glyph horizontally so its arrows read clockwise; the separate Repeat-one
  `1` badge remains unmirrored.
  Previous and Next always switch directly to adjacent queue items.
- Continue in the background and expose metadata and controls to Android.
- Restore the complete queue and position after leaving and reopening the app
  or after process restart, without autoplay.
- Accept one external `audio/*` URI through Open with or Share.
- A horizontal swipe in the full-player region between the identity header and
  progress bar reveals lyrics from the right. Lyrics remain clipped to that
  region, use top and bottom fade masks, highlight the current timestamped
  line, and follow playback time automatically.
- Read timestamped LRC and TTML from an embedded audio tag when present, then
  fall back to same-name external `.lrc` or `.ttml` files beside the audio.
  Missing, unreadable, or untimed lyrics do not interrupt playback.

### UI

- Keep the UixPlayer library design.
- The trailing More icon in every song row keeps its 36 dp button hit area but
  has no pressed overlay. Its action sheet has no leading Close action and
  retains the selected song until the Miuix bottom-sheet exit animation fully
  finishes. The untitled sheet uses the standard Miuix side margins: a song-row
  summary card with the same option background and 56 dp artwork has 12 dp
  left/top/bottom artwork-side padding and is followed by one preference-style
  action card for Play next, Add to queue, Album, Artist, and Song information.
  Immediately before Song information, Edit with Music Tag Editor opens the
  selected audio URI in `com.xjcheng.musictageditor`, and Edit with Lyrico
  opens it in `com.lonx.lyrico`; both actions use the Miuix Edit icon.
  Add to queue uses a 20 dp icon shifted 1 dp right with 2 dp more text gap.
  Album and Artist action labels clamp to one line with an ellipsis. A single
  artist opens its matching library detail page; multiple artists open a
  separate titled `OverlayBottomSheet` whose rows reuse the artist-library list
  presentation, keep 12 dp around the artwork, omit the trailing navigation
  indicator, and open the selected artist detail. Song information opens a
  separate titled `OverlayBottomSheet` with its own leading Close action. It
  groups Title, Artist, Album, and Album artist separately from Duration,
  Format, File size, Bitrate, Sample rate, Bit depth, and file location. Fields
  unavailable from local metadata show the localized unavailable value. The
  complete information row is clickable and copies the exact value displayed
  at its trailing edge. Returning from either external editor re-reads that
  track's tags, audio properties, and lyrics without rebuilding, preparing, or
  interrupting the active playback queue.
  The summary begins directly below the standard Miuix header without clipping, and
  the More and participating-artists lists use Miuix vertical overscroll while
  retaining the standard content-drag downward sheet dismissal.
- The playback queue sheet sizes itself from the number of queue songs while
  remaining within the Miuix bottom-sheet maximum below the status bar. The
  count is capped before row-height multiplication so a large restored queue
  cannot create a Compose constraint outside the representable range. Its
  title is centered in the official sheet header, Close is a leading action,
  and Clear is a trailing Delete icon whose visible glyph ends 28 dp from the
  screen edge. Queue options are direct full-width Miuix rows with no wrapping
  item/card. It opens at the current queue item. Rows use 44 dp artwork with
  24 dp start, 16 dp end, and 12 dp top/bottom padding; title and artist
  use the same `headline2` / `footnote1` hierarchy and one-line ellipsis
  behavior as song rows. The trailing action only removes that queue item; its
  circle-minus glyph shares the Clear icon's 28 dp visual right edge.
  quality badges, duration, and More are omitted. The current row uses
  `BasicComponent`'s persistent `holdDownState` Miuix indication rather than a
  custom colored rounded background. The list viewport extends behind the
  transparent navigation bar; navigation-bar inset plus 12 dp is supplied as
  scrollable list content padding instead of a separate blocking footer. Clear opens a Miuix OverlayDialog
  with separate title, question, Cancel, and Confirm actions. Removing a queue
  item uses a circle minus icon derived from AddCircle.
- Always show a mini player. Before playback it shows placeholder artwork,
  `Melox`, and a localized no-music message and cannot open the full player.
  It has no progress track. Normal navigation keeps the existing rounded
  rectangle; floating navigation uses an iOS-like pill with the same
  blur/liquid-glass/opaque fallback decision as the navigation bar.
- Horizontal metadata swipes expose Previous/Next beneath edge fades and switch
  tracks on release. Next slides from beneath the trailing-control mask during
  a left swipe; Previous slides from beneath the artwork mask during a right
  swipe. Each label stays 12 sp behind the translated metadata region. The
  system threshold haptic fires whenever the drag enters an eligible Previous
  or Next commit region for a different track. Leaving that region rearms the
  same direction, and reversing into the opposite commit region triggers its
  own haptic. Tapping
  or swiping upward opens the full player; tapping the trailing queue icon opens
  the queue.
- Mini and full player stay outside secondary-page navigation while sharing one
  reversible expansion progress. The root page remains composed behind the
  full-player overlay. Taps and accepted vertical releases animate to the
  corresponding endpoint, and Back or a qualifying downward release reverses
  the same path.
- Mini and full player keep independent content layouts and artwork requests.
  Their rendered layers are handed off inside one squircle container that
  expands between their measured screen bounds, while one artwork overlay
  travels between the measured cover bounds. The cover grows, moves right,
  and moves upward into the full-player artwork; it keeps a uniform scale and
  rounded rectangle with no rotation, skew, or narrow-top/wide-bottom
  deformation. Bar content fades out as page content fades in on the same
  reversible progress.
- During shared expansion, the recorded mini-player layer contains only bar
  content. The shared container redraws the existing Miuix blur or liquid-glass
  surface at its current interpolated size using the same backdrop, highlight,
  dark/light, outline, and opaque-fallback parameters. Content handoff reaches
  the full-player layer at `p = 0.25`, while the glass surface remains active
  until the shared container is fully expanded, matching VMusic's
  `isFullyExpanded` boundary. The VMusic backdrop implementation and its fixed
  blur/refraction values are not migrated. The floating highlight and
  navigation-matched shadow fade from full strength at expansion start to zero
  at the content handoff. One observable progress drives both pointer movement
  and spring settlement, so the shared cover cannot remain at the release frame
  while the container continues. The shared cover stays fully visible during
  vertical dragging on the artwork page. Lyrics has no shared cover element:
  once the artwork page is horizontally offscreen, no independent cover fade or
  return path is drawn. During close, the mini-player's own cover remains in the
  recorded bar layer with its title and buttons. That recorded layer keeps the
  original local positions relative to the mini-player's top edge instead of
  scaling those elements with the expanding container; all three fade back and
  settle together.
- The full-player background reuses the cached artwork bitmap and rebuilds it as
  an 8-by-8 HCT color field off the main thread. Every pixel retains its hue,
  caps realized chroma at 32, and uses tone 64 in light theme or 32 in dark
  theme. The center 4-by-4 pixels seed one VMusic-style background bitmap. Its
  four 2-by-2 output quadrants then interpolate through matching pixel positions
  in the center, horizontal side, outer corner, and vertical side regions:
  top-left and bottom-right move clockwise with 24-second then 18-second laps,
  while top-right and bottom-left move counterclockwise with 18-second then
  24-second laps. Independently, the complete 4-by-4 color grid rotates
  clockwise through quarter-turn pixel mappings every 18 seconds. Each endpoint
  rotates the local pixel coordinates with the grid, and adjacent quarter-turn
  states interpolate continuously, preventing a cross-shaped center boundary.
  The output uses the VMusic rendering path: one remembered `BitmapPainter`
  with `FilterQuality.Low`, displayed by `Image` with `ContentScale.Crop`.
  Compose/Skia bilinear sampling and continuous ARGB interpolation blend
  adjacent samples without hard boundaries. The bitmap stays fixed and has no
  geometric rotation, animated scale, translation, second layer, or perspective
  deformation. Animation runs only while the settled full player
  is drawing, the lifecycle is resumed, and the current song is playing. Pause
  preserves both animation phases until playback resumes. Missing or invalid
  artwork falls back to fixed `#242424` in both themes.
- Provide a full player with a safe artwork-derived color field. Song title and
  artist are stacked at the top without a visible back button or "Now
  playing" label. The slightly smaller, less-rounded artwork has a restrained
  shadow that fades in only after shared expansion has finished and is centered
  between that identity and the lower Miuix progress indicator. Playing uses
  100% of its layout bound and pausing reduces the visible artwork to 90%.
  Pausing shrinks with a fast-to-slow non-linear curve. Resuming briefly grows
  the artwork to 102%, then rebounds to 100%; both resume segments use the same
  fast-to-slow curve. The indicator's idle width follows the playing artwork size. It
  supports tap-to-seek and drag-to-seek and scales uniformly
  in both axes while pressed, preserving its foreground progress and rounded
  ends. Its normal width matches the artwork and the time labels sit slightly
  closer to it. Previous,
  play/pause, and next form the first control row; playback mode, queue, and
  track actions form the second row with one consistent icon size. Previous,
  play/pause, and next retain their intentionally distinct primary sizes.
  Song title, Previous, Play/Pause, and Next use solid white in both themes.
  Artist, remaining actions, progress, and time labels use 80% white; none of
  these foreground colors are derived from artwork.
- Once expansion completes, the atmosphere also paints the pixels outside the
  physical screen's large corner radius so devices with very rounded displays
  never expose black wedges.
- While playback is intended to continue, seeking must retain the Pause icon
  through Media3's temporary buffering state. In dark mode the player text,
  lyrics, progress, timestamps, and controls stay white. In light mode the
  header, lyrics, progress, timestamps, and controls use a readable accent
  extracted from the current artwork. Song title, current lyric, progress, and
  previous/play-next controls use a deeper tier of that accent; artist uses the
  slightly softened deep tier, while inactive lyrics, timestamps, and secondary
  controls use the normal tier.
- Switching Play/Pause icons keeps the existing simultaneous old-icon
  shrink/fade and new-icon grow/fade motion. Playback-mode icons switch directly
  with no transition animation. Mini-player Play/Pause uses the same Play/Pause
  icon transition as the full player.
- The pressed progress indicator is scaled as one complete Miuix component.
  Foreground, background, progress fraction, width, height, and rounded ends
  keep the same proportions; no individual axis or internal track is distorted.
- The artwork/lyrics pager accepts a horizontal gesture from the full screen
  edges rather than only from the artwork bounds. Changing tracks while Lyrics
  is selected keeps Lyrics selected and updates only the track-dependent
  background, metadata, and lyric document. Lyrics match the progress and
  playing-artwork width. Primary lyric text is slightly larger, and additional
  same-timestamp translation lines render at 80% of their primary lyric size.
- A host that remains composed in both mini and full-player states warms the
  current, previous, and next artwork through the shared bounded cache.
  Switching songs retains the previous rendered cover and atmosphere until the
  next bitmap is ready; no placeholder frame appears between them.
- While seeking, the Miuix progress indicator is remeasured rather than
  vertically stretched: its width grows only slightly and its height grows to
  roughly twice the idle height while preserving the foreground fraction,
  background track, and rounded ends.
- The alphabet index's idle section letters use the same Miuix action color as
  the scroll-to-top icon. During a drag, the selected section letter changes to
  solid black or white according to surface luminance while the floating
  selection indicator uses a semi-transparent Miuix gray surface and Miuix
  primary blue text/icon. Its top stays 4 dp below one captured expanded
  large-title bar position
  and does not react to title collapse; only SearchBar expansion or removal
  moves it vertically. Music-library TabRow and SearchBar height are counted
  exactly once, including the initial layout frame. The scroll-to-top icon is
  hidden while the large title
  is expanded and fades in/out as that state changes.
- Root-page switching retains the Miuix pager motion through intermediate
  destinations and reuses immutable library projections so page creation and
  repeated grouping do not block the transition. An adjacent destination stays
  ready when Blur is off; with Blur on, the pager avoids recording an extra
  full-screen blurred page.
- In normal navigation, the mini player shares the navigation bar's surface and
  blur treatment, uses a subdued matching outline without a glass highlight,
  keeps 6 dp side margins and a 6 dp navigation gap, and gives its artwork
  10 dp top/bottom/start padding. Its artwork-side metadata mask starts 4 dp
  farther left without moving the resting title or artist. The navigation
  divider is visually reduced.
- In floating navigation, the mini player matches the navigation pill's width,
  64 dp height, blur/lens treatment, and gravity-following highlight with an
  8 dp gap. Its final blurred layer is clipped to the same larger pill corner
  radius. The navigation pill and mini player share one Miuix-style 10 dp black
  drop-shadow implementation (20% dark / 10% light). The mini artwork is
  44 dp, remains shifted inward,
  and the two trailing controls use matching compact geometry with reduced
  spacing.
- Entering a secondary or tertiary page slides only the destination navigation
  bar down. The mini player remains visible above page content and the
  navigation bar rises back when the user returns to a root destination. Its
  blur/background capture refreshes throughout page transitions and must not
  leave stale black or white blocks inside the destination page. Secondary and
  tertiary scene backgrounds continue to the screen bottom behind the retained
  mini player and system-bar inset, while their interactive content consumes
  the live mini-player bottom inset. The mini-player blur therefore samples the
  current secondary or tertiary page during the whole route transition instead
  of a retained root page or an empty theme surface.
- The first application frame draws the top bar, root page skeleton, persisted
  normal/floating/liquid-glass navigation style, and the last cached mini-player
  identity plus a bounded 16-item queue window. The service preloads that same
  window before exposing its media session and starts decoding the complete
  checksummed queue. The decoded
  queue is published before slower per-file readability validation, so opening
  Queue immediately after launch shows at least the first visible page instead
  of only the current item. Final validation remains asynchronous and removes
  unreadable items without replacing queue changes made by the user. One bounded
  settings read resolves the navigation style before composition; MediaStore,
  projections, artwork extraction, and the full library snapshot remain
  asynchronous.
- Keep normal screens visually restrained. If artwork or shader support is unavailable, use Miuix theme surfaces without losing information.

### Settings

- Main settings: Language, default Home page, Theme settings, Scan local music,
  About.
- Theme settings: System/Light/Dark, blur, floating bottom bar, liquid glass,
  predictive back, and dynamic colors at the end of the Appearance group.
  Dynamic colors default to the complete system desktop-wallpaper Monet palette.
  When enabled, a dependent Miuix dropdown titled `Color source` / `颜色来源`
  lets the user switch between `Desktop wallpaper` / `桌面壁纸` and
  `Playing song artwork` / `播放歌曲封面`; the artwork choice falls back to the
  desktop-wallpaper color until a cover is available.
- System light/dark changes update the active composition in place. An expanded
  player remains expanded, the mini-player remains immediately clickable, and
  the artwork field reuses its sampled source pixels so only the HCT tone target
  changes.
- Liquid glass appears only with floating navigation and is disabled with an explanation when unsupported.
  Enabling floating navigation turns liquid glass on by default; disabling
  floating navigation turns it off.
- The liquid-glass preference enters and exits with the official Miuix
  dependent-preference `AnimatedVisibility` motion when floating navigation is
  toggled.
- Blur and predictive back are enabled by default on fresh installs.
- Search uses one Back press to hide the IME and clear focus while retaining the
  open query, and a second Back press to close the search field. Losing focus
  does not briefly clear or flash the retained query.
- Secondary pages use the official Miuix Navigation3 enter, exit, and
  predictive-back motion.

### Release packaging

- Release APKs enable full R8 code optimization, name obfuscation, and optimized
  resource shrinking. The build must retain Android component entry points,
  TagLib JNI bridges, Media3 playback/session behavior, Miuix/Compose runtime
  metadata, English and Simplified Chinese resources, and versioned snapshot
  restoration. Direct Release APKs retain 32-bit and 64-bit ARM support while
  excluding emulator-only x86 ABIs; Debug keeps all dependency-provided ABIs.
- A deliverable R8 Release includes a non-empty mapping file for crash
  retracing. It must pass Release compilation, vital lint, archive validation,
  signing validation, and 16KB alignment checks for APK entries and every
  packaged native ELF LOAD segment without global
  `-dontshrink`, `-dontoptimize`, or `-dontobfuscate` escape hatches.
- With Blur enabled, secondary-page top bars sample their page backdrop through
  the official Miuix texture-blur path instead of rendering an opaque bar.
- Theme-page switches reflect a tap immediately while persistence completes in
  the background.
- Enabling or disabling predictive back while a secondary page is visible must
  not replace its navigation host or crash the app.
- Floating navigation always uses the official Miuix example's iOS-like
  geometry and selection indicator. Blur changes only its backdrop effect; the
  non-blur state keeps the same iOS-like structure instead of falling back to
  the default `FloatingNavigationBar` style.
- About follows the official Miuix
  About page: OS3 effect background on supported devices, opaque fallback on
  older devices, monochrome launcher icon, app name/version header, and
  scroll-driven sequential shrink/fade into the collapsed top bar. Heavy
  decorative layers are deferred briefly after the page content mounts to avoid
  first-entry jank. Its single Developer preference opens the maintainer's
  public GitHub profile.

## Privacy and Permissions

- No Internet permission.
- Android 13+: `READ_MEDIA_AUDIO`.
- Android 9-12: `READ_EXTERNAL_STORAGE`.
- Foreground service permissions are limited to media playback.
- A non-dangerous wake-lock permission is used only while local audio is actively playing.
- App data is excluded from Android cloud backup and device transfer.
- The app never edits or deletes the user's audio files in version 1.

## Out of Scope

Lyric editing or online lyric search, favorites, playlists, equalizer, tag
editing, file deletion, remote artwork, casting, cloud sync, downloads, and
store publishing.

## Acceptance Criteria

- The project compiles, unit tests and lint pass, and a Debug APK is produced.
- API 28 launches and all shader-dependent UI degrades to readable opaque surfaces.
- API 37 supports normal, floating, and liquid-glass navigation without overlap or crashes.
- API 37 secondary top bars visibly blur scrolled page content when Blur is
  enabled, while API 28 keeps an opaque readable fallback.
- Full-player progress responds to both taps and drags, previews the seek time,
  grows only while pressed, and commits the selected position on release.
- Mini-player empty state, horizontal track switching, queue action, direct
  opening from a tap or upward drag, and Back/downward-drag dismissal are usable
  without intercepting transport buttons. Vertical movement updates the shared
  player progress before release.
- Opening and dismissing the full player use one reversible shared-player
  progress and retain the root list position. The shared artwork reaches the
  measured mini/full endpoints without a geometry jump. Release direction picks
  the spring destination; a cancelled drag returns to its origin instead of
  crossing through the opposite endpoint.
- The floating mini player and navigation pill share the same captured backdrop
  and gravity-following highlight. The mini blur is clipped to its larger pill
  radius, and the navigation shadow matches the official Miuix 10 dp treatment.
- Mini and full-player artwork are independently requested at their layout
  sizes. A current cover remains visible while the next track cover loads, then
  crossfades to the replacement without exposing a placeholder or playback-bar
  surface. The full-player HCT background retains its last completed field until
  the replacement is ready, then independently interpolates its current colors
  into the replacement over 640 ms. Rapid changes continue from the in-flight
  interpolated field instead of restarting from a stale endpoint. Missing
  artwork participates as the fixed `#242424` endpoint.
- Artwork thumbnails preserve their source aspect ratio instead of being
  center-cropped during cache generation. Playback-bar and full-player covers
  fit the complete artwork inside their square bounds; library cards retain
  their existing crop presentation.
- Full-player background and controls follow the shared-player progress while
  the artwork overlay is in flight; the settled page uses the normal final
  layout and the settled mini player uses the normal bar layout.
  The artwork-derived full-player background uses the cached 8-by-8 HCT color
  field with realized chroma capped at 32 and tone fixed to 64 or 32. Its
  animated center-seeded 4-by-4 output follows the quadrant orbit timing above,
  while its complete color grid rotates through pixel mappings every 18 seconds
  during active playback. The bitmap geometry does not rotate. It uses
  a fixed `#242424` fallback when artwork is unavailable and adds no local AAR.
  The playback artwork frame retains its original `secondaryContainer` color
  but has no centered Music icon.
- Secondary and tertiary page backgrounds remain visible and blur correctly
  from the mini-player top through the screen bottom while their last
  interactive item remains reachable above the mini player.
- Song file-name indexing, album/artist grouping and sorting, detail-page queue
  creation, and pseudo-random queue restoration are deterministic.
- Tracks sharing one normalized album name and album-artist tag resolve to one
  album regardless of MediaStore album ID or track artist. Albums with the same
  title and different non-empty album artists remain separate.
- Folder grouping, root search/name-or-count sorting, display-path
  normalization, and folder-detail song search/sorting/queue creation are
  deterministic.
- Root indexes remain anchored below the expanded title when their top bars
  collapse, move only with SearchBar expansion, use the scroll-to-top action
  color while idle, switch the actively dragged letter to black/white, retain a
  theme-colored floating indicator, and fade the scroll-to-top action according
  to large-title visibility. Their top gap is 4 dp. Their bottom remains 12 dp
  above normal navigation or 6 dp above floating navigation and does not jump
  while the root navigation bar enters or exits. Music library renders exactly
  one shared index above its three moving tab pages.
- Tapping the index scroll-to-top action always restores item zero and the large
  title. Sorting while an index drag is active cancels stale work and cannot
  scroll to an index outside the new list. Fast index traversal can enqueue
  many artwork reads without making disk-cache eviction compare live mutable
  file timestamps or crash with a TimSort comparator-contract exception.
- Repeated root-page traversal improves frame timing over the recorded
  API 37 default-Blur baseline of 51 frames, P50 81 ms, P90 121 ms, and 39 slow
  UI-thread frames without changing destination order or pager motion.
- Normal and floating mini-player geometry, edge treatment, swipe feedback,
  direct full-player opening/dismissal, and pressed progress-track shape match
  the requested states without overlap or clipping.
- A search left open does not summon the keyboard when its root page is
  revisited. One Music-library query filters Albums, Artists, and Folders
  consistently across tab changes, and the selected tab plus every requested
  sort/column preference restores after process restart.
- Home, Songs, and Library can each be persisted as the default startup
  destination, and English UI labels use `Library`.
- Cold composition waits only for one bounded DataStore settings value so the
  first visible navigation style is already correct. It does not synchronously
  read the full playback/library snapshot; cached mini metadata and artwork are
  still prioritized independently from the asynchronous library scan.
- Song-row More taps do not flash a pressed overlay, and dismissing the action
  sheet never blanks or swaps its content before the exit motion completes.
- Song rows classify representative 96-kHz lossless, CD-quality lossless,
  high-bitrate lossy, low-bitrate lossy, and unknown files as
  `HR`, `SQ`, `HQ`, no badge, and no badge respectively.
- Album/artist/folder root positions survive detail navigation; the fixed album
  identity block, artist count order, and artist Songs/Albums Miuix pager motion
  remain readable above the persistent mini player.
- Timestamped embedded and external LRC/TTML samples load without network
  access, switch through the full-player horizontal gesture, and track a seek
  or playback-position change without escaping the lyric viewport.
- Every theme-mode selection updates both the preference value and popup
  selection immediately before persistence completes, and predictive back can
  be toggled repeatedly from Theme settings without an app restart or crash.
- Switching the system between light and dark mode does not recreate or collapse
  the player, interrupt playback, or leave the mini-player unable to open.
- Startup permission denial, startup permission grant without scanning, manual
  permission grant followed by scanning, empty library, populated library,
  long metadata, and scan failure states are readable and deterministic.
- A local track plays from the library, continues in the background, responds to system controls, and restores paused at the saved position after process restart.
- Queue mutation and search/sort behavior are deterministic. Repeated playback
  mode changes serialize safely, preserve the current item and position, and do
  not pause or crash playback.
- Follow system, Simplified Chinese, and English can be selected in-app. The
  selected row updates immediately even when the chosen language matches the
  current system locale. Locale application is not delayed for popup dismissal:
  the first and later language changes update all visible strings in place
  without a black frame.
