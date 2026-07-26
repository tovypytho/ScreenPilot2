package com.example.service

import com.example.data.GeminiKeyHealth
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

sealed class FailoverAction {
    object StopRotation : FailoverAction()
    data class ContinueToNextKey(
        val healthStatus: String, // GeminiKeyHealth name
        val failureType: String,
        val cooldownMs: Long
    ) : FailoverAction()
}

object FailoverDecision {
    fun evaluate(
        throwable: Throwable,
        cooldownSec: Long
    ): FailoverAction {
        val msg = throwable.message?.lowercase() ?: ""
        
        if (throwable is LocalPreparationException) {
            return FailoverAction.StopRotation
        }
        
        if (throwable is IllegalArgumentException) {
            if (msg.contains("url") || msg.contains("baseurl") || msg.contains("endpoint") || msg.contains("model") || msg.contains("malformed")) {
                return FailoverAction.StopRotation
            }
        }

        return when (throwable) {
            is ApiException -> {
                when (throwable.code) {
                    400 -> FailoverAction.StopRotation
                    404 -> FailoverAction.StopRotation
                    401 -> FailoverAction.ContinueToNextKey(
                        healthStatus = GeminiKeyHealth.AUTH_FAILED.name,
                        failureType = "401",
                        cooldownMs = 0L
                    )
                    403 -> FailoverAction.ContinueToNextKey(
                        healthStatus = GeminiKeyHealth.PERMISSION_DENIED.name,
                        failureType = "403",
                        cooldownMs = 0L
                    )
                    429 -> FailoverAction.ContinueToNextKey(
                        healthStatus = GeminiKeyHealth.COOLDOWN.name,
                        failureType = "429",
                        cooldownMs = cooldownSec * 1000L
                    )
                    else -> FailoverAction.ContinueToNextKey(
                        healthStatus = GeminiKeyHealth.TEMPORARY_FAILURE.name,
                        failureType = throwable.code.toString(),
                        cooldownMs = 0L
                    )
                }
            }
            is SocketTimeoutException -> {
                FailoverAction.ContinueToNextKey(
                    healthStatus = GeminiKeyHealth.TEMPORARY_FAILURE.name,
                    failureType = "Timeout",
                    cooldownMs = 0L
                )
            }
            is UnknownHostException -> {
                FailoverAction.ContinueToNextKey(
                    healthStatus = GeminiKeyHealth.TEMPORARY_FAILURE.name,
                    failureType = "DNS Error",
                    cooldownMs = 0L
                )
            }
            is SSLException, is IOException -> {
                FailoverAction.ContinueToNextKey(
                    healthStatus = GeminiKeyHealth.TEMPORARY_FAILURE.name,
                    failureType = "Network Error",
                    cooldownMs = 0L
                )
            }
            is IllegalArgumentException -> {
                FailoverAction.ContinueToNextKey(
                    healthStatus = GeminiKeyHealth.TEMPORARY_FAILURE.name,
                    failureType = "Response Failure",
                    cooldownMs = 0L
                )
            }
            else -> {
                FailoverAction.ContinueToNextKey(
                    healthStatus = GeminiKeyHealth.TEMPORARY_FAILURE.name,
                    failureType = "Error",
                    cooldownMs = 0L
                )
            }
        }
    }
}
