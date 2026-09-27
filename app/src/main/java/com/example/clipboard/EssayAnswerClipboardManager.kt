package com.example.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle

sealed interface EssayClipboardResult {
    data object Copied : EssayClipboardResult
    data class Failed(val safeReason: String) : EssayClipboardResult
}

object EssayAnswerClipboardManager {
    fun copy(context: Context, answerText: String): EssayClipboardResult {
        if (answerText.isBlank()) return EssayClipboardResult.Failed("empty_answer")

        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return EssayClipboardResult.Failed("clipboard_unavailable")
            val clip = ClipData.newPlainText("ScreenPilot Essay Answer", answerText)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                clip.description.extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            }
            clipboard.setPrimaryClip(clip)
            EssayClipboardResult.Copied
        } catch (e: Exception) {
            EssayClipboardResult.Failed(e::class.java.simpleName.ifBlank { "clipboard_error" })
        }
    }
}
