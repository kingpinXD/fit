# Android App Scaffold Guide

Reusable template for bootstrapping new Kotlin Android apps with the same tech stack.

## Prerequisites

- macOS with Homebrew
- JDK 17: `brew install openjdk@17`
- Android SDK at `$HOME/android-sdk` (cmdline-tools, platform-tools, build-tools, platform 34)
- Firebase project created at console.firebase.google.com

## 1. Project Structure

```
{project}/
├── build.gradle.kts          # Root — plugin versions only
├── settings.gradle.kts        # dependencyResolutionManagement, rootProject.name
├── gradle.properties          # JVM args, AndroidX, Kotlin style
├── gradlew / gradlew.bat      # Gradle wrapper scripts
├── Makefile                   # build, test, install-phone, check-phone, run
├── .gitignore
├── CLAUDE.md                  # Project-specific AI context
├── README.md
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties   # Gradle 8.5
├── app/
│   ├── build.gradle.kts       # App — namespace, SDK versions, dependencies
│   ├── google-services.json   # Firebase config (gitignored)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/{app}/
│       │   │   ├── {App}App.kt          # Application class
│       │   │   ├── LoginActivity.kt      # Google Sign-In entry point
│       │   │   ├── MainActivity.kt       # Main screen
│       │   │   └── data/
│       │   │       ├── AppDatabase.kt    # Room singleton
│       │   │       └── FirebaseSyncManager.kt
│       │   └── res/
│       │       ├── layout/
│       │       │   ├── activity_login.xml
│       │       │   └── activity_main.xml
│       │       └── values/
│       │           ├── strings.xml
│       │           ├── colors.xml
│       │           └── themes.xml
│       └── test/java/com/example/{app}/
│           └── ExampleTest.kt
├── docs/
└── scripts/
```

## 2. Version Matrix

| Component | Version | Notes |
|-----------|---------|-------|
| Gradle | 8.5 | Via wrapper |
| AGP | 8.2.2 | Android Gradle Plugin |
| Kotlin | 1.9.22 | |
| KSP | 1.9.22-1.0.17 | Must match Kotlin version |
| Google Services Plugin | 4.4.0 | Firebase |
| compileSdk | 34 | |
| minSdk | 24 | Android 7.0+ |
| targetSdk | 34 | |
| Java | 17 | source + target + jvmTarget |

## 3. Dependencies

### Core
```kotlin
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.appcompat:appcompat:1.6.1")
implementation("com.google.android.material:material:1.11.0")
implementation("androidx.constraintlayout:constraintlayout:2.1.4")
implementation("androidx.recyclerview:recyclerview:1.3.2")
```

### Room Database
```kotlin
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")
```

### Lifecycle & MVVM
```kotlin
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
implementation("androidx.activity:activity-ktx:1.8.2")
```

### Firebase
```kotlin
implementation(platform("com.google.firebase:firebase-bom:32.7.1"))
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-database-ktx")
```

### Auth
```kotlin
implementation("com.google.android.gms:play-services-auth:20.7.0")
```

### Testing
```kotlin
testImplementation("junit:junit:4.13.2")
testImplementation("org.robolectric:robolectric:4.11.1")
testImplementation("androidx.test:core-ktx:1.5.0")
testImplementation("androidx.test.ext:junit-ktx:1.1.5")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("androidx.arch.core:core-testing:2.2.0")
```

## 4. Gradle Config Notes

### settings.gradle.kts
- Use `dependencyResolutionManagement` (not `dependencyResolution`) — Gradle 8.5 requires the full name.
- `repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)`

### gradle.properties
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

### app/build.gradle.kts
- Enable `buildConfig = true` under `buildFeatures` if you need BuildConfig fields.
- KSP is used for Room annotation processing (not kapt).

## 5. Firebase Setup

1. Create project at console.firebase.google.com
2. Add Android app with your package name (e.g., `com.example.fit`)
3. Download `google-services.json` → `app/google-services.json`
4. Enable Authentication → Google provider
5. Enable Realtime Database
6. Set database rules (start with authenticated read/write):
   ```json
   {
     "rules": {
       ".read": "auth != null",
       ".write": "auth != null"
     }
   }
   ```

### Firebase Sync Best Practices
- Key entries by natural unique field (timestamp millis) to prevent duplicates
- Serialize `onDataChange` processing — never spawn parallel coroutines from listeners
- Add duplicate guards before Room inserts (check by natural key)
- Include startup deduplication as safety net

## 6. Auth Pattern

- Google Sign-In → Firebase Auth credential exchange
- `LoginActivity` is the launcher; if already signed in, skip to `MainActivity`
- `default_web_client_id` string comes from `google-services.json` auto-generation
- Whitelist emails in-app if needed (check `auth.currentUser?.email`)

## 7. Makefile Targets

| Target | Description |
|--------|-------------|
| `build` | assembleDebug, copy APK to ~/Downloads |
| `test` | testDebugUnitTest |
| `install` | adb install single device |
| `install-phone` | build + install on all connected devices |
| `check-phone` | show installed version on all devices |
| `run` | build + install + launch |
| `clean` | gradlew clean |

## 8. Architecture Patterns

- **MVVM**: Activity observes ViewModel LiveData. ViewModel calls Repository/DAO.
- **Room**: Singleton via `getInstance()`. DAOs use suspend functions. Migrations are explicit.
- **Firebase Sync**: Dedicated `*SyncManager` classes. Never put Firebase logic in Activities or ViewModels.
- **Multi-device**: Any auto-triggered logic must be idempotent. Firebase is source of truth, Room is cache.

## 9. .gitignore

```
build/
app/build/
.gradle/
local.properties
.idea/
*.iml
app/google-services.json
.claude/
.DS_Store
.venv/
```

## 10. New App Checklist

- [ ] Copy this scaffold structure
- [ ] Replace `{app}` with app name in all paths and package names
- [ ] Update `rootProject.name` in settings.gradle.kts
- [ ] Update `namespace` and `applicationId` in app/build.gradle.kts
- [ ] Update `PKG` in Makefile
- [ ] Create Firebase project + download google-services.json
- [ ] Register SHA-1 for Google Sign-In: `./gradlew signingReport`
- [ ] Define Room entities and DAOs
- [ ] Set up Firebase RTDB structure
- [ ] Write CLAUDE.md with project-specific context
- [ ] Init git, create GitHub repo, push
