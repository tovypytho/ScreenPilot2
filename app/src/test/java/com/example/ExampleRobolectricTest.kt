package com.example

import android.content.Context
import android.os.Handler
import androidx.test.core.app.ApplicationProvider
import com.example.data.GeminiKeySlot
import com.example.data.GeminiKeySlotSerializer
import com.example.data.KeyStoreHelper
import com.example.data.PreferencesRepository
import com.example.service.AiProvider
import com.example.service.AnalysisRequestContext
import com.example.service.FailoverAction
import com.example.service.FailoverDecision
import com.example.service.ProviderGateway
import com.example.service.ScreenCaptureService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.coroutines.EmptyCoroutineContext

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val dummyKey = javax.crypto.spec.SecretKeySpec(ByteArray(16), "AES")
        KeyStoreHelper.getOrCreateGeminiSecretKeyProvider = { dummyKey }
        KeyStoreHelper.getExistingGeminiSecretKeyProvider = { dummyKey }
        KeyStoreHelper.cipherProvider = { javax.crypto.Cipher.getInstance(it) }
    }

    @Test
    fun testReadStringFromContext() {
        val appName = context.getString(R.string.app_name)
        assertEquals("ScreenPilot", appName)
    }

    @Test
    fun testKeyStoreHelperEncryptDecryptRoundtripAndErrorHandling() {
        val storeRes = KeyStoreHelper.storeGeminiApiKey(context, "test-api-key-abc")
        assertTrue(storeRes.isSuccess)

        val retrieveRes = KeyStoreHelper.getGeminiApiKey(context)
        assertEquals("test-api-key-abc", retrieveRes)

        // For non-existent alias (or decryption failure)
        KeyStoreHelper.getExistingGeminiSecretKeyProvider = { null }
        val missingRes = KeyStoreHelper.getGeminiApiKey(context)
        assertEquals("", missingRes)
    }

    @Test
    fun testScreenCaptureServiceTimeoutExceptionMessage() {
        val ex = ScreenCaptureService.CaptureTimeoutException("Screen capture timed out waiting for fresh frame")
        assertEquals("Screen capture timed out waiting for fresh frame", ex.message)
    }

    @Test
    fun testScreenCaptureServiceIdempotentShutdownBehavior() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()
        
        // Destroy multiple times to verify idempotent cleanup with no crashes
        service.onDestroy()
        service.onDestroy()
    }

    @Test
    fun testFailoverStopsImmediatelyOnCriticalCodes() {
        val criticalCodes = listOf(400, 404)
        for (code in criticalCodes) {
            val exception = com.example.service.ApiException(code, "Error")
            val action = FailoverDecision.evaluate(exception, 30L)
            assertTrue("HTTP $code must Stop rotation", action is FailoverAction.StopRotation)
        }
    }

    @Test
    fun testFailoverContinuesOnNonCriticalCodes() {
        val retryCodes = listOf(401, 403, 429, 500, 503)
        for (code in retryCodes) {
            val exception = com.example.service.ApiException(code, "Error")
            val action = FailoverDecision.evaluate(exception, 30L)
            assertTrue("HTTP $code must Continue rotation", action is FailoverAction.ContinueToNextKey)
        }
    }

    @Test
    fun testChildCoroutineExceptionDoesNotStopService() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()

        // Set initial states via reflection
        val isProcessingField = ScreenCaptureService::class.java.getDeclaredField("isProcessing")
        isProcessingField.isAccessible = true
        val isProcessing = isProcessingField.get(service) as MutableStateFlow<Boolean>
        isProcessing.value = true

        val captureStateField = ScreenCaptureService::class.java.getDeclaredField("captureState")
        captureStateField.isAccessible = true
        val captureState = captureStateField.get(service) as MutableStateFlow<ScreenCaptureService.CaptureState>
        captureState.value = ScreenCaptureService.CaptureState.CAPTURING_FIRST

        // Retrieve and trigger exception handler directly
        val handlerField = ScreenCaptureService::class.java.getDeclaredField("serviceExceptionHandler")
        handlerField.isAccessible = true
        val exceptionHandler = handlerField.get(service) as kotlinx.coroutines.CoroutineExceptionHandler

        exceptionHandler.handleException(EmptyCoroutineContext, RuntimeException("Test unhandled non-fatal child error"))

        // Run shadow main looper to execute mainHandler posts
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Verify state is NOT blindly reset and error is recorded
        println("DEBUG TEST: isProcessing = ${isProcessing.value}, captureState = ${captureState.value}")
        assertEquals("Test unhandled non-fatal child error", ScreenCaptureService.lastSanitizedError)
        assertTrue(isProcessing.value)
        assertEquals(ScreenCaptureService.CaptureState.CAPTURING_FIRST, captureState.value)

        service.onDestroy()
    }

    @Test
    fun testSingleHandlerThreadLifecycle() {
        val controller = Robolectric.buildService(ScreenCaptureService::class.java)
        val service = controller.create().get()

        val threadField = ScreenCaptureService::class.java.getDeclaredField("imageHandlerThread")
        threadField.isAccessible = true
        val thread = threadField.get(service) as? android.os.HandlerThread
        assertNotNull("HandlerThread must be created on Service onCreate", thread)
        assertTrue("HandlerThread must be alive", thread!!.isAlive)

        service.onDestroy()

        val threadAfter = threadField.get(service) as? android.os.HandlerThread
        assertNull("HandlerThread must be null after Service destroy", threadAfter)
        assertFalse("HandlerThread must not be alive after Service destroy", thread.isAlive)
    }

    @Test
    fun testOkHttpRequestBodyReuse() {
        val requestContext = AnalysisRequestContext(
            provider = AiProvider.GEMINI,
            requestedModel = "gemini-1.5-flash",
            normalizedBaseUrl = "https://generativelanguage.googleapis.com",
            jpegBytes = byteArrayOf(1, 2, 3),
            imageWidth = 10,
            imageHeight = 20,
            requestStartedAt = 1000L
        )

        assertNull(requestContext.preparedRequestBody)

        val mockRequestBody = "{}".toRequestBody("application/json".toMediaType())
        requestContext.preparedRequestBody = mockRequestBody

        // Verify request context retains reference to prebuilt request body
        assertEquals(mockRequestBody, requestContext.preparedRequestBody)
    }

    @Test
    fun testKeyStoreHelperSlotKeyOperations() {
        val storeRes = KeyStoreHelper.storeSlotKey(context, "1", "slot_key_xyz")
        assertTrue(storeRes.isSuccess)

        val retrieved = KeyStoreHelper.retrieveSlotKey(context, "1")
        assertEquals("slot_key_xyz", retrieved)

        KeyStoreHelper.clearSlotKey(context, "1")
        val cleared = KeyStoreHelper.retrieveSlotKey(context, "1")
        assertEquals("", cleared)
    }

    @Test
    fun testSecureLegacyKeyMigrationSimulated() {
        KeyStoreHelper.storeGeminiApiKey(context, "legacy_super_key")

        val oldKey = KeyStoreHelper.getGeminiApiKey(context)
        assertEquals("legacy_super_key", oldKey)

        val storeResult = KeyStoreHelper.storeSlotKey(context, "1", oldKey)
        assertTrue(storeResult.isSuccess)

        val migrated = KeyStoreHelper.retrieveSlotKey(context, "1")
        assertEquals("legacy_super_key", migrated)
    }
}
