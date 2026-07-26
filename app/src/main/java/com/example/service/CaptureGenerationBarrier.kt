package com.example.service

import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe barrier that gates fresh-frame capture to post-hide frames only.
 *
 * Avoids cross-clock timestamp comparisons (Image.timestamp vs
 * SystemClock.elapsedRealtimeNanos) that fail on some Vivo / MediaTek vendor
 * devices where the two clocks use different sources.
 *
 * Usage inside captureScreen():
 *  1. Call [snapshot] to record the current generation (before hiding overlays).
 *  2. Register the ImageReader listener.
 *  3. On Dispatchers.Main.immediate: hide overlays, then call [advance].
 *  4. In the listener: call [isPostHide] with the snapshot — accept the frame only
 *     when it returns true.
 *  5. Optionally attempt an immediate post-hide drain via [isPostHide] for static
 *     screens where no new frame will arrive.
 *
 * Every extracted abstraction must be used by ScreenCaptureService in production.
 */
class CaptureGenerationBarrier {
    private val generation = AtomicLong(0L)

    /**
     * Snapshot the current generation before hiding overlays.
     * Store the returned value and pass it to [isPostHide] from the listener.
     */
    fun snapshot(): Long = generation.get()

    /**
     * Advance the generation. Must be called on the Main thread immediately
     * after hiding ScreenPilot overlays, so that the ImageReader listener
     * begins accepting frames.
     */
    fun advance(): Long = generation.incrementAndGet()

    /**
     * Returns true if the barrier has been armed (i.e. overlays have been
     * hidden) and a frame arriving now is a post-hide frame.
     *
     * @param preHideSnapshot The value returned by [snapshot] before the hide.
     */
    fun isPostHide(preHideSnapshot: Long): Boolean = generation.get() != preHideSnapshot
}
