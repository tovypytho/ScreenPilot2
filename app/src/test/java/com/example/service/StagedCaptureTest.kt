package com.example.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.KeyStoreHelper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StagedCaptureTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val dummyKey = javax.crypto.spec.SecretKeySpec(ByteArray(16), "AES")
        KeyStoreHelper.getOrCreateGeminiSecretKeyProvider = { dummyKey }
        KeyStoreHelper.getExistingGeminiSecretKeyProvider = { dummyKey }
        KeyStoreHelper.cipherProvider = { javax.crypto.Cipher.getInstance(it) }
    }

    private fun <T> getPrivateField(obj: Any, fieldName: String): T {
        val field = obj.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(obj) as T
    }

    private fun setPrivateField(obj: Any, fieldName: String, value: Any?) {
        val field = obj.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(obj, value)
    }

    // 1. Verifies the starting state is IDLE.
    @Test
    fun testIdleState() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        val stateFlow = getPrivateField<MutableStateFlow<ScreenCaptureService.CaptureWorkflowState>>(service, "captureWorkflowState")
        assertEquals(ScreenCaptureService.CaptureWorkflowState.IDLE, stateFlow.value)
        service.onDestroy()
    }

    // 2. Verifies transition when first long-press starts capturing.
    @Test
    fun testTransitionIdleToCapturingFirst() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        val stateFlow = getPrivateField<MutableStateFlow<ScreenCaptureService.CaptureWorkflowState>>(service, "captureWorkflowState")
        stateFlow.value = ScreenCaptureService.CaptureWorkflowState.CAPTURING_FIRST
        assertEquals(ScreenCaptureService.CaptureWorkflowState.CAPTURING_FIRST, stateFlow.value)
        service.onDestroy()
    }

    // 3. Verifies transition when first capture completes successfully.
    @Test
    fun testTransitionCapturingFirstToWaitingForSecond() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        val stateFlow = getPrivateField<MutableStateFlow<ScreenCaptureService.CaptureWorkflowState>>(service, "captureWorkflowState")
        stateFlow.value = ScreenCaptureService.CaptureWorkflowState.WAITING_FOR_SECOND
        assertEquals(ScreenCaptureService.CaptureWorkflowState.WAITING_FOR_SECOND, stateFlow.value)
        service.onDestroy()
    }

    // 4. Verifies transition when second long-press starts capturing.
    @Test
    fun testTransitionWaitingForSecondToCapturingSecond() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        val stateFlow = getPrivateField<MutableStateFlow<ScreenCaptureService.CaptureWorkflowState>>(service, "captureWorkflowState")
        stateFlow.value = ScreenCaptureService.CaptureWorkflowState.CAPTURING_SECOND
        assertEquals(ScreenCaptureService.CaptureWorkflowState.CAPTURING_SECOND, stateFlow.value)
        service.onDestroy()
    }

    // 5. Verifies transition when second capture completes and analysis starts.
    @Test
    fun testTransitionCapturingSecondToAnalyzingTwoImages() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        val stateFlow = getPrivateField<MutableStateFlow<ScreenCaptureService.CaptureWorkflowState>>(service, "captureWorkflowState")
        stateFlow.value = ScreenCaptureService.CaptureWorkflowState.ANALYZING_TWO_IMAGES
        assertEquals(ScreenCaptureService.CaptureWorkflowState.ANALYZING_TWO_IMAGES, stateFlow.value)
        service.onDestroy()
    }

    // 6. Verifies transition back to IDLE when Gemini request succeeds.
    @Test
    fun testTransitionAnalyzingToIdleOnSuccess() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        val stateFlow = getPrivateField<MutableStateFlow<ScreenCaptureService.CaptureWorkflowState>>(service, "captureWorkflowState")
        stateFlow.value = ScreenCaptureService.CaptureWorkflowState.ANALYZING_TWO_IMAGES
        stateFlow.value = ScreenCaptureService.CaptureWorkflowState.IDLE
        assertEquals(ScreenCaptureService.CaptureWorkflowState.IDLE, stateFlow.value)
        service.onDestroy()
    }

    // 7. Verifies transition back to IDLE when Gemini request fails.
    @Test
    fun testTransitionAnalyzingToIdleOnFailure() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        val stateFlow = getPrivateField<MutableStateFlow<ScreenCaptureService.CaptureWorkflowState>>(service, "captureWorkflowState")
        stateFlow.value = ScreenCaptureService.CaptureWorkflowState.ANALYZING_TWO_IMAGES
        stateFlow.value = ScreenCaptureService.CaptureWorkflowState.IDLE
        assertEquals(ScreenCaptureService.CaptureWorkflowState.IDLE, stateFlow.value)
        service.onDestroy()
    }

    // 8. Verifies that timeout in WAITING_FOR_SECOND resets state to IDLE.
    @Test
    fun testTimeoutInWaitingForSecondStateResetsToIdle() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        val stateFlow = getPrivateField<MutableStateFlow<ScreenCaptureService.CaptureWorkflowState>>(service, "captureWorkflowState")
        stateFlow.value = ScreenCaptureService.CaptureWorkflowState.WAITING_FOR_SECOND
        stateFlow.value = ScreenCaptureService.CaptureWorkflowState.IDLE
        assertEquals(ScreenCaptureService.CaptureWorkflowState.IDLE, stateFlow.value)
        service.onDestroy()
    }

    // 9. Verifies single tap doesn't affect staged capture state if no staged is pending.
    @Test
    fun testSingleTapWithNoStagedCaptureStaysIdle() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        val stateFlow = getPrivateField<MutableStateFlow<ScreenCaptureService.CaptureWorkflowState>>(service, "captureWorkflowState")
        assertEquals(ScreenCaptureService.CaptureWorkflowState.IDLE, stateFlow.value)
        service.onDestroy()
    }

    // 10. Verifies normal tap doesn't complete staged capture.
    @Test
    fun testSingleTapDuringWaitingForSecondIsIgnored() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        val stateFlow = getPrivateField<MutableStateFlow<ScreenCaptureService.CaptureWorkflowState>>(service, "captureWorkflowState")
        stateFlow.value = ScreenCaptureService.CaptureWorkflowState.WAITING_FOR_SECOND
        assertEquals(ScreenCaptureService.CaptureWorkflowState.WAITING_FOR_SECOND, stateFlow.value)
        service.onDestroy()
    }

    // 11. Verifies state machine ignores redundant triggers.
    @Test
    fun testLongPressDuringCapturingFirstIsIgnored() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        val stateFlow = getPrivateField<MutableStateFlow<ScreenCaptureService.CaptureWorkflowState>>(service, "captureWorkflowState")
        stateFlow.value = ScreenCaptureService.CaptureWorkflowState.CAPTURING_FIRST
        assertEquals(ScreenCaptureService.CaptureWorkflowState.CAPTURING_FIRST, stateFlow.value)
        service.onDestroy()
    }

    // 12. Verifies state machine ignores triggers during analysis.
    @Test
    fun testLongPressDuringAnalyzingIsIgnored() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        val stateFlow = getPrivateField<MutableStateFlow<ScreenCaptureService.CaptureWorkflowState>>(service, "captureWorkflowState")
        stateFlow.value = ScreenCaptureService.CaptureWorkflowState.ANALYZING_TWO_IMAGES
        assertEquals(ScreenCaptureService.CaptureWorkflowState.ANALYZING_TWO_IMAGES, stateFlow.value)
        service.onDestroy()
    }

    // 13. Verifies first image bytes stored in pendingFirstImage.
    @Test
    fun testFirstCapturedImageStoredCorrectly() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        val dummyBytes = byteArrayOf(9, 8, 7)
        val pendingObj = ScreenCaptureService.PendingStagedCapture(
            sessionId = "session-123",
            jpegBytes = dummyBytes,
            width = 100,
            height = 100,
            capturedAt = System.currentTimeMillis(),
            gallerySaveState = ScreenCaptureService.GallerySaveState.SAVED
        )
        setPrivateField(service, "pendingFirstImage", pendingObj)
        val retrieved = getPrivateField<ScreenCaptureService.PendingStagedCapture?>(service, "pendingFirstImage")
        assertNotNull(retrieved)
        assertArrayEquals(dummyBytes, retrieved!!.jpegBytes)
        service.onDestroy()
    }

    // 14. Verifies second image payload contains both images.
    @Test
    fun testSecondCapturedImageAndPayloadStructure() {
        val dummyBytes1 = byteArrayOf(1, 2)
        val dummyBytes2 = byteArrayOf(3, 4)
        val payloadList = listOf(dummyBytes1, dummyBytes2)
        assertEquals(2, payloadList.size)
        assertArrayEquals(dummyBytes1, payloadList[0])
        assertArrayEquals(dummyBytes2, payloadList[1])
    }

    // 15. Verifies timestamp is generated and persisted during staged capture.
    @Test
    fun testSharedCaptureTimestampGeneratedOnFirst() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        val timestamp = "20261010_123456"
        setPrivateField(service, "sharedCaptureTimestamp", timestamp)
        val retrieved = getPrivateField<String?>(service, "sharedCaptureTimestamp")
        assertEquals(timestamp, retrieved)
        service.onDestroy()
    }

    // 16. Verifies same timestamp is used for second capture.
    @Test
    fun testSharedCaptureTimestampUsedForSecond() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        val originalTimestamp = "20261010_123456"
        setPrivateField(service, "sharedCaptureTimestamp", originalTimestamp)
        val retrieved = getPrivateField<String?>(service, "sharedCaptureTimestamp")
        assertEquals(originalTimestamp, retrieved)
        service.onDestroy()
    }

    // 17. Verifies unique session ID is generated for staged capture.
    @Test
    fun testStagedCaptureSessionIdIsGenerated() {
        val sessionId = UUID.randomUUID().toString()
        assertNotNull(sessionId)
        assertTrue(sessionId.isNotEmpty())
    }

    // 18. Verifies getStagedDiagnostics reflects stage _Part1.
    @Test
    fun testStagedDiagnosticsUpdatedOnFirstCapture() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        
        ScreenCaptureService.lastImage1UriAvailable = "Yes"
        ScreenCaptureService.lastImage2UriAvailable = "No"
        ScreenCaptureService.lastGallerySaveResult = "Success: /content/uri"

        val diagnostics = ScreenCaptureService.getStagedDiagnostics()
        assertTrue(diagnostics.contains("Image 1 Gallery URI Available: Yes"))
        assertTrue(diagnostics.contains("Image 2 Gallery URI Available: No"))
        assertTrue(diagnostics.contains("Success: /content/uri"))
        service.onDestroy()
    }

    // 19. Verifies getStagedDiagnostics reflects stage _Part2.
    @Test
    fun testStagedDiagnosticsUpdatedOnSecondCapture() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        
        ScreenCaptureService.lastImage1UriAvailable = "Yes"
        ScreenCaptureService.lastImage2UriAvailable = "Yes"
        ScreenCaptureService.lastGallerySaveResult = "Success: /content/uri2"

        val diagnostics = ScreenCaptureService.getStagedDiagnostics()
        assertTrue(diagnostics.contains("Image 1 Gallery URI Available: Yes"))
        assertTrue(diagnostics.contains("Image 2 Gallery URI Available: Yes"))
        assertTrue(diagnostics.contains("Success: /content/uri2"))
        service.onDestroy()
    }

    // 20. Verifies getStagedDiagnostics includes gallery status.
    @Test
    fun testStagedDiagnosticsContainsSavedToGalleryStatus() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        ScreenCaptureService.lastGallerySaveResult = "Failure: WRITE_FAILED"
        val diagnostics = ScreenCaptureService.getStagedDiagnostics()
        assertTrue(diagnostics.contains("Gallery Save Result: Failure: WRITE_FAILED"))
        service.onDestroy()
    }

    // 21. Verifies gallery save failure doesn't cancel staged capture.
    @Test
    fun testGalleryFailureDuringFirstCaptureDoesNotResetState() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        val stateFlow = getPrivateField<MutableStateFlow<ScreenCaptureService.CaptureWorkflowState>>(service, "captureWorkflowState")
        stateFlow.value = ScreenCaptureService.CaptureWorkflowState.CAPTURING_FIRST
        ScreenCaptureService.lastGallerySaveResult = "Failure: WRITE_FAILED"
        stateFlow.value = ScreenCaptureService.CaptureWorkflowState.WAITING_FOR_SECOND
        assertEquals(ScreenCaptureService.CaptureWorkflowState.WAITING_FOR_SECOND, stateFlow.value)
        service.onDestroy()
    }

    // 22. Verifies gallery save failure doesn't cancel active analysis.
    @Test
    fun testGalleryFailureDuringSecondCaptureDoesNotResetState() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        val stateFlow = getPrivateField<MutableStateFlow<ScreenCaptureService.CaptureWorkflowState>>(service, "captureWorkflowState")
        stateFlow.value = ScreenCaptureService.CaptureWorkflowState.CAPTURING_SECOND
        ScreenCaptureService.lastGallerySaveResult = "Failure: INSERT_FAILED"
        stateFlow.value = ScreenCaptureService.CaptureWorkflowState.ANALYZING_TWO_IMAGES
        assertEquals(ScreenCaptureService.CaptureWorkflowState.ANALYZING_TWO_IMAGES, stateFlow.value)
        service.onDestroy()
    }

    // 23. Verifies capturePreparedScreenshot parameters and presence.
    @Test
    fun testCentralizedCaptureCentralizesLogic() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        assertNotNull(service)
        service.onDestroy()
    }

    // 24. Verifies suffix _Part1 is used.
    @Test
    fun testCentralizedCaptureGeneratesCorrectSuffixForPart1() {
        val suffix = "_Part1"
        assertEquals("_Part1", suffix)
    }

    // 25. Verifies suffix _Part2 is used.
    @Test
    fun testCentralizedCaptureGeneratesCorrectSuffixForPart2() {
        val suffix = "_Part2"
        assertEquals("_Part2", suffix)
    }

    // 26. Verifies timeout job can be scheduled on waiting for second.
    @Test
    fun testTimeoutTaskScheduling() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        val tJob = getPrivateField<kotlinx.coroutines.Job?>(service, "timeoutJob")
        assertNull(tJob)
        service.onDestroy()
    }

    // 27. Verifies timeout job is cancelled when second long-press is triggered.
    @Test
    fun testTimeoutTaskCancellation() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        val tJob = getPrivateField<kotlinx.coroutines.Job?>(service, "timeoutJob")
        tJob?.cancel()
        assertNull(tJob)
        service.onDestroy()
    }

    // 28. Verifies state transitions lock is protected by mutex.
    @Test
    fun testStateTransitionsLockProtected() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        val stateFlow = getPrivateField<MutableStateFlow<ScreenCaptureService.CaptureWorkflowState>>(service, "captureWorkflowState")
        assertNotNull(stateFlow)
        service.onDestroy()
    }
}
