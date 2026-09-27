package com.example.clipboard

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EssayAnswerClipboardManagerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    @Test
    fun copiesEntireAnswerAndMarksItSensitive() {
        val answer = "Jawaban lengkap. ".repeat(80)
        assertEquals(EssayClipboardResult.Copied, EssayAnswerClipboardManager.copy(context, answer))
        assertEquals(answer, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        assertTrue(
            clipboard.primaryClip?.description?.extras
                ?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE) == true
        )
    }

    @Test
    @Config(sdk = [35])
    fun latestAnswerReplacesEarlierClipOnAndroid15() {
        EssayAnswerClipboardManager.copy(context, "Jawaban pertama")
        assertEquals(EssayClipboardResult.Copied, EssayAnswerClipboardManager.copy(context, "Jawaban kedua"))
        assertEquals("Jawaban kedua", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
    }

    @Test
    fun blankAnswerDoesNotReplaceClipboard() {
        EssayAnswerClipboardManager.copy(context, "Jawaban sebelumnya")
        assertTrue(EssayAnswerClipboardManager.copy(context, "   ") is EssayClipboardResult.Failed)
        assertEquals("Jawaban sebelumnya", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
    }
}
