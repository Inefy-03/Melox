# Bundled Android Libraries

## RenderScript Intrinsics Replacement Toolkit (Blur Only)

- File: `renderscript-toolkit-blur-344be3f-arm64-16k.aar`
- Source: [AOSP Toolkit](https://github.com/android/renderscript-intrinsics-replacement-toolkit/tree/344be3f6bf03fb6b63a80b36f08f8dccac59d784)
- Source commit: `344be3f6bf03fb6b63a80b36f08f8dccac59d784`
- License: Apache License 2.0; upstream copyright notices are retained.
- Source archive SHA-256: `1243a1cb118cbeb3de596f8c03aacba52b98758bd9c7ed57c87f215bc8ef8044`
- AAR SHA-256: `6e463cee1f427f711ef6aecde508133de39240297f5809dd2f775b8d19669e7d`

This subset contains only Bitmap/ByteArray `Toolkit.blur`, `Range2d` and the
native create/destroy/blur entry points. The blur kernel, arm64 NEON assembly,
validation, worker pool and bitmap access are unchanged. The sole Kotlin API
compatibility adjustment is `requireNotNull(Bitmap.config)` after validation.
Blend, color matrix, convolve, histogram, LUT, resize and YUV conversion are
excluded from both the Kotlin API and native build.

The checked-in AAR is consumed directly by the app's Gradle build. The repository
does not include a script to regenerate it. APK native libraries stay
uncompressed and page-aligned.

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

The app places this extension before the system Media3 renderer for supported
formats, while retaining the system renderer as fallback. The AAR contains only
`jni/arm64-v8a/libffmpegJNI.so`.
