package com.example.service

data class CaptureDimensions(
    val width: Int,
    val height: Int,
    val densityDpi: Int
) {
    init {
        require(width > 0) { "Width must be greater than zero" }
        require(height > 0) { "Height must be greater than zero" }
        require(densityDpi > 0) { "Density DPI must be greater than zero" }
    }
}

object CaptureDimensionHelper {
    
    fun isValid(width: Int, height: Int, densityDpi: Int): Boolean {
        return width > 0 && height > 0 && densityDpi > 0
    }

    fun isResizeRequired(current: CaptureDimensions?, newWidth: Int, newHeight: Int, newDensity: Int): Boolean {
        if (current == null) return true
        return current.width != newWidth || current.height != newHeight || current.densityDpi != newDensity
    }

    fun clampCoordinates(
        x: Int,
        y: Int,
        screenWidth: Int,
        screenHeight: Int,
        elementWidth: Int,
        elementHeight: Int
    ): Pair<Int, Int> {
        val maxX = (screenWidth - elementWidth).coerceAtLeast(0)
        val maxY = (screenHeight - elementHeight).coerceAtLeast(0)
        return Pair(x.coerceIn(0, maxX), y.coerceIn(0, maxY))
    }

    fun normalizedToPixel(
        normX: Float,
        normY: Float,
        screenWidth: Int,
        screenHeight: Int,
        elementWidth: Int,
        elementHeight: Int
    ): Pair<Int, Int> {
        val rawX = (normX * screenWidth).toInt()
        val rawY = (normY * screenHeight).toInt()
        return clampCoordinates(rawX, rawY, screenWidth, screenHeight, elementWidth, elementHeight)
    }

    fun scaleToMax(width: Int, height: Int, maxDim: Int): Pair<Int, Int> {
        if (width <= 0 || height <= 0 || maxDim <= 0) return Pair(width, height)
        if (width <= maxDim && height <= maxDim) return Pair(width, height)
        val ratio = width.toFloat() / height.toFloat()
        return if (width > height) {
            Pair(maxDim, (maxDim / ratio).toInt().coerceAtLeast(1))
        } else {
            Pair((maxDim * ratio).toInt().coerceAtLeast(1), maxDim)
        }
    }
}
