# ScanMate AI Pro v1.5.0 Implementation Report

This source package was upgraded against `ScanMate_Design_10_10.html` and the requested P0/P1 prompt.

## Implemented

### P0
- Added Nunito font resources under `app/src/main/res/font/` using Android downloadable font XML resources. No font binary files are bundled.
- Replaced Compose typography with a `Nunito` `FontFamily` in `ui/theme/Type.kt`.
- Updated light/dark design tokens to the requested palette:
  - Light background `#F5F6F8`, surface `#FFFFFF`, primary `#1A56DB`, accent `#0D9488`
  - Dark background `#0F1117`, surface `#181C26`, primary `#4B8EF8`, accent `#2DD4BF`
- Disabled dynamic color in `ScanMateTheme` so the supplied design tokens are respected.
- OCR preprocessing now performs EXIF orientation correction, max-2048 scaling, grayscale conversion, high-contrast filter processing via `FilterType.HIGH_CONTRAST`, sorted text blocks, and ML Kit symbol confidence.
- ML Kit text recognizer is now a lazy singleton and is closed from ViewModel `onCleared()`.
- Release build now enables R8/resource shrinking and sets version name to `1.5.0`.
- Added ProGuard keep rules for ZXing and ML Kit.
- PDF export now supports `PdfPageSize.AUTO`, `PdfPageSize.A4`, and `PdfPageSize.LETTER`, letterboxes pages, recycles per-page bitmaps, and supports `onProgress` messages like `Building page X of Y`.
- Export dialog defaults to `FileUtils.sanitizeFileBaseName(document.title)`.
- Home screen was rebuilt into three clear zones: header/search, hero scan/import/stat card, and document list.
- Home tool actions were moved into a compact horizontal chip row.
- Home blocks were extracted into `ui/screens/home/` files.
- Camera screen now saves/imports documents through `CameraViewModel` instead of calling `AppDatabase` directly.
- Document detail screen now gets its DAO through `DocumentDetailViewModel`, removing direct `AppDatabase.getDatabase(context).docDao()` from the screen.
- Added `DocDao.getFirstPagePerDocument()` and changed `DocumentViewModel.allPages` to use first pages for the home list.
- Page editor perspective correction was changed from numeric sliders to a Canvas-based 4-corner drag UI.

### P1
- Replaced the custom home bottom bar with Material 3 `NavigationBar` and a center camera FAB.
- Added `HomeLoadingSkeleton` shimmer using `animateFloat` and a linear gradient brush sweep.
- Export name defaults are document-title based rather than timestamp based.
- OCR recognizer singleton lifecycle cleanup is implemented in ViewModels.

## Verification note

A Gradle compile attempt was made with:

```bash
./gradlew :app:compileDebugKotlin --stacktrace --no-daemon
```

The sandbox could not download the Gradle distribution because external DNS/network access is blocked:

```text
UnknownHostException: services.gradle.org
```

Run the same command locally or in GitHub Actions to complete final compile verification.
