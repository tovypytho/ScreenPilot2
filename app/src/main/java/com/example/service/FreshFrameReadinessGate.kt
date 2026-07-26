package com.example.service

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Production readiness gate controlling frame acceptance.
 * Replaces the previous generation-counter barrier with a true post-overlay-hide gate.
 */
class FreshFrameReadinessGate {
    private val armed = AtomicBoolean(false)
    private val generation = AtomicLong(0L)

    /** Reset before starting capture sequence. */
    fun reset() {
        armed.set(false)
        generation.incrementAndGet()
    }

    /** Arm gate post-overlay-hide and post-frame-boundary. */
    fun arm() {
        armed.set(true)
    }

    /** Returns true if the capture sequence has been armed post-hide. */
    fun isArmed(): Boolean = armed.get()

    /** Current generation token. */
    fun generation(): Long = generation.get()
}
