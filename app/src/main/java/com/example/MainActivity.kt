package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import android.media.projection.MediaProjectionManager
import java.util.concurrent.TimeUnit
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.AppDatabase
import com.example.data.HistoryEntry
import com.example.data.HistoryQuestionType
import com.example.data.HistoryRepository
import com.example.data.KeyStoreHelper
import com.example.data.ApiKeyStore
import com.example.data.PreferencesRepository
import com.example.data.GeminiKeySlot
import com.example.data.GeminiKeyHealth
import com.example.data.GeminiKeySlotSerializer
import com.example.service.ScreenCaptureService
import com.example.service.ConnectionTester
import com.example.ui.theme.MyApplicationTheme
import android.util.Base64
import com.example.service.ApiException
import com.example.service.ProviderGateway
import com.example.service.AiProvider
import com.example.service.AnalysisRequestContext
import com.example.service.GeminiProviderClient
import com.example.service.LocalPreparationException
import com.example.service.ParsedAnswer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Send
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun <T> SegmentedControl(
    label: String,
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    displayConverter: (T) -> String = { it.toString() }
) {
    Column {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                val containerColor = if (isSelected) Color(0xFF6750A4) else Color(0xFFE7E0EB)
                val contentColor = if (isSelected) Color.White else Color(0xFF49454F)

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(containerColor)
                        .clickable { onSelected(option) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = displayConverter(option),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(apiKeyStore: ApiKeyStore = KeyStoreHelper) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val priorExitReason = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val exitInfos = am.getHistoricalProcessExitReasons(null, 0, 1)
            if (exitInfos.isNotEmpty()) {
                val info = exitInfos[0]
                val reasonStr = when (info.reason) {
                    android.app.ApplicationExitInfo.REASON_ANR -> "ANR"
                    android.app.ApplicationExitInfo.REASON_CRASH -> "Crash (Java Exception)"
                    android.app.ApplicationExitInfo.REASON_CRASH_NATIVE -> "Native Crash"
                    android.app.ApplicationExitInfo.REASON_EXIT_SELF -> "Exit Self"
                    android.app.ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "Initialization Failure"
                    android.app.ApplicationExitInfo.REASON_LOW_MEMORY -> "Low Memory Kill"
                    android.app.ApplicationExitInfo.REASON_OTHER -> "Other"
                    android.app.ApplicationExitInfo.REASON_SIGNALED -> "System Signal Kill"
                    android.app.ApplicationExitInfo.REASON_USER_REQUESTED -> "User Force Stop"
                    android.app.ApplicationExitInfo.REASON_USER_STOPPED -> "User Stopped"
                    android.app.ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "Dependency Died"
                    android.app.ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "Excessive CPU/Memory/Battery"
                    else -> "Unknown Reason (${info.reason})"
                }
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(info.timestamp))
                "Reason: $reasonStr\nDescription: ${info.description ?: "None"}\nTimestamp: $dateStr"
            } else {
                "No prior exit history found."
            }
        } else {
            "Not supported on Android < 11 (API 30)"
        }
    }

    // Preferences & Database Repositories
    val preferencesRepository = remember { PreferencesRepository(context) }
    val historyRepository = remember {
        HistoryRepository(AppDatabase.getDatabase(context).historyDao())
    }

    // Collect settings flows
    val saveScreenshots by preferencesRepository.saveScreenshotsFlow.collectAsState(initial = true)
    val buttonOpacity by preferencesRepository.buttonOpacityFlow.collectAsState(initial = 0.10f)
    val buttonSizeDp by preferencesRepository.buttonSizeDpFlow.collectAsState(initial = 36)
    val lockPosition by preferencesRepository.lockPositionFlow.collectAsState(initial = true)
    val dismissTimeoutSec by preferencesRepository.dismissTimeoutSecFlow.collectAsState(initial = 8)
    val displayErrorSymbol by preferencesRepository.displayErrorSymbolFlow.collectAsState(initial = false)
    val stagedStatusBackground by preferencesRepository.stagedStatusBackgroundFlow.collectAsState(initial = "None")

    val popupSizePercent by preferencesRepository.popupSizePercentFlow.collectAsState(initial = 0.75f)
    val popupFontSizeSp by preferencesRepository.popupFontSizeSpFlow.collectAsState(initial = 30)
    val popupFontWeight by preferencesRepository.popupFontWeightFlow.collectAsState(initial = "Semi Bold")
    val popupBgOpacity by preferencesRepository.popupBgOpacityFlow.collectAsState(initial = 0.78f)
    val popupTextOpacity by preferencesRepository.popupTextOpacityFlow.collectAsState(initial = 1.0f)
    val popupCornerRadius by preferencesRepository.popupCornerRadiusDpFlow.collectAsState(initial = 20)
    val popupPaddingHorizontal by preferencesRepository.popupPaddingHorizontalDpFlow.collectAsState(initial = 22)
    val popupPaddingVertical by preferencesRepository.popupPaddingVerticalDpFlow.collectAsState(initial = 12)
    val popupBottomOffset by preferencesRepository.popupBottomOffsetDpFlow.collectAsState(initial = 120)
    val popupStyle by preferencesRepository.popupStyleFlow.collectAsState(initial = "Compact Rounded")
    val popupBgTheme by preferencesRepository.popupBgThemeFlow.collectAsState(initial = "Dark")
    val popupTextColor by preferencesRepository.popupTextColorFlow.collectAsState(initial = "White")
    val popupShowConfidence by preferencesRepository.popupShowConfidenceFlow.collectAsState(initial = false)

    val aiProvider by preferencesRepository.aiProviderFlow.collectAsState(initial = "Google Gemini Direct")
    val fallbackPrimaryProvider by preferencesRepository.fallbackPrimaryProviderFlow.collectAsState(initial = "Google Gemini")
    val geminiModel by preferencesRepository.geminiModelFlow.collectAsState(initial = "gemini-3.1-flash-lite")
    val geminiBaseUrl by preferencesRepository.geminiBaseUrlFlow.collectAsState(initial = "https://generativelanguage.googleapis.com/v1beta")

    val screenshotMaxDimension by preferencesRepository.screenshotMaxDimensionFlow.collectAsState(initial = 1200)
    val apiJpegQuality by preferencesRepository.apiJpegQualityFlow.collectAsState(initial = 70)
    val galleryJpegQuality by preferencesRepository.galleryJpegQualityFlow.collectAsState(initial = 90)
    val historyLimit by preferencesRepository.historyLimitFlow.collectAsState(initial = 30)

    val geminiKeySlotsMetadata by preferencesRepository.geminiKeySlotsMetadataFlow.collectAsState(initial = "")
    val keyStrategy by preferencesRepository.keyStrategyFlow.collectAsState(initial = "Sticky Success with Sequential Failover")
    val maxKeyAttempts by preferencesRepository.maxKeyAttemptsFlow.collectAsState(initial = 10)
    val cooldownDurationSec by preferencesRepository.cooldownDurationSecFlow.collectAsState(initial = 60)
    val skipCoolingDown by preferencesRepository.skipCoolingDownFlow.collectAsState(initial = true)
    val skipAuthFailed by preferencesRepository.skipAuthFailedFlow.collectAsState(initial = true)
    val skipPermissionDenied by preferencesRepository.skipPermissionDeniedFlow.collectAsState(initial = true)

    // Active Session Journal states
    val sessionServiceStarted by preferencesRepository.sessionServiceStartedFlow.collectAsState(initial = false)
    val sessionId by preferencesRepository.sessionIdFlow.collectAsState(initial = "")
    val sessionActivationTime by preferencesRepository.sessionActivationTimeFlow.collectAsState(initial = 0L)
    val sessionLastHealthyTime by preferencesRepository.sessionLastHealthyTimeFlow.collectAsState(initial = 0L)
    val sessionLastActionStage by preferencesRepository.sessionLastActionStageFlow.collectAsState(initial = "")
    val sessionGracefulShutdown by preferencesRepository.sessionGracefulShutdownFlow.collectAsState(initial = true)
    val sessionShutdownReason by preferencesRepository.sessionShutdownReasonFlow.collectAsState(initial = "")
    val sessionForegroundPromoted by preferencesRepository.sessionForegroundPromotedFlow.collectAsState(initial = false)
    val sessionProjectionInitialized by preferencesRepository.sessionProjectionInitializedFlow.collectAsState(initial = false)
    val sessionFloatingCreated by preferencesRepository.sessionFloatingCreatedFlow.collectAsState(initial = false)

    // Flow states
    val isServiceActive by ScreenCaptureService.isServiceActive.collectAsState()
    val lastServiceError by ScreenCaptureService.lastError.collectAsState()
    val historyList by historyRepository.allHistory.collectAsState(initial = emptyList())

    // Permission States
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    // API Key form state
    var geminiApiKeyInput by remember { mutableStateOf("") }
    var isGeminiApiKeyVisible by remember { mutableStateOf(false) }
    var isGeminiApiKeyStored by remember { mutableStateOf(false) }

    // Test API Key state
    var isTestingGeminiApi by remember { mutableStateOf(false) }
    var geminiApiTestResult by remember { mutableStateOf<String?>(null) }

    // Refresh permission statuses periodically on resume / launch
    LaunchedEffect(Unit) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
        val hasKey = withContext(Dispatchers.IO) {
            KeyStoreHelper.retrieveGeminiApiKey(context).isNotEmpty()
        }
        isGeminiApiKeyStored = hasKey
        if (hasKey) {
            geminiApiKeyInput = "••••••••••••••••••••••••"
        }
    }

    // Activity Result Launchers
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    val captureSessionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_START
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            Toast.makeText(context, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "ScreenPilot",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1B1B1F),
                            letterSpacing = (-0.5).sp
                        )
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE1E1E5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .border(2.dp, Color(0xFF44474E), shape = RoundedCornerShape(2.dp))
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFFDFBFF)
                    )
                )
                HorizontalDivider(color = Color(0xFFE1E1E5), thickness = 1.dp)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 1. ScreenPilot status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, Color(0xFFE7E0EB)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF3F0F5)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Service Status",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF44474E)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isServiceActive) "Active" else "Inactive",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 24.sp,
                                    color = if (isServiceActive) Color(0xFF6750A4) else Color(0xFF1B1B1F)
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(width = 48.dp, height = 24.dp)
                                    .clip(CircleShape)
                                    .background(if (isServiceActive) Color(0xFF6750A4) else Color(0xFFC9C5D0)),
                                contentAlignment = if (isServiceActive) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }

                        lastServiceError?.let { error ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Status badges / chips row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Overlay tag
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFDED8E1), CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (hasOverlayPermission) "OVERLAY OK" else "OVERLAY REQ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = if (hasOverlayPermission) Color(0xFF6750A4) else Color(0xFFB3261E)
                                )
                            }

                            // Notification tag
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFDED8E1), CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (hasNotificationPermission) "NOTIFS OK" else "NOTIFS REQ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = if (hasNotificationPermission) Color(0xFF6750A4) else Color(0xFFB3261E)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Permission Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE1E1E5)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Permissions Required",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        PermissionRow(
                            name = "Display Over Other Apps",
                            isGranted = hasOverlayPermission,
                            onRequest = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                overlayPermissionLauncher.launch(intent)
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PermissionRow(
                            name = "Notifications",
                            isGranted = hasNotificationPermission,
                            onRequest = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )

                        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Warning: Notification permission is recommended to ensure Android 13+ foreground service reliability.",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 3. Primary Button Control
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val hasPending by ScreenCaptureService.isStagedPending.collectAsState()
                    if (isServiceActive) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val intent = Intent(context, ScreenCaptureService::class.java).apply {
                                        action = ScreenCaptureService.ACTION_STOP
                                    }
                                    context.startService(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = CircleShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("stop_service_button")
                            ) {
                                Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Stop ScreenPilot", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                            }

                            if (hasPending) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(context, ScreenCaptureService::class.java).apply {
                                            action = ScreenCaptureService.ACTION_CANCEL_STAGED
                                        }
                                        context.startService(intent)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("cancel_two_image_capture_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Cancel Pending Two-Image Capture", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (!hasOverlayPermission) {
                                    Toast.makeText(context, "Please grant Overlay permission first", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    Toast.makeText(context, "Starting ScreenPilot (Notification permission is recommended for reliability)", Toast.LENGTH_SHORT).show()
                                }

                                val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                captureSessionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("activate_service_button")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Activate ScreenPilot", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                        }
                    }
                }
            }

            // Google Gemini Direct Settings & Multi-Key Pool Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, Color(0xFFE1E1E5)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = Color(0xFF6750A4))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Google Gemini Key Pool",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Configure up to 10 Gemini API keys to enable seamless automatic failover, priority scheduling, and high availability.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // General Model & Base URL
                        var geminiModelInput by remember { mutableStateOf(geminiModel) }
                        LaunchedEffect(geminiModel) {
                            geminiModelInput = geminiModel
                        }
                        OutlinedTextField(
                            value = geminiModelInput,
                            onValueChange = {
                                geminiModelInput = it
                                coroutineScope.launch {
                                    preferencesRepository.setGeminiModel(it)
                                }
                            },
                            label = { Text("Gemini Model") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_model_field")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        var geminiBaseUrlInput by remember { mutableStateOf(geminiBaseUrl) }
                        LaunchedEffect(geminiBaseUrl) {
                            geminiBaseUrlInput = geminiBaseUrl
                        }
                        OutlinedTextField(
                            value = geminiBaseUrlInput,
                            onValueChange = {
                                geminiBaseUrlInput = it
                                coroutineScope.launch {
                                    preferencesRepository.setGeminiBaseUrl(it)
                                }
                            },
                            label = { Text("Gemini Base URL") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_base_url_field")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        var selectedSlotId by remember { mutableStateOf(1) }
                        var editedLabelTextLocal by remember { mutableStateOf("") }
                        var targetPriority by remember { mutableStateOf("1") }

                        var geminiApiKeyInputLocal by remember { mutableStateOf("") }
                        var isGeminiApiKeyVisibleLocal by remember { mutableStateOf(false) }
                        val currentSlots = GeminiKeySlotSerializer.deserialize(geminiKeySlotsMetadata)
                        val isAnyKeyConfigured = isGeminiApiKeyStored || currentSlots.any { it.healthStatus != "NOT_CONFIGURED" }

                        OutlinedTextField(
                            value = geminiApiKeyInputLocal,
                            onValueChange = { geminiApiKeyInputLocal = it },
                            label = { Text("Gemini API Key") },
                            placeholder = { Text(if (isAnyKeyConfigured) "••••••••••••••••••••••••" else "Enter Gemini API Key") },
                            visualTransformation = if (isGeminiApiKeyVisibleLocal) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isGeminiApiKeyVisibleLocal = !isGeminiApiKeyVisibleLocal }) {
                                    Icon(
                                        imageVector = if (isGeminiApiKeyVisibleLocal) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isGeminiApiKeyVisibleLocal) "Hide Key" else "Show Key"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_api_key_field")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Submit button under the text field
                        Button(
                            onClick = {
                                if (geminiApiKeyInputLocal.trim().isEmpty()) {
                                    Toast.makeText(context, "Please enter an API Key first", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val targetSlotIdStr = selectedSlotId.toString()
                                val priorityVal = targetPriority.toIntOrNull() ?: 1
                                coroutineScope.launch {
                                    val keyToStore = geminiApiKeyInputLocal.trim()
                                    
                                    // Store slot key securely
                                    val storeResult = withContext(Dispatchers.IO) {
                                        KeyStoreHelper.storeSlotKey(context, targetSlotIdStr, keyToStore)
                                    }
                                    if (storeResult.isFailure) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Error: Failed to encrypt and store key securely. Please check logs.", Toast.LENGTH_LONG).show()
                                        }
                                        return@launch
                                    }
                                    
                                    // If targeting Slot 1, also store as main key
                                    if (targetSlotIdStr == "1") {
                                        val mainStoreResult = withContext(Dispatchers.IO) {
                                            KeyStoreHelper.storeGeminiApiKey(context, keyToStore)
                                        }
                                        if (mainStoreResult.isFailure) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Error: Failed to encrypt and store main key securely. Please check logs.", Toast.LENGTH_LONG).show()
                                            }
                                            return@launch
                                        }
                                        isGeminiApiKeyStored = true
                                    }

                                    // Update metadata
                                    val slotsJson = preferencesRepository.geminiKeySlotsMetadataFlow.first()
                                    val slotsList = GeminiKeySlotSerializer.deserialize(slotsJson).toMutableList()
                                    
                                    val existingIndex = slotsList.indexOfFirst { it.id == targetSlotIdStr }
                                    val suffix = if (keyToStore.length > 4) "••••••••${keyToStore.takeLast(4)}" else "••••$keyToStore"
                                    
                                    val finalLabel = if (editedLabelTextLocal.trim().isNotEmpty()) {
                                        editedLabelTextLocal.trim()
                                    } else {
                                        if (existingIndex >= 0 && slotsList[existingIndex].label.isNotEmpty()) {
                                            slotsList[existingIndex].label
                                        } else {
                                            "Main"
                                        }
                                    }

                                    if (existingIndex >= 0) {
                                        slotsList[existingIndex] = slotsList[existingIndex].copy(
                                            label = finalLabel,
                                            enabled = true,
                                            priority = priorityVal,
                                            healthStatus = GeminiKeyHealth.READY.name,
                                            maskedSuffix = suffix
                                        )
                                    } else {
                                        slotsList.add(GeminiKeySlot(
                                            id = targetSlotIdStr,
                                            label = finalLabel,
                                            enabled = true,
                                            priority = priorityVal,
                                            maskedSuffix = suffix,
                                            healthStatus = GeminiKeyHealth.READY.name
                                        ))
                                    }
                                    
                                    preferencesRepository.setGeminiKeySlotsMetadata(GeminiKeySlotSerializer.serialize(slotsList))
                                    
                                    // Reset inputs
                                    geminiApiKeyInputLocal = ""
                                    editedLabelTextLocal = ""
                                    targetPriority = "1"
                                    
                                    Toast.makeText(context, "Submitted key to Slot #$targetSlotIdStr successfully!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("submit_api_key_slot_quick_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Submit to Selected Slot #$selectedSlotId", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Target slot selector
                        Text(
                            text = "Target Slot to Save Key:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // 2 rows of 5 slots each
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (row in 0 until 2) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (col in 1..5) {
                                        val slotNum = row * 5 + col
                                        val isSelected = selectedSlotId == slotNum
                                        
                                        // Find slot metadata if exists
                                        val slotMeta = currentSlots.find { it.id == slotNum.toString() }
                                        val isSlotConfigured = slotMeta != null && slotMeta.healthStatus != "NOT_CONFIGURED"
                                        
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else if (isSlotConfigured) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                    else Color(0xFFF1F0F4)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary 
                                                            else if (isSlotConfigured) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                                            else Color(0xFFDED8E1),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { selectedSlotId = slotNum }
                                                .testTag("slot_select_box_$slotNum"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                                Text(
                                                    text = "#$slotNum",
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White 
                                                            else if (isSlotConfigured) MaterialTheme.colorScheme.primary
                                                            else Color(0xFF44474E)
                                                )
                                                if (slotMeta != null && slotMeta.label.isNotEmpty()) {
                                                    Text(
                                                        text = slotMeta.label,
                                                        fontSize = 8.sp,
                                                        maxLines = 1,
                                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editedLabelTextLocal,
                                onValueChange = { editedLabelTextLocal = it },
                                label = { Text("Slot Label (Optional)") },
                                placeholder = { Text("e.g. Backup Key $selectedSlotId") },
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )

                            OutlinedTextField(
                                value = targetPriority,
                                onValueChange = { targetPriority = it },
                                label = { Text("Priority (1-10)") },
                                modifier = Modifier.width(100.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Submit / Save Button
                        Button(
                            onClick = {
                                if (geminiApiKeyInputLocal.trim().isEmpty()) {
                                    Toast.makeText(context, "Please enter an API Key first", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val targetSlotIdStr = selectedSlotId.toString()
                                val priorityVal = targetPriority.toIntOrNull() ?: 1
                                coroutineScope.launch {
                                    val keyToStore = geminiApiKeyInputLocal.trim()
                                    
                                    // Store slot key securely
                                    val storeResult = withContext(Dispatchers.IO) {
                                        KeyStoreHelper.storeSlotKey(context, targetSlotIdStr, keyToStore)
                                    }
                                    if (storeResult.isFailure) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Error: Failed to encrypt and store key securely. Please check logs.", Toast.LENGTH_LONG).show()
                                        }
                                        return@launch
                                    }
                                    
                                    // If targeting Slot 1, also store as main key
                                    if (targetSlotIdStr == "1") {
                                        val mainStoreResult = withContext(Dispatchers.IO) {
                                            KeyStoreHelper.storeGeminiApiKey(context, keyToStore)
                                        }
                                        if (mainStoreResult.isFailure) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Error: Failed to encrypt and store main key securely. Please check logs.", Toast.LENGTH_LONG).show()
                                            }
                                            return@launch
                                        }
                                        isGeminiApiKeyStored = true
                                    }

                                    // Update metadata
                                    val slotsJson = preferencesRepository.geminiKeySlotsMetadataFlow.first()
                                    val slotsList = GeminiKeySlotSerializer.deserialize(slotsJson).toMutableList()
                                    
                                    val existingIndex = slotsList.indexOfFirst { it.id == targetSlotIdStr }
                                    val suffix = if (keyToStore.length > 4) "••••••••${keyToStore.takeLast(4)}" else "••••$keyToStore"
                                    
                                    val finalLabel = if (editedLabelTextLocal.trim().isNotEmpty()) {
                                        editedLabelTextLocal.trim()
                                    } else {
                                        if (existingIndex >= 0 && slotsList[existingIndex].label.isNotEmpty()) {
                                            slotsList[existingIndex].label
                                        } else {
                                            "Main"
                                        }
                                    }

                                    if (existingIndex >= 0) {
                                        slotsList[existingIndex] = slotsList[existingIndex].copy(
                                            label = finalLabel,
                                            enabled = true,
                                            priority = priorityVal,
                                            healthStatus = GeminiKeyHealth.READY.name,
                                            maskedSuffix = suffix
                                        )
                                    } else {
                                        slotsList.add(GeminiKeySlot(
                                            id = targetSlotIdStr,
                                            label = finalLabel,
                                            enabled = true,
                                            priority = priorityVal,
                                            maskedSuffix = suffix,
                                            healthStatus = GeminiKeyHealth.READY.name
                                        ))
                                    }
                                    
                                    // Save update
                                    preferencesRepository.setGeminiKeySlotsMetadata(GeminiKeySlotSerializer.serialize(slotsList))
                                    
                                    // Reset inputs
                                    geminiApiKeyInputLocal = ""
                                    editedLabelTextLocal = ""
                                    targetPriority = "1"
                                    
                                    Toast.makeText(context, "Saved to Slot #$targetSlotIdStr successfully!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_api_key_slot_button")
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submit to Selected Slot", fontWeight = FontWeight.Medium)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFE1E1E5), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // STRATEGY SELECTION
                        Text("Failover Strategy", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        val strategies = listOf(
                            "Sticky Success with Sequential Failover",
                            "Always Start at Key 1",
                            "Round Robin"
                        )
                        strategies.forEach { strategy ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch {
                                            preferencesRepository.setKeyStrategy(strategy)
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = keyStrategy == strategy,
                                    onClick = {
                                        coroutineScope.launch {
                                            preferencesRepository.setKeyStrategy(strategy)
                                        }
                                    }
                                )
                                Text(strategy, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // FAILOVER PARAMETERS
                        Text("Failover & Cooldown Rules", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Max Key Attempts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Max Key Attempts per Trigger:", fontSize = 13.sp)
                            var attemptsInput by remember { mutableStateOf(maxKeyAttempts.toString()) }
                            LaunchedEffect(maxKeyAttempts) {
                                attemptsInput = maxKeyAttempts.toString()
                            }
                            OutlinedTextField(
                                value = attemptsInput,
                                onValueChange = {
                                    attemptsInput = it
                                    it.toIntOrNull()?.let { attempts ->
                                        if (attempts in 1..10) {
                                            coroutineScope.launch {
                                                preferencesRepository.setMaxKeyAttempts(attempts)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.width(60.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Cooldown Duration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Cooldown Duration (sec):", fontSize = 13.sp)
                            var cooldownInput by remember { mutableStateOf(cooldownDurationSec.toString()) }
                            LaunchedEffect(cooldownDurationSec) {
                                cooldownInput = cooldownDurationSec.toString()
                            }
                            OutlinedTextField(
                                value = cooldownInput,
                                onValueChange = {
                                    cooldownInput = it
                                    it.toIntOrNull()?.let { cooldown ->
                                        if (cooldown >= 0) {
                                            coroutineScope.launch {
                                                preferencesRepository.setCooldownDurationSec(cooldown)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.width(80.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Switches
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(
                                checked = skipCoolingDown,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        preferencesRepository.setSkipCoolingDown(it)
                                    }
                                }
                            )
                            Text("Skip keys currently in cooldown", fontSize = 12.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(
                                checked = skipAuthFailed,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        preferencesRepository.setSkipAuthFailed(it)
                                    }
                                }
                            )
                            Text("Skip keys with prior auth failures", fontSize = 12.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(
                                checked = skipPermissionDenied,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        preferencesRepository.setSkipPermissionDenied(it)
                                    }
                                }
                            )
                            Text("Skip keys with permission denied (403)", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFE1E1E5), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // KEY SLOTS MANAGER
                        Text("Manage Individual Slots (1 to 10)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        val slots: List<GeminiKeySlot> = GeminiKeySlotSerializer.deserialize(geminiKeySlotsMetadata)
                        var editingSlot by remember { mutableStateOf<GeminiKeySlot?>(null) }
                        var editedKeyText by remember { mutableStateOf("") }
                        var editedLabelText by remember { mutableStateOf("") }
                        var editedPriorityText by remember { mutableStateOf("1") }

                        slots.forEach { slot ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                border = BorderStroke(1.dp, Color(0xFFE1E1E5)),
                                colors = CardDefaults.cardColors(containerColor = if (slot.enabled) Color(0xFFFBF8FD) else Color(0xFFF1F0F4)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Slot #${slot.id}: ${slot.label.ifEmpty { "Key" }}",
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "P${slot.priority}",
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Status Indicator
                                            val statusText = when (slot.healthStatus) {
                                                GeminiKeyHealth.READY.name -> "Ready"
                                                GeminiKeyHealth.COOLDOWN.name -> "Cooldown"
                                                GeminiKeyHealth.AUTH_FAILED.name -> "Auth Failed"
                                                GeminiKeyHealth.PERMISSION_DENIED.name -> "Permission Denied"
                                                GeminiKeyHealth.TEMPORARY_FAILURE.name -> "Temp Failure"
                                                GeminiKeyHealth.DISABLED.name -> "Disabled"
                                                GeminiKeyHealth.NOT_CONFIGURED.name -> "Not Configured"
                                                GeminiKeyHealth.NOT_TESTED.name -> "Not Tested"
                                                "READY" -> "Ready"
                                                "FAILED" -> "Failed"
                                                "COOLDOWN" -> "Cooldown"
                                                else -> "Not Configured"
                                            }
                                            val statusColor = when (slot.healthStatus) {
                                                "READY" -> Color(0xFF2E7D32)
                                                "COOLDOWN" -> Color(0xFFE65100)
                                                "FAILED" -> Color(0xFFC62828)
                                                else -> Color.Gray
                                            }
                                            Text(
                                                text = "Status: $statusText",
                                                fontSize = 11.sp,
                                                color = statusColor,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if ((slot.healthStatus == GeminiKeyHealth.COOLDOWN.name || slot.healthStatus == "COOLDOWN") && slot.cooldownExpiration > 0) {
                                                val left = (slot.cooldownExpiration - System.currentTimeMillis()) / 1000
                                                if (left > 0) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("(${left}s left)", fontSize = 11.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = slot.enabled,
                                            onCheckedChange = { checked ->
                                                coroutineScope.launch {
                                                    val updatedList = slots.map {
                                                        if (it.id == slot.id) it.copy(enabled = checked) else it
                                                    }
                                                    preferencesRepository.setGeminiKeySlotsMetadata(
                                                        GeminiKeySlotSerializer.serialize(updatedList)
                                                    )
                                                }
                                            },
                                            modifier = Modifier.scale(0.7f)
                                        )

                                        IconButton(onClick = {
                                            editingSlot = slot
                                            editedKeyText = "" // keep blank to avoid showing full key
                                            editedLabelText = slot.label
                                            editedPriorityText = slot.priority.toString()
                                        }) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Key Slot", modifier = Modifier.size(18.dp))
                                        }

                                        IconButton(onClick = {
                                            coroutineScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    KeyStoreHelper.clearSlotKey(context, slot.id)
                                                }
                                                val updatedList = slots.map {
                                                    if (it.id == slot.id) {
                                                        it.copy(
                                                            enabled = false,
                                                            healthStatus = GeminiKeyHealth.NOT_CONFIGURED.name,
                                                            cooldownExpiration = 0
                                                        )
                                                    } else it
                                                }
                                                preferencesRepository.setGeminiKeySlotsMetadata(
                                                    GeminiKeySlotSerializer.serialize(updatedList)
                                                )
                                                Toast.makeText(context, "Slot #${slot.id} Cleared", Toast.LENGTH_SHORT).show()
                                            }
                                        }) {
                                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear Key Slot", modifier = Modifier.size(18.dp), tint = Color.Red)
                                        }
                                    }
                                }
                            }
                        }

                        // Dialog for editing slot
                        editingSlot?.let { slot ->
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { editingSlot = null },
                                title = { Text("Edit Slot #${slot.id}") },
                                text = {
                                    Column {
                                        Text("Set Gemini API key and details for this slot. Key is securely stored in Android KeyStore.", fontSize = 12.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedTextField(
                                            value = editedLabelText,
                                            onValueChange = { editedLabelText = it },
                                            label = { Text("Slot Label (e.g. Primary Key)") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = editedKeyText,
                                            onValueChange = { editedKeyText = it },
                                            label = { Text("API Key (leave blank to keep existing)") },
                                            placeholder = { Text(if (slot.healthStatus != "NOT_CONFIGURED") "••••••••••••••••" else "Enter new API key") },
                                            visualTransformation = PasswordVisualTransformation(),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = editedPriorityText,
                                            onValueChange = { editedPriorityText = it },
                                            label = { Text("Priority (1 to 10)") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        val priorityVal = editedPriorityText.toIntOrNull() ?: 1
                                        coroutineScope.launch {
                                            var storeSuccess = true
                                            if (editedKeyText.isNotEmpty()) {
                                                val storeResult = withContext(Dispatchers.IO) {
                                                    KeyStoreHelper.storeSlotKey(context, slot.id, editedKeyText)
                                                }
                                                if (storeResult.isFailure) {
                                                    storeSuccess = false
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(context, "Error: Failed to encrypt and store key securely. Please check logs.", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                            if (storeSuccess) {
                                                val updatedList = slots.map {
                                                    if (it.id == slot.id) {
                                                        it.copy(
                                                            label = editedLabelText,
                                                            priority = priorityVal,
                                                            healthStatus = if (editedKeyText.isNotEmpty() || (it.healthStatus != GeminiKeyHealth.NOT_CONFIGURED.name && it.healthStatus != "NOT_CONFIGURED")) GeminiKeyHealth.READY.name else GeminiKeyHealth.NOT_CONFIGURED.name,
                                                            enabled = true
                                                        )
                                                    } else it
                                                }
                                                preferencesRepository.setGeminiKeySlotsMetadata(
                                                    GeminiKeySlotSerializer.serialize(updatedList)
                                                )
                                                editingSlot = null
                                                Toast.makeText(context, "Slot #${slot.id} Saved", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }) {
                                        Text("Save")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { editingSlot = null }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 5. Floating Button Configuration
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, Color(0xFFE1E1E5)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = Color(0xFF6750A4))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Floating Button Config",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Opacity slider
                        Text(
                            text = "Opacity: ${(buttonOpacity * 100).toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = buttonOpacity.coerceIn(0f, 1f),
                            onValueChange = {
                                coroutineScope.launch {
                                    preferencesRepository.setButtonOpacity(it.coerceIn(0f, 1f))
                                }
                            },
                            valueRange = 0.0f..1.0f,
                            modifier = Modifier.testTag("opacity_slider")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Size slider
                        Text(
                            text = "Size: ${buttonSizeDp}dp",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = buttonSizeDp.toFloat(),
                            onValueChange = {
                                coroutineScope.launch {
                                    preferencesRepository.setButtonSizeDp(it.toInt())
                                }
                            },
                            valueRange = 36f..96f,
                            modifier = Modifier.testTag("size_slider")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (lockPosition) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Lock Button Position", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            Switch(
                                checked = lockPosition,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        preferencesRepository.setLockPosition(it)
                                    }
                                },
                                modifier = Modifier.testTag("lock_position_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        SegmentedControl(
                            label = "Staged Status Background",
                            options = listOf("None", "Dark", "Light"),
                            selected = stagedStatusBackground,
                            onSelected = {
                                coroutineScope.launch {
                                    preferencesRepository.setStagedStatusBackground(it)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        preferencesRepository.resetButtonPosition()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reset_position_button")
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset Position", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (isServiceActive) {
                                        val intent = Intent(context, ScreenCaptureService::class.java).apply {
                                            action = ScreenCaptureService.ACTION_LOCATE_BUTTON
                                        }
                                        context.startService(intent)
                                    } else {
                                        Toast.makeText(context, "ScreenPilot is not active", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("locate_button")
                            ) {
                                Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Locate Button", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Popup Appearance Settings Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, Color(0xFFE1E1E5)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = Color(0xFF6750A4))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Answer Popup Appearance",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Sliders & settings
                        Text("Popup Size: ${(popupSizePercent * 100).toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Slider(
                            value = popupSizePercent,
                            onValueChange = {
                                coroutineScope.launch {
                                    preferencesRepository.setPopupSizePercent(it)
                                }
                            },
                            valueRange = 0.5f..1.5f
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Answer Font Size: ${popupFontSizeSp}sp", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Slider(
                            value = popupFontSizeSp.toFloat(),
                            onValueChange = {
                                coroutineScope.launch {
                                    preferencesRepository.setPopupFontSizeSp(it.toInt())
                                }
                            },
                            valueRange = 16f..64f
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SegmentedControl(
                            label = "Font Weight",
                            options = listOf("Normal", "Medium", "Semi Bold", "Bold"),
                            selected = popupFontWeight,
                            onSelected = {
                                coroutineScope.launch {
                                    preferencesRepository.setPopupFontWeight(it)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Background Opacity: ${(popupBgOpacity * 100).toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Slider(
                            value = popupBgOpacity,
                            onValueChange = {
                                coroutineScope.launch {
                                    preferencesRepository.setPopupBgOpacity(it)
                                }
                            },
                            valueRange = 0f..1f
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Text Opacity: ${(popupTextOpacity * 100).toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Slider(
                            value = popupTextOpacity,
                            onValueChange = {
                                coroutineScope.launch {
                                    preferencesRepository.setPopupTextOpacity(it)
                                }
                            },
                            valueRange = 0.2f..1f
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Corner Radius: ${popupCornerRadius}dp", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Slider(
                            value = popupCornerRadius.toFloat(),
                            onValueChange = {
                                coroutineScope.launch {
                                    preferencesRepository.setPopupCornerRadiusDp(it.toInt())
                                }
                            },
                            valueRange = 0f..40f
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Horizontal Padding: ${popupPaddingHorizontal}dp", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Slider(
                            value = popupPaddingHorizontal.toFloat(),
                            onValueChange = {
                                coroutineScope.launch {
                                    preferencesRepository.setPopupPaddingHorizontalDp(it.toInt())
                                }
                            },
                            valueRange = 8f..40f
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Vertical Padding: ${popupPaddingVertical}dp", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Slider(
                            value = popupPaddingVertical.toFloat(),
                            onValueChange = {
                                coroutineScope.launch {
                                    preferencesRepository.setPopupPaddingVerticalDp(it.toInt())
                                }
                            },
                            valueRange = 4f..28f
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Bottom Position Offset: ${popupBottomOffset}dp", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Slider(
                            value = popupBottomOffset.toFloat(),
                            onValueChange = {
                                coroutineScope.launch {
                                    preferencesRepository.setPopupBottomOffsetDp(it.toInt())
                                }
                            },
                            valueRange = 40f..400f
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SegmentedControl(
                            label = "Popup Style",
                            options = listOf("Compact Rounded", "Circle", "Pill", "Text Only"),
                            selected = popupStyle,
                            onSelected = {
                                coroutineScope.launch {
                                    preferencesRepository.setPopupStyle(it)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SegmentedControl(
                            label = "Background Theme",
                            options = listOf("Dark", "Light", "Auto Contrast"),
                            selected = popupBgTheme,
                            onSelected = {
                                coroutineScope.launch {
                                    preferencesRepository.setPopupBgTheme(it)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SegmentedControl(
                            label = "Answer Text Color",
                            options = listOf("White", "Black", "Auto"),
                            selected = popupTextColor,
                            onSelected = {
                                coroutineScope.launch {
                                    preferencesRepository.setPopupTextColor(it)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Show Confidence", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = popupShowConfidence,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        preferencesRepository.setPopupShowConfidence(it)
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (!isServiceActive) {
                                        Toast.makeText(context, "ScreenPilot is not active. Activate it first to preview overlays.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val intent = Intent(context, ScreenCaptureService::class.java).apply {
                                        action = ScreenCaptureService.ACTION_SHOW_PREVIEW
                                    }
                                    context.startService(intent)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("preview_popup_button")
                            ) {
                                Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Preview Popup", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        preferencesRepository.resetPopupAppearance()
                                        Toast.makeText(context, "Popup settings reset to defaults", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reset_popup_button")
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset Style", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 6. Capture Configuration
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, Color(0xFFE1E1E5)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color(0xFF6750A4))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Capture & Quality Settings",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Save screenshots to gallery", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = saveScreenshots,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        preferencesRepository.setSaveScreenshots(it)
                                    }
                                },
                                modifier = Modifier.testTag("save_screenshots_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Display error symbol (?) on error", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = displayErrorSymbol,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        preferencesRepository.setDisplayErrorSymbol(it)
                                    }
                                },
                                modifier = Modifier.testTag("display_error_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Answer Auto-dismiss Fallback: ${dismissTimeoutSec}s",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = dismissTimeoutSec.toFloat(),
                            onValueChange = {
                                coroutineScope.launch {
                                    preferencesRepository.setDismissTimeoutSec(it.toInt())
                                }
                            },
                            valueRange = 3f..20f,
                            modifier = Modifier.testTag("dismiss_timeout_slider")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Screenshot Max Dimension: ${screenshotMaxDimension}px",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = screenshotMaxDimension.toFloat(),
                            onValueChange = {
                                coroutineScope.launch {
                                    preferencesRepository.setScreenshotMaxDimension(it.toInt())
                                }
                            },
                            valueRange = 300f..2000f,
                            steps = 17,
                            modifier = Modifier.testTag("max_dimension_slider")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "API Upload JPEG Quality: ${apiJpegQuality}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = apiJpegQuality.toFloat(),
                            onValueChange = {
                                coroutineScope.launch {
                                    preferencesRepository.setApiJpegQuality(it.toInt())
                                }
                            },
                            valueRange = 10f..100f,
                            modifier = Modifier.testTag("api_quality_slider")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Gallery Local Save JPEG Quality: ${galleryJpegQuality}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = galleryJpegQuality.toFloat(),
                            onValueChange = {
                                coroutineScope.launch {
                                    preferencesRepository.setGalleryJpegQuality(it.toInt())
                                }
                            },
                            valueRange = 10f..100f,
                            modifier = Modifier.testTag("gallery_quality_slider")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Answer History Limit: ${historyLimit}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = historyLimit.toFloat(),
                            onValueChange = {
                                coroutineScope.launch {
                                    preferencesRepository.setHistoryLimit(it.toInt())
                                }
                            },
                            valueRange = 10f..100f,
                            modifier = Modifier.testTag("history_limit_slider")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    preferencesRepository.restoreRecommendedDefaults()
                                    Toast.makeText(context, "All settings restored to recommended defaults", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("restore_defaults_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore Recommended Defaults")
                        }
                    }
                }
            }

            // TASK 10 — DIAGNOSTICS CARD & TASK 11 — TEST IMAGE ANALYSIS PIPELINE
            item {
                val context = LocalContext.current
                var isDiagnosticsExpanded by remember { mutableStateOf(false) }
                val diagnostics by ProviderGateway.diagnosticsFlow.collectAsState()
                var testRunning by remember { mutableStateOf(false) }
                var testOutput by remember { mutableStateOf<String?>(null) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFFE1E1E5)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isDiagnosticsExpanded = !isDiagnosticsExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Floating Trigger Diagnostics",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            }
                            Icon(
                                imageVector = if (isDiagnosticsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isDiagnosticsExpanded) "Collapse" else "Expand"
                            )
                        }

                        if (isDiagnosticsExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Active Provider:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(diagnostics.provider, fontSize = 13.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Model Used:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(diagnostics.model, fontSize = 13.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Capture Size:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("${diagnostics.width} × ${diagnostics.height}", fontSize = 13.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("JPEG Compressed:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(String.format(java.util.Locale.US, "%.1f KB", diagnostics.jpegSizeKb), fontSize = 13.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Request Payload Size:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(String.format(java.util.Locale.US, "%.1f KB", diagnostics.requestBodySizeKb), fontSize = 13.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Current/Last Stage:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(diagnostics.stage, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("HTTP Status:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(diagnostics.httpStatus?.toString() ?: "N/A", fontSize = 13.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Duration:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("${diagnostics.durationMs} ms", fontSize = 13.sp)
                                }
                                diagnostics.error?.let { err ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Sanitized Error:\n$err",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        testRunning = true
                                        testOutput = null
                                        coroutineScope.launch(Dispatchers.IO) {
                                            try {
                                                val dummyBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
                                                val canvas = android.graphics.Canvas(dummyBitmap)
                                                val paintBg = android.graphics.Paint().apply { color = android.graphics.Color.BLUE }
                                                canvas.drawRect(0f, 0f, 200f, 200f, paintBg)
                                                val paintText = android.graphics.Paint().apply {
                                                    color = android.graphics.Color.WHITE
                                                    textSize = 18f
                                                    isAntiAlias = true
                                                }
                                                canvas.drawText("Test Question Image", 20f, 100f, paintText)

                                                val maxDim = preferencesRepository.screenshotMaxDimensionFlow.first()
                                                val apiQuality = preferencesRepository.apiJpegQualityFlow.first()

                                                val scaled = if (dummyBitmap.width > maxDim || dummyBitmap.height > maxDim) {
                                                    Bitmap.createScaledBitmap(dummyBitmap, maxDim, maxDim, true)
                                                } else {
                                                    dummyBitmap
                                                }
                                                val outputStream = java.io.ByteArrayOutputStream()
                                                scaled.compress(Bitmap.CompressFormat.JPEG, apiQuality, outputStream)
                                                val jpegBytes = outputStream.toByteArray()
                                                val kbSize = jpegBytes.size / 1024.0

                                                val base64Data = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
                                                if (base64Data.isEmpty()) {
                                                    throw LocalPreparationException("Base64 encoding yielded empty result.")
                                                }

                                                val slotsJson = preferencesRepository.geminiKeySlotsMetadataFlow.first()
                                                val slots = GeminiKeySlotSerializer.deserialize(slotsJson)
                                                val enabledCount = slots.count { it.enabled }

                                                withContext(Dispatchers.Main) {
                                                    testOutput = """
                                                        Local Pipeline Stable!
                                                        • Dummy Canvas: 200×200 px
                                                        • JPEG Compressed: ${String.format(java.util.Locale.US, "%.2f", kbSize)} KB
                                                        • Base64 string length: ${base64Data.length} chars
                                                        • Enabled pool keys: $enabledCount slots
                                                        • Validation: All local checks passed!
                                                    """.trimIndent()
                                                    testRunning = false
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    testOutput = "Local Pipeline check failed: ${e.message}"
                                                    testRunning = false
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("test_local_pipeline_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    enabled = !testRunning
                                ) {
                                    Text("Local Check", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        testRunning = true
                                        testOutput = null
                                        coroutineScope.launch(Dispatchers.IO) {
                                            val startTime = System.currentTimeMillis()
                                            try {
                                                val dummyBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
                                                val canvas = android.graphics.Canvas(dummyBitmap)
                                                val paintBg = android.graphics.Paint().apply { color = android.graphics.Color.BLUE }
                                                canvas.drawRect(0f, 0f, 200f, 200f, paintBg)
                                                val paintText = android.graphics.Paint().apply {
                                                    color = android.graphics.Color.WHITE
                                                    textSize = 18f
                                                    isAntiAlias = true
                                                }
                                                canvas.drawText("Test Question Image", 20f, 100f, paintText)

                                                val maxDim = preferencesRepository.screenshotMaxDimensionFlow.first()
                                                val apiQuality = preferencesRepository.apiJpegQualityFlow.first()
                                                val selectedModel = preferencesRepository.geminiModelFlow.first()

                                                val scaled = if (dummyBitmap.width > maxDim || dummyBitmap.height > maxDim) {
                                                    Bitmap.createScaledBitmap(dummyBitmap, maxDim, maxDim, true)
                                                } else {
                                                    dummyBitmap
                                                }
                                                val outputStream = java.io.ByteArrayOutputStream()
                                                scaled.compress(Bitmap.CompressFormat.JPEG, apiQuality, outputStream)
                                                val jpegBytes = outputStream.toByteArray()
                                                if (scaled != dummyBitmap) {
                                                    scaled.recycle()
                                                }
                                                dummyBitmap.recycle()

                                                val slotsJson = preferencesRepository.geminiKeySlotsMetadataFlow.first()
                                                var slots = GeminiKeySlotSerializer.deserialize(slotsJson).toMutableList()

                                                if (slots.isEmpty()) {
                                                    val oldKey = withContext(Dispatchers.IO) {
                                                        KeyStoreHelper.getGeminiApiKey(context)
                                                    }
                                                    if (oldKey.trim().isNotEmpty()) {
                                                        val suffix = if (oldKey.trim().length > 4) "••••••••${oldKey.trim().takeLast(4)}" else "••••${oldKey.trim()}"
                                                        val firstSlot = GeminiKeySlot(
                                                            id = "1",
                                                            label = "Main",
                                                            enabled = true,
                                                            priority = 1,
                                                            maskedSuffix = suffix,
                                                            healthStatus = GeminiKeyHealth.READY.name
                                                        )
                                                        val storeResult = withContext(Dispatchers.IO) {
                                                            KeyStoreHelper.storeSlotKey(context, "1", oldKey)
                                                        }
                                                        if (storeResult.isSuccess) {
                                                            slots.add(firstSlot)
                                                            preferencesRepository.setGeminiKeySlotsMetadata(GeminiKeySlotSerializer.serialize(slots))
                                                        }
                                                    }
                                                }

                                                val enabledSlots = slots.filter { it.enabled }.sortedBy { it.priority }
                                                if (enabledSlots.isEmpty()) {
                                                    throw LocalPreparationException("No Gemini API keys are configured or enabled in your pool.")
                                                }

                                                val strategy = preferencesRepository.keyStrategyFlow.first()
                                                val lastSuccessId = preferencesRepository.lastSuccessfulKeyIdFlow.first()
                                                val lastRrIndex = preferencesRepository.roundRobinLastKeyIndexFlow.first()

                                                val orderedSlots = when (strategy) {
                                                    "Sticky Success with Sequential Failover" -> {
                                                        val stickyIndex = enabledSlots.indexOfFirst { it.id == lastSuccessId }
                                                        if (stickyIndex >= 0) {
                                                            val list = mutableListOf<GeminiKeySlot>()
                                                            list.add(enabledSlots[stickyIndex])
                                                            for (i in enabledSlots.indices) {
                                                                if (i != stickyIndex) list.add(enabledSlots[i])
                                                            }
                                                            list
                                                        } else enabledSlots
                                                    }
                                                    "Always Start at Key 1" -> enabledSlots
                                                    "Round Robin" -> {
                                                        if (enabledSlots.isNotEmpty()) {
                                                            val startIndex = (lastRrIndex + 1) % enabledSlots.size
                                                            val list = mutableListOf<GeminiKeySlot>()
                                                            for (i in enabledSlots.indices) {
                                                                val idx = (startIndex + i) % enabledSlots.size
                                                                list.add(enabledSlots[idx])
                                                            }
                                                            list
                                                        } else enabledSlots
                                                    }
                                                    else -> enabledSlots
                                                }

                                                val maxAttempts = preferencesRepository.maxKeyAttemptsFlow.first()
                                                val attemptsLimit = kotlin.math.min(maxAttempts, orderedSlots.size)

                                                var responseText: String? = null
                                                var parsedAnswerInfo: String? = null
                                                var successfulSlotId: String? = null
                                                var successfulSlotLabel: String? = null
                                                var lastError: Throwable? = null

                                                for (attempt in 0 until attemptsLimit) {
                                                    val slot = orderedSlots[attempt]
                                                    val key = withContext(Dispatchers.IO) {
                                                        KeyStoreHelper.retrieveSlotKey(context, slot.id)
                                                    }
                                                    if (key.trim().isEmpty()) continue

                                                    try {
                                                        val geminiBaseUrl = preferencesRepository.geminiBaseUrlFlow.first()
                                                        val contextObj = AnalysisRequestContext(
                                                            provider = AiProvider.GEMINI,
                                                            requestedModel = selectedModel,
                                                            normalizedBaseUrl = geminiBaseUrl,
                                                            jpegBytes = jpegBytes,
                                                            imageWidth = 200,
                                                            imageHeight = 200,
                                                            requestStartedAt = System.currentTimeMillis()
                                                        )

                                                        val rawRes = GeminiProviderClient.executeImageRequest(contextObj, key)
                                                        val text = com.example.service.ResponseParser.extractGeminiText(rawRes)
                                                        val parsed = com.example.service.ResponseParser.parse(text)
                                                        responseText = text
                                                        parsedAnswerInfo = when (parsed) {
                                                            is ParsedAnswer.MultipleChoice ->
                                                                "Type: Multiple Choice | Index: ${parsed.answerIndex} (Confidence: ${parsed.confidence ?: "N/A"})"
                                                            is ParsedAnswer.MultipleSelect ->
                                                                "Type: Multiple Select | Indices: (${parsed.answerIndices.joinToString(",")}) (Confidence: ${parsed.confidence ?: "N/A"})"
                                                            is ParsedAnswer.FreeResponse ->
                                                                "Type: Free Response | ${parsed.answerText} (Confidence: ${parsed.confidence ?: "N/A"})"
                                                            is ParsedAnswer.Unclear ->
                                                                "Type: Unclear (Confidence: ${parsed.confidence ?: "N/A"})"
                                                        }
                                                        successfulSlotId = slot.id
                                                        successfulSlotLabel = slot.label

                                                        val updated = slots.map {
                                                            if (it.id == slot.id) {
                                                                it.copy(healthStatus = GeminiKeyHealth.READY.name, lastSuccessTimestamp = System.currentTimeMillis(), lastFailureType = "")
                                                            } else it
                                                        }
                                                        preferencesRepository.setGeminiKeySlotsMetadata(GeminiKeySlotSerializer.serialize(updated))
                                                        preferencesRepository.setLastSuccessfulKeyId(slot.id)
                                                        break
                                                    } catch (e: Exception) {
                                                        lastError = e
                                                        val cooldownSec = preferencesRepository.cooldownDurationSecFlow.first().toLong()
                                                        val action = com.example.service.FailoverDecision.evaluate(e, cooldownSec)
                                                        when (action) {
                                                            is com.example.service.FailoverAction.StopRotation -> {
                                                                val failureType = if (e is ApiException) e.code.toString() else "Error"
                                                                val updated = slots.map {
                                                                    if (it.id == slot.id) {
                                                                        it.copy(healthStatus = GeminiKeyHealth.TEMPORARY_FAILURE.name, lastFailureType = failureType)
                                                                    } else it
                                                                }
                                                                slots = updated.toMutableList()
                                                                preferencesRepository.setGeminiKeySlotsMetadata(GeminiKeySlotSerializer.serialize(updated))
                                                                break
                                                            }
                                                            is com.example.service.FailoverAction.ContinueToNextKey -> {
                                                                val cooldownExp = if (action.cooldownMs > 0) System.currentTimeMillis() + action.cooldownMs else 0L
                                                                val updated = slots.map {
                                                                    if (it.id == slot.id) {
                                                                        it.copy(
                                                                            healthStatus = action.healthStatus,
                                                                            lastFailureType = action.failureType,
                                                                            cooldownExpiration = cooldownExp
                                                                        )
                                                                    } else it
                                                                 }
                                                                 slots = updated.toMutableList()
                                                                 preferencesRepository.setGeminiKeySlotsMetadata(GeminiKeySlotSerializer.serialize(updated))
                                                            }
                                                        }
                                                    }
                                                }

                                                if (responseText != null) {
                                                    val duration = System.currentTimeMillis() - startTime
                                                    withContext(Dispatchers.Main) {
                                                        testOutput = """
                                                            End-to-End Success!
                                                            • Key Slot: Slot #$successfulSlotId ($successfulSlotLabel)
                                                            • Round-trip time: $duration ms
                                                            • Extracted text: $responseText
                                                            • Parsed Answer: $parsedAnswerInfo
                                                        """.trimIndent()
                                                        testRunning = false
                                                    }
                                                } else {
                                                    if (lastError != null) {
                                                        throw lastError
                                                    } else {
                                                        throw LocalPreparationException("No Gemini keys succeeded.")
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    testOutput = "End-to-End check failed: ${e.message}"
                                                    testRunning = false
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("test_e2e_pipeline_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    enabled = !testRunning
                                ) {
                                    if (testRunning) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = MaterialTheme.colorScheme.onPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Running...", fontSize = 11.sp)
                                    } else {
                                        Text("E2E Check", fontSize = 11.sp)
                                    }
                                }

                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val diags = com.example.service.ScreenCaptureService.getStagedDiagnostics()
                                        val clip = android.content.ClipData.newPlainText("ScreenPilot Staged Diagnostics", diags)
                                        clipboard.setPrimaryClip(clip)
                                        android.widget.Toast.makeText(context, "Diagnostics copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .testTag("copy_diagnostics_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                ) {
                                    Text("Copy Diags", fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Preview Feature Notifikasi Essay",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Mengirim contoh jawaban ke panel notifikasi.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    com.example.notification.EssayAnswerNotificationManager.showAnswer(
                                        context,
                                        "Fotosintesis mengubah energi cahaya menjadi energi kimia yang disimpan dalam bentuk glukosa."
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("test_essay_notification_button")
                            ) {
                                Text("Test Notifikasi Essay", fontSize = 12.sp)
                            }

                            testOutput?.let { out ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = out,
                                    fontSize = 12.sp,
                                    color = if (out.startsWith("Local Pipeline Stable") || out.startsWith("End-to-End Success")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // 7. Recent History Screen
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, Color(0xFFE1E1E5)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF6750A4))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Recent History (Last $historyLimit)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            }
                            if (historyList.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            historyRepository.clear()
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear history", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (historyList.isEmpty()) {
                            Text(
                                text = "No history available. Tap the floating button to capture and analyze the screen.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                historyList.take(30).forEach { entry ->
                                    HistoryRowItem(entry = entry)
                                }
                            }
                        }
                    }
                }
            }

            // 8. Device Setup / Troubleshooting Help (Vivo/Funtouch OS)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, Color(0xFFE1E1E5)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFF6750A4))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Vivo Y36 / Funtouch OS 15 Setup",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "To ensure ScreenPilot functions reliably on Funtouch OS and is not terminated by aggressive battery optimization, please perform the following configurations:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        HelpBullet(text = "Allow 'Display over other apps' so the floating pilot overlay can render.")
                        HelpBullet(text = "Enable Notifications to permit the required non-intrusive foreground service notifier.")
                        HelpBullet(text = "Set battery usage setting to 'Unrestricted' and enable 'High background power consumption'.")
                        HelpBullet(text = "Lock ScreenPilot in Recents view and toggle 'Autostart' if available in your build.")
                        HelpBullet(text = "Toggle off 'Manage app if unused' to prevent system from stripping projection permissions.")

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Configure System Pages:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                },
                                shape = CircleShape,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("App Info", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }

                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Battery Optimization setting not accessible directly", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = CircleShape,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Battery", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // Active Session Journal & Prior Process Exit Diagnostics Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("active_session_journal_card"),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, Color(0xFFE1E1E5)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.BugReport, contentDescription = null, tint = Color(0xFF6750A4))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Session Journal & Diagnostics",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        // Current Session Metrics Sub-section
                        Text("Active Session Metrics", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF6750A4))
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (sessionServiceStarted) "Status: RUNNING" else "Status: INACTIVE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (sessionServiceStarted) Color(0xFF2E7D32) else Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        if (sessionServiceStarted) {
                            val activeDurationMin = if (sessionActivationTime > 0L) {
                                val diff = System.currentTimeMillis() - sessionActivationTime
                                val min = diff / 60000
                                val sec = (diff % 60000) / 1000
                                "${min}m ${sec}s"
                            } else {
                                "N/A"
                            }
                            Text("• Session ID: $sessionId", fontSize = 12.sp)
                            Text("• Activation: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(sessionActivationTime))}", fontSize = 12.sp)
                            Text("• Duration Since Active: $activeDurationMin", fontSize = 12.sp)
                            Text("• Health check (last): ${if (sessionLastHealthyTime > 0L) SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(sessionLastHealthyTime)) else "Pending"}", fontSize = 12.sp)
                            Text("• Overlay controls: ${if (sessionFloatingCreated) "Created" else "Not created"}", fontSize = 12.sp)
                            Text("• Projection session: ${if (sessionProjectionInitialized) "Initialized" else "Not initialized"}", fontSize = 12.sp)
                            Text("• Foreground promoted: ${if (sessionForegroundPromoted) "Yes" else "No"}", fontSize = 12.sp)
                        } else {
                            Text("• Last Closed Session Reason: ${sessionShutdownReason.ifEmpty { "Graceful User Stop" }}", fontSize = 12.sp)
                            Text("• Last Shutdown Was Graceful: ${if (sessionGracefulShutdown) "Yes" else "No"}", fontSize = 12.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Last Action Stage: ${sessionLastActionStage.ifEmpty { "None" }}", fontSize = 12.sp, fontWeight = FontWeight.Medium)

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFE1E1E5), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // ApplicationExitInfo exit diagnostic sub-section
                        Text("Prior Process Exit Diagnostic", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFB3261E))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = priorExitReason,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.DarkGray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F0F4), shape = RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFE1E1E5), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Simulation buttons sub-section
                        Text("Developer Fault Simulations", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFE65100))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Simulate system, memory, or network conditions to test robustness.", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (!isServiceActive) {
                                            Toast.makeText(context, "Please start ScreenPilot first", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val intent = Intent(context, ScreenCaptureService::class.java).apply {
                                            action = ScreenCaptureService.ACTION_SIMULATE_LOW_MEM
                                        }
                                        context.startService(intent)
                                        Toast.makeText(context, "Simulated Low Memory event", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Low Memory", fontSize = 11.sp, color = Color.White)
                                }

                                Button(
                                    onClick = {
                                        if (!isServiceActive) {
                                            Toast.makeText(context, "Please start ScreenPilot first", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val intent = Intent(context, ScreenCaptureService::class.java).apply {
                                            action = ScreenCaptureService.ACTION_SIMULATE_TIMEOUT
                                        }
                                        context.startService(intent)
                                        Toast.makeText(context, "Simulated network timeout", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Timeout", fontSize = 11.sp, color = Color.White)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (!isServiceActive) {
                                            Toast.makeText(context, "Please start ScreenPilot first", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val intent = Intent(context, ScreenCaptureService::class.java).apply {
                                            action = ScreenCaptureService.ACTION_SIMULATE_GALLERY_FAIL
                                        }
                                        context.startService(intent)
                                        Toast.makeText(context, "Simulated local gallery failure", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Gallery Fail", fontSize = 11.sp, color = Color.White)
                                }

                                Button(
                                    onClick = {
                                        if (!isServiceActive) {
                                            Toast.makeText(context, "Please start ScreenPilot first", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val intent = Intent(context, ScreenCaptureService::class.java).apply {
                                            action = ScreenCaptureService.ACTION_SIMULATE_PARSING_FAIL
                                        }
                                        context.startService(intent)
                                        Toast.makeText(context, "Simulated response parsing failure", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Parse Fail", fontSize = 11.sp, color = Color.White)
                                }
                            }

                            Button(
                                onClick = {
                                    if (!isServiceActive) {
                                        Toast.makeText(context, "Please start ScreenPilot first", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val intent = Intent(context, ScreenCaptureService::class.java).apply {
                                        action = ScreenCaptureService.ACTION_SIMULATE_COROUTINE_FAIL
                                    }
                                    context.startService(intent)
                                    Toast.makeText(context, "Simulated Coroutine Exception", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Simulate Exception (Crash Isolation)", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // 9. Privacy Disclosure
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F0F5)),
                    border = BorderStroke(1.dp, Color(0xFFE7E0EB)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF6750A4))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Privacy Disclosure", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "• Screen content is captured ONLY when you explicitly tap the floating circular button.\n" +
                                    "• The captured frame is resized, optimized, and securely transmitted via HTTPS directly to your configured Gemini API endpoint.\n" +
                                    "• Screenshots are written to Pictures/ScreenPilot in your local gallery ONLY when 'Save screenshots to gallery' is enabled in Settings.\n" +
                                    "• ScreenPilot is designed purely for user accessibility and analysis assistance. It NEVER performs automated answer selections or clicks.\n" +
                                    "• Secure windows protected by target systems (e.g., DRM or secure entry forms) cannot be captured.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun PermissionRow(name: String, isGranted: Boolean, onRequest: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFF1B1B1F))
            Text(
                text = if (isGranted) "Permission Granted" else "Action Required",
                fontSize = 12.sp,
                color = if (isGranted) Color(0xFF6750A4) else Color(0xFFB3261E),
                fontWeight = FontWeight.Normal
            )
        }
        if (!isGranted) {
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4), contentColor = Color.White),
                shape = CircleShape
            ) {
                Text("Authorize", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        } else {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Permission Granted Indicator",
                tint = Color(0xFF6750A4),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun HistoryRowItem(entry: HistoryEntry) {
    val dateString = remember(entry.timestamp) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        sdf.format(Date(entry.timestamp))
    }

    val normalizedType = when (entry.questionType) {
        HistoryQuestionType.MULTIPLE_CHOICE,
        HistoryQuestionType.MULTIPLE_SELECT,
        HistoryQuestionType.FREE_RESPONSE,
        HistoryQuestionType.UNCLEAR,
        HistoryQuestionType.ERROR -> entry.questionType
        else -> if (entry.answerIndex in 1..5) {
            HistoryQuestionType.MULTIPLE_CHOICE
        } else {
            HistoryQuestionType.ERROR
        }
    }

    val badgeBackground = when (normalizedType) {
        HistoryQuestionType.MULTIPLE_CHOICE,
        HistoryQuestionType.MULTIPLE_SELECT,
        HistoryQuestionType.FREE_RESPONSE -> Color(0xFFE8F5E9)
        HistoryQuestionType.UNCLEAR -> Color(0xFFFFF4E5)
        else -> Color(0xFFFFEBEE)
    }
    val badgeTextColor = when (normalizedType) {
        HistoryQuestionType.MULTIPLE_CHOICE,
        HistoryQuestionType.MULTIPLE_SELECT,
        HistoryQuestionType.FREE_RESPONSE -> Color(0xFF2E7D32)
        HistoryQuestionType.UNCLEAR -> Color(0xFF8A5300)
        else -> Color(0xFFC62828)
    }
    val badgeText = when (normalizedType) {
        HistoryQuestionType.MULTIPLE_CHOICE -> "Answer: ${entry.answerIndex}"
        HistoryQuestionType.MULTIPLE_SELECT -> {
            val indices = entry.answerText.orEmpty().trim()
            if (indices.isNotEmpty()) "Answers: ($indices)" else "Multiple answers"
        }
        HistoryQuestionType.FREE_RESPONSE -> "Essay"
        HistoryQuestionType.UNCLEAR -> "Unclear"
        else -> "Failed"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE1E1E5)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(badgeBackground)
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = badgeTextColor
                        )
                    }
                    entry.confidence?.let { conf ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(conf * 100).toInt()}% conf",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF44474E)
                        )
                    }
                }

                val essayAnswer = entry.answerText
                if (normalizedType == HistoryQuestionType.FREE_RESPONSE && !essayAnswer.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = essayAnswer,
                        fontSize = 12.sp,
                        color = Color(0xFF1B1B1F),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${entry.modelName} • ${entry.requestDurationMs}ms" +
                            if (entry.imageCount > 1) " • Images: ${entry.imageCount} (${entry.captureMode})" else "",
                    fontSize = 12.sp,
                    color = Color(0xFF44474E)
                )
                entry.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = error,
                        fontSize = 11.sp,
                        color = Color(0xFFB3261E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = dateString,
                fontSize = 11.sp,
                color = Color(0xFF74777F)
            )
        }
    }
}

@Composable
fun HelpBullet(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = "• ", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(text = text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

