package com.example.service

import com.example.data.ScreenPilotPreferenceDefaults
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Production safe reader for DataStore preferences in the hot failover loop.
 * Depends on FailoverPreferenceSource interface to allow mock-free Kotlin test doubles.
 */
class FailoverPreferenceReader(
    private val source: FailoverPreferenceSource,
    private val diagnostic: (String) -> Unit = {}
) {

    private fun reportFallback(name: String, fallback: Any?, throwable: Throwable) {
        try {
            diagnostic("DataStore read failed for $name; using fallback=$fallback (${throwable::class.java.simpleName})")
        } catch (_: Throwable) {
            // Diagnostics must never interfere with failover behavior.
        }
    }

    suspend fun safeCooldownDurationSec(defaultSec: Int = ScreenPilotPreferenceDefaults.COOLDOWN_DURATION_SEC): Int {
        return try {
            source.cooldownDurationSecFlow.first()
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            reportFallback("cooldownDurationSec", defaultSec, t)
            defaultSec
        }
    }

    suspend fun safeSameKeyRetryEnabled(defaultEnabled: Boolean = ScreenPilotPreferenceDefaults.SAME_KEY_RETRY_ENABLED): Boolean {
        return try {
            source.sameKeyRetryEnabledFlow.first()
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            reportFallback("sameKeyRetryEnabled", defaultEnabled, t)
            defaultEnabled
        }
    }

    suspend fun safeHistoryLimit(defaultLimit: Int = ScreenPilotPreferenceDefaults.HISTORY_LIMIT): Int {
        return try {
            source.historyLimitFlow.first()
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            reportFallback("historyLimit", defaultLimit, t)
            defaultLimit
        }
    }

    suspend fun safeDisplayErrorSymbol(defaultShow: Boolean = ScreenPilotPreferenceDefaults.DISPLAY_ERROR_SYMBOL): Boolean {
        return try {
            source.displayErrorSymbolFlow.first()
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            reportFallback("displayErrorSymbol", defaultShow, t)
            defaultShow
        }
    }

    suspend fun safeDismissTimeoutSec(defaultSec: Long = ScreenPilotPreferenceDefaults.DISMISS_TIMEOUT_SEC): Long {
        return try {
            source.dismissTimeoutSecFlow.first().toLong()
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            reportFallback("dismissTimeoutSec", defaultSec, t)
            defaultSec
        }
    }

    suspend fun safeSkipCoolingDown(defaultSkip: Boolean = ScreenPilotPreferenceDefaults.SKIP_COOLING_DOWN): Boolean {
        return try {
            source.skipCoolingDownFlow.first()
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            reportFallback("skipCoolingDown", defaultSkip, t)
            defaultSkip
        }
    }

    suspend fun safeSkipAuthFailed(defaultSkip: Boolean = ScreenPilotPreferenceDefaults.SKIP_AUTH_FAILED): Boolean {
        return try {
            source.skipAuthFailedFlow.first()
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            reportFallback("skipAuthFailed", defaultSkip, t)
            defaultSkip
        }
    }

    suspend fun safeSkipPermissionDenied(defaultSkip: Boolean = ScreenPilotPreferenceDefaults.SKIP_PERMISSION_DENIED): Boolean {
        return try {
            source.skipPermissionDeniedFlow.first()
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            reportFallback("skipPermissionDenied", defaultSkip, t)
            defaultSkip
        }
    }

    suspend fun safeKeyStrategy(defaultStrategy: String = ScreenPilotPreferenceDefaults.KEY_STRATEGY): String {
        return try {
            source.keyStrategyFlow.first()
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            reportFallback("keyStrategy", defaultStrategy, t)
            defaultStrategy
        }
    }

    suspend fun safeLastSuccessfulKeyId(): String? {
        return try {
            source.lastSuccessfulKeyIdFlow.first()
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            reportFallback("lastSuccessfulKeyId", null, t)
            null
        }
    }

    suspend fun safeRoundRobinLastKeyIndex(): Int {
        return try {
            source.roundRobinLastKeyIndexFlow.first()
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            reportFallback("roundRobinLastKeyIndex", ScreenPilotPreferenceDefaults.ROUND_ROBIN_LAST_KEY_INDEX, t)
            ScreenPilotPreferenceDefaults.ROUND_ROBIN_LAST_KEY_INDEX
        }
    }

    suspend fun safeMaxKeyAttempts(defaultMax: Int = ScreenPilotPreferenceDefaults.MAX_KEY_ATTEMPTS): Int {
        return try {
            source.maxKeyAttemptsFlow.first()
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            reportFallback("maxKeyAttempts", defaultMax, t)
            defaultMax
        }
    }
}
