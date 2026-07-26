package com.example.data

enum class PopupFontWeight {
    NORMAL, MEDIUM, SEMI_BOLD, BOLD;
    companion object {
        fun fromString(value: String): PopupFontWeight {
            return when (value) {
                "Normal" -> NORMAL
                "Medium" -> MEDIUM
                "Semi Bold", "SemiBold" -> SEMI_BOLD
                "Bold" -> BOLD
                else -> SEMI_BOLD
            }
        }
    }
}

enum class PopupStyle {
    COMPACT_ROUNDED, CIRCLE, PILL, TEXT_ONLY;
    companion object {
        fun fromString(value: String): PopupStyle {
            return when (value) {
                "Compact Rounded" -> COMPACT_ROUNDED
                "Circle" -> CIRCLE
                "Pill" -> PILL
                "Text Only" -> TEXT_ONLY
                else -> COMPACT_ROUNDED
            }
        }
    }
}

enum class PopupBackgroundTheme {
    DARK, LIGHT, AUTO_CONTRAST;
    companion object {
        fun fromString(value: String): PopupBackgroundTheme {
            return when (value) {
                "Dark" -> DARK
                "Light" -> LIGHT
                "Auto Contrast" -> AUTO_CONTRAST
                else -> DARK
            }
        }
    }
}

enum class PopupTextColorMode {
    WHITE, BLACK, AUTO;
    companion object {
        fun fromString(value: String): PopupTextColorMode {
            return when (value) {
                "White" -> WHITE
                "Black" -> BLACK
                "Auto" -> AUTO
                else -> WHITE
            }
        }
    }
}

data class AnswerPopupStyle(
    val popupScale: Float,
    val fontSizeSp: Float,
    val fontWeight: PopupFontWeight,
    val backgroundOpacity: Float,
    val textOpacity: Float,
    val cornerRadiusDp: Float,
    val horizontalPaddingDp: Float,
    val verticalPaddingDp: Float,
    val bottomOffsetDp: Float,
    val popupStyle: PopupStyle,
    val backgroundTheme: PopupBackgroundTheme,
    val textColorMode: PopupTextColorMode,
    val showConfidence: Boolean
)
