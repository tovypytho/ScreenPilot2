package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "screen_pilot_prefs")

class PreferencesRepository(private val context: Context) {

    companion object {
        val SAVE_SCREENSHOTS = booleanPreferencesKey("save_screenshots")
        val BUTTON_OPACITY = floatPreferencesKey("button_opacity")
        val BUTTON_SIZE_DP = intPreferencesKey("button_size_dp")
        val LOCK_POSITION = booleanPreferencesKey("lock_position")
        val DISMISS_TIMEOUT_SEC = intPreferencesKey("dismiss_timeout_sec")
        val DISPLAY_ERROR_SYMBOL = booleanPreferencesKey("display_error_symbol")
        val BUTTON_POS_X = floatPreferencesKey("button_pos_x")
        val BUTTON_POS_Y = floatPreferencesKey("button_pos_y")

        val POPUP_SIZE_PERCENT = floatPreferencesKey("popup_size_percent")
        val POPUP_FONT_SIZE_SP = intPreferencesKey("popup_font_size_sp")
        val POPUP_FONT_WEIGHT = stringPreferencesKey("popup_font_weight")
        val POPUP_BG_OPACITY = floatPreferencesKey("popup_bg_opacity")
        val POPUP_TEXT_OPACITY = floatPreferencesKey("popup_text_opacity")
        val POPUP_CORNER_RADIUS_DP = intPreferencesKey("popup_corner_radius_dp")
        val POPUP_PADDING_HORIZONTAL_DP = intPreferencesKey("popup_padding_horizontal_dp")
        val POPUP_PADDING_VERTICAL_DP = intPreferencesKey("popup_padding_vertical_dp")
        val POPUP_BOTTOM_OFFSET_DP = intPreferencesKey("popup_bottom_offset_dp")
        val POPUP_STYLE = stringPreferencesKey("popup_style")
        val POPUP_BG_THEME = stringPreferencesKey("popup_bg_theme")
        val POPUP_TEXT_COLOR = stringPreferencesKey("popup_text_color")
        val POPUP_SHOW_CONFIDENCE = booleanPreferencesKey("popup_show_confidence")

        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val FALLBACK_PRIMARY_PROVIDER = stringPreferencesKey("fallback_primary_provider")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val GEMINI_BASE_URL = stringPreferencesKey("gemini_base_url")

        val GEMINI_KEY_SLOTS_METADATA = stringPreferencesKey("gemini_key_slots_metadata")
        val KEY_STRATEGY = stringPreferencesKey("key_strategy")
        val LAST_SUCCESSFUL_KEY_ID = stringPreferencesKey("last_successful_key_id")
        val ROUND_ROBIN_LAST_KEY_INDEX = intPreferencesKey("round_robin_last_key_index")
        val ROUND_ROBIN_LAST_SLOT_ID = stringPreferencesKey("round_robin_last_slot_id")
        val KEY_STRATEGY_MIGRATED = booleanPreferencesKey("key_strategy_migrated")
        val ESSAY_NOTIFICATION_ENABLED = booleanPreferencesKey("essay_notification_enabled")
        val MAX_KEY_ATTEMPTS = intPreferencesKey("max_key_attempts")
        val SAME_KEY_RETRY_ENABLED = booleanPreferencesKey("same_key_retry_enabled")
        val COOLDOWN_DURATION_SEC = intPreferencesKey("cooldown_duration_sec")
        val SKIP_AUTH_FAILED = booleanPreferencesKey("skip_auth_failed")
        val SKIP_PERMISSION_DENIED = booleanPreferencesKey("skip_permission_denied")
        val SKIP_COOLING_DOWN = booleanPreferencesKey("skip_cooling_down")
        val MIGRATED_TO_KEY_POOL = booleanPreferencesKey("migrated_to_key_pool")

        // New Capture & History preferences
        val SCREENSHOT_MAX_DIMENSION = intPreferencesKey("screenshot_max_dimension")
        val API_JPEG_QUALITY = intPreferencesKey("api_jpeg_quality")
        val GALLERY_JPEG_QUALITY = intPreferencesKey("gallery_jpeg_quality")
        val HISTORY_LIMIT = intPreferencesKey("history_limit")

        val TWO_IMAGE_CAPTURE_ENABLED = booleanPreferencesKey("two_image_capture_enabled")
        val LONG_PRESS_THRESHOLD_MS = intPreferencesKey("long_press_threshold_ms")
        val TWO_IMAGE_TIMEOUT_SEC = intPreferencesKey("two_image_timeout_sec")
        val PENDING_STATUS_BUBBLE_ENABLED = booleanPreferencesKey("pending_status_bubble_enabled")
        val PENDING_STATUS_DURATION_MS = intPreferencesKey("pending_status_duration_ms")
        val STAGED_STATUS_BACKGROUND = stringPreferencesKey("staged_status_background")

        // Session Journal keys
        val SESSION_ID = stringPreferencesKey("session_id")
        val SESSION_ACTIVATION_TIME = longPreferencesKey("session_activation_time")
        val SESSION_SERVICE_STARTED = booleanPreferencesKey("session_service_started")
        val SESSION_FOREGROUND_PROMOTED = booleanPreferencesKey("session_foreground_promoted")
        val SESSION_PROJECTION_INITIALIZED = booleanPreferencesKey("session_projection_initialized")
        val SESSION_FLOATING_CREATED = booleanPreferencesKey("session_floating_created")
        val SESSION_LAST_HEALTHY_TIME = longPreferencesKey("session_last_healthy_time")
        val SESSION_LAST_ACTION_STAGE = stringPreferencesKey("session_last_action_stage")
        val SESSION_GRACEFUL_SHUTDOWN = booleanPreferencesKey("session_graceful_shutdown")
        val SESSION_SHUTDOWN_REASON = stringPreferencesKey("session_shutdown_reason")
    }

    val sessionIdFlow: Flow<String> = context.dataStore.data.map { prefs -> prefs[SESSION_ID] ?: "" }
    val sessionActivationTimeFlow: Flow<Long> = context.dataStore.data.map { prefs -> prefs[SESSION_ACTIVATION_TIME] ?: 0L }
    val sessionServiceStartedFlow: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[SESSION_SERVICE_STARTED] ?: false }
    val sessionForegroundPromotedFlow: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[SESSION_FOREGROUND_PROMOTED] ?: false }
    val sessionProjectionInitializedFlow: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[SESSION_PROJECTION_INITIALIZED] ?: false }
    val sessionFloatingCreatedFlow: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[SESSION_FLOATING_CREATED] ?: false }
    val sessionLastHealthyTimeFlow: Flow<Long> = context.dataStore.data.map { prefs -> prefs[SESSION_LAST_HEALTHY_TIME] ?: 0L }
    val sessionLastActionStageFlow: Flow<String> = context.dataStore.data.map { prefs -> prefs[SESSION_LAST_ACTION_STAGE] ?: "" }
    val sessionGracefulShutdownFlow: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[SESSION_GRACEFUL_SHUTDOWN] ?: true }
    val sessionShutdownReasonFlow: Flow<String> = context.dataStore.data.map { prefs -> prefs[SESSION_SHUTDOWN_REASON] ?: "" }

    suspend fun updateSessionId(value: String) {
        context.dataStore.edit { prefs -> prefs[SESSION_ID] = value }
    }
    suspend fun updateSessionShutdown(graceful: Boolean, reason: String) {
        context.dataStore.edit { prefs ->
            prefs[SESSION_GRACEFUL_SHUTDOWN] = graceful
            prefs[SESSION_SHUTDOWN_REASON] = reason
            prefs[SESSION_SERVICE_STARTED] = false
        }
    }
    suspend fun updateSessionActivationTime(value: Long) {
        context.dataStore.edit { prefs -> prefs[SESSION_ACTIVATION_TIME] = value }
    }
    suspend fun updateSessionServiceStarted(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[SESSION_SERVICE_STARTED] = value }
    }
    suspend fun updateSessionForegroundPromoted(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[SESSION_FOREGROUND_PROMOTED] = value }
    }
    suspend fun updateSessionProjectionInitialized(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[SESSION_PROJECTION_INITIALIZED] = value }
    }
    suspend fun updateSessionFloatingCreated(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[SESSION_FLOATING_CREATED] = value }
    }
    suspend fun updateSessionLastHealthyTime(value: Long) {
        context.dataStore.edit { prefs -> prefs[SESSION_LAST_HEALTHY_TIME] = value }
    }
    suspend fun updateSessionLastActionStage(value: String) {
        context.dataStore.edit { prefs -> prefs[SESSION_LAST_ACTION_STAGE] = value }
    }
    suspend fun updateSessionGracefulShutdown(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[SESSION_GRACEFUL_SHUTDOWN] = value }
    }
    suspend fun updateSessionShutdownReason(value: String) {
        context.dataStore.edit { prefs -> prefs[SESSION_SHUTDOWN_REASON] = value }
    }

    val saveScreenshotsFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SAVE_SCREENSHOTS] ?: true
    }

    val buttonOpacityFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[BUTTON_OPACITY] ?: 0.10f
    }

    val buttonSizeDpFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[BUTTON_SIZE_DP] ?: 36
    }

    val lockPositionFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[LOCK_POSITION] ?: true
    }

    val dismissTimeoutSecFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[DISMISS_TIMEOUT_SEC] ?: ScreenPilotPreferenceDefaults.DISMISS_TIMEOUT_SEC.toInt()
    }

    val displayErrorSymbolFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DISPLAY_ERROR_SYMBOL] ?: ScreenPilotPreferenceDefaults.DISPLAY_ERROR_SYMBOL
    }

    val buttonPosXFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[BUTTON_POS_X] ?: 0.85f
    }

    val buttonPosYFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[BUTTON_POS_Y] ?: 0.5f
    }

    val popupSizePercentFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[POPUP_SIZE_PERCENT] ?: 0.55f
    }

    val popupFontSizeSpFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[POPUP_FONT_SIZE_SP] ?: 24
    }

    val popupFontWeightFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[POPUP_FONT_WEIGHT] ?: "Medium"
    }

    val popupBgOpacityFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[POPUP_BG_OPACITY] ?: 0.60f
    }

    val popupTextOpacityFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[POPUP_TEXT_OPACITY] ?: 1.0f
    }

    val popupCornerRadiusDpFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[POPUP_CORNER_RADIUS_DP] ?: 14
    }

    val popupPaddingHorizontalDpFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[POPUP_PADDING_HORIZONTAL_DP] ?: 14
    }

    val popupPaddingVerticalDpFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[POPUP_PADDING_VERTICAL_DP] ?: 6
    }

    val popupBottomOffsetDpFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[POPUP_BOTTOM_OFFSET_DP] ?: 110
    }

    val popupStyleFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[POPUP_STYLE] ?: "Compact Rounded"
    }

    val popupBgThemeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[POPUP_BG_THEME] ?: "Dark"
    }

    val popupTextColorFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[POPUP_TEXT_COLOR] ?: "White"
    }

    val popupShowConfidenceFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[POPUP_SHOW_CONFIDENCE] ?: false
    }

    val popupStyleSnapshotFlow: Flow<AnswerPopupStyle> = kotlinx.coroutines.flow.combine(
        popupSizePercentFlow,
        popupFontSizeSpFlow,
        popupFontWeightFlow,
        popupBgOpacityFlow,
        popupTextOpacityFlow,
        popupCornerRadiusDpFlow,
        popupPaddingHorizontalDpFlow,
        popupPaddingVerticalDpFlow,
        popupBottomOffsetDpFlow,
        popupStyleFlow,
        popupBgThemeFlow,
        popupTextColorFlow,
        popupShowConfidenceFlow
    ) { array ->
        AnswerPopupStyle(
            popupScale = array[0] as Float,
            fontSizeSp = (array[1] as Int).toFloat(),
            fontWeight = PopupFontWeight.fromString(array[2] as String),
            backgroundOpacity = array[3] as Float,
            textOpacity = array[4] as Float,
            cornerRadiusDp = (array[5] as Int).toFloat(),
            horizontalPaddingDp = (array[6] as Int).toFloat(),
            verticalPaddingDp = (array[7] as Int).toFloat(),
            bottomOffsetDp = (array[8] as Int).toFloat(),
            popupStyle = PopupStyle.fromString(array[9] as String),
            backgroundTheme = PopupBackgroundTheme.fromString(array[10] as String),
            textColorMode = PopupTextColorMode.fromString(array[11] as String),
            showConfidence = array[12] as Boolean
        )
    }

    val aiProviderFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[AI_PROVIDER] ?: "Google Gemini Direct"
    }

    val fallbackPrimaryProviderFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[FALLBACK_PRIMARY_PROVIDER] ?: "Google Gemini"
    }

    val geminiModelFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[GEMINI_MODEL] ?: "gemini-3.1-flash-lite"
    }

    val geminiBaseUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[GEMINI_BASE_URL] ?: "https://generativelanguage.googleapis.com/v1beta"
    }

    val geminiKeySlotsMetadataFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[GEMINI_KEY_SLOTS_METADATA] ?: ""
    }

    val keyStrategyFlow: Flow<String> = context.dataStore.data.map { prefs ->
        if (prefs[KEY_STRATEGY_MIGRATED] != true) {
            ScreenPilotPreferenceDefaults.KEY_STRATEGY
        } else prefs[KEY_STRATEGY] ?: ScreenPilotPreferenceDefaults.KEY_STRATEGY
    }
    val essayNotificationEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ESSAY_NOTIFICATION_ENABLED] ?: false
    }

    suspend fun setEssayNotificationEnabled(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[ESSAY_NOTIFICATION_ENABLED] = value }
    }

    suspend fun migrateKeyStrategyIfNeeded() {
        context.dataStore.edit { prefs ->
            if (prefs[KEY_STRATEGY_MIGRATED] != true) {
                prefs[KEY_STRATEGY] = ScreenPilotPreferenceDefaults.KEY_STRATEGY
                prefs[KEY_STRATEGY_MIGRATED] = true
            }
        }
    }

    /** Reserves the next starting slot atomically before any network request begins. */
    suspend fun reserveKeyOrder(eligible: List<GeminiKeySlot>): List<GeminiKeySlot> {
        if (eligible.isEmpty()) return emptyList()
        val sorted = eligible.sortedWith(compareBy<GeminiKeySlot> { it.priority }.thenBy { it.id.toIntOrNull() ?: Int.MAX_VALUE })
        var ordered = sorted
        context.dataStore.edit { prefs ->
            if (prefs[KEY_STRATEGY_MIGRATED] != true) {
                prefs[KEY_STRATEGY] = ScreenPilotPreferenceDefaults.KEY_STRATEGY
                prefs[KEY_STRATEGY_MIGRATED] = true
            }
            when (prefs[KEY_STRATEGY] ?: ScreenPilotPreferenceDefaults.KEY_STRATEGY) {
                "Round Robin" -> {
                    val lastId = prefs[ROUND_ROBIN_LAST_SLOT_ID]
                    val lastPosition = sorted.indexOfFirst { it.id == lastId }
                    val start = if (lastPosition < 0) 0 else (lastPosition + 1) % sorted.size
                    ordered = sorted.drop(start) + sorted.take(start)
                    prefs[ROUND_ROBIN_LAST_SLOT_ID] = ordered.first().id
                }
                "Sticky Success with Sequential Failover" -> {
                    val lastId = prefs[LAST_SUCCESSFUL_KEY_ID]
                    ordered = sorted.firstOrNull { it.id == lastId }?.let { listOf(it) + sorted.filterNot { slot -> slot.id == it.id } } ?: sorted
                }
            }
        }
        return ordered
    }

    suspend fun updateGeminiKeySlot(id: String, update: (GeminiKeySlot) -> GeminiKeySlot) {
        context.dataStore.edit { prefs ->
            val slots = GeminiKeySlotSerializer.deserialize(prefs[GEMINI_KEY_SLOTS_METADATA] ?: "")
            prefs[GEMINI_KEY_SLOTS_METADATA] = GeminiKeySlotSerializer.serialize(
                slots.map { if (it.id == id) update(it) else it }
            )
        }
    }

    val lastSuccessfulKeyIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LAST_SUCCESSFUL_KEY_ID] ?: ""
    }

    val roundRobinLastKeyIndexFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[ROUND_ROBIN_LAST_KEY_INDEX] ?: ScreenPilotPreferenceDefaults.ROUND_ROBIN_LAST_KEY_INDEX
    }

    val maxKeyAttemptsFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[MAX_KEY_ATTEMPTS] ?: ScreenPilotPreferenceDefaults.MAX_KEY_ATTEMPTS
    }

    val sameKeyRetryEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SAME_KEY_RETRY_ENABLED] ?: ScreenPilotPreferenceDefaults.SAME_KEY_RETRY_ENABLED
    }

    val cooldownDurationSecFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[COOLDOWN_DURATION_SEC] ?: ScreenPilotPreferenceDefaults.COOLDOWN_DURATION_SEC
    }

    val skipAuthFailedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SKIP_AUTH_FAILED] ?: ScreenPilotPreferenceDefaults.SKIP_AUTH_FAILED
    }

    val skipPermissionDeniedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SKIP_PERMISSION_DENIED] ?: ScreenPilotPreferenceDefaults.SKIP_PERMISSION_DENIED
    }

    val skipCoolingDownFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SKIP_COOLING_DOWN] ?: ScreenPilotPreferenceDefaults.SKIP_COOLING_DOWN
    }

    val migratedToKeyPoolFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[MIGRATED_TO_KEY_POOL] ?: false
    }

    // Flow getters for new configs
    val screenshotMaxDimensionFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[SCREENSHOT_MAX_DIMENSION] ?: 1280
    }

    val apiJpegQualityFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[API_JPEG_QUALITY] ?: 75
    }

    val galleryJpegQualityFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[GALLERY_JPEG_QUALITY] ?: 92
    }

    val historyLimitFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[HISTORY_LIMIT] ?: ScreenPilotPreferenceDefaults.HISTORY_LIMIT
    }

    val twoImageCaptureEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[TWO_IMAGE_CAPTURE_ENABLED] ?: true
    }

    val longPressThresholdMsFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[LONG_PRESS_THRESHOLD_MS] ?: 650
    }

    val twoImageTimeoutSecFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[TWO_IMAGE_TIMEOUT_SEC] ?: 90
    }

    val pendingStatusBubbleEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PENDING_STATUS_BUBBLE_ENABLED] ?: true
    }

    val pendingStatusDurationMsFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PENDING_STATUS_DURATION_MS] ?: 750
    }

    val stagedStatusBackgroundFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[STAGED_STATUS_BACKGROUND] ?: "None"
    }

    suspend fun setStagedStatusBackground(value: String) {
        context.dataStore.edit { prefs -> prefs[STAGED_STATUS_BACKGROUND] = value }
    }

    suspend fun setSaveScreenshots(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[SAVE_SCREENSHOTS] = value }
    }

    suspend fun setButtonOpacity(value: Float) {
        context.dataStore.edit { prefs -> prefs[BUTTON_OPACITY] = value }
    }

    suspend fun setButtonSizeDp(value: Int) {
        context.dataStore.edit { prefs -> prefs[BUTTON_SIZE_DP] = value }
    }

    suspend fun setLockPosition(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[LOCK_POSITION] = value }
    }

    suspend fun setDismissTimeoutSec(value: Int) {
        context.dataStore.edit { prefs -> prefs[DISMISS_TIMEOUT_SEC] = value }
    }

    suspend fun setDisplayErrorSymbol(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[DISPLAY_ERROR_SYMBOL] = value }
    }

    suspend fun setButtonPosition(x: Float, y: Float) {
        context.dataStore.edit { prefs ->
            prefs[BUTTON_POS_X] = x
            prefs[BUTTON_POS_Y] = y
        }
    }

    suspend fun resetButtonPosition() {
        context.dataStore.edit { prefs ->
            prefs[BUTTON_POS_X] = 0.85f
            prefs[BUTTON_POS_Y] = 0.5f
        }
    }

    suspend fun setPopupSizePercent(value: Float) {
        context.dataStore.edit { prefs -> prefs[POPUP_SIZE_PERCENT] = value }
    }

    suspend fun setPopupFontSizeSp(value: Int) {
        context.dataStore.edit { prefs -> prefs[POPUP_FONT_SIZE_SP] = value }
    }

    suspend fun setPopupFontWeight(value: String) {
        context.dataStore.edit { prefs -> prefs[POPUP_FONT_WEIGHT] = value }
    }

    suspend fun setPopupBgOpacity(value: Float) {
        context.dataStore.edit { prefs -> prefs[POPUP_BG_OPACITY] = value }
    }

    suspend fun setPopupTextOpacity(value: Float) {
        context.dataStore.edit { prefs -> prefs[POPUP_TEXT_OPACITY] = value }
    }

    suspend fun setPopupCornerRadiusDp(value: Int) {
        context.dataStore.edit { prefs -> prefs[POPUP_CORNER_RADIUS_DP] = value }
    }

    suspend fun setPopupPaddingHorizontalDp(value: Int) {
        context.dataStore.edit { prefs -> prefs[POPUP_PADDING_HORIZONTAL_DP] = value }
    }

    suspend fun setPopupPaddingVerticalDp(value: Int) {
        context.dataStore.edit { prefs -> prefs[POPUP_PADDING_VERTICAL_DP] = value }
    }

    suspend fun setPopupBottomOffsetDp(value: Int) {
        context.dataStore.edit { prefs -> prefs[POPUP_BOTTOM_OFFSET_DP] = value }
    }

    suspend fun setPopupStyle(value: String) {
        context.dataStore.edit { prefs -> prefs[POPUP_STYLE] = value }
    }

    suspend fun setPopupBgTheme(value: String) {
        context.dataStore.edit { prefs -> prefs[POPUP_BG_THEME] = value }
    }

    suspend fun setPopupTextColor(value: String) {
        context.dataStore.edit { prefs -> prefs[POPUP_TEXT_COLOR] = value }
    }

    suspend fun setPopupShowConfidence(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[POPUP_SHOW_CONFIDENCE] = value }
    }

    suspend fun setAiProvider(value: String) {
        context.dataStore.edit { prefs -> prefs[AI_PROVIDER] = value }
    }

    suspend fun setFallbackPrimaryProvider(value: String) {
        context.dataStore.edit { prefs -> prefs[FALLBACK_PRIMARY_PROVIDER] = value }
    }

    suspend fun setGeminiModel(value: String) {
        context.dataStore.edit { prefs -> prefs[GEMINI_MODEL] = value }
    }

    suspend fun setGeminiBaseUrl(value: String) {
        context.dataStore.edit { prefs -> prefs[GEMINI_BASE_URL] = value }
    }

    // Setters for new configs
    suspend fun setScreenshotMaxDimension(value: Int) {
        context.dataStore.edit { prefs -> prefs[SCREENSHOT_MAX_DIMENSION] = value }
    }

    suspend fun setApiJpegQuality(value: Int) {
        context.dataStore.edit { prefs -> prefs[API_JPEG_QUALITY] = value }
    }

    suspend fun setGalleryJpegQuality(value: Int) {
        context.dataStore.edit { prefs -> prefs[GALLERY_JPEG_QUALITY] = value }
    }

    suspend fun setHistoryLimit(value: Int) {
        context.dataStore.edit { prefs -> prefs[HISTORY_LIMIT] = value }
    }

    suspend fun setTwoImageCaptureEnabled(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[TWO_IMAGE_CAPTURE_ENABLED] = value }
    }

    suspend fun setLongPressThresholdMs(value: Int) {
        context.dataStore.edit { prefs -> prefs[LONG_PRESS_THRESHOLD_MS] = value }
    }

    suspend fun setTwoImageTimeoutSec(value: Int) {
        context.dataStore.edit { prefs -> prefs[TWO_IMAGE_TIMEOUT_SEC] = value }
    }

    suspend fun setPendingStatusBubbleEnabled(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[PENDING_STATUS_BUBBLE_ENABLED] = value }
    }

    suspend fun setPendingStatusDurationMs(value: Int) {
        context.dataStore.edit { prefs -> prefs[PENDING_STATUS_DURATION_MS] = value }
    }

    suspend fun setGeminiKeySlotsMetadata(value: String) {
        context.dataStore.edit { prefs -> prefs[GEMINI_KEY_SLOTS_METADATA] = value }
    }

    suspend fun setKeyStrategy(value: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_STRATEGY] = value
            prefs[KEY_STRATEGY_MIGRATED] = true
        }
    }

    suspend fun setLastSuccessfulKeyId(value: String) {
        context.dataStore.edit { prefs -> prefs[LAST_SUCCESSFUL_KEY_ID] = value }
    }

    suspend fun setRoundRobinLastKeyIndex(value: Int) {
        context.dataStore.edit { prefs -> prefs[ROUND_ROBIN_LAST_KEY_INDEX] = value }
    }

    suspend fun setMaxKeyAttempts(value: Int) {
        context.dataStore.edit { prefs -> prefs[MAX_KEY_ATTEMPTS] = value }
    }

    suspend fun setSameKeyRetryEnabled(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[SAME_KEY_RETRY_ENABLED] = value }
    }

    suspend fun setCooldownDurationSec(value: Int) {
        context.dataStore.edit { prefs -> prefs[COOLDOWN_DURATION_SEC] = value }
    }

    suspend fun setSkipAuthFailed(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[SKIP_AUTH_FAILED] = value }
    }

    suspend fun setSkipPermissionDenied(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[SKIP_PERMISSION_DENIED] = value }
    }

    suspend fun setSkipCoolingDown(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[SKIP_COOLING_DOWN] = value }
    }

    suspend fun setMigratedToKeyPool(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[MIGRATED_TO_KEY_POOL] = value }
    }

    suspend fun resetPopupAppearance() {
        context.dataStore.edit { prefs ->
            prefs.remove(POPUP_SIZE_PERCENT)
            prefs.remove(POPUP_FONT_SIZE_SP)
            prefs.remove(POPUP_FONT_WEIGHT)
            prefs.remove(POPUP_BG_OPACITY)
            prefs.remove(POPUP_TEXT_OPACITY)
            prefs.remove(POPUP_CORNER_RADIUS_DP)
            prefs.remove(POPUP_PADDING_HORIZONTAL_DP)
            prefs.remove(POPUP_PADDING_VERTICAL_DP)
            prefs.remove(POPUP_BOTTOM_OFFSET_DP)
            prefs.remove(POPUP_STYLE)
            prefs.remove(POPUP_BG_THEME)
            prefs.remove(POPUP_TEXT_COLOR)
            prefs.remove(POPUP_SHOW_CONFIDENCE)
        }
    }

    suspend fun restoreRecommendedDefaults() {
        context.dataStore.edit { prefs ->
            // popup appearance
            prefs.remove(POPUP_SIZE_PERCENT)
            prefs.remove(POPUP_FONT_SIZE_SP)
            prefs.remove(POPUP_FONT_WEIGHT)
            prefs.remove(POPUP_BG_OPACITY)
            prefs.remove(POPUP_TEXT_OPACITY)
            prefs.remove(POPUP_CORNER_RADIUS_DP)
            prefs.remove(POPUP_PADDING_HORIZONTAL_DP)
            prefs.remove(POPUP_PADDING_VERTICAL_DP)
            prefs.remove(POPUP_BOTTOM_OFFSET_DP)
            prefs.remove(POPUP_STYLE)
            prefs.remove(POPUP_BG_THEME)
            prefs.remove(POPUP_TEXT_COLOR)
            prefs.remove(POPUP_SHOW_CONFIDENCE)

            // floating-button appearance
            prefs.remove(BUTTON_OPACITY)
            prefs.remove(BUTTON_SIZE_DP)
            prefs.remove(LOCK_POSITION)
            prefs.remove(BUTTON_POS_X)
            prefs.remove(BUTTON_POS_Y)

            // capture quality
            prefs.remove(SAVE_SCREENSHOTS)
            prefs.remove(SCREENSHOT_MAX_DIMENSION)
            prefs.remove(API_JPEG_QUALITY)
            prefs.remove(GALLERY_JPEG_QUALITY)

            // provider selection
            prefs.remove(AI_PROVIDER)
            prefs.remove(FALLBACK_PRIMARY_PROVIDER)

            // model and base URL
            prefs.remove(GEMINI_MODEL)
            prefs.remove(GEMINI_BASE_URL)

            // timeout settings & limits
            prefs.remove(DISMISS_TIMEOUT_SEC)
            prefs.remove(DISPLAY_ERROR_SYMBOL)
            prefs.remove(HISTORY_LIMIT)

            // key pool strategy settings (but NOT keys metadata)
            prefs.remove(KEY_STRATEGY)
            prefs.remove(LAST_SUCCESSFUL_KEY_ID)
            prefs.remove(ROUND_ROBIN_LAST_KEY_INDEX)
            prefs.remove(ROUND_ROBIN_LAST_SLOT_ID)
            prefs.remove(ESSAY_NOTIFICATION_ENABLED)
            prefs.remove(MAX_KEY_ATTEMPTS)
            prefs.remove(SAME_KEY_RETRY_ENABLED)
            prefs.remove(COOLDOWN_DURATION_SEC)
            prefs.remove(SKIP_AUTH_FAILED)
            prefs.remove(SKIP_PERMISSION_DENIED)
            prefs.remove(SKIP_COOLING_DOWN)
            prefs.remove(STAGED_STATUS_BACKGROUND)
        }
    }
}
