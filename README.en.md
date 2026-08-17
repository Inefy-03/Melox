<h1 align="center">
  <img src="assets/Melox-icon.png" alt="Melox app icon" width="96"><br>
  Melox
</h1>

<p align="center">
  <b>An Android local music player built on [Miuix](https://github.com/compose-miuix-ui/miuix)</b>
</p>

<p align="center">
  <a href="README.md"><img src="https://img.shields.io/badge/文档-简体中文-3478F6?style=flat" alt="Simplified Chinese"></a>
  <img src="https://img.shields.io/badge/Android-9%2B-3DDC84?style=flat&logo=android&logoColor=white" alt="Android 9+">
  <a href="https://github.com/compose-miuix-ui/miuix"><img src="https://img.shields.io/badge/Miuix-0.9.3-4F6BED?style=flat" alt="Miuix 0.9.3"></a>
  <a href="https://developer.android.com/media/media3"><img src="https://img.shields.io/badge/Media3-1.11.0-4F6BED?style=flat" alt="AndroidX Media3 1.11.0"></a>
</p>

---

## Overview

Melox is an Android local music player built with Jetpack Compose, Miuix, and AndroidX Media3.

## Features

### Library and Home

- Scan the system media library or restrict scanning to selected folders
- Browse songs, albums, artists, and folders with search, sorting, and alphabetical indexes
- Open dedicated album and artist details, then start a queue from the current page
- Use Home recommendations and recently added music to rediscover tracks in the local library
- Receive one `audio/*` item from Android Open with or Share

### Playback, queue, and restoration

- Mini and full players share the current track, artwork, and playback state
- Seek through a track, use Previous and Next, and switch between ordered, repeat-one, and random playback
- Open or clear the queue, remove individual songs, play next, or add songs to the current queue
- Restore the queue, current item, and position after reopening or process restart without starting playback unexpectedly
- Open an external audio file through Android and hand it directly to Melox

### Local lyrics and track information

- Support word-by-word timing, translation lines, text size, and font weight
- Configure alignment, inactive-line blur, and whether playback controls remain visible on the Lyrics page
- Show title, artist, album, format, bitrate, sample rate, bit depth, duration, and file location
- If Music Tag Editor or Lyrico is installed, open the selected track for editing and refresh only that track's local information on return

### Player and appearance

- System, light, and dark themes
- Artwork- or wallpaper-derived dynamic colors
- Blurred artwork, flowing colors, floating navigation, and liquid glass
- Predictive back, a configurable startup page, scan refresh behavior, and folder scope
- Simplified Chinese, English, and follow-system language options

## Requirements

- Android 9 (API 28) or later
- Current APK builds target `arm64-v8a`
- Local-music read permission on the first scan

Liquid glass and other runtime visual effects require Android 13 or later.

## Download and install

Download an APK from [Releases](../../releases), open Melox, grant the permission, and use **Scan music** to create or refresh the library.

The current version deliberately stays offline. Online music, network lyrics, cloud sync, casting, downloads, favorites, and user playlists are not included.

## Build

JDK 17 or newer and an Android SDK with `compileSdk 37` are required.

```bash
./gradlew :app:assembleDebug
```

The Debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Before developing or submitting a change, also run:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
```

Release signing uses local `local.properties`; keystores and passwords are not repository content and must never be committed.

## Credits

- [Miuix](https://github.com/compose-miuix-ui/miuix) - UI components and design system
- [AndroidX Media3](https://developer.android.com/media/media3) - local playback and system media sessions

Melox is under active development.