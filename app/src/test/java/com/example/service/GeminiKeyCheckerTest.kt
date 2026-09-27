package com.example.service

import com.example.data.GeminiKeyHealth
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class GeminiKeyCheckerTest {
    @Test fun checksExactlyTheSuppliedKeyAndModel() = runBlocking {
        var seenKey = ""
        var seenPath = ""
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            seenKey = chain.request().header("x-goog-api-key").orEmpty()
            seenPath = chain.request().url.encodedPath
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("""{"candidates":[{"content":{"parts":[{"text":"OK"}]}}]}""".toResponseBody())
                .build()
        }.build()
        val result = GeminiKeyChecker.check(" slot-two-key ", "https://example.com/v1beta", "gemini-test", 60, client)
        assertEquals(GeminiKeyHealth.READY, result.health)
        assertEquals("slot-two-key", seenKey)
        assertTrue(seenPath.endsWith("/models/gemini-test:generateContent"))
    }

    @Test fun httpStatusesDistinguishInvalidKeyPermissionAndQuota() {
        assertEquals(GeminiKeyHealth.AUTH_FAILED, GeminiKeyChecker.classifyHttp(401, 60).health)
        assertEquals(GeminiKeyHealth.PERMISSION_DENIED, GeminiKeyChecker.classifyHttp(403, 60).health)
        assertEquals(GeminiKeyHealth.COOLDOWN, GeminiKeyChecker.classifyHttp(429, 60).health)
        assertEquals(60_000L, GeminiKeyChecker.classifyHttp(429, 60).cooldownMs)
        assertEquals(GeminiKeyHealth.TEMPORARY_FAILURE, GeminiKeyChecker.classifyHttp(404, 60).health)
    }
}
