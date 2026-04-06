# Fit - Fitness Tracking App

## Tech Stack
- **Language:** Kotlin
- **Build:** Gradle 8.5 with Kotlin DSL, AGP 8.2.2, KSP 1.9.22-1.0.17
- **Android:** compileSdk=34, minSdk=24, targetSdk=34, Java 17
- **Database:** Room 2.6.1 (local), Firebase RTDB (sync)
- **Auth:** Firebase Auth + Google Sign-In (play-services-auth 20.7.0)
- **UI:** Jetpack Compose with Material3 (migrating from XML layouts)
- **Theme:** Black and gold — dark surfaces (#121212, #1A1A1A), gold primary (#D4AF37), light gold highlights (#E6C867)
- **Architecture:** MVVM (ViewModel + Compose state / LiveData for Room)
- **Testing:** JUnit 4.13.2, Robolectric 4.11.1, coroutines-test 1.7.3

## Build Commands
- `make build` — assembleDebug → ~/Downloads/Fit.apk
- `make test` — unit tests
- `make install-phone` — build + install on all connected phones
- `make check-phone` — show installed version on connected phones
- `make run` — build + install + launch

## Environment
- `ANDROID_HOME=$HOME/android-sdk`
- JDK 17 (Homebrew OpenJDK)
- Package: `com.example.fit`

## Guidelines

### Multi-Device Awareness
- App may be installed on multiple phones. Any automatic logic (timers, scheduled resets) must be idempotent and safe to run on multiple devices simultaneously.
- Use Firebase as source of truth for shared state. Local Room DB is a cache.

### Firebase Sync Lessons (from weekly-totals)
- **Never** spawn concurrent coroutines from `onDataChange` listeners. Serialize sync operations using `Channel.CONFLATED` or equivalent.
- Always add defensive duplicate guards before Room inserts — check by natural key (e.g., `createdAt` timestamp) before inserting.
- Add a startup deduplication pass (`deduplicateByCreatedAt()`) as a safety net.
- Key Firebase entries by a natural unique field (e.g., timestamp in millis) to prevent duplicates at the source.

### Testability
- Keep functions short and pure where possible for easy unit testing.
- All Room queries and sync logic should be independently testable.
- Local DB should be queryable for debugging — use `adb shell run-as com.example.fit` to inspect state on-device.

### Code Style
- Early returns over nested if/else
- ViewModels expose state via LiveData (Room) or Compose State
- Room DAOs use suspend functions with coroutines
- Firebase operations in dedicated SyncManager classes, not in Activities/ViewModels
- UI in Jetpack Compose — no new XML layouts

### Decisions
- (2026-04-06) Migrating from XML layouts to Jetpack Compose with Material3. Compose reduces boilerplate (no adapters, ViewHolders, findViewById), makes animations and custom components trivial, and provides better state management for the exercise tracking UI. Black and gold dark theme.
