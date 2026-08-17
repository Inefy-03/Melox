# Third-Party Notices

Melox includes or adapts software distributed under the Apache License 2.0:

- [compose-miuix-ui](https://github.com/compose-miuix-ui/miuix), including adapted pager, animation, and liquid-glass components.
- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass), used as a reference for the liquid-glass renderer.
- [MaterialKolor](https://github.com/jordond/materialkolor), used for HCT conversion in the full-player artwork color field.
- [RenderScript Intrinsics Replacement Toolkit](https://github.com/android/renderscript-intrinsics-replacement-toolkit), rebuilt from the official source with NDK 29 flexible-page-size support and used for one-time cached recommendation-artwork blur processing.
- AndroidX Media3 `decoder_ffmpeg`, rebuilt locally from the `1.11.0` source
  module and bundled as an `arm64-v8a` AAR. The AndroidX source is Apache
  License 2.0.
- [FFmpeg](https://ffmpeg.org/), version `9.0`, rebuilt locally as the static
  audio-decoding libraries used by the Media3 FFmpeg extension. This build is
  configured under the GNU Lesser General Public License 2.1 or later and
  contains no GPL components. The enabled decoders are listed in
  `app/libs/README.md`.
- [Lyrico](https://github.com/Replica0110/Lyrico), used as a reference for music-row information hierarchy and alphabet-index interaction.
- [Halcyon](https://github.com/Kifranei/Halcyon), used as a reference for external music-tag editor intent compatibility and fallback behavior.
- [accompanist-lyrics-ui](https://github.com/6xingyv/accompanist-lyrics-ui), with its Apache-2.0 Canvas glyph rendering, karaoke timing, and Lookahead spring-placement behavior adapted for the local lyrics view.
- [TagLib for Android](https://github.com/Kyant0/taglib), used to read local
  audio properties through TagLib. Its bundled upstream
  [TagLib](https://github.com/taglib/taglib) code remains subject to its
  applicable MPL/LGPL terms.
- AndroidX, AndroidX Media3, Kotlin, and [TinyPinyin](https://github.com/promeG/TinyPinyin), used as application dependencies.

Applicable adapted source files retain copyright and SPDX headers. The Apache License 2.0 is available at <https://www.apache.org/licenses/LICENSE-2.0>.
