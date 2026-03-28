# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

All commands use the Gradle wrapper from the project root:

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests (JVM, no device needed)
./gradlew test

# Run a single unit test class
./gradlew :app:testDebugUnitTest --tests "com.mikec.macautravellogger.ExampleUnitTest"

# Run instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Clean build
./gradlew clean
```

## Architecture

**MacauTravelLogger** — single-module Android app tracking time spent in Macau for residency compliance.

Package: `com.mikec.macautravellogger`
Pattern: MVVM + Repository | Min SDK 26, Target SDK 34

> All source files live under `app/src/main/java/com/example/myapplication/` but declare package `com.mikec.macautravellogger.*` — the directory path does not match the package name (Kotlin allows this).

### Layer overview

```
di/                          Hilt modules (DatabaseModule, DataStoreModule)
data/
  local/                     Room: TravelEntry entity, TravelEntryDao, TravelDatabase
  repository/TripRepository  Single data access point; calculates durationHours on checkOut/update
  preferences/               DataStore: UserPreferencesRepository (geofence radius, compliance days)
util/DateUtils               java.time helpers (minSdk 26+): dates, month arithmetic, duration calc
ui/
  home/TripViewModel         Active trip + month stats (tripCount, dayCount, compliancePercent)
  history/HistoryViewModel   Per-month entry list + edit with durationHours recalculation
  report/ReportViewModel     MonthlySummary aggregation + exportToCsv/exportToPdf triggers
  settings/SettingsViewModel Reads/writes UserSettings via UserPreferencesRepository
  theme/                     MacauTravelLoggerTheme (Material3, dynamic color on API 31+)
```

### Key design decisions

- **Duration is always recalculated** by `TripRepository.updateEntry()` whenever both `checkInTime` and `checkOutTime` are non-null — never trust the stored value when editing times.
- **Active trip** is observed via `TravelEntryDao.getActiveFlow()` (a separate Room `@Query` returning `Flow<TravelEntry?>`), not derived from `getAll()`.
- **Month navigation** caps forward at the current month (`DateUtils.isCurrentOrPastMonth()`).
- **Export** (PDF via iTextG, CSV via OpenCSV) is Phase 3; `ReportViewModel` emits `ExportEvent` via `SharedFlow` — the UI layer owns file-system interactions.
- `flatMapLatest` in `HistoryViewModel` and `ReportViewModel` requires `@OptIn(ExperimentalCoroutinesApi::class)`.

### Key technical details

- `compileSdk` uses the new `release(36) { minorApiLevel = 1 }` DSL (AGP 9.x syntax).
- Kotlin 2.2.10, AGP 9.1.0, KSP 2.2.10-2.0.2, Hilt 2.59.2 (minimum for AGP 9 support), Room 2.7.0.
- `android.disallowKotlinSourceSets=false` in `gradle.properties` — required because KSP registers generated sources via `kotlin.sourceSets` DSL, which AGP 9 restricts by default.
- Dependencies managed via `gradle/libs.versions.toml` (version catalog).
- Two test source sets: `test/` for JVM unit tests (JUnit 4), `androidTest/` for instrumented tests.
