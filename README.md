# ScreenPilot

ScreenPilot is a native Android (Kotlin + Jetpack Compose) screen-analysis assistant.

Current answer modes:

- **Single-answer multiple choice**: ScreenPilot recognizes radio-style/circular controls and returns one selected answer index through the compact overlay popup.
- **Multiple-select questions**: ScreenPilot recognizes checkbox-style/square controls or explicit multi-answer wording and returns all correct indices in a compact form such as `(1,2)`.
- **Free response / essay**: ScreenPilot posts a concise answer silently to the Android notification shade.
- **Unclear / incomplete capture**: no answer is fabricated and no API-key failover is triggered solely because the visible question is incomplete.

The app supports both normal single-screenshot analysis and the staged two-long-press flow for questions that span more than one screen.

## API keys

Gemini API keys are entered at runtime inside ScreenPilot and stored using the app's Android Keystore-backed encrypted key storage. Do **not** add Gemini keys to `.env`, `BuildConfig`, `local.properties`, or source control.

## Build locally

Prerequisites:

- Android Studio / Android SDK matching the project configuration
- JDK 17 for the Gradle/Android build toolchain

Open the repository root in Android Studio, let Gradle sync, then build the debug variant.

Command-line validation:

```bash
./gradlew :app:compileDebugKotlin --rerun-tasks
./gradlew :app:testDebugUnitTest --rerun-tasks
./gradlew :app:assembleDebug --rerun-tasks
./gradlew :app:lintDebug --rerun-tasks
```

The debug APK is normally produced at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions

The included `.github/workflows/android-build.yml` compiles, tests, assembles, lints, and uploads the debug APK as a workflow artifact.

## Important

Do not migrate this repository into a React/Vite/Node application. ScreenPilot is a native Android project and its MediaProjection/overlay behavior depends on Android platform APIs.
