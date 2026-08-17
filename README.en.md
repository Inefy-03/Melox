<div style="text-align: center;">
  <img src="assets/Melox-icon.png" alt="Melox app icon" width="96"><br>
  <h1>Melox</h1>
  <p>
    <strong>An Android local music player based on <a href="https://github.com/compose-miuix-ui/miuix">Miuix</a></strong>
  </p>
  <p>
    <img src="https://img.shields.io/badge/Android-9%2B-3DDC84?style=flat&logo=android&logoColor=white" alt="Android 9+">
    <a href="https://github.com/compose-miuix-ui/miuix"><img src="https://img.shields.io/badge/Miuix-0.9.3-4F6BED?style=flat" alt="Miuix 0.9.3"></a>
    <a href="https://developer.android.com/media/media3"><img src="https://img.shields.io/badge/Media3-1.11.0-4F6BED?style=flat" alt="AndroidX Media3 1.11.0"></a>
  </p>
  <p><a href="README.md">简体中文</a></p>
</div>

---

## Overview

Melox is an Android local music player built with Jetpack Compose, Miuix, and AndroidX Media3.

## Features

### Library and Home

- Scan the system media library or restrict the scan scope to selected folders
- Search, sort, and browse songs, albums, artists, and folders with alphabetical indexes
- Open dedicated album and artist details, then start playback from the current page queue
- Use Home recommendations and recently added music to rediscover tracks in the local library

### Playback, queue, and restoration

- Seek through tracks, use Previous and Next, and switch between ordered, repeat-one, and random playback
- Open or clear the queue, remove individual songs, play a song next, or add songs to the current queue
- Restore the queue, current track, and playback position after reopening or process restart without starting playback automatically
- Open an external audio file through Android and play it directly in Melox

### Local lyrics and track information

- Support word-by-word timing, translation lines, text size, and font weight adjustments
- Choose lyric alignment, blur inactive lines, and whether playback controls remain visible on the Lyrics page
- Show title, artist, album, format, bitrate, sample rate, bit depth, duration, and file location
- With Music Tag Editor or Lyrico installed, jump to editing from track actions

### Player and appearance

- Follow the system theme, or use light and dark themes
- Use dynamic colors based on the current artwork or system wallpaper
- Enable blurred artwork, flowing colors, a floating bottom bar, and liquid glass effects
- Configure predictive back, the default startup page, scan refresh behavior, and folder scope
- Switch between Simplified Chinese, English, and the system language in the app

## Requirements

- Android 9 (API 28) or later
- Current APK builds target `arm64-v8a`
- Local-music read permission is required for the first scan
- Liquid glass and other runtime visual effects require Android 13 or later

## Download and install

Download an APK from [Releases](https://github.com/Inefy-03/Melox/releases).

## Credits

- [Miuix](https://github.com/compose-miuix-ui/miuix) - UI components and design system
- [AndroidX Media3](https://developer.android.com/media/media3) - local playback and system media sessions

Melox is under active development.
