package com.example.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

sealed interface EssayNotificationResult {
    data object Posted : EssayNotificationResult
    data object PermissionDenied : EssayNotificationResult
    data class Failed(val safeReason: String) : EssayNotificationResult
}

/**
 * Posts the latest free-response answer to a dedicated silent notification.
 *
 * Android 15 hides ordinary notification contents while MediaProjection screen sharing is active.
 * ScreenPilot intentionally supplies a PUBLIC publicVersion containing the same answer so the
 * answer remains readable in the notification shade while the capture session stays active.
 */
object EssayAnswerNotificationManager {

    const val CHANNEL_ID = "screen_pilot_essay_answers_v1"
    const val NOTIFICATION_ID = 54322

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Jawaban Essay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Jawaban singkat untuk soal isian atau essay"
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
                setSound(null, null)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showAnswer(context: Context, answerText: String): EssayNotificationResult {
        val normalizedAnswer = answerText.trim()
        if (normalizedAnswer.isEmpty()) {
            return EssayNotificationResult.Failed("empty_answer")
        }

        return try {
            ensureChannel(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return EssayNotificationResult.PermissionDenied
            }

            val shortenedPreview = if (normalizedAnswer.length > 96) {
                normalizedAnswer.take(95).trimEnd() + "…"
            } else {
                normalizedAnswer
            }

            val contentIntent = createOpenAppPendingIntent(context)
            val publicNotification = buildPublicNotification(
                context = context,
                shortenedPreview = shortenedPreview,
                fullAnswer = normalizedAnswer,
                contentIntent = contentIntent
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("ScreenPilot")
                .setContentText(shortenedPreview)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(normalizedAnswer)
                )
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                // ScreenPilot is itself a MediaProjection host. Android 15 redacts notification
                // contents during screen sharing unless a public replacement is supplied.
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPublicVersion(publicNotification)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            EssayNotificationResult.Posted
        } catch (_: SecurityException) {
            EssayNotificationResult.PermissionDenied
        } catch (e: Exception) {
            EssayNotificationResult.Failed(e::class.java.simpleName.ifBlank { "notification_error" })
        }
    }

    private fun createOpenAppPendingIntent(context: Context): PendingIntent {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildPublicNotification(
        context: Context,
        shortenedPreview: String,
        fullAnswer: String,
        contentIntent: PendingIntent
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("ScreenPilot")
            .setContentText(shortenedPreview)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(fullAnswer)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
}
