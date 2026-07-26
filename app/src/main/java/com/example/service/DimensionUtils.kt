package com.example.service

/**
 * Production utility for dimension unit conversions.
 */
object DimensionUtils {
    /**
     * Converts density-independent pixels (dp) to screen pixels (px).
     */
    fun dpToPx(dp: Float, density: Float): Int {
        return (dp * density + 0.5f).toInt()
    }
}
