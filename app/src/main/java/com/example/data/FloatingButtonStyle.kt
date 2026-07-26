package com.example.data

data class FloatingButtonStyle(
    val sizePercent: Float,
    val opacityPercent: Float,
    val lockPosition: Boolean
)

data class FloatingButtonStyleSnapshot(
    val opacity: Float,
    val visualSizeDp: Int,
    val positionLocked: Boolean,
    val normalizedX: Float,
    val normalizedY: Float
)
