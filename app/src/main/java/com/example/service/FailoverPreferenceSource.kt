package com.example.service

import com.example.data.PreferencesRepository
import kotlinx.coroutines.flow.Flow

/**
 * Interface decoupling FailoverPreferenceReader from concrete PreferencesRepository for 100% JVM testability.
 */
interface FailoverPreferenceSource {
    val cooldownDurationSecFlow: Flow<Int>
    val historyLimitFlow: Flow<Int>
    val displayErrorSymbolFlow: Flow<Boolean>
    val sameKeyRetryEnabledFlow: Flow<Boolean>
    val skipCoolingDownFlow: Flow<Boolean>
    val skipAuthFailedFlow: Flow<Boolean>
    val skipPermissionDeniedFlow: Flow<Boolean>
    val keyStrategyFlow: Flow<String>
    val lastSuccessfulKeyIdFlow: Flow<String>
    val roundRobinLastKeyIndexFlow: Flow<Int>
    val maxKeyAttemptsFlow: Flow<Int>
    val dismissTimeoutSecFlow: Flow<Int>
}

/**
 * Production adapter wrapping PreferencesRepository as a FailoverPreferenceSource.
 */
class PreferencesRepositoryFailoverSource(
    private val repository: PreferencesRepository
) : FailoverPreferenceSource {
    override val cooldownDurationSecFlow: Flow<Int> get() = repository.cooldownDurationSecFlow
    override val historyLimitFlow: Flow<Int> get() = repository.historyLimitFlow
    override val displayErrorSymbolFlow: Flow<Boolean> get() = repository.displayErrorSymbolFlow
    override val sameKeyRetryEnabledFlow: Flow<Boolean> get() = repository.sameKeyRetryEnabledFlow
    override val skipCoolingDownFlow: Flow<Boolean> get() = repository.skipCoolingDownFlow
    override val skipAuthFailedFlow: Flow<Boolean> get() = repository.skipAuthFailedFlow
    override val skipPermissionDeniedFlow: Flow<Boolean> get() = repository.skipPermissionDeniedFlow
    override val keyStrategyFlow: Flow<String> get() = repository.keyStrategyFlow
    override val lastSuccessfulKeyIdFlow: Flow<String> get() = repository.lastSuccessfulKeyIdFlow
    override val roundRobinLastKeyIndexFlow: Flow<Int> get() = repository.roundRobinLastKeyIndexFlow
    override val maxKeyAttemptsFlow: Flow<Int> get() = repository.maxKeyAttemptsFlow
    override val dismissTimeoutSecFlow: Flow<Int> get() = repository.dismissTimeoutSecFlow
}
