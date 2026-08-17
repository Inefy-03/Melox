# Bundled Android Libraries

## RenderScript Intrinsics Replacement Toolkit

- File: `renderscript-intrinsics-replacement-toolkit-344be3f-16k.aar`
- Source: Android Open Source Project `android/renderscript-intrinsics-replacement-toolkit`
- Source commit: `344be3f6bf03fb6b63a80b36f08f8dccac59d784`
- License: Apache License 2.0
- SHA-256: `2d4c4f44bcfad3502c2aaca8473376912db256b2e66e7bda7208a9c1e6380a61`

The AAR was rebuilt with Android Gradle Plugin 9.2.1, compile SDK 37, NDK
29.0.14206865, CMake 3.22.1, and flexible page sizes enabled. The only source
compatibility adjustment wraps `Bitmap.config` in `requireNotNull` after the
upstream bitmap validation, preserving behavior while compiling against the
newer nullable Android API declaration.

The bundled 64-bit native libraries use 16 KB (`0x4000`) ELF load-segment
alignment.

## Media3 FFmpeg audio decoder

- File: `media3-decoder-ffmpeg-1.11.0-ffmpeg9.0-arm64-v8a.aar`
- AndroidX Media3 source module: `1.11.0`, `lib-decoder-ffmpeg`
- FFmpeg source: `9.0`, supplied locally at build time
- License: AndroidX module under Apache License 2.0; FFmpeg libraries under
  LGPL 2.1 or later
- Build: NDK `29.0.14206865`, CMake `3.22.1`, `arm64-v8a` only, API 28
  toolchain
- FFmpeg configuration: static `avcodec`, `avutil`, and `swresample`; no
  programs, demuxers, network, video, `iconv`, or GPL components
- Enabled audio decoders: AAC, MP3, AC-3, E-AC-3, TrueHD, DTS, Vorbis, Opus,
  AMR-NB, AMR-WB, FLAC, ALAC, μ-law, and A-law
- SHA-256:
  `c03dacbed68c55782100f8bb715e85483fc6c95cea18c97591dd8c8e311ad4fa`

The app keeps the system Media3 renderer first and loads this extension as the
FFmpeg fallback. The AAR contains only `jni/arm64-v8a/libffmpegJNI.so`.
