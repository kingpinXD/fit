# Fit

Fitness tracking Android app.

## Build

```bash
export ANDROID_HOME=$HOME/android-sdk
make build        # Build debug APK → ~/Downloads/Fit.apk
make test         # Run unit tests
make install-phone # Build + install on all connected devices
make run          # Build + install + launch
```

## Setup

1. Create a Firebase project and download `google-services.json` to `app/`
2. Enable Google Sign-In in Firebase Authentication
3. Enable Realtime Database

## Tech Stack

- Kotlin, Android SDK 34, min SDK 24
- Room (local DB), Firebase RTDB (sync), Firebase Auth (Google Sign-In)
- MVVM with ViewModel + LiveData
- Material Components, ConstraintLayout
- Gradle 8.5, AGP 8.2.2, KSP 1.9.22
