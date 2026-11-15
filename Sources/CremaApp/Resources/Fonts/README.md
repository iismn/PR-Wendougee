# Pretendard bundled fonts

Unmodified static OpenType faces from the official [Pretendard1.3.9 release](https://github.com/orioncactus/pretendard/releases/tag/v1.3.9). The accompanying LICENSE.txt is the release's SIL Open Font License1.1. PROVENANCE.json records source URLs, bytes and SHA-256 hashes.

The actual name-table PostScript names are Pretendard-Regular, Pretendard-Medium, Pretendard-SemiBold and Pretendard-Bold. CremaTypography selects these names; design requests such as serif/rounded intentionally stay in Pretendard. Thin/light requests use the bundled Regular face; heavy/black use Bold.

Both Xcode targets copy this Fonts folder intact. iOS lists Fonts/*.otf in UIAppFonts. macOS declares ATSApplicationFontsPath=Fonts. CremaTypography.registerFonts() additionally uses process-scoped CoreText registration and is called during app initialization. The font helper also registers lazily, covering previews.

The offline review companion's official variable WOFF2 and license live in outputs/ui-preview/fonts. Its web family is Pretendard Variable, with weight range45–920.

This Windows host cannot verify native Xcode compilation or font enumeration at runtime; those require the iOS/macOS build environment.
