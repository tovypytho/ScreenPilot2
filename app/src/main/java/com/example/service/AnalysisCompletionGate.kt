package com.example.service

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Guards analysis cleanup so that [finishAnalysisAndReturnToIdle] executes
 * exactly once per analysis request, regardless of how many code paths could
 * trigger completion.
 *
 * Usage:
 *  - Call [reset] at the start of each analysis (before entering the try block).
 *  - In the analysis finally block, call [tryComplete]; run cleanup only when it
 *    returns true.
 *
 * Every extracted abstraction must be used by ScreenCaptureService in production.
 */
class AnalysisCompletionGate {
    private val completed = AtomicBoolean(false)

    /**
     * Claims completion. Returns true the first time it is called per analysis;
     * returns false on all subsequent calls.
     */
    fun tryComplete(): Boolean = completed.compareAndSet(false, true)

    /**
     * Reset the gate for the next analysis. Must be called before starting a
     * new [runGeminiRequestChain] invocation.
     */
    fun reset() { completed.set(false) }

    /** True if completion has already been claimed. */
    fun isCompleted(): Boolean = completed.get()
}
