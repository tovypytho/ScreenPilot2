package com.example.service

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeminiUrlHelperTest {

    @Test
    fun testBaseUrlIsSanitizedTrailingSlash() {
        val sanitized = GeminiUrlHelper.normalizeBaseUrl("https://generativelanguage.googleapis.com/v1beta/")
        assertEquals("https://generativelanguage.googleapis.com/v1beta", sanitized)
    }

    @Test
    fun testBaseUrlMissingHttpsIsRejected() {
        try {
            GeminiUrlHelper.normalizeBaseUrl("generativelanguage.googleapis.com/v1beta")
            fail("Should fail if missing https://")
        } catch (e: IllegalArgumentException) {
            assertEquals("Invalid Gemini base URL.", e.message)
        }
    }

    @Test
    fun testModelSlugIsNormalized() {
        val sanitized = GeminiUrlHelper.normalizeModel("models/gemini-2.5-flash")
        assertEquals("gemini-2.5-flash", sanitized)
    }

    @Test
    fun testModelSlugAlreadyNormalized() {
        val sanitized = GeminiUrlHelper.normalizeModel("gemini-3.1-flash-lite")
        assertEquals("gemini-3.1-flash-lite", sanitized)
    }

    @Test
    fun testEndpointUrlIsCorrectlyBuilt() {
        val endpoint = GeminiUrlHelper.buildEndpoint(
            "https://generativelanguage.googleapis.com/v1beta/",
            "models/gemini-3.1-flash-lite"
        )
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent",
            endpoint
        )
    }
}
