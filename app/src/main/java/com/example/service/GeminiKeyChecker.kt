package com.example.service

import com.example.data.GeminiKeyHealth
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class KeyCheckResult(val health: GeminiKeyHealth, val reason: String, val cooldownMs: Long = 0L)

/** Checks exactly one key against the configured generation model, without provider failover. */
object GeminiKeyChecker {
    suspend fun check(
        key: String,
        baseUrl: String,
        model: String,
        cooldownSeconds: Int,
        client: OkHttpClient = ProviderGateway.okHttpClient
    ): KeyCheckResult {
        if (key.isBlank()) return KeyCheckResult(GeminiKeyHealth.NOT_CONFIGURED, "Empty key")
        return try {
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", "Reply OK")))))
                put("generationConfig", JSONObject().put("temperature", 0).put("maxOutputTokens", 128))
            }
            val request = Request.Builder()
                .url(GeminiUrlHelper.buildEndpoint(baseUrl, model))
                .header("x-goog-api-key", key.trim())
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val call = client.newCall(request)
            val body = suspendCancellableCoroutine<String> { continuation ->
                continuation.invokeOnCancellation { call.cancel() }
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                    override fun onResponse(call: Call, response: Response) {
                        try {
                            response.use {
                                val content = it.body?.string().orEmpty()
                                if (continuation.isActive) {
                                    if (it.isSuccessful) continuation.resume(content)
                                    else {
                                        val invalidKey = it.code == 400 &&
                                            (content.contains("API_KEY_INVALID", ignoreCase = true) ||
                                                content.contains("API key not valid", ignoreCase = true))
                                        continuation.resumeWithException(ApiException(it.code, if (invalidKey) "API_KEY_INVALID" else "HTTP ${it.code}"))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            if (continuation.isActive) continuation.resumeWithException(e)
                        }
                    }
                })
            }
            val parts = JSONObject(body).optJSONArray("candidates")
                ?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
            val hasText = parts != null && (0 until parts.length()).any { index ->
                parts.optJSONObject(index)?.optString("text")?.isNotBlank() == true
            }
            if (!hasText) KeyCheckResult(GeminiKeyHealth.TEMPORARY_FAILURE, "No generated text")
            else KeyCheckResult(GeminiKeyHealth.READY, "Generation succeeded")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: ApiException) {
            if (e.code == 400 && e.message == "API_KEY_INVALID") {
                KeyCheckResult(GeminiKeyHealth.AUTH_FAILED, "Invalid key (400)")
            } else classifyHttp(e.code, cooldownSeconds)
        } catch (e: IOException) {
            KeyCheckResult(GeminiKeyHealth.TEMPORARY_FAILURE, "Network error")
        } catch (e: Exception) {
            KeyCheckResult(GeminiKeyHealth.TEMPORARY_FAILURE, e.javaClass.simpleName)
        }
    }

    internal fun classifyHttp(code: Int, cooldownSeconds: Int): KeyCheckResult = when (code) {
        401 -> KeyCheckResult(GeminiKeyHealth.AUTH_FAILED, "Invalid key (401)")
        403 -> KeyCheckResult(GeminiKeyHealth.PERMISSION_DENIED, "Model access denied (403)")
        429 -> KeyCheckResult(GeminiKeyHealth.COOLDOWN, "Rate limit (429)", cooldownSeconds.coerceAtLeast(0) * 1000L)
        404 -> KeyCheckResult(GeminiKeyHealth.TEMPORARY_FAILURE, "Model unavailable (404)")
        else -> KeyCheckResult(GeminiKeyHealth.TEMPORARY_FAILURE, "HTTP $code")
    }
}
