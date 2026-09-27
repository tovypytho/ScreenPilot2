package com.example.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class EssayAnswerNotificationManagerTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        notificationManager.deleteNotificationChannel(EssayAnswerNotificationManager.CHANNEL_ID)
    }

    @Test
    fun channelIsLowImportanceAndSilent() {
        EssayAnswerNotificationManager.ensureChannel(context)
        val channel = notificationManager.getNotificationChannel(EssayAnswerNotificationManager.CHANNEL_ID)
        assertEquals(NotificationManager.IMPORTANCE_LOW, channel.importance)
        assertNull(channel.sound)
        assertTrue(!channel.shouldVibrate())
        assertTrue(!channel.shouldShowLights())
    }

    @Test
    fun essayAnswerHidesLockScreenTextAndKeepsTapIntent() {
        val answer = "Fotosintesis mengubah energi cahaya menjadi energi kimia yang disimpan dalam bentuk glukosa."
        val result = EssayAnswerNotificationManager.showAnswer(context, answer)
        assertTrue(result is EssayNotificationResult.Posted)

        val active = notificationManager.activeNotifications
        assertEquals(1, active.size)
        assertEquals(EssayAnswerNotificationManager.NOTIFICATION_ID, active[0].id)

        val notification = active[0].notification
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
        assertEquals(answer, notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString())
        assertNotNull(notification.contentIntent)

        val publicVersion = notification.publicVersion
        assertNotNull(publicVersion)
        assertEquals(Notification.VISIBILITY_PUBLIC, publicVersion.visibility)
        assertTrue(publicVersion.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.contains(answer) != true)
        assertNotNull(publicVersion.contentIntent)
    }

    @Test
    fun latestEssayAnswerReplacesPreviousNotification() {
        EssayAnswerNotificationManager.showAnswer(context, "Jawaban pertama")
        EssayAnswerNotificationManager.showAnswer(context, "Jawaban kedua")

        val active = notificationManager.activeNotifications
        assertEquals(1, active.size)
        assertEquals(EssayAnswerNotificationManager.NOTIFICATION_ID, active[0].id)
        assertEquals(
            "Jawaban kedua",
            active[0].notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        )
    }

    @Test
    fun blankAnswerFailsWithoutPostingNotification() {
        val result = EssayAnswerNotificationManager.showAnswer(context, "   ")
        assertTrue(result is EssayNotificationResult.Failed)
        assertEquals(0, notificationManager.activeNotifications.size)
    }
}
