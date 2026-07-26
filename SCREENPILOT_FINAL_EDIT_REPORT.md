# ScreenPilot Essay / Free-Response Implementation Report

Baseline: `screenpilot(11).zip` (AI Studio Android export built from the physical-known-good `screenpilot(10)` line).

## Baseline suitability

The export was suitable for continued engineering work. The AI Studio scaffold only added a static essay-notification preview in `MainActivity.kt` and `EssayAnswerNotificationManager.kt`; the known-good capture, staged two-long-press, MediaProjection, Gemini failover, KeyStore, and fresh-frame infrastructure remained intact.

## Production implementation

### Typed answer contract

`ResponseParser` now returns a sealed `ParsedAnswer`:

- `MultipleChoice(answerIndex, confidence)`
- `FreeResponse(answerText, confidence)`
- `Unclear(confidence)`

The parser remains backward-compatible with the previous MC-only JSON and standalone `1`-`5` response format.

### Gemini request contract

The image request now asks Gemini to classify the visible question as:

- `MULTIPLE_CHOICE`
- `FREE_RESPONSE`
- `UNCLEAR`

The structured response requires:

- `question_type`
- `answer_index` (`1..5` for MC, `0` otherwise)
- `answer_text` (concise text for free response, empty string otherwise)

`confidence` remains optional.

The prompt explicitly avoids treating cropped/off-screen answer choices as an essay question. In incomplete cases the model must return `UNCLEAR` instead of fabricating a response.

Staged two-image requests continue to send Image 1 followed by Image 2 in one Gemini request.

### Presentation routing

- Multiple choice -> existing answer popup, unchanged.
- Free response / essay -> silent notification-shade answer.
- Unclear -> no fabricated popup/notification answer and no provider-key failover caused solely by incomplete content.

Notification delivery failure or missing Android notification permission does not convert a valid Gemini response into provider failure. History still records the result.

### Essay notification

Channel: `screen_pilot_essay_answers_v1`

Notification ID: `54322` (latest essay answer replaces the previous one)

Behavior:

- `IMPORTANCE_LOW`
- silent
- no vibration
- no notification light
- no badge
- `BigTextStyle`
- `VISIBILITY_PRIVATE`
- auto-cancel
- no Toast/popup fallback

The AI Studio static `Test Notifikasi Essay` preview remains available.

### History and database migration

`HistoryEntry` now stores:

- `questionType`
- `answerText`

Database version: `3 -> 4`

`MIGRATION_3_4` preserves existing history by adding the two columns instead of relying on destructive migration. Old success rows remain `MULTIPLE_CHOICE`; old error rows are explicitly marked `ERROR`.

`questionType` uses a Room `@ColumnInfo` default matching the SQL migration default to avoid migration-schema default mismatch.

History UI now distinguishes:

- MC answer
- Essay answer (two-line preview)
- Unclear
- Failed

## Protected logic preserved

The following known-good source files were intentionally left byte-for-byte unchanged from `screenpilot(11)`:

- `KeyStoreHelper.kt`
- `FreshFrameReadinessGate.kt`
- `CaptureSurfaceResizeCoordinator.kt`
- `ImageUtils.kt`
- `FailoverPreferenceReader.kt`

`ScreenCaptureService.kt` was changed only at imports and final answer presentation/history routing. The screenshot capture / fresh-frame / staged-gesture sections were not edited.

## Tests added/updated

- `ResponseParserTest`: typed MC/free-response/unclear parsing, legacy compatibility, validation, normalization and bounds.
- `ProviderGatewayTest`: structured schema classification contract and staged Image 1 -> Image 2 ordering.
- `EssayAnswerNotificationManagerTest`: low/silent channel, BigText/private notification, replacement behavior, blank-answer rejection.

Existing reliability tests remain.

Total test annotations currently present in `app/src/test/java`: 153.

## GitHub readiness

Added:

- `.gitignore` excluding Gradle/build/IDE/local signing state.
- `.github/workflows/android-build.yml` to run real compile, unit tests, debug APK assembly and lint with JDK 17 and the project Gradle wrapper.

The stale AI Studio README instructions referring to `.env` Gemini keys and removed debug signing were replaced. ScreenPilot API keys remain runtime Android-Keystore-only.

## Validation performed in this environment

- Gradle wrapper JAR structure validated with `jar tf`.
- Full modified Kotlin source was passed through `kotlinc` parsing; no Kotlin parser/syntax errors were detected. Android/AndroidX references cannot resolve here because this environment has no Android SDK/classpath.
- `ResponseParser.kt` independently type-compiled with a minimal `org.json` compatibility stub: success.
- `HistoryEntity.kt` independently type-compiled with minimal Room annotation stubs: success.
- Room migration SQL was executed against an in-memory SQLite v3-style table and preserved MC/error classification as intended.
- Recursive static checks found no executable `acquireNextImage`, cross-clock `Image.timestamp`/`elapsedRealtimeNanos`, BuildConfig Gemini API key fallback, Mockito dependency, or web/React/Vite project files.
- Protected capture helper hashes were compared against the exported baseline and remain identical.

## Build status

**FULL ANDROID BUILD NOT EXECUTED IN THIS SANDBOX.**

The wrapper is valid and targets Gradle 9.3.1, but this sandbox cannot resolve/download `services.gradle.org` and has no Android SDK. Therefore this report does not claim `compileDebugKotlin`, Android unit tests, `assembleDebug`, or `lintDebug` passed.

The included GitHub Actions workflow or a fresh Google AI Studio Android import is the intended real Android build gate.

## Physical validation still required

On the Vivo Android 15 device:

1. Existing MC single-tap flow remains unchanged.
2. Existing two-long-press MC flow remains unchanged.
3. Single-screen free-response question -> silent notification answer.
4. Two-screen free-response question -> one Gemini request over Image 1 + Image 2, silent notification answer.
5. Notification does not heads-up, vibrate, or make sound.
6. Pull-down shade shows the concise answer and expands via BigText.
7. Second essay answer replaces the first notification.
8. Notification permission denied -> no crash and no popup fallback; history still records result.
9. Incomplete/cropped MC question -> no fabricated essay answer (`UNCLEAR`).
10. Existing history survives database migration from the currently installed build.

## Final artifact audit

Final source hashes after the essay implementation:

- `MainActivity.kt`: `3e7cf923bfbcb001640d30b075d36270f3dadccc7d04315682c91572e91e70f0`
- `ScreenCaptureService.kt`: `e79102106af71b743878916f0295b6957e806b333e67f916de706045dc75ded6`
- `ProviderGateway.kt`: `cfc2f1ab8c04c502e58e514e685b097609ee209c133583e6406983637216916b`
- `ResponseParser.kt`: `6ecb2794ab7455f4a2a483112ebe059e088c781677fdc21e2e09c01773a3c164`
- `HistoryEntity.kt`: `964ea71015c94e266e022f66ebb58f674e4972bee5a0238e867e75b739e0e4a7`
- `AppDatabase.kt`: `a66af6a6c3e49d20ce9e9c012b82e748592022e407f18761a01e7a2d0a92b793`
- `EssayAnswerNotificationManager.kt`: `b64db9e10c99946a33d911840a0a33996f08cd42ee1390769f197ce1244d5d0a`

The final Gemini structured-output budget is `maxOutputTokens = 512`; the prompt still requests a concise answer and the parser independently bounds stored/displayed free-response text to 500 characters. This avoids treating a merely truncated JSON response as a provider/key failure when an answer needs somewhat more room than the former MC-only payload.

Known-good capture helper hashes remain unchanged from the AI Studio export:

- `KeyStoreHelper.kt`: `d3557ff51ec8cf5feafd1113dfcb12753579ece44025c6d36f4caacafa3c0c4a`
- `FreshFrameReadinessGate.kt`: `d827df4b1e64fa353c1f9552ae07e0f05105330d937ea9d10746ce3417343ff5`
- `CaptureSurfaceResizeCoordinator.kt`: `efe91686d11bab34f0f40aa2e265cb5022c983c75ae7592c9346e2204475da86`
- `ImageUtils.kt`: `ba22b605690d6caeddb253cfc04ab9e0f07ba9c5a886b92509211ad20187e712`
- `FailoverPreferenceReader.kt`: `9416a76d4a74230edeb4e03fa89d5b5916df3e0c2d19aeae69b84f241975608a`
