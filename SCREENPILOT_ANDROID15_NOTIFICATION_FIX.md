# ScreenPilot Android 15 Essay Notification Fix

## Scope

This checkpoint is based on `ScreenPilot_Essay_Ready_GitHub.zip` and changes only the Android notification presentation/test/CI configuration needed after physical Vivo/Funtouch OS 15 testing.

## Physical issue reproduced by the user

While ScreenPilot's MediaProjection session was active, Android 15/Funtouch OS showed the essay notification as only `Notifikasi baru`. As soon as ScreenPilot was deactivated and MediaProjection stopped, the same notification revealed the full essay answer.

Android 15 intentionally hides notification contents during screen sharing unless the notification supplies a public replacement notification.

## Production fix

`EssayAnswerNotificationManager.kt` now:

- keeps the dedicated silent `IMPORTANCE_LOW` essay channel;
- keeps one fixed notification ID (`54322`);
- builds a full-answer `NotificationCompat.BigTextStyle` public replacement;
- uses `NotificationCompat.VISIBILITY_PUBLIC`;
- attaches the replacement with `setPublicVersion(...)` so the answer can remain readable during the active MediaProjection session;
- adds a `PendingIntent` through `setContentIntent(...)` so tapping the notification opens ScreenPilot;
- keeps `setSilent(true)`, `setOnlyAlertOnce(true)`, and `setAutoCancel(true)`;
- still refuses blank answers and still respects `POST_NOTIFICATIONS` permission.

## Protected logic left byte-for-byte unchanged

- `ScreenCaptureService.kt`
- `ProviderGateway.kt`
- `ResponseParser.kt`
- `FreshFrameReadinessGate.kt`
- `CaptureSurfaceResizeCoordinator.kt`
- `KeyStoreHelper.kt`
- `ImageUtils.kt`

Therefore the known-good capture pipeline, two-long-press staged capture, Gemini routing, failover, and encrypted API-key logic were not modified by this fix.

## Tests

`EssayAnswerNotificationManagerTest` now verifies:

- low-importance/silent channel;
- main notification uses `VISIBILITY_PUBLIC`;
- BigText contains the complete answer;
- a non-null tap `contentIntent` exists;
- a non-null `publicVersion` exists;
- the public version is also public and contains the complete answer;
- latest essay answer replaces the previous notification;
- blank answers do not post.

The total test count is unchanged because an existing notification test was strengthened rather than adding a redundant new test.

## CI portability preserved

The ZIP also includes the already-proven GitHub CI corrections:

- JDK 21 in `.github/workflows/android-build.yml`;
- Robolectric Java module `--add-opens` arguments in `app/build.gradle.kts`.

## Required physical verification

On Vivo/Funtouch OS 15:

1. Activate ScreenPilot so MediaProjection remains active.
2. Analyze one free-response/essay question.
3. Pull down the notification shade **without stopping ScreenPilot**.
4. Confirm the actual answer is visible instead of `Notifikasi baru`.
5. Tap the essay notification and confirm ScreenPilot opens.
6. Confirm no sound, vibration, or heads-up banner occurs.
7. Analyze a second essay question and confirm the same notification is updated instead of stacking.

Note: `VISIBILITY_PUBLIC` intentionally allows the full answer to be used as the notification's public representation. This is required for the requested Android 15 screen-sharing behavior and may also make the answer visible on the lock screen depending on device settings.
