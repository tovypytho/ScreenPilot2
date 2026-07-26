package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R

sealed interface EssayNotificationResult {
    data object Posted : EssayNotificationResult
    data object PermissionDenied : EssayNotificationResult
    data class Failed(val safeReason: String) : EssayNotificationResult
}

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
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            EssayNotificationResult.Posted
        } catch (_: SecurityException) {
            EssayNotificationResult.PermissionDenied
        } catch (e: Exception) {
            EssayNotificationResult.Failed(e::class.java.simpleName.ifBlank { "notification_error" })
        }
    }
}
