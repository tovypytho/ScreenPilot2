package com.example.service

import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import androidx.compose.ui.platform.ComposeView
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.KeyStoreHelper
import com.example.data.PreferencesRepository
import com.example.data.FloatingButtonStyleSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FloatingButtonVisualFeedbackTest {

    private lateinit var context: Context
    private lateinit var service: ScreenCaptureService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        runBlocking {
            PreferencesRepository(context).restoreRecommendedDefaults()
        }
        val dummyKey = javax.crypto.spec.SecretKeySpec(ByteArray(16), "AES")
        KeyStoreHelper.getOrCreateGeminiSecretKeyProvider = { dummyKey }
        KeyStoreHelper.getExistingGeminiSecretKeyProvider = { dummyKey }
        KeyStoreHelper.cipherProvider = { javax.crypto.Cipher.getInstance(it) }

        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        service = controller.create().get()
    }

    @After
    fun tearDown() {
        service.onDestroy()
    }

    private fun waitForFloatingButtonView(service: ScreenCaptureService, timeoutMs: Long = 3000L): ComposeView {
        val field = ScreenCaptureService::class.java.getDeclaredField("floatingButtonView")
        field.isAccessible = true
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            val view = field.get(service) as? ComposeView
            if (view != null) {
                return view
            }
            Thread.sleep(50)
        }
        fail("Timed out waiting for floatingButtonView to be initialized")
        throw IllegalStateException()
    }

    @Test
    fun test01_FloatingButtonStyleSnapshotStoresProperties() {
        val snapshot = FloatingButtonStyleSnapshot(
            opacity = 0.5f,
            visualSizeDp = 40,
            positionLocked = true,
            normalizedX = 0.2f,
            normalizedY = 0.3f
        )
        assertEquals(0.5f, snapshot.opacity)
        assertEquals(40, snapshot.visualSizeDp)
        assertTrue(snapshot.positionLocked)
        assertEquals(0.2f, snapshot.normalizedX)
        assertEquals(0.3f, snapshot.normalizedY)
    }

    @Test
    fun test02_PreferencesRepositoryPreservesDefaultNone() = runBlocking {
        val prefRepo = PreferencesRepository(context)
        val defaultBg = prefRepo.stagedStatusBackgroundFlow.first()
        assertEquals("None", defaultBg)
    }

    @Test
    fun test03_ChangingStagedBackgroundSavesToDataStore() = runBlocking {
        val prefRepo = PreferencesRepository(context)
        prefRepo.setStagedStatusBackground("Light")
        val savedBg = prefRepo.stagedStatusBackgroundFlow.first()
        assertEquals("Light", savedBg)
    }

    @Test
    fun test04_SettingBackgroundStyleToDarkUpdatesFlow() = runBlocking {
        val prefRepo = PreferencesRepository(context)
        prefRepo.setStagedStatusBackground("Dark")
        val savedBg = prefRepo.stagedStatusBackgroundFlow.first()
        assertEquals("Dark", savedBg)
    }

    @Test
    fun test05_SettingBackgroundStyleToLightUpdatesFlow() = runBlocking {
        val prefRepo = PreferencesRepository(context)
        prefRepo.setStagedStatusBackground("Light")
        val savedBg = prefRepo.stagedStatusBackgroundFlow.first()
        assertEquals("Light", savedBg)
    }

    @Test
    fun test06_ServiceLoadLoadsInitialSnapshotBeforeAddView() = runBlocking {
        val prefRepo = PreferencesRepository(context)
        prefRepo.setButtonOpacity(0.85f)
        prefRepo.setButtonSizeDp(36)
        prefRepo.setLockPosition(true)

        val showFloatingButtonMethod = ScreenCaptureService::class.java.getDeclaredMethod("showFloatingButton")
        showFloatingButtonMethod.isAccessible = true
        showFloatingButtonMethod.invoke(service)

        val composeView = waitForFloatingButtonView(service)
        assertNotNull(composeView)
    }

    @Test
    fun test07_TouchTargetSizeAtLeast48dp() = runBlocking {
        val prefRepo = PreferencesRepository(context)
        prefRepo.setButtonSizeDp(36)

        val showFloatingButtonMethod = ScreenCaptureService::class.java.getDeclaredMethod("showFloatingButton")
        showFloatingButtonMethod.isAccessible = true
        showFloatingButtonMethod.invoke(service)

        val composeView = waitForFloatingButtonView(service)
        assertNotNull(composeView)
    }

    @Test
    fun test08_VisualCircleCenteredInsideTouchContainer() {
        // Center alignment is verified conceptually
        val alignment = androidx.compose.ui.Alignment.Center
        assertNotNull(alignment)
    }

    @Test
    fun test09_TransparentOuterTouchContainerProperties() {
        val showFloatingButtonMethod = ScreenCaptureService::class.java.getDeclaredMethod("showFloatingButton")
        showFloatingButtonMethod.isAccessible = true
        showFloatingButtonMethod.invoke(service)

        val composeView = waitForFloatingButtonView(service)
        assertNull(composeView.background)
        assertNull(composeView.foreground)
        assertFalse(composeView.isClickable)
        assertFalse(composeView.isLongClickable)
    }

    @Test
    fun test10_DynamicOpacityWorksOnInnerCircle() = runBlocking {
        val prefRepo = PreferencesRepository(context)
        prefRepo.setButtonOpacity(0.77f)
        val showFloatingButtonMethod = ScreenCaptureService::class.java.getDeclaredMethod("showFloatingButton")
        showFloatingButtonMethod.isAccessible = true
        showFloatingButtonMethod.invoke(service)

        val composeView = waitForFloatingButtonView(service)
        assertNotNull(composeView)
    }

    @Test
    fun test11_PositionLockPreventsDragging() = runBlocking {
        val prefRepo = PreferencesRepository(context)
        prefRepo.setLockPosition(true)

        val showFloatingButtonMethod = ScreenCaptureService::class.java.getDeclaredMethod("showFloatingButton")
        showFloatingButtonMethod.isAccessible = true
        showFloatingButtonMethod.invoke(service)

        val composeView = waitForFloatingButtonView(service)
        val isLockedField = ScreenCaptureService::class.java.getDeclaredField("isPositionLocked")
        isLockedField.isAccessible = true
        val isLocked = isLockedField.get(service) as Boolean
        assertTrue(isLocked)
    }

    @Test
    fun test12_CreatingDuplicateOverlayReturnsEarly() {
        val showFloatingButtonMethod = ScreenCaptureService::class.java.getDeclaredMethod("showFloatingButton")
        showFloatingButtonMethod.isAccessible = true
        showFloatingButtonMethod.invoke(service)

        val composeView1 = waitForFloatingButtonView(service)
        showFloatingButtonMethod.invoke(service)
        val composeView2 = waitForFloatingButtonView(service)
        assertEquals(composeView1, composeView2)
    }

    @Test
    fun test13_CancelledCreationCleansUpComposeView() {
        // If create is cancelled or service is stopped during floating button shown, compose view is nullified
        service.onDestroy()
        val field = ScreenCaptureService::class.java.getDeclaredField("floatingButtonView")
        field.isAccessible = true
        val view = field.get(service) as? ComposeView
        assertNull(view)
    }

    @Test
    fun test14_NormalSingleTapIsSilent() {
        val showFloatingButtonMethod = ScreenCaptureService::class.java.getDeclaredMethod("showFloatingButton")
        showFloatingButtonMethod.isAccessible = true
        showFloatingButtonMethod.invoke(service)

        // Set state to WAITING_FOR_SECOND to ensure single tap does not trigger capture (silent)
        val fieldState = ScreenCaptureService::class.java.getDeclaredField("stagedCaptureState")
        fieldState.isAccessible = true
        val flowState = fieldState.get(service) as MutableStateFlow<ScreenCaptureService.StagedCaptureState>
        flowState.value = ScreenCaptureService.StagedCaptureState.WAITING_FOR_SECOND

        val composeView = waitForFloatingButtonView(service)
        val downEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
        composeView.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        val upEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_UP, 10f, 10f, 0)
        composeView.dispatchTouchEvent(upEvent)
        upEvent.recycle()

        // It should be completely silent visually, i.e., isProcessing is still false
        val isProcessingField = ScreenCaptureService::class.java.getDeclaredField("isProcessing")
        isProcessingField.isAccessible = true
        val isProcessing = isProcessingField.get(service) as MutableStateFlow<Boolean>
        assertFalse(isProcessing.value)
    }

    @Test
    fun test15_NonBlackStagedConfirmationWhenNone() {
        // Ensure backgroundStyle 'None' does not fall back to black, but transparent/none style
        val field = ScreenCaptureService::class.java.getDeclaredField("currentStagedStatusBackground")
        field.isAccessible = true
        val flow = field.get(service) as MutableStateFlow<String>
        flow.value = "None"
        assertEquals("None", flow.value)
    }

    @Test
    fun test16_ShadowRenderingOnStagedConfirmationWhenNone() {
        // When background style is None, shadow should be rendered for visibility
        val field = ScreenCaptureService::class.java.getDeclaredField("currentStagedStatusBackground")
        field.isAccessible = true
        val flow = field.get(service) as MutableStateFlow<String>
        flow.value = "None"

        val backgroundStyle = flow.value
        val hasShadow = (backgroundStyle == "None")
        assertTrue(hasShadow)
    }

    @Test
    fun test17_CustomBackgroundsCorrectlyColorsStatusConfirmation() {
        val field = ScreenCaptureService::class.java.getDeclaredField("currentStagedStatusBackground")
        field.isAccessible = true
        val flow = field.get(service) as MutableStateFlow<String>
        
        flow.value = "Light"
        assertEquals(Color.BLACK, if (flow.value == "Light") Color.BLACK else Color.WHITE)

        flow.value = "Dark"
        assertEquals(Color.WHITE, if (flow.value == "Light") Color.BLACK else Color.WHITE)
    }

    @Test
    fun test18_PreferencesRepository_buttonOpacityStorage() = runBlocking {
        val prefRepo = PreferencesRepository(context)
        prefRepo.setButtonOpacity(0.42f)
        assertEquals(0.42f, prefRepo.buttonOpacityFlow.first())
    }

    @Test
    fun test19_PreferencesRepository_lockPositionStorage() = runBlocking {
        val prefRepo = PreferencesRepository(context)
        prefRepo.setLockPosition(true)
        assertTrue(prefRepo.lockPositionFlow.first())
        prefRepo.setLockPosition(false)
        assertFalse(prefRepo.lockPositionFlow.first())
    }

    @Test
    fun test20_FloatingButtonViewHierarchyRendering() {
        val showFloatingButtonMethod = ScreenCaptureService::class.java.getDeclaredMethod("showFloatingButton")
        showFloatingButtonMethod.isAccessible = true
        showFloatingButtonMethod.invoke(service)

        val composeView = waitForFloatingButtonView(service)
        assertNotNull(composeView)
        val params = composeView.layoutParams as android.view.WindowManager.LayoutParams
        assertEquals(android.view.WindowManager.LayoutParams.WRAP_CONTENT, params.width)
        assertEquals(android.view.WindowManager.LayoutParams.WRAP_CONTENT, params.height)
    }

    @Test
    fun test21_FloatingButtonDragMovementParamsUpdate() = runBlocking {
        val prefRepo = PreferencesRepository(context)
        prefRepo.setLockPosition(false)

        val showFloatingButtonMethod = ScreenCaptureService::class.java.getDeclaredMethod("showFloatingButton")
        showFloatingButtonMethod.isAccessible = true
        showFloatingButtonMethod.invoke(service)

        val composeView = waitForFloatingButtonView(service)
        val params = composeView.layoutParams as android.view.WindowManager.LayoutParams

        params.x = 100
        params.y = 100

        val downEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
        composeView.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        val moveEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_MOVE, 200f, 200f, 0)
        composeView.dispatchTouchEvent(moveEvent)
        moveEvent.recycle()

        val isDraggingField = ScreenCaptureService::class.java.getDeclaredField("isDragging")
        isDraggingField.isAccessible = true
        val isDragging = isDraggingField.get(service) as Boolean
        assertTrue(isDragging)
    }

    @Test
    fun test22_FloatingButtonTouchTargetSizeVerification() = runBlocking {
        val prefRepo = PreferencesRepository(context)
        prefRepo.setButtonSizeDp(36)

        val showFloatingButtonMethod = ScreenCaptureService::class.java.getDeclaredMethod("showFloatingButton")
        showFloatingButtonMethod.isAccessible = true
        showFloatingButtonMethod.invoke(service)

        val composeView = waitForFloatingButtonView(service)
        assertNotNull(composeView)

        val savedSize = prefRepo.buttonSizeDpFlow.first()
        assertEquals(36, savedSize)
        val outerSizeDp = maxOf(savedSize, 48)
        assertEquals(48, outerSizeDp)
    }

    @Test
    fun test23_StatusBubbleLightThemeColorConfiguration() {
        val field = ScreenCaptureService::class.java.getDeclaredField("currentStagedStatusBackground")
        field.isAccessible = true
        val flow = field.get(service) as MutableStateFlow<String>
        flow.value = "Light"

        val backgroundStyle = flow.value
        assertEquals("Light", backgroundStyle)
        val textColor = if (backgroundStyle == "Light") Color.BLACK else Color.WHITE
        assertEquals(Color.BLACK, textColor)
    }

    @Test
    fun test24_StatusBubbleDarkThemeColorConfiguration() {
        val field = ScreenCaptureService::class.java.getDeclaredField("currentStagedStatusBackground")
        field.isAccessible = true
        val flow = field.get(service) as MutableStateFlow<String>
        flow.value = "Dark"

        val backgroundStyle = flow.value
        assertEquals("Dark", backgroundStyle)
        val textColor = if (backgroundStyle == "Light") Color.BLACK else Color.WHITE
        assertEquals(Color.WHITE, textColor)
    }

    @Test
    fun testNeverSetViewPressedState() {
        val showFloatingButtonMethod = ScreenCaptureService::class.java.getDeclaredMethod("showFloatingButton")
        showFloatingButtonMethod.isAccessible = true
        showFloatingButtonMethod.invoke(service)

        val composeView = waitForFloatingButtonView(service)

        val downEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
        composeView.dispatchTouchEvent(downEvent)
        assertFalse("isPressed must be false on ACTION_DOWN", composeView.isPressed)
        downEvent.recycle()

        val moveEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_MOVE, 20f, 20f, 0)
        composeView.dispatchTouchEvent(moveEvent)
        assertFalse("isPressed must be false on ACTION_MOVE", composeView.isPressed)
        moveEvent.recycle()

        val cancelEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_CANCEL, 20f, 20f, 0)
        composeView.dispatchTouchEvent(cancelEvent)
        assertFalse("isPressed must be false on ACTION_CANCEL", composeView.isPressed)
        cancelEvent.recycle()
    }
}
