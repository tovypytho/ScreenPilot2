package com.example.service

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.data.KeyStoreHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProviderGatewayTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ProviderGateway.updateDiagnostics(AnalysisDiagnostics())

        val dummyKey = javax.crypto.spec.SecretKeySpec(ByteArray(16), "AES")
        KeyStoreHelper.getOrCreateGeminiSecretKeyProvider = { dummyKey }
        KeyStoreHelper.getExistingGeminiSecretKeyProvider = { dummyKey }
        KeyStoreHelper.cipherProvider = { javax.crypto.Cipher.getInstance(it) }
    }

    @Test
    fun testDiagnosticsStateFlowUpdatesCorrectly() {
        val initialDiag = ProviderGateway.diagnosticsFlow.value
        assertEquals("NONE", initialDiag.stage)

        ProviderGateway.updateDiagnostics(AnalysisDiagnostics(stage = "PREPARING"))
        assertEquals("PREPARING", ProviderGateway.diagnosticsFlow.value.stage)

        ProviderGateway.updateDiagnostics(AnalysisDiagnostics(stage = "COMPLETE"))
        assertEquals("COMPLETE", ProviderGateway.diagnosticsFlow.value.stage)
    }

    @Test
    fun testBitmapResizingScaleCorrectly() {
        val maxDim = 100
        val largeBitmap = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888)
        
        val width = largeBitmap.width
        val height = largeBitmap.height
        
        val scaled = if (width > maxDim || height > maxDim) {
            val (newWidth, newHeight) = if (width > height) {
                Pair(maxDim, (height * (maxDim.toFloat() / width)).toInt())
            } else {
                Pair((width * (maxDim.toFloat() / height)).toInt(), maxDim)
            }
            Bitmap.createScaledBitmap(largeBitmap, newWidth, newHeight, true)
        } else {
            largeBitmap
        }

        assertTrue(scaled.width <= maxDim)
        assertTrue(scaled.height <= maxDim)
        assertEquals(maxDim, scaled.height) // since height (300) is larger, it becomes maxDim (100)
        
        largeBitmap.recycle()
        scaled.recycle()
    }

    @Test
    fun testGeminiApiKeySecureAccessRoundtrip() {
        val originalKey = "gemini_secure_12345"
        val storeRes = KeyStoreHelper.storeGeminiApiKey(context, originalKey)
        assertTrue(storeRes.isSuccess)

        val retrieved = KeyStoreHelper.getGeminiApiKey(context)
        assertEquals(originalKey, retrieved)

        // Clear and verify
        KeyStoreHelper.clearGeminiApiKey(context)
        val cleared = KeyStoreHelper.getGeminiApiKey(context)
        assertEquals("", cleared)
    }

    @Test
    fun testAnalysisRequestContextPrebuiltRequestRetention() {
        val requestContext = AnalysisRequestContext(
            provider = AiProvider.GEMINI,
            requestedModel = "gemini-1.5-flash",
            normalizedBaseUrl = "https://generativelanguage.googleapis.com",
            jpegBytes = byteArrayOf(1, 2, 3),
            imageWidth = 10,
            imageHeight = 20,
            requestStartedAt = 1000L
        )

        assertNull(requestContext.prebuiltJsonRequest)
        val mockJsonString = "{\"test\": true}"
        requestContext.prebuiltJsonRequest = mockJsonString
        assertEquals(mockJsonString, requestContext.prebuiltJsonRequest)
    }

    @Test
    fun testAnalysisRequestContextDefaultParams() {
        val requestContext = AnalysisRequestContext(
            provider = AiProvider.GEMINI,
            requestedModel = "gemini-1.5-flash",
            normalizedBaseUrl = "https://generativelanguage.googleapis.com",
            jpegBytes = byteArrayOf(4, 5),
            imageWidth = 100,
            imageHeight = 200,
            requestStartedAt = 2000L
        )

        assertEquals(AiProvider.GEMINI, requestContext.provider)
        assertEquals("gemini-1.5-flash", requestContext.requestedModel)
        assertEquals("https://generativelanguage.googleapis.com", requestContext.normalizedBaseUrl)
        assertEquals(100, requestContext.imageWidth)
        assertEquals(200, requestContext.imageHeight)
        assertEquals(2000L, requestContext.requestStartedAt)
        assertTrue(!requestContext.isStagedTwoImage)
        assertNull(requestContext.jpegBytesPart2)
        assertEquals(0, requestContext.imageWidthPart2)
        assertEquals(0, requestContext.imageHeightPart2)
        assertNull(requestContext.prebuiltJsonRequest)
    }

    @Test
    fun testEncodeGeminiInlineImageCorrectness() {
        val sampleBytes = byteArrayOf(10, 20, 30, 40)
        val encoded = encodeGeminiInlineImage(sampleBytes)
        assertNotNull(encoded)
        assertTrue(encoded.isNotEmpty())
        
        val decoded = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
        assertTrue(sampleBytes.contentEquals(decoded))
    }

    @Test
    fun typedQuestionSchemaIncludesAllFourQuestionTypes() {
        val contextObj = AnalysisRequestContext(
            provider = AiProvider.GEMINI,
            requestedModel = "gemini-test",
            normalizedBaseUrl = "https://generativelanguage.googleapis.com/v1beta",
            jpegBytes = byteArrayOf(1, 2, 3),
            imageWidth = 100,
            imageHeight = 100,
            requestStartedAt = 1L
        )

        val json = org.json.JSONObject(buildGeminiImageRequestJson(contextObj, "ZmFrZQ=="))
        val generationConfig = json.getJSONObject("generationConfig")
        assertTrue(!generationConfig.has("maxOutputTokens"))
        val schema = generationConfig.getJSONObject("response_schema")
        val properties = schema.getJSONObject("properties")
        val questionType = properties.getJSONObject("question_type")
        val enumValues = questionType.getJSONArray("enum")

        assertEquals("MULTIPLE_CHOICE", enumValues.getString(0))
        assertEquals("MULTIPLE_SELECT", enumValues.getString(1))
        assertEquals("FREE_RESPONSE", enumValues.getString(2))
        assertEquals("UNCLEAR", enumValues.getString(3))
        assertTrue(properties.has("answer_index"))
        assertTrue(properties.has("answer_indices"))
        assertTrue(properties.has("answer_text"))
        assertEquals(0, properties.getJSONObject("answer_index").getInt("minimum"))
        val required = schema.getJSONArray("required")
        assertEquals("question_type", required.getString(0))
        assertEquals("answer_index", required.getString(1))
        assertEquals("answer_indices", required.getString(2))
        assertEquals("answer_text", required.getString(3))
        val answerIndicesSchema = properties.getJSONObject("answer_indices")
        assertEquals("ARRAY", answerIndicesSchema.getString("type"))
        assertEquals(1, answerIndicesSchema.getJSONObject("items").getInt("minimum"))
        assertEquals(5, answerIndicesSchema.getJSONObject("items").getInt("maximum"))
        assertTrue(QUESTION_ANALYSIS_SYSTEM_INSTRUCTION.contains("circular radio", ignoreCase = true))
        assertTrue(QUESTION_ANALYSIS_SYSTEM_INSTRUCTION.contains("square checkbox", ignoreCase = true))
        assertTrue(QUESTION_ANALYSIS_SYSTEM_INSTRUCTION.contains("Do not assume", ignoreCase = true))
        assertTrue(QUESTION_ANALYSIS_SYSTEM_INSTRUCTION.contains("cropped", ignoreCase = true))
        assertTrue(QUESTION_ANALYSIS_SYSTEM_INSTRUCTION.contains("UNCLEAR"))
        assertTrue(QUESTION_ANALYSIS_SYSTEM_INSTRUCTION.contains("every part the question asks"))
        assertTrue(!QUESTION_ANALYSIS_SYSTEM_INSTRUCTION.contains("under 320 characters"))
    }

    @Test
    fun stagedRequestPreservesImageOneThenImageTwoOrder() {
        val contextObj = AnalysisRequestContext(
            provider = AiProvider.GEMINI,
            requestedModel = "gemini-test",
            normalizedBaseUrl = "https://generativelanguage.googleapis.com/v1beta",
            jpegBytes = byteArrayOf(1),
            imageWidth = 100,
            imageHeight = 100,
            requestStartedAt = 1L,
            isStagedTwoImage = true,
            jpegBytesPart2 = byteArrayOf(2),
            imageWidthPart2 = 100,
            imageHeightPart2 = 100
        )

        val firstEncoded = "Zmlyc3Q="
        val json = org.json.JSONObject(buildGeminiImageRequestJson(contextObj, firstEncoded))
        val parts = json.getJSONArray("contents").getJSONObject(0).getJSONArray("parts")

        assertEquals(STAGED_TWO_IMAGE_USER_INSTRUCTION, parts.getJSONObject(0).getString("text"))
        assertEquals(firstEncoded, parts.getJSONObject(1).getJSONObject("inline_data").getString("data"))
        val secondEncoded = parts.getJSONObject(2).getJSONObject("inline_data").getString("data")
        assertEquals(encodeGeminiInlineImage(byteArrayOf(2)), secondEncoded)
    }

    @Test(expected = LocalPreparationException::class)
    fun stagedRequestWithoutSecondImageIsRejectedLocally() {
        val contextObj = AnalysisRequestContext(
            provider = AiProvider.GEMINI,
            requestedModel = "gemini-test",
            normalizedBaseUrl = "https://generativelanguage.googleapis.com/v1beta",
            jpegBytes = byteArrayOf(1),
            imageWidth = 100,
            imageHeight = 100,
            requestStartedAt = 1L,
            isStagedTwoImage = true,
            jpegBytesPart2 = null
        )
        buildGeminiImageRequestJson(contextObj, "ZmFrZQ==")
    }

}
