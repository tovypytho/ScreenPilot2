package com.example.service

import android.app.Activity
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.HistoryEntry
import com.example.data.HistoryRepository
import com.example.data.HistoryQuestionType
import com.example.data.KeyStoreHelper
import com.example.data.PreferencesRepository
import com.example.data.FloatingButtonStyleSnapshot
import com.example.data.AnswerPopupStyle
import com.example.data.GeminiKeySlot
import com.example.data.GeminiKeySlotSerializer
import com.example.data.GeminiKeyHealth
import com.example.notification.EssayAnswerNotificationManager
import com.example.notification.EssayNotificationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import java.net.ConnectException
import javax.net.ssl.SSLException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ScreenCaptureService : Service() {

    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val NOTIFICATION_ID = 54321
        private const val CHANNEL_ID = "screen_pilot_capture_channel"

        const val ACTION_START = "com.example.action.START"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_LOCATE_BUTTON = "com.example.action.LOCATE_BUTTON"
        const val ACTION_SHOW_PREVIEW = "com.example.action.SHOW_PREVIEW"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        val isServiceActive = MutableStateFlow(false)
        val lastError = MutableStateFlow<String?>(null)
        val isLocating = MutableStateFlow(false)
        const val ACTION_CANCEL_STAGED = "com.example.action.CANCEL_STAGED"
        val isStagedPending = MutableStateFlow(false)

        // Diagnostics fields
        var lastGestureType = "None"
        var lastCapturePurpose = "None"
        var lastCaptureDimensions = "None"
        var lastJpegSize = "None"
        var lastGallerySaveResult = "None"
        var lastImage1Age = "None"
        var lastImage1UriAvailable = "No"
        var lastImage2UriAvailable = "No"
        var lastFailedStage = "None"
        var lastSanitizedError = "None"
        var currentStagedStateText = "IDLE"

        fun getStagedDiagnostics(): String {
            return """
                Staged Capture Diagnostics:
                - Current Staged State: $currentStagedStateText
                - Last Gesture Type: $lastGestureType
                - Long-Press Duration: 650 ms
                - Capture Purpose: $lastCapturePurpose
                - Capture Dimensions: $lastCaptureDimensions
                - JPEG Size: $lastJpegSize
                - Gallery Save Result: $lastGallerySaveResult
                - Pending Image 1 Age: $lastImage1Age
                - Image 1 Gallery URI Available: $lastImage1UriAvailable
                - Image 2 Gallery URI Available: $lastImage2UriAvailable
                - Failed Stage: $lastFailedStage
                - Sanitized Error: $lastSanitizedError
            """.trimIndent()
        }

        const val ACTION_SIMULATE_TIMEOUT = "com.example.action.SIMULATE_TIMEOUT"
        const val ACTION_SIMULATE_GALLERY_FAIL = "com.example.action.SIMULATE_GALLERY_FAIL"
        const val ACTION_SIMULATE_PARSING_FAIL = "com.example.action.SIMULATE_PARSING_FAIL"
        const val ACTION_SIMULATE_STAGED_TIMEOUT = "com.example.action.SIMULATE_STAGED_TIMEOUT"
        const val ACTION_SIMULATE_OVERLAY_RECREATE = "com.example.action.SIMULATE_OVERLAY_RECREATE"
        const val ACTION_SIMULATE_COROUTINE_FAIL = "com.example.action.SIMULATE_COROUTINE_FAIL"
        const val ACTION_SIMULATE_LOW_MEM = "com.example.action.SIMULATE_LOW_MEM"
    }

    class CaptureTimeoutException(message: String) : Exception(message)

    enum class CaptureState {
        IDLE, CAPTURING_FIRST, WAITING_FOR_SECOND, CAPTURING_SECOND, ANALYZING, CANCELLED
    }
    private val captureState = MutableStateFlow(CaptureState.IDLE)
    private val stateMutex = Mutex()

    enum class StagedCaptureState {
        IDLE,
        CAPTURING_FIRST,
        WAITING_FOR_SECOND,
        CAPTURING_SECOND,
        ANALYZING_TWO_IMAGES,
        CANCELLING
    }

    private val stagedCaptureMutex = Mutex()
    private val stagedCaptureState = MutableStateFlow(StagedCaptureState.IDLE)

    enum class CaptureWorkflowState {
        IDLE,
        CAPTURING_SINGLE,
        ANALYZING_SINGLE,
        CAPTURING_FIRST,
        WAITING_FOR_SECOND,
        CAPTURING_SECOND,
        ANALYZING_TWO_IMAGES,
        STOPPING
    }
    private val captureWorkflowState = MutableStateFlow(CaptureWorkflowState.IDLE)

    enum class GallerySaveState {
        PENDING,
        SAVED,
        FAILED,
        DISABLED
    }

    enum class CapturePurpose {
        SINGLE,
        STAGED_PART_1,
        STAGED_PART_2
    }

    data class PreparedScreenshot(
        val apiJpegBytes: ByteArray,
        val galleryJpegBytes: ByteArray?,
        val width: Int,
        val height: Int,
        val capturedAt: Long,
        val purpose: CapturePurpose
    )

    private fun updateWorkflowState(newState: CaptureWorkflowState) {
        captureWorkflowState.value = newState

        // Synchronize with legacy stagedCaptureState
        stagedCaptureState.value = when (newState) {
            CaptureWorkflowState.IDLE -> StagedCaptureState.IDLE
            CaptureWorkflowState.CAPTURING_FIRST -> StagedCaptureState.CAPTURING_FIRST
            CaptureWorkflowState.WAITING_FOR_SECOND -> StagedCaptureState.WAITING_FOR_SECOND
            CaptureWorkflowState.CAPTURING_SECOND -> StagedCaptureState.CAPTURING_SECOND
            CaptureWorkflowState.ANALYZING_TWO_IMAGES -> StagedCaptureState.ANALYZING_TWO_IMAGES
            CaptureWorkflowState.STOPPING -> StagedCaptureState.CANCELLING
            else -> StagedCaptureState.IDLE
        }

        // Synchronize with legacy captureState
        captureState.value = when (newState) {
            CaptureWorkflowState.IDLE -> CaptureState.IDLE
            CaptureWorkflowState.CAPTURING_FIRST -> CaptureState.CAPTURING_FIRST
            CaptureWorkflowState.WAITING_FOR_SECOND -> CaptureState.WAITING_FOR_SECOND
            CaptureWorkflowState.CAPTURING_SECOND -> CaptureState.CAPTURING_SECOND
            CaptureWorkflowState.CAPTURING_SINGLE -> CaptureState.CAPTURING_FIRST
            CaptureWorkflowState.ANALYZING_SINGLE -> CaptureState.ANALYZING
            CaptureWorkflowState.ANALYZING_TWO_IMAGES -> CaptureState.ANALYZING
            else -> CaptureState.IDLE
        }

        isStagedPending.value = (newState == CaptureWorkflowState.WAITING_FOR_SECOND)
        currentStagedStateText = newState.name
    }

    private var gestureConsumed = false
    private var lastTapGuidanceTime = 0L

    data class PendingStagedCapture(
        val sessionId: String,
        val jpegBytes: ByteArray,
        val width: Int,
        val height: Int,
        val capturedAt: Long,
        val gallerySaveState: GallerySaveState
    )
    private var pendingFirstImage: PendingStagedCapture? = null
    private var sharedCaptureTimestamp: String? = null

    private val currentPopupStyle = MutableStateFlow<com.example.data.AnswerPopupStyle?>(null)
    private var longPressThresholdMs = 650
    private var twoImageTimeoutSec = 90
    private var timeoutJob: kotlinx.coroutines.Job? = null
    private var twoImageCaptureEnabled = true

    private val serviceExceptionHandler: kotlinx.coroutines.CoroutineExceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        val safeMessage = throwable.message ?: "Unknown coroutine error"
        Log.e(TAG, "Unhandled exception in ScreenCaptureService scope: $safeMessage", throwable)
        lastSanitizedError = safeMessage
        // Phase 4: Use one service-owned diagnosticScope for best-effort diagnostic persistence.
        diagnosticScope.launch {
            try {
                preferencesRepository.updateSessionLastActionStage("CHILD_COROUTINE_ERROR: $safeMessage")
                preferencesRepository.updateSessionLastHealthyTime(System.currentTimeMillis())
            } catch (t: Throwable) {
                Log.e(TAG, "Diagnostic persistence failed (swallowed): ${t.message}")
            }
        }
    }
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Main.immediate + serviceExceptionHandler)
    private val diagnosticJob = SupervisorJob()
    private val diagnosticScope = CoroutineScope(diagnosticJob + Dispatchers.IO)

    private fun launchNonCritical(
        name: String,
        block: suspend CoroutineScope.() -> Unit
    ): kotlinx.coroutines.Job {
        return serviceScope.launch(Dispatchers.IO) {
            try {
                block()
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (t: Throwable) {
                Log.e(TAG, "Non-critical error in task '$name': ${t.message ?: "no message"}")
            }
        }
    }
    private var healthWatcherJob: kotlinx.coroutines.Job? = null
    private val shutdownInProgress = java.util.concurrent.atomic.AtomicBoolean(false)
    private val cleanupCompleted = java.util.concurrent.atomic.AtomicBoolean(false)
    private var locateJob: kotlinx.coroutines.Job? = null
    private lateinit var windowManager: WindowManager
    private lateinit var mediaProjectionManager: MediaProjectionManager

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var imageHandlerThread: android.os.HandlerThread? = null
    private var imageHandler: Handler? = null

    // Track capture dimensions
    private var captureWidth: Int = 0
    private var captureHeight: Int = 0
    private var captureDensityDpi: Int = 0

    // Mutex for capture, replace, resize operations
    private val captureSurfaceMutex = Mutex()
    // Phase 1: FreshFrameReadinessGate for post-overlay-hide frame readiness
    private val freshFrameGate = FreshFrameReadinessGate()
    // Phase 3: Popup invalidation token coordinator
    private val popupAttachmentCoordinator = AnswerPopupAttachmentCoordinator()
    // Phase 7: Capture surface resize coordinator
    private val resizeCoordinator = CaptureSurfaceResizeCoordinator<ImageReader> { message -> Log.e(TAG, message) }
    private var isInfrastructureHealthy = true
    @Volatile
    private var internalProjectionStopReason: String? = null
    @Volatile
    private var terminalShutdownReason: String? = null
    @Volatile
    private var geminiKeySlotsSnapshot: List<GeminiKeySlot> = emptyList()

    private lateinit var database: AppDatabase
    private lateinit var historyRepository: HistoryRepository
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var failoverPrefReader: FailoverPreferenceReader

    // Overlay Views
    private var floatingButtonView: ComposeView? = null
    private val floatingCreationInProgress = java.util.concurrent.atomic.AtomicBoolean(false)
    private var answerPopupView: ComposeView? = null

    private var floatingButtonParams: WindowManager.LayoutParams? = null
    private var answerPopupParams: WindowManager.LayoutParams? = null

    // Touch and Drag State
    private var isDragging = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var longPressTriggered = false
    private var longPressRunnable: Runnable? = null
    private var touchSlop = 0f
    private var lastTapTime = 0L
    private val doubleTapTimeout = 300L

    private var isPositionLocked = false
    private var buttonOpacity = 0.75f
    private var buttonSizeDp = 54

    // API Call state
    private val isProcessing = MutableStateFlow(false)
    private val currentStagedStatusBackground = MutableStateFlow("None")

    // Fallback dismiss handler
    private val mainHandler = Handler(Looper.getMainLooper())
    private val dismissRunnable = Runnable { removeAnswerPopup() }

    // Custom Lifecycle Owners for WindowManager ComposeViews
    private lateinit var floatingLifecycleOwner: ServiceLifecycleOwner
    private lateinit var answerLifecycleOwner: ServiceLifecycleOwner

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        touchSlop = android.view.ViewConfiguration.get(this).scaledTouchSlop.toFloat()

        database = AppDatabase.getDatabase(this)
        historyRepository = HistoryRepository(database.historyDao())
        preferencesRepository = PreferencesRepository(this)
        failoverPrefReader = FailoverPreferenceReader(
            PreferencesRepositoryFailoverSource(preferencesRepository)
        ) { message -> Log.w(TAG, message) }

        floatingLifecycleOwner = ServiceLifecycleOwner()
        answerLifecycleOwner = ServiceLifecycleOwner()

        floatingLifecycleOwner.start()
        answerLifecycleOwner.start()

        val thread = android.os.HandlerThread("ScreenPilotImageReader").apply { start() }
        imageHandlerThread = thread
        imageHandler = Handler(thread.looper)

        observePreferences()

        serviceScope.launch {
            stagedCaptureState.collect { state ->
                isStagedPending.value = (state == StagedCaptureState.WAITING_FOR_SECOND)
                currentStagedStateText = state.name
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        val action = intent.action
        if (action == ACTION_STOP) {
            requestServiceStop("USER_STOP")
            return START_NOT_STICKY
        }

        if (action == ACTION_LOCATE_BUTTON) {
            locateJob?.cancel()
            locateJob = serviceScope.launch {
                isLocating.value = true
                delay(3000)
                isLocating.value = false
            }
            return START_NOT_STICKY
        }

        if (action == ACTION_SHOW_PREVIEW) {
            // showAnswerPopup is now suspend — must be called from a coroutine.
            serviceScope.launch { showAnswerPopup("3", 0.95) }
            return START_NOT_STICKY
        }

        if (action == ACTION_CANCEL_STAGED) {
            serviceScope.launch {
                stagedCaptureMutex.withLock {
                    timeoutJob?.cancel()
                    timeoutJob = null
                    pendingFirstImage = null
                    sharedCaptureTimestamp = null
                    updateWorkflowState(CaptureWorkflowState.IDLE)
                    removeStatusBubble()
                    removeAnswerPopup()
                    updateJournalStage("STAGED_CANCELLED")
                    Log.d(TAG, "Staged capture cancelled by user request.")
                }
                showStatusBubble("Mode dua gambar dibatalkan", 750L)
            }
            return START_NOT_STICKY
        }

        // Developer simulations (complying with Part 23)
        if (action == ACTION_SIMULATE_TIMEOUT) {
            serviceScope.launch {
                showStatusBubble("Simulating Timeout...", 1500L)
                delay(1000)
                processApiError("Network Connection Error: Connection timed out.", "Google Gemini", 3000L, 0, 1, "Single")
            }
            return START_NOT_STICKY
        }

        if (action == ACTION_SIMULATE_GALLERY_FAIL) {
            serviceScope.launch {
                showStatusBubble("Simulated Gallery Failure", 1500L)
                Log.e(TAG, "Simulated IOException during saveBitmapToGallery", java.io.IOException("Simulated write failure to external storage"))
            }
            return START_NOT_STICKY
        }

        if (action == ACTION_SIMULATE_PARSING_FAIL) {
            serviceScope.launch {
                showStatusBubble("Simulating Parsing Failure...", 1500L)
                delay(1000)
                processApiError("Provider Response Error: JSON parsing failed.", "Google Gemini", 1000L, 200, 1, "Single")
            }
            return START_NOT_STICKY
        }

        if (action == ACTION_SIMULATE_STAGED_TIMEOUT) {
            serviceScope.launch {
                stagedCaptureMutex.withLock {
                    if (pendingFirstImage != null) {
                        timeoutJob?.cancel()
                        timeoutJob = null
                        pendingFirstImage = null
                        sharedCaptureTimestamp = null
                        updateWorkflowState(CaptureWorkflowState.IDLE)
                        showStatusBubble("Tangkapan 1/2 kedaluwarsa", 1500L)
                    } else {
                        showStatusBubble("No staged image to timeout", 1500L)
                    }
                }
            }
            return START_NOT_STICKY
        }

        if (action == ACTION_SIMULATE_OVERLAY_RECREATE) {
            serviceScope.launch {
                showStatusBubble("Recreating Overlays...", 1500L)
                removeFloatingButton()
                showFloatingButton()
            }
            return START_NOT_STICKY
        }

        if (action == ACTION_SIMULATE_COROUTINE_FAIL) {
            serviceScope.launch {
                showStatusBubble("Simulating Child Job Failure...", 1500L)
                launch {
                    throw RuntimeException("Simulated child coroutine exception")
                }
            }
            return START_NOT_STICKY
        }

        if (action == ACTION_SIMULATE_LOW_MEM) {
            showStatusBubble("Simulating Low Memory...", 1500L)
            onTrimMemory(android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
            return START_NOT_STICKY
        }

        if (action == ACTION_START) {
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
            val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)

            if (resultCode == Activity.RESULT_OK && resultData != null) {
                val sessionId = java.util.UUID.randomUUID().toString()
                serviceScope.launch {
                    preferencesRepository.updateSessionId(sessionId)
                    preferencesRepository.updateSessionActivationTime(System.currentTimeMillis())
                    preferencesRepository.updateSessionServiceStarted(true)
                    preferencesRepository.updateSessionGracefulShutdown(false)
                    preferencesRepository.updateSessionShutdownReason("")
                    preferencesRepository.updateSessionForegroundPromoted(false)
                    preferencesRepository.updateSessionProjectionInitialized(false)
                    preferencesRepository.updateSessionFloatingCreated(false)
                    preferencesRepository.updateSessionLastActionStage("STARTING")
                    preferencesRepository.updateSessionLastHealthyTime(System.currentTimeMillis())
                }

                startForegroundWithNotification()
                serviceScope.launch {
                    preferencesRepository.updateSessionForegroundPromoted(true)
                    val success = initializeMediaProjection(resultCode, resultData)
                    if (success) {
                        preferencesRepository.updateSessionProjectionInitialized(true)
                        showFloatingButton()
                        preferencesRepository.updateSessionFloatingCreated(true)
                        isServiceActive.value = true
                        lastError.value = null
                        
                        preferencesRepository.updateSessionLastActionStage("RUNNING")
                        preferencesRepository.updateSessionLastHealthyTime(System.currentTimeMillis())
                        
                        startSessionHealthWatcher()
                    } else {
                        preferencesRepository.updateSessionGracefulShutdown(false)
                        preferencesRepository.updateSessionShutdownReason("INITIALIZATION_FAILED")
                        preferencesRepository.updateSessionServiceStarted(false)

                        releaseVirtualDisplay()
                        mediaProjection?.stop()
                        mediaProjection = null

                        lastError.value = "Failed to initialize screen capture"
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            } else {
                Log.e(TAG, "MediaProjection result code not OK or null intent data")
                lastError.value = "Screen capture permission denied"
                serviceScope.launch {
                    preferencesRepository.updateSessionGracefulShutdown(false)
                    preferencesRepository.updateSessionShutdownReason("PROJECTION_PERMISSION_DENIED")
                    preferencesRepository.updateSessionServiceStarted(false)
                }
                stopSelf()
                return START_NOT_STICKY
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        createNotificationChannel()

        val stopIntent = Intent(this, ScreenCaptureService::class.java).apply {
            this.action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("ScreenPilot Active")
            .setContentText("Screen capture session is active")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setContentIntent(mainPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop ScreenPilot", stopPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "ScreenPilot Active",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notification channel for ScreenPilot screen capture session"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private suspend fun initializeMediaProjection(resultCode: Int, resultData: Intent): Boolean {
        try {
            internalProjectionStopReason = null
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)
            val projectionCallback = object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.w(TAG, "MediaProjection session stopped.")
                    val rootReason = internalProjectionStopReason ?: "PROJECTION_REVOKED"
                    requestServiceStop(rootReason)
                }
            }
            mediaProjection?.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))

            val vDisplaySuccess = createInitialVirtualDisplay()
            if (!vDisplaySuccess) {
                return false
            }

            return mediaProjection != null &&
                    imageReader != null &&
                    virtualDisplay != null &&
                    captureWidth > 0 &&
                    captureHeight > 0 &&
                    captureDensityDpi > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MediaProjection: ${e.message}", e)
            lastError.value = "Failed to initialize screen capture: ${e.message}"
            return false
        }
    }

    private suspend fun createInitialVirtualDisplay(): Boolean {
        return captureSurfaceMutex.withLock {
            val projection = mediaProjection ?: return@withLock false
            val bounds = getScreenBounds()
            val w = bounds.width()
            val h = bounds.height()
            val d = resources.displayMetrics.densityDpi

            if (!CaptureDimensionHelper.isValid(w, h, d)) {
                return@withLock false
            }

            var tempReader: ImageReader? = null
            var tempDisplay: VirtualDisplay? = null
            try {
                tempReader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
                tempDisplay = projection.createVirtualDisplay(
                    "ScreenPilotCapture",
                    w,
                    h,
                    d,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    tempReader.surface,
                    null,
                    null
                )

                if (tempDisplay != null) {
                    // Assign fields only after creation succeeds
                    imageReader = tempReader
                    virtualDisplay = tempDisplay
                    captureWidth = w
                    captureHeight = h
                    captureDensityDpi = d
                    true
                } else {
                    tempReader.close()
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create virtual display: ${e.message}")
                try {
                    tempDisplay?.release()
                } catch (ex: Exception) {}
                try {
                    tempReader?.close()
                } catch (ex: Exception) {}
                false
            }
        }
    }

    private suspend fun resizeVirtualDisplayIfNeeded(): Boolean {
        return captureSurfaceMutex.withLock {
            if (!isInfrastructureHealthy) return@withLock false
            val display = virtualDisplay ?: return@withLock false
            val bounds = getScreenBounds()
            val newW = bounds.width()
            val newH = bounds.height()
            val newD = resources.displayMetrics.densityDpi

            if (!CaptureDimensionHelper.isValid(newW, newH, newD)) {
                return@withLock false
            }

            val current = CaptureDimensions(
                captureWidth.coerceAtLeast(1),
                captureHeight.coerceAtLeast(1),
                captureDensityDpi.coerceAtLeast(1)
            )
            if (!CaptureDimensionHelper.isResizeRequired(current, newW, newH, newD)) {
                return@withLock true
            }

            val oldReader = imageReader
            val oldW = captureWidth
            val oldH = captureHeight
            val oldD = captureDensityDpi

            val ops = object : SurfaceResizeOps<ImageReader> {
                override fun createNewReader(): ImageReader {
                    return ImageReader.newInstance(newW, newH, PixelFormat.RGBA_8888, 2)
                }
                override fun resizeToNew() {
                    display.resize(newW, newH, newD)
                }
                override fun attachNew(reader: ImageReader) {
                    display.setSurface(reader.surface)
                }
                override fun resizeToOld() {
                    display.resize(oldW, oldH, oldD)
                }
                override fun attachOld(reader: ImageReader?) {
                    if (reader != null) display.setSurface(reader.surface)
                }
                override fun closeReader(reader: ImageReader?) {
                    reader?.setOnImageAvailableListener(null, null)
                    reader?.close()
                }
            }

            val (result, resReader) = resizeCoordinator.resize(
                display != null, oldReader, ops
            )

            when (result) {
                is CaptureSurfaceResizeResult.Success -> {
                    imageReader = resReader
                    captureWidth = newW
                    captureHeight = newH
                    captureDensityDpi = newD
                    true
                }
                is CaptureSurfaceResizeResult.RolledBack -> {
                    lastError.value = "Failed to adjust screen layout"
                    false
                }
                is CaptureSurfaceResizeResult.InfrastructureBroken -> {
                    isInfrastructureHealthy = false
                    lastError.value = result.safeReason
                    internalProjectionStopReason = "INFRASTRUCTURE_BROKEN: ${result.safeReason}"
                    try {
                        virtualDisplay?.release()
                        virtualDisplay = null
                        mediaProjection?.stop()
                        mediaProjection = null
                        imageReader = null
                    } catch (e: Exception) {
                        Log.e(TAG, "Error cleaning broken infrastructure: ${e.message}")
                    }

                    // Phase 6: Controlled reaction to InfrastructureBroken
                    timeoutJob?.cancel()
                    stagedCaptureMutex.withLock {
                        pendingFirstImage = null
                        sharedCaptureTimestamp = null
                        updateWorkflowState(CaptureWorkflowState.IDLE)
                    }
                    withContext(Dispatchers.Main.immediate) {
                        removeAnswerPopup()
                        removeFloatingButton()
                        removeStatusBubble()
                        showStatusBubble("Tangkapan layar perlu diaktifkan ulang", 3000L)
                    }
                    isServiceActive.value = false
                    launchNonCritical("infrastructureBrokenDiagnostic") {
                        preferencesRepository.updateSessionGracefulShutdown(false)
                        preferencesRepository.updateSessionShutdownReason("INFRASTRUCTURE_BROKEN: ${result.safeReason}")
                    }
                    false
                }
            }
        }
    }

    private fun getScreenBounds(): android.graphics.Rect {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds
        } else {
            val size = android.graphics.Point()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealSize(size)
            android.graphics.Rect(0, 0, size.x, size.y)
        }
    }

    private fun observePreferences() {
        serviceScope.launch {
            preferencesRepository.lockPositionFlow.collect { locked ->
                isPositionLocked = locked
            }
        }
        serviceScope.launch {
            preferencesRepository.buttonOpacityFlow.collect { opacity ->
                buttonOpacity = opacity
                updateFloatingButtonLayout()
            }
        }
        serviceScope.launch {
            preferencesRepository.buttonSizeDpFlow.collect { size ->
                buttonSizeDp = size
                updateFloatingButtonLayout()
            }
        }
        serviceScope.launch {
            preferencesRepository.popupStyleSnapshotFlow.collect { style ->
                currentPopupStyle.value = style
            }
        }
        serviceScope.launch {
            preferencesRepository.longPressThresholdMsFlow.collect { ms ->
                longPressThresholdMs = ms
            }
        }
        serviceScope.launch {
            preferencesRepository.twoImageTimeoutSecFlow.collect { sec ->
                twoImageTimeoutSec = sec
            }
        }
        serviceScope.launch {
            preferencesRepository.stagedStatusBackgroundFlow.collect { background ->
                currentStagedStatusBackground.value = background
            }
        }
        serviceScope.launch {
            preferencesRepository.twoImageCaptureEnabledFlow.collect { enabled ->
                twoImageCaptureEnabled = enabled
                if (!enabled) {
                    stagedCaptureMutex.withLock {
                        val curState = captureWorkflowState.value
                        if (curState == CaptureWorkflowState.CAPTURING_FIRST ||
                            curState == CaptureWorkflowState.WAITING_FOR_SECOND ||
                            curState == CaptureWorkflowState.CAPTURING_SECOND ||
                            curState == CaptureWorkflowState.ANALYZING_TWO_IMAGES) {

                            timeoutJob?.cancel()
                            timeoutJob = null
                            pendingFirstImage = null
                            isStagedPending.value = false
                            updateWorkflowState(CaptureWorkflowState.IDLE)
                            sharedCaptureTimestamp = null
                            removeStatusBubble()
                            removeAnswerPopup()
                            updateJournalStage("STAGED_CANCELLED_BY_PREF_DISABLED")
                            Log.d(TAG, "Staged capture cancelled because two-image mode was disabled.")
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingButton() {
        if (floatingButtonView != null) return
        if (shutdownInProgress.get()) return
        if (!floatingCreationInProgress.compareAndSet(false, true)) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        serviceScope.launch {
            try {
                val metrics = resources.displayMetrics
                val posX = preferencesRepository.buttonPosXFlow.first()
                val posY = preferencesRepository.buttonPosYFlow.first()
                val savedOpacity = preferencesRepository.buttonOpacityFlow.first()
                val savedSize = preferencesRepository.buttonSizeDpFlow.first()
                val isLocked = preferencesRepository.lockPositionFlow.first()

                val snapshot = FloatingButtonStyleSnapshot(
                    opacity = savedOpacity,
                    visualSizeDp = savedSize,
                    positionLocked = isLocked,
                    normalizedX = posX,
                    normalizedY = posY
                )

                isPositionLocked = snapshot.positionLocked

                params.x = (snapshot.normalizedX * metrics.widthPixels).toInt().coerceIn(0, metrics.widthPixels)
                params.y = (snapshot.normalizedY * metrics.heightPixels).toInt().coerceIn(0, metrics.heightPixels)

                if (shutdownInProgress.get()) {
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    if (shutdownInProgress.get() || floatingButtonView != null) {
                        return@withContext
                    }

                    val composeView = ComposeView(this@ScreenCaptureService).apply {
                        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                        setViewTreeLifecycleOwner(floatingLifecycleOwner)
                        setViewTreeSavedStateRegistryOwner(floatingLifecycleOwner)

                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        background = null
                        foreground = null
                        stateListAnimator = null
                        elevation = 0f
                        translationZ = 0f
                        isClickable = false
                        isLongClickable = false
                        isFocusable = false
                        isFocusableInTouchMode = false
                        isSoundEffectsEnabled = false
                        isHapticFeedbackEnabled = false

                        setContent {
                            val flowOpacity by preferencesRepository.buttonOpacityFlow.collectAsState(initial = snapshot.opacity)
                            val locating by isLocating.collectAsState()
                            val opacity = if (locating) 0.35f else flowOpacity
                            val sizeDp by preferencesRepository.buttonSizeDpFlow.collectAsState(initial = snapshot.visualSizeDp)
                            val hasPending by isStagedPending.collectAsState()

                            val outerSizeDp = maxOf(sizeDp, 48)

                            Box(
                                modifier = Modifier
                                    .size(outerSizeDp.dp)
                                    .testTag("floating_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(sizeDp.dp)
                                        .alpha(opacity.coerceIn(0f, 1f))
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(
                                            width = if (hasPending) 3.dp else 1.dp,
                                            color = if (hasPending) Color(0xFF4CAF50) else Color.Gray.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size((sizeDp / 4).dp)
                                            .clip(CircleShape)
                                            .background(if (hasPending) Color(0xFF4CAF50) else Color.Gray.copy(alpha = 0.4f))
                                    )
                                }
                            }
                        }
                    }

                    composeView.setOnTouchListener { view, event ->
                        val curParams = floatingButtonParams ?: return@setOnTouchListener false
                        val gestureThreshold = longPressThresholdMs.toLong()

                        view.isPressed = false

                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                view.isPressed = false
                                initialX = curParams.x
                                initialY = curParams.y
                                initialTouchX = event.rawX
                                initialTouchY = event.rawY
                                isDragging = false
                                longPressTriggered = false
                                gestureConsumed = false

                                longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                                val runnable = Runnable {
                                    if (!isDragging) {
                                        longPressTriggered = true
                                        gestureConsumed = true
                                        handleLongPressCapture()
                                    }
                                }
                                longPressRunnable = runnable
                                mainHandler.postDelayed(runnable, gestureThreshold)
                                true
                            }
                            MotionEvent.ACTION_MOVE -> {
                                view.isPressed = false
                                val dx = event.rawX - initialTouchX
                                val dy = event.rawY - initialTouchY
                                val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()

                                if (dist > touchSlop) {
                                    longPressRunnable?.let {
                                        mainHandler.removeCallbacks(it)
                                        longPressRunnable = null
                                    }
                                    if (!isPositionLocked) {
                                        isDragging = true
                                        gestureConsumed = true
                                    }
                                }

                                if (isDragging && !isPositionLocked) {
                                    curParams.x = initialX + dx.toInt()
                                    curParams.y = initialY + dy.toInt()

                                    val bounds = getScreenBounds()
                                    curParams.x = curParams.x.coerceIn(0, bounds.width() - view.width)
                                    curParams.y = curParams.y.coerceIn(0, bounds.height() - view.height)

                                    try {
                                        windowManager.updateViewLayout(view, curParams)
                                    } catch (e: Exception) {
                                        // ignore
                                    }
                                }
                                true
                            }
                            MotionEvent.ACTION_UP -> {
                                view.isPressed = false
                                longPressRunnable?.let {
                                    mainHandler.removeCallbacks(it)
                                    longPressRunnable = null
                                }
                                if (isDragging) {
                                    gestureConsumed = true
                                    if (!isPositionLocked) {
                                        val bounds = getScreenBounds()
                                        val normX = curParams.x.toFloat() / bounds.width()
                                        val normY = curParams.y.toFloat() / bounds.height()
                                        serviceScope.launch {
                                            preferencesRepository.setButtonPosition(normX, normY)
                                        }
                                    }
                                } else if (longPressTriggered || gestureConsumed) {
                                    // consumed!
                                } else {
                                    handleSingleTapCapture()
                                }
                                true
                            }
                            MotionEvent.ACTION_CANCEL -> {
                                view.isPressed = false
                                longPressRunnable?.let {
                                    mainHandler.removeCallbacks(it)
                                    longPressRunnable = null
                                }
                                isDragging = false
                                longPressTriggered = false
                                gestureConsumed = false
                                true
                            }
                            else -> false
                        }
                    }

                    if (shutdownInProgress.get() || floatingButtonView != null) {
                        try {
                            composeView.disposeComposition()
                        } catch (e: Exception) {}
                        return@withContext
                    }

                    try {
                        windowManager.addView(composeView, params)
                        floatingButtonParams = params
                        floatingButtonView = composeView
                    } catch (e: Exception) {
                        try {
                            composeView.disposeComposition()
                        } catch (ex: Exception) {}
                        floatingButtonView = null
                        floatingButtonParams = null
                        floatingCreationInProgress.set(false)
                        Log.e(TAG, "Failed to attach floating button overlay transactionally: ${e.message}")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                floatingCreationInProgress.set(false)
            }
        }
    }

    private fun updateFloatingButtonLayout() {
        val view = floatingButtonView ?: return
        val params = floatingButtonParams ?: return
        val metrics = resources.displayMetrics

        // Trigger dynamic layout updates
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating floating button layout: ${e.message}")
        }
    }

    private fun setOverlayVisibility(visible: Boolean) {
        val fView = floatingButtonView
        val aView = answerPopupView
        val sView = statusBubbleView

        if (visible) {
            fView?.visibility = View.VISIBLE
            aView?.visibility = View.VISIBLE
            sView?.visibility = View.VISIBLE
        } else {
            fView?.visibility = View.GONE
            aView?.visibility = View.GONE
            sView?.visibility = View.GONE
        }
    }

    private var statusBubbleView: ComposeView? = null

    private fun showStatusBubble(text: String, durationMs: Long) {
        removeStatusBubble()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            // Phase 11: Use dp-to-pixel conversion instead of raw pixel value.
            y = (120 * resources.displayMetrics.density + 0.5f).toInt()
        }

        val backgroundStyle = currentStagedStatusBackground.value

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(floatingLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(floatingLifecycleOwner)
            setContent {
                val bgModifier = when (backgroundStyle) {
                    "Dark" -> Modifier.background(Color(0xE0111111), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 10.dp)
                    "Light" -> Modifier.background(Color(0xF0FAFAFA), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 10.dp)
                    else -> Modifier.padding(horizontal = 16.dp, vertical = 10.dp) // "None"
                }

                val textShadow = if (backgroundStyle == "None") {
                    androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.8f),
                        offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                        blurRadius = 4f
                    )
                } else {
                    null
                }

                val textColor = if (backgroundStyle == "Light") Color.Black else Color.White

                Box(
                    modifier = bgModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        color = textColor,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            shadow = textShadow
                        )
                    )
                }
            }
        }

        try {
            windowManager.addView(composeView, params)
            statusBubbleView = composeView
        } catch (e: Exception) {
            try {
                composeView.disposeComposition()
            } catch (ex: Exception) {}
            statusBubbleView = null
            Log.e(TAG, "Failed to attach status bubble overlay transactionally: ${e.message}")
        }

        serviceScope.launch {
            delay(durationMs)
            if (statusBubbleView == composeView) {
                removeStatusBubble()
            }
        }
    }

    private fun removeStatusBubble() {
        val view = statusBubbleView
        if (view != null) {
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                // ignore
            }
            try {
                view.disposeComposition()
            } catch (e: Exception) {
                // ignore
            }
            statusBubbleView = null
        }
    }

    private suspend fun capturePreparedScreenshot(
        purpose: CapturePurpose,
        timestamp: String
    ): PreparedScreenshot {
        removeAnswerPopup()
        resizeVirtualDisplayIfNeeded()

        try {
            val bitmap = captureScreen() ?: throw Exception("Captured bitmap is null")
            updateJournalStage("SCREENSHOT_COMPLETED")

            lastCapturePurpose = purpose.name
            lastCaptureDimensions = "${bitmap.width}×${bitmap.height} px"

            val saveEnabled = preferencesRepository.saveScreenshotsFlow.first()
            val galleryQuality = preferencesRepository.galleryJpegQualityFlow.first()
            val apiQuality = preferencesRepository.apiJpegQualityFlow.first()
            val maxDim = preferencesRepository.screenshotMaxDimensionFlow.first()

            val galleryJpegBytes = if (saveEnabled) {
                java.io.ByteArrayOutputStream().use { stream ->
                    val success = bitmap.compress(Bitmap.CompressFormat.JPEG, galleryQuality, stream)
                    if (!success) throw Exception("Gallery JPEG compression failed")
                    stream.toByteArray()
                }
            } else {
                null
            }

            val scaledBitmap = resizeBitmap(bitmap, maxDim)
            val apiJpegBytes = java.io.ByteArrayOutputStream().use { stream ->
                val success = scaledBitmap.compress(Bitmap.CompressFormat.JPEG, apiQuality, stream)
                if (!success) throw Exception("API JPEG compression failed")
                stream.toByteArray()
            }

            val width = bitmap.width
            val height = bitmap.height

            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            bitmap.recycle()

            lastJpegSize = "${String.format(java.util.Locale.US, "%.2f", apiJpegBytes.size / 1024.0)} KB"

            return PreparedScreenshot(
                apiJpegBytes = apiJpegBytes,
                galleryJpegBytes = galleryJpegBytes,
                width = width,
                height = height,
                capturedAt = System.currentTimeMillis(),
                purpose = purpose
            )
        } finally {
            setOverlayVisibility(true)
        }
    }

    private fun handleLongPressCapture() {
        lastGestureType = "Long Press"
        serviceScope.launch {
            val action = reserveLongPressAction()
            when (action) {
                LongPressAction.CAPTURE_FIRST -> {
                    captureFirstStagedImage()
                }
                LongPressAction.CAPTURE_SECOND -> {
                    captureSecondStagedImageAndAnalyze()
                }
                LongPressAction.IGNORE -> {
                    // Do nothing
                }
            }
        }
    }

    private fun handleSingleTapCapture() {
        lastGestureType = "Single Tap"
        serviceScope.launch {
            val action = reserveSingleTapAction()
            when (action) {
                SingleTapAction.PERFORM_SINGLE -> {
                    performSingleTapCaptureAndAnalyze()
                }
                SingleTapAction.SHOW_GUIDANCE -> {
                    val durationMs = preferencesRepository.pendingStatusDurationMsFlow.first().toLong()
                    showStatusBubble("Tahan untuk gambar 2", durationMs)
                }
                SingleTapAction.IGNORE -> {
                    // Do nothing
                }
            }
        }
    }

    enum class LongPressAction {
        CAPTURE_FIRST,
        CAPTURE_SECOND,
        IGNORE
    }

    private suspend fun reserveLongPressAction(): LongPressAction =
        stagedCaptureMutex.withLock {
            if (!twoImageCaptureEnabled) {
                showStatusBubble("Mode dua gambar dinonaktifkan", 1500L)
                return@withLock LongPressAction.IGNORE
            }
            if (isProcessing.value) {
                Log.d(TAG, "Already processing, ignore long press")
                return@withLock LongPressAction.IGNORE
            }
            when (captureWorkflowState.value) {
                CaptureWorkflowState.IDLE -> {
                    isProcessing.value = true
                    updateWorkflowState(CaptureWorkflowState.CAPTURING_FIRST)
                    LongPressAction.CAPTURE_FIRST
                }
                CaptureWorkflowState.WAITING_FOR_SECOND -> {
                    isProcessing.value = true
                    updateWorkflowState(CaptureWorkflowState.CAPTURING_SECOND)
                    timeoutJob?.cancel()
                    timeoutJob = null
                    LongPressAction.CAPTURE_SECOND
                }
                else -> LongPressAction.IGNORE
            }
        }

    enum class SingleTapAction {
        PERFORM_SINGLE,
        SHOW_GUIDANCE,
        IGNORE
    }

    private suspend fun reserveSingleTapAction(): SingleTapAction =
        stagedCaptureMutex.withLock {
            if (isProcessing.value) {
                Log.d(TAG, "Already processing, ignore single tap")
                return@withLock SingleTapAction.IGNORE
            }
            when (captureWorkflowState.value) {
                CaptureWorkflowState.IDLE -> {
                    isProcessing.value = true
                    updateWorkflowState(CaptureWorkflowState.CAPTURING_SINGLE)
                    SingleTapAction.PERFORM_SINGLE
                }
                CaptureWorkflowState.WAITING_FOR_SECOND -> {
                    SingleTapAction.SHOW_GUIDANCE
                }
                else -> SingleTapAction.IGNORE
            }
        }

    private suspend fun captureFirstStagedImage() {
        try {
            val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
            val timestamp = sdf.format(java.util.Date())
            sharedCaptureTimestamp = timestamp

            val prepared = capturePreparedScreenshot(
                purpose = CapturePurpose.STAGED_PART_1,
                timestamp = timestamp
            )

            val currentSessionId = java.util.UUID.randomUUID().toString()

            stagedCaptureMutex.withLock {
                pendingFirstImage = PendingStagedCapture(
                    sessionId = currentSessionId,
                    jpegBytes = prepared.apiJpegBytes,
                    width = prepared.width,
                    height = prepared.height,
                    capturedAt = prepared.capturedAt,
                    gallerySaveState = GallerySaveState.PENDING
                )
                updateWorkflowState(CaptureWorkflowState.WAITING_FOR_SECOND)
            }

            updateJournalStage("STAGED_PART1_SAVED")

            val timeoutSec = preferencesRepository.twoImageTimeoutSecFlow.first()
            scheduleStagedTimeout(timeoutSec, currentSessionId)

            setOverlayVisibility(true)

            val durationMs = preferencesRepository.pendingStatusDurationMsFlow.first().toLong()
            showStatusBubble("1/2 tersimpan", durationMs)

            if (prepared.galleryJpegBytes != null) {
                serviceScope.launch(Dispatchers.IO) {
                    val galleryResult = com.example.data.savePreparedJpegToGallery(
                        applicationContext,
                        prepared.galleryJpegBytes,
                        "ScreenPilot_${timestamp}_Part1"
                    )
                    stagedCaptureMutex.withLock {
                        val currentPending = pendingFirstImage
                        if (currentPending != null && currentPending.sessionId == currentSessionId && currentPending.gallerySaveState == GallerySaveState.PENDING) {
                            val nextState = when (galleryResult) {
                                is com.example.data.GallerySaveResult.Success -> {
                                    lastImage1UriAvailable = "Yes"
                                    lastGallerySaveResult = "Success: ${galleryResult.uri}"
                                    GallerySaveState.SAVED
                                }
                                is com.example.data.GallerySaveResult.Failure -> {
                                    lastImage1UriAvailable = "No"
                                    lastGallerySaveResult = "Failure: ${galleryResult.reason.name}"
                                    GallerySaveState.FAILED
                                }
                            }
                            pendingFirstImage = currentPending.copy(gallerySaveState = nextState)

                            if (nextState == GallerySaveState.FAILED) {
                                serviceScope.launch(Dispatchers.Main) {
                                    showStatusBubble("Gambar 1 siap • album gagal", durationMs)
                                }
                            }
                        }
                    }
                }
            }

        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "Error in captureFirstStagedImage", e)
            stagedCaptureMutex.withLock {
                pendingFirstImage = null
                updateWorkflowState(CaptureWorkflowState.IDLE)
            }
            lastFailedStage = "CAPTURING_FIRST"
            lastSanitizedError = e.message ?: "Unknown capturing error"

            if (e is CaptureTimeoutException) {
                showStatusBubble("Waktu tangkapan habis • coba lagi", 1500L)
            } else {
                val msg = e.message ?: ""
                if (msg.contains("compress") || msg.contains("preparing") || msg.contains("menyiapkan")) {
                    showStatusBubble("Gagal menyiapkan gambar 1", 1500L)
                } else {
                    showStatusBubble("Gagal mengambil gambar 1", 1500L)
                }
            }
        } finally {
            isProcessing.value = false
        }
    }

    private suspend fun captureSecondStagedImageAndAnalyze() {
        val firstImage = stagedCaptureMutex.withLock { pendingFirstImage }
        if (firstImage == null) {
            Log.e(TAG, "No first image pending when trying to capture second!")
            stagedCaptureMutex.withLock {
                updateWorkflowState(CaptureWorkflowState.IDLE)
            }
            isProcessing.value = false
            return
        }

        val timestamp = sharedCaptureTimestamp ?: java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())

        try {
            val prepared = capturePreparedScreenshot(
                purpose = CapturePurpose.STAGED_PART_2,
                timestamp = timestamp
            )

            if (prepared.galleryJpegBytes != null) {
                launchNonCritical("saveStagedBitmap2ToGallery") {
                    val galleryResult = com.example.data.savePreparedJpegToGallery(
                        applicationContext,
                        prepared.galleryJpegBytes,
                        "ScreenPilot_${timestamp}_Part2"
                    )
                    lastGallerySaveResult = when (galleryResult) {
                        is com.example.data.GallerySaveResult.Success -> {
                            lastImage2UriAvailable = "Yes"
                            "Success: ${galleryResult.uri}"
                        }
                        is com.example.data.GallerySaveResult.Failure -> {
                            lastImage2UriAvailable = "No"
                            "Failure: ${galleryResult.reason.name}"
                        }
                    }
                }
            }

            stagedCaptureMutex.withLock {
                updateWorkflowState(CaptureWorkflowState.ANALYZING_TWO_IMAGES)
            }

            val geminiBaseUrl = preferencesRepository.geminiBaseUrlFlow.first()
            val selectedModel = preferencesRepository.geminiModelFlow.first()

            val requestContext = AnalysisRequestContext(
                provider = AiProvider.GEMINI,
                requestedModel = selectedModel,
                normalizedBaseUrl = geminiBaseUrl,
                jpegBytes = firstImage.jpegBytes,
                imageWidth = firstImage.width,
                imageHeight = firstImage.height,
                requestStartedAt = System.currentTimeMillis(),
                isStagedTwoImage = true,
                jpegBytesPart2 = prepared.apiJpegBytes,
                imageWidthPart2 = prepared.width,
                imageHeightPart2 = prepared.height
            )

            val ageMs = System.currentTimeMillis() - firstImage.capturedAt
            lastImage1Age = "${String.format(java.util.Locale.US, "%.1f", ageMs / 1000.0)}s"

            runGeminiRequestChain(requestContext, "Staged")

        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "Error in captureSecondStagedImageAndAnalyze", e)
            lastFailedStage = "CAPTURING_SECOND"
            lastSanitizedError = e.message ?: "Unknown capturing error"

            stagedCaptureMutex.withLock {
                updateWorkflowState(CaptureWorkflowState.WAITING_FOR_SECOND)
            }

            if (e is CaptureTimeoutException) {
                showStatusBubble("Waktu tangkapan habis • coba lagi", 1500L)
            } else {
                val msg = e.message ?: ""
                if (msg.contains("compress") || msg.contains("preparing") || msg.contains("menyiapkan")) {
                    showStatusBubble("Gagal menyiapkan gambar 2 • tahan lagi", 1500L)
                } else {
                    showStatusBubble("Gagal mengambil gambar 2 • tahan lagi", 1500L)
                }
            }

            val timeoutSec = preferencesRepository.twoImageTimeoutSecFlow.first()
            scheduleStagedTimeout(timeoutSec, firstImage.sessionId)

            isProcessing.value = false
        }
    }

    private suspend fun performSingleTapCaptureAndAnalyze() {
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())

        try {
            val prepared = capturePreparedScreenshot(
                purpose = CapturePurpose.SINGLE,
                timestamp = timestamp
            )

            if (prepared.galleryJpegBytes != null) {
                launchNonCritical("saveSingleToGallery") {
                    val galleryResult = com.example.data.savePreparedJpegToGallery(
                        applicationContext,
                        prepared.galleryJpegBytes,
                        "ScreenPilot_${timestamp}"
                    )
                    lastGallerySaveResult = when (galleryResult) {
                        is com.example.data.GallerySaveResult.Success -> {
                            "Success: ${galleryResult.uri}"
                        }
                        is com.example.data.GallerySaveResult.Failure -> {
                            "Failure: ${galleryResult.reason.name}"
                        }
                    }
                }
            }

            stagedCaptureMutex.withLock {
                updateWorkflowState(CaptureWorkflowState.ANALYZING_SINGLE)
            }

            val geminiBaseUrl = preferencesRepository.geminiBaseUrlFlow.first()
            val selectedModel = preferencesRepository.geminiModelFlow.first()

            val requestContext = AnalysisRequestContext(
                provider = AiProvider.GEMINI,
                requestedModel = selectedModel,
                normalizedBaseUrl = geminiBaseUrl,
                jpegBytes = prepared.apiJpegBytes,
                imageWidth = prepared.width,
                imageHeight = prepared.height,
                requestStartedAt = System.currentTimeMillis()
            )

            runGeminiRequestChain(requestContext, "Single")

        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "Error in performSingleTapCaptureAndAnalyze", e)
            stagedCaptureMutex.withLock {
                updateWorkflowState(CaptureWorkflowState.IDLE)
            }
            lastFailedStage = "Single Capture"
            lastSanitizedError = e.message ?: "Unknown capturing error"

            if (e is CaptureTimeoutException) {
                showStatusBubble("Waktu tangkapan habis • coba lagi", 1500L)
            } else {
                val msg = e.message ?: ""
                if (msg.contains("compress") || msg.contains("preparing") || msg.contains("menyiapkan") || msg.contains("Preparing")) {
                    showStatusBubble("Gagal menyiapkan gambar", 1500L)
                } else {
                    showStatusBubble("Gagal mengambil gambar", 1500L)
                }
            }
            isProcessing.value = false
        }
    }

    private fun scheduleStagedTimeout(seconds: Int, sessionId: String) {
        timeoutJob?.cancel()
        timeoutJob = serviceScope.launch {
            delay(seconds * 1000L)
            stagedCaptureMutex.withLock {
                val currentPending = pendingFirstImage
                if (currentPending != null && currentPending.sessionId == sessionId && captureWorkflowState.value == CaptureWorkflowState.WAITING_FOR_SECOND) {
                    pendingFirstImage = null
                    updateWorkflowState(CaptureWorkflowState.IDLE)
                    sharedCaptureTimestamp = null
                    showStatusBubble("Tangkapan 1/2 kedaluwarsa", 1000L)
                }
            }
        }
    }

    private suspend fun runGeminiRequestChain(
        requestContext: AnalysisRequestContext,
        captureMode: String
    ) {
        // Phase 11: Local per-analysis completion gate guarantees no cross-request race.
        val completionGate = AnalysisCompletionGate()

        val imageCount = if (requestContext.isStagedTwoImage) 2 else 1
        val selectedModel = requestContext.requestedModel
        val resolvedModel = "Google Gemini\n$selectedModel"
        val startTime = System.currentTimeMillis()

        var attemptsCount = 0
        var totalSameKeyRetries = 0
        var failoverUsed = false
        var successfulSlotId: String? = null
        var successfulKeyLabel: String? = null

        try {
            val slotsJson = try {
                preferencesRepository.geminiKeySlotsMetadataFlow.first()
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                Log.w(TAG, "DataStore geminiKeySlotsMetadata read failed: ${t.message}")
                ""
            }

            var slots = if (slotsJson.trim().isNotEmpty()) {
                val decoded = GeminiKeySlotSerializer.deserialize(slotsJson).toMutableList()
                if (decoded.isNotEmpty()) geminiKeySlotsSnapshot = decoded
                decoded
            } else if (geminiKeySlotsSnapshot.isNotEmpty()) {
                geminiKeySlotsSnapshot.toMutableList()
            } else {
                mutableListOf()
            }

            if (slots.isEmpty()) {
                val oldKey = withContext(Dispatchers.IO) {
                    KeyStoreHelper.getGeminiApiKey(applicationContext)
                }
                if (oldKey.trim().isNotEmpty()) {
                    val storeResult = withContext(Dispatchers.IO) {
                        KeyStoreHelper.storeSlotKey(applicationContext, "1", oldKey)
                    }
                    if (storeResult.isSuccess) {
                        val suffix = if (oldKey.trim().length > 4) "••••••••${oldKey.trim().takeLast(4)}" else "••••${oldKey.trim()}"
                        val firstSlot = GeminiKeySlot(
                            id = "1",
                            label = "Main",
                            enabled = true,
                            priority = 1,
                            maskedSuffix = suffix,
                            healthStatus = GeminiKeyHealth.READY.name
                        )
                        slots.add(firstSlot)
                        geminiKeySlotsSnapshot = slots
                        launchNonCritical("persistLegacyMigratedSlot") {
                            preferencesRepository.setGeminiKeySlotsMetadata(GeminiKeySlotSerializer.serialize(slots))
                        }
                    } else {
                        Log.e(TAG, "Legacy key migration failed during encrypted storage: ${storeResult.exceptionOrNull()?.message}")
                    }
                }
            }

            val enabledSlots = slots.filter { it.enabled }.sortedBy { it.priority }

            val now = System.currentTimeMillis()
            // Phase 3: Safe DataStore preference reads for all provider selection options
            val skipCooling = failoverPrefReader.safeSkipCoolingDown()
            val skipAuth = failoverPrefReader.safeSkipAuthFailed()
            val skipPerm = failoverPrefReader.safeSkipPermissionDenied()

            val eligibleSlots = enabledSlots.filter { slot ->
                if (skipCooling && slot.cooldownExpiration > now) return@filter false
                if (skipAuth && slot.lastFailureType == "401") return@filter false
                if (skipPerm && slot.lastFailureType == "403") return@filter false
                true
            }

            if (eligibleSlots.isEmpty()) {
                throw LocalPreparationException("No eligible Gemini API keys available. Please check your key pool.")
            }

            val strategy = failoverPrefReader.safeKeyStrategy()
            val lastSuccessId = failoverPrefReader.safeLastSuccessfulKeyId() ?: ""
            val lastRrIndex = failoverPrefReader.safeRoundRobinLastKeyIndex()

            val orderedSlots = when (strategy) {
                "Sticky Success with Sequential Failover" -> {
                    val stickyIndex = eligibleSlots.indexOfFirst { it.id == lastSuccessId }
                    if (stickyIndex >= 0) {
                        val list = mutableListOf<GeminiKeySlot>()
                        list.add(eligibleSlots[stickyIndex])
                        for (i in eligibleSlots.indices) {
                            if (i != stickyIndex) {
                                list.add(eligibleSlots[i])
                            }
                        }
                        list
                    } else {
                        eligibleSlots
                    }
                }
                "Always Start at Key 1" -> {
                    eligibleSlots
                }
                "Round Robin" -> {
                    if (eligibleSlots.isNotEmpty()) {
                        val startIndex = (lastRrIndex + 1) % eligibleSlots.size
                        val list = mutableListOf<GeminiKeySlot>()
                        for (i in eligibleSlots.indices) {
                            val idx = (startIndex + i) % eligibleSlots.size
                            list.add(eligibleSlots[idx])
                        }
                        list
                    } else {
                        eligibleSlots
                    }
                }
                else -> eligibleSlots
            }

            val maxAttempts = failoverPrefReader.safeMaxKeyAttempts()
            val attemptsLimit = kotlin.math.min(maxAttempts, orderedSlots.size)

            var responseJson: String? = null
            var successfulParsedAnswer: ParsedAnswer? = null
            successfulSlotId = null
            successfulKeyLabel = null
            totalSameKeyRetries = 0
            var lastError: Throwable? = null
            attemptsCount = 0

            for (attempt in 0 until attemptsLimit) {
                val slot = orderedSlots[attempt]
                // Phase 7: Android Keystore / AES-GCM / Base64 work must run on IO.
                val key = withContext(Dispatchers.IO) {
                    KeyStoreHelper.retrieveSlotKey(applicationContext, slot.id)
                }
                if (key.trim().isEmpty()) {
                    // Phase 6: Metadata writes are best-effort; DataStore failure must not
                    // break the key-rotation loop.
                    try {
                        updateSlotStatus(slot.id, health = GeminiKeyHealth.TEMPORARY_FAILURE.name, failureType = "Empty Key", cooldown = 0)
                    } catch (ce: CancellationException) { throw ce
                    } catch (t: Throwable) { Log.w(TAG, "Best-effort metadata update failed for empty-key slot ${slot.id}: ${t.message}") }
                    continue
                }

                attemptsCount++
                var rawRes: String? = null
                var parsedAnswer: ParsedAnswer? = null
                var keyAttemptError: Throwable? = null
                
                // Phase 5: Safe DataStore preference read
                val sameKeyRetryEnabled = failoverPrefReader.safeSameKeyRetryEnabled()

                var runAttempt = 1
                val maxRunAttempts = if (sameKeyRetryEnabled) 2 else 1
                
                while (runAttempt <= maxRunAttempts) {
                    try {
                        val res = GeminiProviderClient.executeImageRequest(requestContext, key)
                        val text = ResponseParser.extractGeminiText(res)
                        val parsed = ResponseParser.parse(text)
                        
                        rawRes = res
                        parsedAnswer = parsed
                        keyAttemptError = null
                        break
                    } catch (e: Throwable) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        keyAttemptError = e
                        
                        val isRetryable = sameKeyRetryEnabled && runAttempt < maxRunAttempts && (
                            e is SocketTimeoutException ||
                            (e is IOException && e.message?.contains("reset", ignoreCase = true) == true) ||
                            (e is ApiException && e.code in listOf(500, 502, 503, 504))
                        )
                        
                        if (isRetryable) {
                            runAttempt++
                            totalSameKeyRetries++
                            val delayMs = java.util.concurrent.ThreadLocalRandom.current().nextLong(800, 1501)
                            delay(delayMs)
                        } else {
                            break
                        }
                    }
                }

                if (rawRes != null && parsedAnswer != null) {
                    val successfulId = slot.id
                    val strategySnapshot = strategy
                    val eligibleSlotsSnapshot = eligibleSlots
                    launchNonCritical("updateKeySuccessMetadata") {
                        updateSlotSuccess(successfulId)
                        preferencesRepository.setLastSuccessfulKeyId(successfulId)
                        if (strategySnapshot == "Round Robin") {
                            val origIndex = eligibleSlotsSnapshot.indexOfFirst { it.id == successfulId }
                            if (origIndex >= 0) {
                                preferencesRepository.setRoundRobinLastKeyIndex(origIndex)
                            }
                        }
                    }
                    
                    if (attempt > 0) {
                        launchNonCritical("updateJournalFailover") {
                            updateJournalStage("PROVIDER_FAILOVER")
                        }
                    }
                    
                    responseJson = rawRes
                    successfulParsedAnswer = parsedAnswer
                    successfulSlotId = slot.id
                    successfulKeyLabel = slot.label
                    break
                } else {
                    val e = keyAttemptError ?: Exception("Unknown response failure")
                    lastError = e
                    
                    // Phase 5: Safe DataStore preference read inside failover loop
                    val cooldownSec = failoverPrefReader.safeCooldownDurationSec()
                    val action = FailoverDecision.evaluate(e, cooldownSec.toLong())
                    
                    // Phase 6: Wrap every slot-status update as best-effort so DataStore
                    // failures cannot halt the in-memory failover decision.
                    when (action) {
                        is FailoverAction.StopRotation -> {
                            try {
                                updateSlotStatus(
                                    slot.id,
                                    health = GeminiKeyHealth.TEMPORARY_FAILURE.name,
                                    failureType = if (e is ApiException) e.code.toString() else "Error",
                                    cooldown = 0L
                                )
                            } catch (ce: CancellationException) { throw ce
                            } catch (t: Throwable) { Log.w(TAG, "Best-effort slot-status (StopRotation) failed: ${t.message}") }
                            break
                        }
                        is FailoverAction.ContinueToNextKey -> {
                            try {
                                updateSlotStatus(
                                    slot.id,
                                    health = action.healthStatus,
                                    failureType = action.failureType,
                                    cooldown = action.cooldownMs
                                )
                            } catch (ce: CancellationException) { throw ce
                            } catch (t: Throwable) { Log.w(TAG, "Best-effort slot-status (ContinueToNextKey) failed: ${t.message}") }
                        }
                    }
                }
            }

            failoverUsed = attemptsCount > 1

            if (responseJson != null && successfulParsedAnswer != null) {
                val duration = System.currentTimeMillis() - startTime
                if (requestContext.isStagedTwoImage) {
                    updateJournalStage("STAGED_COMPLETED")
                } else {
                    updateJournalStage("GEMINI_ANSWER_COMPLETED")
                }
                processApiResponse(
                    responseJson = responseJson!!,
                    parsed = successfulParsedAnswer!!,
                    modelName = resolvedModel,
                    durationMs = duration,
                    statusCode = 200,
                    imageCount = imageCount,
                    captureMode = captureMode,
                    successfulKeySlotId = successfulSlotId,
                    successfulKeyLabel = successfulKeyLabel,
                    keyAttempts = attemptsCount,
                    sameKeyRetries = totalSameKeyRetries,
                    failoverUsed = failoverUsed
                )
            } else {
                if (lastError != null) {
                    throw lastError
                } else {
                    throw LocalPreparationException("No eligible Gemini keys succeeded.")
                }
            }
        } catch (e: CancellationException) {
            // Phase 4: Cancellation is not an analysis failure.
            // Re-throw without creating provider-error history, without showing '?',
            // without rotating to another key, and without marking a key unhealthy.
            throw e
        } catch (e: Throwable) {
            val duration = System.currentTimeMillis() - startTime
            val (errorMsg, statusCode) = classifyException(e)
            
            lastFailedStage = "runGeminiRequestChain API Call"
            lastSanitizedError = errorMsg

            processApiError(
                errorMsg = errorMsg,
                modelName = resolvedModel,
                durationMs = duration,
                statusCode = statusCode,
                imageCount = imageCount,
                captureMode = captureMode,
                keyAttempts = attemptsCount,
                sameKeyRetries = totalSameKeyRetries,
                failoverUsed = failoverUsed
            )

            val sanitizedForBubble = sanitizeErrorString(errorMsg)
            showStatusBubble(sanitizedForBubble, 3000L)
        } finally {
            // Phase 11: Single authoritative cleanup owner via local completionGate.
            withContext(NonCancellable) {
                if (completionGate.tryComplete()) {
                    finishAnalysisAndReturnToIdle(clearPendingStagedImages = requestContext.isStagedTwoImage)
                }
            }
        }
    }

    private suspend fun awaitFrameBoundary() {
        withContext(Dispatchers.Main.immediate) {
            kotlinx.coroutines.suspendCancellableCoroutine<Unit> { cont ->
                try {
                    android.view.Choreographer.getInstance().postFrameCallback {
                        if (cont.isActive) cont.resume(Unit) {}
                    }
                } catch (e: Exception) {
                    if (cont.isActive) cont.resume(Unit) {}
                }
            }
        }
    }

    private suspend fun captureScreen(): Bitmap? {
        return captureSurfaceMutex.withLock {
            if (!isInfrastructureHealthy) return@withLock null
            val reader = imageReader ?: return@withLock null
            val handler = imageHandler ?: return@withLock null

            // 1. Drain existing stale queued images BEFORE registering listener
            var staleImage: android.media.Image?
            do {
                staleImage = try { reader.acquireLatestImage() } catch (e: Exception) { null }
                staleImage?.close()
            } while (staleImage != null)

            // 2. Reset FreshFrameReadinessGate before registering pre-arm listener
            freshFrameGate.reset()
            val bitmapDeferred = kotlinx.coroutines.CompletableDeferred<Bitmap>()

            // Pre-arm listener: drops and closes any frame arriving before handler barrier task executes
            val preArmListener = ImageReader.OnImageAvailableListener { r ->
                try {
                    val img = r.acquireLatestImage()
                    img?.close()
                } catch (e: Exception) {}
            }
            reader.setOnImageAvailableListener(preArmListener, handler)

            try {
                // 3. Hide overlays on Main thread
                popupAttachmentCoordinator.invalidate()
                withContext(Dispatchers.Main.immediate) {
                    removeAnswerPopup()
                    setOverlayVisibility(false)
                }

                // 4. Await UI frame boundary
                awaitFrameBoundary()

                // 5. Post ARMING BARRIER TASK onto imageHandler queue
                val armedDeferred = kotlinx.coroutines.CompletableDeferred<Unit>()
                handler.post {
                    try {
                        // Drain any remaining pre-barrier images
                        var img: android.media.Image?
                        do {
                            img = try { reader.acquireLatestImage() } catch (e: Exception) { null }
                            img?.close()
                        } while (img != null)

                        // Arm gate and transition epoch
                        freshFrameGate.arm()
                        val postArmEpoch = freshFrameGate.generation()

                        // Post-arm listener: processes the first fresh frame arriving in post-arm epoch
                        val postArmListener = ImageReader.OnImageAvailableListener { r ->
                            var image: android.media.Image? = null
                            try {
                                image = r.acquireLatestImage()
                                if (image == null) return@OnImageAvailableListener

                                if (!freshFrameGate.isArmed() || freshFrameGate.generation() != postArmEpoch) {
                                    image.close()
                                    return@OnImageAvailableListener
                                }

                                if (bitmapDeferred.isCompleted) {
                                    image.close()
                                    return@OnImageAvailableListener
                                }

                                val planes = image.planes
                                val buffer = planes[0].buffer
                                val pixelStride = planes[0].pixelStride
                                val rowStride = planes[0].rowStride
                                val width = image.width
                                val height = image.height

                                val rowPadding = rowStride - pixelStride * width
                                val tempBitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
                                buffer.rewind()
                                tempBitmap.copyPixelsFromBuffer(buffer)

                                val bitmap = Bitmap.createBitmap(tempBitmap, 0, 0, width, height)
                                tempBitmap.recycle()

                                if (!bitmapDeferred.complete(bitmap)) {
                                    bitmap.recycle()
                                }
                            } catch (e: Exception) {
                                if (!bitmapDeferred.isCompleted) {
                                    Log.e(TAG, "Error in fresh frame capture: ${e.message}", e)
                                    bitmapDeferred.completeExceptionally(e)
                                }
                            } finally {
                                image?.close()
                            }
                        }

                        reader.setOnImageAvailableListener(postArmListener, handler)
                        armedDeferred.complete(Unit)
                    } catch (e: Exception) {
                        armedDeferred.completeExceptionally(e)
                    }
                }

                // 6. Await arming barrier task execution
                armedDeferred.await()

                // 7. Await next POST-ARM image callback
                val timeoutMs = 1500L
                val bitmap = kotlinx.coroutines.withTimeout(timeoutMs) {
                    bitmapDeferred.await()
                }
                return bitmap
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                throw CaptureTimeoutException("Waktu tangkapan habis • coba lagi")
            } finally {
                reader.setOnImageAvailableListener(null, null)
            }
        }
    }

    private fun classifyException(e: Throwable): Pair<String, Int> {
        return when (e) {
            is LocalPreparationException -> {
                Pair("Local Preparation Error: ${e.message}", 0)
            }
            is UnknownHostException -> {
                Pair("Network Connection Error: DNS lookup failed.", 0)
            }
            is SocketTimeoutException -> {
                Pair("Network Connection Error: Connection timed out.", 0)
            }
            is SSLException -> {
                Pair("Network Connection Error: TLS connection failed.", 0)
            }
            is ConnectException -> {
                Pair("Network Connection Error: Internet connection unavailable.", 0)
            }
            is ApiException -> {
                Pair("Provider Response Error: HTTP ${e.code}\n${e.message}", e.code)
            }
            is IOException -> {
                val msg = e.message ?: ""
                val cleanMsg = if (msg.isEmpty()) "Connection reset" else msg
                Pair("Network Connection Error: $cleanMsg", 0)
            }
            is org.json.JSONException -> {
                Pair("Provider Response Error: JSON parsing failed.", 0)
            }
            is java.util.concurrent.CancellationException -> {
                Pair("Request Cancelled: Operation cancelled.", 0)
            }
            else -> {
                Pair("Provider Response Error: ${e.message ?: "Unexpected error"}", 0)
            }
        }
    }

    private suspend fun updateSlotSuccess(slotId: String) {
        val json = preferencesRepository.geminiKeySlotsMetadataFlow.first()
        val slots = GeminiKeySlotSerializer.deserialize(json).toMutableList()
        val index = slots.indexOfFirst { it.id == slotId }
        if (index >= 0) {
            val s = slots[index]
            slots[index] = s.copy(
                healthStatus = GeminiKeyHealth.READY.name,
                lastSuccessTimestamp = System.currentTimeMillis(),
                lastFailureType = "",
                cooldownExpiration = 0L
            )
            preferencesRepository.setGeminiKeySlotsMetadata(GeminiKeySlotSerializer.serialize(slots))
        }
    }

    private suspend fun updateSlotStatus(slotId: String, health: String, failureType: String, cooldown: Long) {
        val json = preferencesRepository.geminiKeySlotsMetadataFlow.first()
        val slots = GeminiKeySlotSerializer.deserialize(json).toMutableList()
        val index = slots.indexOfFirst { it.id == slotId }
        if (index >= 0) {
            val s = slots[index]
            val cooldownExp = if (cooldown > 0) System.currentTimeMillis() + cooldown else 0L
            slots[index] = s.copy(
                healthStatus = health,
                lastFailureType = failureType,
                cooldownExpiration = cooldownExp
            )
            preferencesRepository.setGeminiKeySlotsMetadata(GeminiKeySlotSerializer.serialize(slots))
        }
    }

    private suspend fun finishAnalysisAndReturnToIdle(clearPendingStagedImages: Boolean) {
        stagedCaptureMutex.withLock {
            if (clearPendingStagedImages) {
                pendingFirstImage = null
                sharedCaptureTimestamp = null
            }
            val cur = captureWorkflowState.value
            if (cur != CaptureWorkflowState.IDLE && cur != CaptureWorkflowState.WAITING_FOR_SECOND) {
                updateWorkflowState(CaptureWorkflowState.IDLE)
            }
        }
        withContext(Dispatchers.Main.immediate) {
            setOverlayVisibility(true)
        }
        isProcessing.value = false
    }

    private suspend fun processApiResponse(
        responseJson: String,
        parsed: ParsedAnswer,
        modelName: String,
        durationMs: Long,
        statusCode: Int,
        imageCount: Int = 1,
        captureMode: String = "Single",
        successfulKeySlotId: String? = null,
        successfulKeyLabel: String? = null,
        keyAttempts: Int = 0,
        sameKeyRetries: Int = 0,
        failoverUsed: Boolean = false
    ) {
        // A parsed UNCLEAR result is a valid provider response, not a key failure.
        // Presentation depends only on the parsed answer type:
        // MC -> existing overlay popup, FREE_RESPONSE -> silent notification,
        // UNCLEAR -> no fabricated answer and no provider failover.
        val historyQuestionType: String
        val historyAnswerIndex: Int
        val historyAnswerText: String?

        when (parsed) {
            is ParsedAnswer.MultipleChoice -> {
                val popupResult = showAnswerPopup(
                    parsed.answerIndex.toString(),
                    parsed.confidence
                )
                if (popupResult is PopupAttachmentResult.Failed) {
                    lastFailedStage = "PopupAttachmentFailed: ${popupResult.safeReason}"
                    Log.w(TAG, "Answer popup attachment failed: ${popupResult.safeReason}")
                }
                historyQuestionType = HistoryQuestionType.MULTIPLE_CHOICE
                historyAnswerIndex = parsed.answerIndex
                historyAnswerText = null
            }

            is ParsedAnswer.FreeResponse -> {
                when (val notificationResult = EssayAnswerNotificationManager.showAnswer(
                    applicationContext,
                    parsed.answerText
                )) {
                    EssayNotificationResult.Posted -> Unit
                    EssayNotificationResult.PermissionDenied -> {
                        Log.w(TAG, "Essay answer notification skipped: notification permission unavailable")
                    }
                    is EssayNotificationResult.Failed -> {
                        Log.w(TAG, "Essay answer notification failed: ${notificationResult.safeReason}")
                    }
                }
                historyQuestionType = HistoryQuestionType.FREE_RESPONSE
                historyAnswerIndex = 0
                historyAnswerText = parsed.answerText
            }

            is ParsedAnswer.Unclear -> {
                // The model responded successfully but the visible content is not enough
                // to classify/answer safely. Do not rotate keys and do not fabricate any
                // popup/notification answer. Keep only a diagnostic + history record.
                Log.i(TAG, "Question classified as UNCLEAR; no answer surface shown")
                historyQuestionType = HistoryQuestionType.UNCLEAR
                historyAnswerIndex = 0
                historyAnswerText = null
            }
        }

        // The single authoritative workflow cleanup owner remains
        // runGeminiRequestChain.finally via AnalysisCompletionGate.
        val capturedConfidence = parsed.confidence
        launchNonCritical("saveHistorySuccess") {
            val historyLimit = failoverPrefReader.safeHistoryLimit()
            val entry = HistoryEntry(
                answerIndex = historyAnswerIndex,
                confidence = capturedConfidence,
                modelName = modelName,
                timestamp = System.currentTimeMillis(),
                requestDurationMs = durationMs,
                httpStatus = statusCode,
                errorMessage = null,
                imageCount = imageCount,
                captureMode = captureMode,
                successfulKeySlotId = successfulKeySlotId,
                successfulKeyLabel = successfulKeyLabel,
                keyAttempts = keyAttempts,
                sameKeyRetries = sameKeyRetries,
                failoverUsed = failoverUsed,
                questionType = historyQuestionType,
                answerText = historyAnswerText
            )
            historyRepository.insert(entry, historyLimit)
        }
    }

    private suspend fun processApiError(
        errorMsg: String,
        modelName: String,
        durationMs: Long,
        statusCode: Int,
        imageCount: Int = 1,
        captureMode: String = "Single",
        keyAttempts: Int = 0,
        sameKeyRetries: Int = 0,
        failoverUsed: Boolean = false
    ) {
        val sanitizedError = sanitizeErrorString(errorMsg)
        launchNonCritical("saveHistoryError") {
            val historyLimit = failoverPrefReader.safeHistoryLimit()
            val entry = HistoryEntry(
                answerIndex = -1,
                confidence = null,
                modelName = modelName,
                timestamp = System.currentTimeMillis(),
                requestDurationMs = durationMs,
                httpStatus = statusCode,
                errorMessage = sanitizedError,
                imageCount = imageCount,
                captureMode = captureMode,
                successfulKeySlotId = null,
                successfulKeyLabel = null,
                keyAttempts = keyAttempts,
                sameKeyRetries = sameKeyRetries,
                failoverUsed = failoverUsed,
                questionType = HistoryQuestionType.ERROR,
                answerText = null
            )
            historyRepository.insert(entry, historyLimit)
        }

        val showSymbol = failoverPrefReader.safeDisplayErrorSymbol()
        if (showSymbol) {
            showAnswerPopup("?")
        }
    }

    private fun sanitizeErrorString(errorMsg: String): String {
        var s = errorMsg
        if (s.contains("Bearer")) {
            s = s.replace("""Bearer\s+[A-Za-z0-9_-]+""".toRegex(), "Bearer [REDACTED]")
        }
        if (s.length > 300) {
            s = s.substring(0, 300) + "..."
        }
        return s
    }

    private fun showError(msg: String) {
        serviceScope.launch {
            val showSymbol = failoverPrefReader.safeDisplayErrorSymbol()
            if (showSymbol) {
                showAnswerPopup("?")
            }
        }
    }

    private fun dpToPx(dp: Float): Int = DimensionUtils.dpToPx(dp, resources.displayMetrics.density)

    // Phase 3 & 8 & 10: showAnswerPopup is suspend, uses generation tokens, converts dp to px,
    // and returns a PopupAttachmentResult so attachment failures do not re-trigger key failover.
    private suspend fun showAnswerPopup(answer: String, confidence: Double? = null): PopupAttachmentResult {
        if (shutdownInProgress.get()) return PopupAttachmentResult.Invalidated

        val token = popupAttachmentCoordinator.nextToken()

        val style = currentPopupStyle.value ?: preferencesRepository.popupStyleSnapshotFlow.first()
        currentPopupStyle.value = style
        val dismissTimeoutMs = failoverPrefReader.safeDismissTimeoutSec() * 1000L

        return withContext(Dispatchers.Main.immediate) {
            if (shutdownInProgress.get() || !popupAttachmentCoordinator.isValid(token)) {
                return@withContext PopupAttachmentResult.Invalidated
            }

            removeAnswerPopup()

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                // Phase 8: Convert bottomOffsetDp to px
                y = dpToPx(style.bottomOffsetDp)
            }

            val composeView = ComposeView(this@ScreenCaptureService).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setViewTreeLifecycleOwner(answerLifecycleOwner)
                setViewTreeSavedStateRegistryOwner(answerLifecycleOwner)
                setContent {
                    val isDark = when (style.backgroundTheme) {
                        com.example.data.PopupBackgroundTheme.DARK -> true
                        com.example.data.PopupBackgroundTheme.LIGHT -> false
                        com.example.data.PopupBackgroundTheme.AUTO_CONTRAST -> true
                    }
                    val bgColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
                    val defaultTextColor = if (isDark) Color.White else Color.Black

                    val textColor = when (style.textColorMode) {
                        com.example.data.PopupTextColorMode.WHITE -> Color.White
                        com.example.data.PopupTextColorMode.BLACK -> Color.Black
                        com.example.data.PopupTextColorMode.AUTO -> {
                            if (style.backgroundTheme == com.example.data.PopupBackgroundTheme.AUTO_CONTRAST) Color.White else defaultTextColor
                        }
                    }

                    val fontWeightVal = when (style.fontWeight) {
                        com.example.data.PopupFontWeight.NORMAL -> FontWeight.Normal
                        com.example.data.PopupFontWeight.MEDIUM -> FontWeight.Medium
                        com.example.data.PopupFontWeight.SEMI_BOLD -> FontWeight.SemiBold
                        com.example.data.PopupFontWeight.BOLD -> FontWeight.Bold
                    }

                    val scaledFontSize = (style.fontSizeSp * style.popupScale).sp
                    val scaledHorizontalPadding = (style.horizontalPaddingDp * style.popupScale).dp
                    val scaledVerticalPadding = (style.verticalPaddingDp * style.popupScale).dp
                    val scaledCornerRadius = (style.cornerRadiusDp * style.popupScale).dp

                    val finalShape = when (style.popupStyle) {
                        com.example.data.PopupStyle.CIRCLE -> CircleShape
                        com.example.data.PopupStyle.PILL -> CircleShape
                        com.example.data.PopupStyle.TEXT_ONLY -> RoundedCornerShape(0.dp)
                        com.example.data.PopupStyle.COMPACT_ROUNDED -> RoundedCornerShape(scaledCornerRadius)
                    }

                    val showBackground = style.popupStyle != com.example.data.PopupStyle.TEXT_ONLY

                    Box(
                        modifier = Modifier
                            .clickable { removeAnswerPopup() }
                            .padding(8.dp)
                            .testTag("answer_popup"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (style.popupStyle == com.example.data.PopupStyle.CIRCLE) {
                            val circleSize = (style.fontSizeSp * 2 * style.popupScale).dp
                            Box(
                                modifier = Modifier
                                    .size(circleSize)
                                    .clip(CircleShape)
                                    .background(if (showBackground) bgColor.copy(alpha = style.backgroundOpacity) else Color.Transparent)
                                    .then(
                                        if (showBackground) Modifier.border(1.dp, Color.Gray.copy(alpha = 0.5f * style.backgroundOpacity), CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = answer,
                                        fontSize = scaledFontSize,
                                        fontWeight = fontWeightVal,
                                        color = textColor.copy(alpha = style.textOpacity)
                                    )
                                    if (style.showConfidence && confidence != null) {
                                        Text(
                                            text = "${(confidence * 100).toInt()}%",
                                            fontSize = (scaledFontSize.value * 0.45f).sp,
                                            fontWeight = FontWeight.Normal,
                                            color = textColor.copy(alpha = style.textOpacity * 0.7f)
                                        )
                                    }
                                }
                            }
                        } else {
                            val actualHorizontalPadding = if (style.popupStyle == com.example.data.PopupStyle.PILL) {
                                scaledHorizontalPadding * 1.5f
                            } else {
                                scaledHorizontalPadding
                            }
                            Surface(
                                shape = finalShape,
                                color = if (showBackground) bgColor.copy(alpha = style.backgroundOpacity) else Color.Transparent,
                                border = if (showBackground) BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f * style.backgroundOpacity)) else null,
                                shadowElevation = if (showBackground) 8.dp else 0.dp,
                                modifier = Modifier.wrapContentSize()
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(
                                        horizontal = actualHorizontalPadding,
                                        vertical = scaledVerticalPadding
                                    )
                                ) {
                                    Text(
                                        text = answer,
                                        fontSize = scaledFontSize,
                                        fontWeight = fontWeightVal,
                                        color = textColor.copy(alpha = style.textOpacity)
                                    )
                                    if (style.showConfidence && confidence != null) {
                                        Spacer(modifier = Modifier.height((2 * style.popupScale).dp))
                                        Text(
                                            text = "${(confidence * 100).toInt()}%",
                                            fontSize = (scaledFontSize.value * 0.5f).sp,
                                            fontWeight = FontWeight.Normal,
                                            color = textColor.copy(alpha = style.textOpacity * 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            composeView.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    removeAnswerPopup()
                    return@setOnTouchListener true
                }
                false
            }

            if (!popupAttachmentCoordinator.isValid(token)) {
                try { composeView.disposeComposition() } catch (ex: Exception) {}
                return@withContext PopupAttachmentResult.Invalidated
            }

            try {
                windowManager.addView(composeView, params)
                if (!popupAttachmentCoordinator.isValid(token)) {
                    try { windowManager.removeView(composeView) } catch (ex: Exception) {}
                    try { composeView.disposeComposition() } catch (ex: Exception) {}
                    return@withContext PopupAttachmentResult.Invalidated
                }
                answerPopupView = composeView
                answerPopupParams = params
                mainHandler.removeCallbacks(dismissRunnable)
                mainHandler.postDelayed(dismissRunnable, dismissTimeoutMs)
                PopupAttachmentResult.Attached
            } catch (e: Exception) {
                try { composeView.disposeComposition() } catch (ex: Exception) {}
                answerPopupView = null
                answerPopupParams = null
                Log.e(TAG, "Failed to attach answer popup overlay: ${e.message?.take(120)}")
                PopupAttachmentResult.Failed(e.message ?: "Attachment failure")
            }
        }
    }

    private fun removeAnswerPopup() {
        mainHandler.removeCallbacks(dismissRunnable)
        val view = answerPopupView
        if (view != null) {
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                // ignore
            }
            try {
                view.disposeComposition()
            } catch (e: Exception) {
                // ignore
            }
            answerPopupView = null
            answerPopupParams = null
        }
    }

    private fun removeFloatingButton() {
        val view = floatingButtonView
        if (view != null) {
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                // ignore
            }
            try {
                view.disposeComposition()
            } catch (e: Exception) {
                // ignore
            }
            floatingButtonView = null
            floatingButtonParams = null
        }
    }

    private fun releaseVirtualDisplay() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
    }

    private fun writeShutdownJournal(graceful: Boolean, reason: String) {
        try {
            kotlinx.coroutines.runBlocking {
                kotlinx.coroutines.withTimeout(250) {
                    preferencesRepository.updateSessionGracefulShutdown(graceful)
                    preferencesRepository.updateSessionShutdownReason(reason)
                    preferencesRepository.updateSessionServiceStarted(false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "writeShutdownJournal timed out or failed: ${e.message}")
        }
    }

    private fun requestServiceStop(reason: String = "USER_STOP") {
        if (shutdownInProgress.getAndSet(true)) {
            Log.d(TAG, "requestServiceStop: Shutdown already in progress, skipping")
            return
        }
        Log.d(TAG, "requestServiceStop: Stopping ScreenCaptureService, reason = $reason")
        terminalShutdownReason = reason

        writeShutdownJournal(true, reason)

        performCleanup(reason)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun performCleanup(reason: String) {
        if (cleanupCompleted.getAndSet(true)) {
            Log.d(TAG, "performCleanup: Cleanup already completed, skipping")
            return
        }
        Log.d(TAG, "Performing resource cleanup: reason = $reason")

        ProviderGateway.cancelActiveCall()
        isServiceActive.value = false
        locateJob?.cancel()
        isLocating.value = false
        healthWatcherJob?.cancel()
        timeoutJob?.cancel()
        diagnosticJob.cancel()

        removeAnswerPopup()
        removeFloatingButton()
        removeStatusBubble()

        pendingFirstImage = null
        updateWorkflowState(CaptureWorkflowState.IDLE)
        sharedCaptureTimestamp = null

        releaseVirtualDisplay()

        mediaProjection?.stop()
        mediaProjection = null

        floatingLifecycleOwner.stop()
        answerLifecycleOwner.stop()

        try {
            imageHandlerThread?.quitSafely()
        } catch (e: Exception) {
            // ignore
        }
        imageHandlerThread = null
        imageHandler = null
    }

    override fun onDestroy() {
        val graceful = shutdownInProgress.get()
        val finalReason = terminalShutdownReason
            ?: if (graceful) "SERVICE_STOPPED" else "SERVICE_DESTROYED_UNEXPECTED"
        writeShutdownJournal(graceful, finalReason)
        performCleanup("SERVICE_DESTROYED")
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved: MainActivity swiped away while service active")
        serviceScope.launch {
            preferencesRepository.updateSessionLastActionStage("ACTIVITY_TASK_REMOVED")
            preferencesRepository.updateSessionLastHealthyTime(System.currentTimeMillis())
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.d(TAG, "onTrimMemory: level = $level")
        if (level == android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL || 
            level == android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            Log.w(TAG, "Low memory detected, releasing non-critical UI overlays.")
            removeAnswerPopup()
            serviceScope.launch {
                stagedCaptureMutex.withLock {
                    if (captureWorkflowState.value == CaptureWorkflowState.WAITING_FOR_SECOND) {
                        pendingFirstImage = null
                        sharedCaptureTimestamp = null
                        updateWorkflowState(CaptureWorkflowState.IDLE)
                        Log.d(TAG, "Low memory: Cleared pending staged first image")
                    }
                }
            }
        }
    }

    private fun startSessionHealthWatcher() {
        healthWatcherJob?.cancel()
        healthWatcherJob = serviceScope.launch {
            while (isServiceActive.value) {
                delay(30000L) // check every 30 seconds
                runHealthCheck()
            }
        }
    }

    private fun runHealthCheck() {
        Log.d(TAG, "Running session health check")
        serviceScope.launch {
            preferencesRepository.updateSessionLastHealthyTime(System.currentTimeMillis())
        }

        if (!isServiceActive.value) return

        var hasFatalError = false
        var fatalReason = ""

        if (mediaProjection == null) {
            hasFatalError = true
            fatalReason = "MediaProjection is null"
        }
        if (virtualDisplay == null) {
            hasFatalError = true
            fatalReason = "VirtualDisplay is null"
        }
        if (imageReader == null) {
            hasFatalError = true
            fatalReason = "ImageReader is null"
        }

        if (hasFatalError) {
            Log.e(TAG, "Health check failed: $fatalReason. Shutting down broken session cleanly.")
            serviceScope.launch {
                preferencesRepository.updateSessionGracefulShutdown(false)
                preferencesRepository.updateSessionShutdownReason("FATAL_CAPTURE_INFRASTRUCTURE_FAILURE: $fatalReason")
            }
            requestServiceStop("HEALTH_CHECK_FATAL: $fatalReason")
            return
        }

        // Recoverable check: floating button view
        if (floatingButtonView == null) {
            Log.w(TAG, "Floating button view is missing! Attempting to restore overlay.")
            showFloatingButton()
        }
    }

    private fun updateJournalStage(stage: String) {
        serviceScope.launch {
            preferencesRepository.updateSessionLastActionStage(stage)
            preferencesRepository.updateSessionLastHealthyTime(System.currentTimeMillis())
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Refresh VirtualDisplay structure on configuration / orientation change
        serviceScope.launch {
            delay(300) // Wait briefly for WindowMetrics to update
            
            resizeVirtualDisplayIfNeeded()

            // Recalculate floating button bounds
            val bounds = getScreenBounds()
            val screenW = bounds.width()
            val screenH = bounds.height()

            val viewW = floatingButtonView?.width ?: 0
            val viewH = floatingButtonView?.height ?: 0

            // Restore the button using its normalized saved position when available
            val posX = preferencesRepository.buttonPosXFlow.first()
            val posY = preferencesRepository.buttonPosYFlow.first()

            // Clamp the existing overlay inside the new screen bounds
            val clampedPos = CaptureDimensionHelper.normalizedToPixel(
                posX, posY, screenW, screenH, viewW, viewH
            )

            val params = floatingButtonParams
            if (params != null && floatingButtonView != null) {
                params.x = clampedPos.first
                params.y = clampedPos.second
                try {
                    windowManager.updateViewLayout(floatingButtonView, params)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update layout in onConfigurationChanged: ${e.message}")
                }
            }
        }
    }

    // Helper classes to host compose views in a background service
    private class ServiceLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

        init {
            savedStateRegistryController.performRestore(null)
        }

        fun start() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun stop() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        return if (width > maxDim || height > maxDim) {
            val (newWidth, newHeight) = if (width > height) {
                Pair(maxDim, (height * (maxDim.toFloat() / width)).toInt())
            } else {
                Pair((width * (maxDim.toFloat() / height)).toInt(), maxDim)
            }
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }
}
