# ScreenPilot Multiple-Select Feature

## Purpose

ScreenPilot now distinguishes between:

- `MULTIPLE_CHOICE`: single-answer questions, usually represented by circular radio controls.
- `MULTIPLE_SELECT`: questions allowing more than one answer, usually represented by square checkbox controls or explicit multi-answer wording.
- `FREE_RESPONSE`: typed, written, short-answer, calculation, or essay questions without selectable choices.
- `UNCLEAR`: incomplete or ambiguous captures.

## Runtime behavior

For `MULTIPLE_SELECT`, Gemini returns `answer_indices` as a sorted integer array. The parser validates every index as an integral value from 1 through 5, removes duplicates, and sorts the final result. The overlay displays the result compactly, for example `(1,2)` or `(3,5)`.

The model instruction explicitly warns that controls already shown as checked or selected may be the user's previous choices. ScreenPilot must solve the question rather than copy the visible checkmarks.

## Storage compatibility

No Room schema migration is required. Existing fields are reused safely:

- `questionType = MULTIPLE_SELECT`
- `answerIndex = 0`
- `answerText = comma-separated indices`, such as `1,2`

The history UI renders this as `Answers: (1,2)`. Existing history rows remain compatible.

## Build and installation

This change does not require a new Google AI Studio UI project. The existing overlay already accepts arbitrary answer text, and the history screen is maintained in Kotlin/Compose. Upload this source to GitHub and let the existing GitHub Actions workflow build the debug APK.

The app version is bumped to `versionCode 2` / `versionName 1.1`. A GitHub Actions debug APK may be signed with a different ephemeral debug key from a previous run. If Android reports an incompatible signature, the older debug app must be removed before installation, which also removes locally stored API keys and app history. For update-in-place releases, configure a persistent release keystore through GitHub Secrets.

## Files changed

- `app/src/main/java/com/example/service/ProviderGateway.kt`
- `app/src/main/java/com/example/service/ResponseParser.kt`
- `app/src/main/java/com/example/service/ScreenCaptureService.kt`
- `app/src/main/java/com/example/data/HistoryEntity.kt`
- `app/src/main/java/com/example/MainActivity.kt`
- `app/src/test/java/com/example/service/ProviderGatewayTest.kt`
- `app/src/test/java/com/example/service/ResponseParserTest.kt`
- `README.md`
- `app/build.gradle.kts`
