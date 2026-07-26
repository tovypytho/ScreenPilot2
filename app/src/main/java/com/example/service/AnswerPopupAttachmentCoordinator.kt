package com.example.service

import java.util.concurrent.atomic.AtomicLong

sealed interface PopupAttachmentResult {
    data object Attached : PopupAttachmentResult
    data object Invalidated : PopupAttachmentResult
    data class Failed(val safeReason: String) : PopupAttachmentResult
}

/**
 * Production coordinator for popup attachment tokens, eliminating attachment race conditions.
 */
class AnswerPopupAttachmentCoordinator {
    private val generation = AtomicLong(0L)

    /** Generate next token for upcoming popup request. */
    fun nextToken(): Long = generation.incrementAndGet()

    /** Invalidate all in-flight popup requests. */
    fun invalidate(): Long = generation.incrementAndGet()

    /** Verify token is current. */
    fun isValid(token: Long): Boolean = generation.get() == token

    /** Current generation. */
    fun currentToken(): Long = generation.get()
}
