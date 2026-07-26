package com.example.data

/**
 * Centralized single source of truth for all ScreenPilot preference default values.
 * Used by both PreferencesRepository and FailoverPreferenceReader to prevent default drift.
 */
object ScreenPilotPreferenceDefaults {
    const val DISMISS_TIMEOUT_SEC = 5L
    const val DISPLAY_ERROR_SYMBOL = false
    const val KEY_STRATEGY = "Sticky Success with Sequential Failover"
    const val ROUND_ROBIN_LAST_KEY_INDEX = 0
    const val MAX_KEY_ATTEMPTS = 10
    const val SAME_KEY_RETRY_ENABLED = true
    const val COOLDOWN_DURATION_SEC = 60
    const val SKIP_AUTH_FAILED = true
    const val SKIP_PERMISSION_DENIED = true
    const val SKIP_COOLING_DOWN = true
    const val HISTORY_LIMIT = 30
}
