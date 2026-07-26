package com.example.service

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

enum class AiProvider {
    GEMINI
}

enum class AnalysisStage {
    CAPTURE_STARTED,
    CAPTURE_COMPLETE,
    JPEG_ENCODING,
    CONFIG_LOADING,
    REQUEST_BUILDING,
    REQUEST_READY,
    NETWORK_CONNECTING,
    HTTP_RESPONSE,
    RESPONSE_PARSING,
    COMPLETED,
    CANCELLED
}

data class AnalysisRequestContext(
    val provider: AiProvider,
    val requestedModel: String,
    val normalizedBaseUrl: String,
    val jpegBytes: ByteArray,
    val imageWidth: Int,
    val imageHeight: Int,
    val requestStartedAt: Long,
    val isStagedTwoImage: Boolean = false,
    val jpegBytesPart2: ByteArray? = null,
    val imageWidthPart2: Int = 0,
    val imageHeightPart2: Int = 0,
    var prebuiltJsonRequest: String? = null,
    var preparedRequestBody: RequestBody? = null,
    var requestBodySizeBytes: Long = 0L
)

data class SuccessfulGeminiResult(
    val responseJson: String,
    val parsedAnswer: ParsedAnswer,
    val slotId: String,
    val attempts: Int,
    val httpStatus: Int,
    val durationMs: Long
)

data class AnalysisDiagnostics(
    val provider: String = "Unknown",
    val model: String = "Unknown",
    val width: Int = 0,
    val height: Int = 0,
    val jpegSizeKb: Double = 0.0,
    val requestBodySizeKb: Double = 0.0,
    val stage: String = "NONE",
    val httpStatus: Int? = null,
    val durationMs: Long = 0,
    val error: String? = null
)

class LocalPreparationException(message: String, cause: Throwable? = null) : Exception(message, cause)
class NetworkConnectionException(message: String, cause: Throwable? = null) : Exception(message, cause)
class ProviderResponseException(message: String, cause: Throwable? = null) : Exception(message, cause)
class ApiException(val code: Int, message: String) : Exception(message)

fun encodeGeminiInlineImage(jpegBytes: ByteArray): String {
    return Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
}

internal const val QUESTION_ANALYSIS_SYSTEM_INSTRUCTION = """You analyze educational question screenshots and return exactly one structured result. Classify MULTIPLE_CHOICE only when visible selectable answer choices are actually present; set answer_index to the correct choice counted from top to bottom (1 through 5) and set answer_text to an empty string. If the correct visible choice cannot be safely mapped to 1 through 5, classify UNCLEAR. Classify FREE_RESPONSE only when the visible question clearly expects a typed, written, fill-in, calculation, short-answer, or essay response and no selectable choices are present; set answer_index to 0 and put the direct answer in answer_text. If answer choices may be cropped, off-screen, hidden by scrolling, the question is incomplete, or the content is not clear enough to distinguish the format, classify UNCLEAR instead of guessing FREE_RESPONSE; for UNCLEAR set answer_index to 0 and answer_text to an empty string. FREE_RESPONSE answer_text must be in the same language as the question, have no preamble, usually be one to three short sentences and preferably under 320 characters. For a numeric or factual short-answer question, return only the concise value or phrase needed. Never invent an answer for UNCLEAR."""

internal const val SINGLE_IMAGE_USER_INSTRUCTION =
    "Analyze the visible question in this screenshot, classify its answer format, and return the best structured result."

internal const val STAGED_TWO_IMAGE_USER_INSTRUCTION =
    "These two screenshots are sequential parts of the same question. Image 1 is the earlier/top part and Image 2 is the later/lower part after scrolling. Combine both images before classifying the question and answering it."

internal fun buildGeminiImageRequestJson(
    context: AnalysisRequestContext,
    base64Data: String
): String {
    if (base64Data.isEmpty()) {
        throw LocalPreparationException("Screenshot raw base64 result is empty.")
    }

    return JSONObject().apply {
        put("system_instruction", JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().apply {
                    put("text", QUESTION_ANALYSIS_SYSTEM_INSTRUCTION)
                })
            })
        })
        put("contents", JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put(
                            "text",
                            if (context.isStagedTwoImage) {
                                STAGED_TWO_IMAGE_USER_INSTRUCTION
                            } else {
                                SINGLE_IMAGE_USER_INSTRUCTION
                            }
                        )
                    })
                    put(JSONObject().apply {
                        put("inline_data", JSONObject().apply {
                            put("mime_type", "image/jpeg")
                            put("data", base64Data)
                        })
                    })
                    if (context.isStagedTwoImage) {
                        val part2 = context.jpegBytesPart2
                            ?: throw LocalPreparationException("Staged capture is missing Image 2.")
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", encodeGeminiInlineImage(part2))
                            })
                        })
                    }
                })
            })
        })
        put("generationConfig", JSONObject().apply {
            put("temperature", 0)
            put("candidateCount", 1)
            put("maxOutputTokens", 512)
            put("response_mime_type", "application/json")
            put("response_schema", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("question_type", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "MULTIPLE_CHOICE when visible selectable choices are present; FREE_RESPONSE for clearly typed/written answers without choices; UNCLEAR when the question format or complete content is not safely visible.")
                        put("enum", JSONArray().apply {
                            put("MULTIPLE_CHOICE")
                            put("FREE_RESPONSE")
                            put("UNCLEAR")
                        })
                    })
                    put("answer_index", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "1 through 5 for MULTIPLE_CHOICE; 0 for FREE_RESPONSE or UNCLEAR.")
                        put("minimum", 0)
                        put("maximum", 5)
                    })
                    put("answer_text", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Direct concise answer for FREE_RESPONSE; empty string for MULTIPLE_CHOICE or UNCLEAR.")
                    })
                    put("confidence", JSONObject().apply {
                        put("type", "NUMBER")
                        put("description", "Optional confidence score from 0 to 1.")
                        put("minimum", 0)
                        put("maximum", 1)
                    })
                })
                put("required", JSONArray().apply {
                    put("question_type")
                    put("answer_index")
                    put("answer_text")
                })
            })
        })
    }.toString()
}

object ProviderGateway {
    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private var activeCall: Call? = null

    private val _diagnosticsFlow = MutableStateFlow(AnalysisDiagnostics())
    val diagnosticsFlow: StateFlow<AnalysisDiagnostics> = _diagnosticsFlow.asStateFlow()

    @Synchronized
    fun setActiveCall(call: Call?) {
        activeCall = call
    }

    @Synchronized
    fun cancelActiveCall() {
        activeCall?.cancel()
        activeCall = null
    }

    fun updateDiagnostics(diags: AnalysisDiagnostics) {
        _diagnosticsFlow.value = diags
    }
}

object GeminiProviderClient {
    suspend fun executeTextRequest(
        apiKey: String,
        baseUrl: String,
        model: String,
        prompt: String
    ): String = withContext(Dispatchers.IO) {
        val url = GeminiUrlHelper.buildEndpoint(baseUrl, model)
        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.0)
                put("maxOutputTokens", 20)
            })
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey.trim())
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(requestBody)
            .build()

        val call = ProviderGateway.okHttpClient.newCall(request)
        ProviderGateway.setActiveCall(call)
        try {
            call.execute().use { response ->
                val code = response.code
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    throw ApiException(code, formatGeminiError(code, body))
                }
                body
            }
        } finally {
            ProviderGateway.setActiveCall(null)
        }
    }

    suspend fun executeImageRequest(
        context: AnalysisRequestContext,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        val url = GeminiUrlHelper.buildEndpoint(context.normalizedBaseUrl, context.requestedModel)
        
        val requestBody = context.preparedRequestBody ?: run {
            val jsonString = context.prebuiltJsonRequest ?: run {
                val base64Data = encodeGeminiInlineImage(context.jpegBytes)
                if (base64Data.isEmpty()) {
                    throw LocalPreparationException("Screenshot raw base64 result is empty.")
                }

                buildGeminiImageRequestJson(context, base64Data)
            }

            val bodyBytes = jsonString.toByteArray(Charsets.UTF_8).size
            // TASK 6 size check: Reject locally when the total JSON request is 19 MB or more.
            if (bodyBytes >= 19 * 1024 * 1024) {
                throw LocalPreparationException("Gemini request size limit exceeded (19 MB).")
            }

            context.requestBodySizeBytes = bodyBytes.toLong()
            val prepared = jsonString.toRequestBody("application/json".toMediaType())
            context.preparedRequestBody = prepared
            // Clear memory duplication
            context.prebuiltJsonRequest = null
            prepared
        }

        // Store safe diagnostics
        val sizeInKb = context.requestBodySizeBytes / 1024.0
        val diag = AnalysisDiagnostics(
            provider = "Google Gemini",
            model = context.requestedModel,
            width = context.imageWidth,
            height = context.imageHeight,
            jpegSizeKb = context.jpegBytes.size / 1024.0,
            requestBodySizeKb = sizeInKb,
            stage = AnalysisStage.REQUEST_READY.name
        )
        ProviderGateway.updateDiagnostics(diag)

        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey.trim())
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(requestBody)
            .build()

        val call = ProviderGateway.okHttpClient.newCall(request)
        ProviderGateway.setActiveCall(call)
        try {
            ProviderGateway.updateDiagnostics(diag.copy(stage = AnalysisStage.NETWORK_CONNECTING.name))
            call.execute().use { response ->
                val code = response.code
                val responseBody = response.body?.string() ?: ""
                ProviderGateway.updateDiagnostics(diag.copy(stage = AnalysisStage.HTTP_RESPONSE.name, httpStatus = code))
                if (!response.isSuccessful) {
                    throw ApiException(code, formatGeminiError(code, responseBody))
                }
                responseBody
            }
        } finally {
            ProviderGateway.setActiveCall(null)
        }
    }

    fun formatGeminiError(code: Int, body: String): String {
        val classification = when (code) {
            400 -> "Invalid Gemini request"
            401 -> "Gemini authentication failed"
            403 -> "Gemini key or project permission denied"
            404 -> "Gemini model or endpoint not found"
            408 -> "Gemini request timed out"
            413 -> "Screenshot request too large"
            429 -> "Gemini rate limit exceeded"
            in 500..599 -> "Gemini service temporarily unavailable"
            else -> "HTTP Error $code"
        }
        var googleMessage = ""
        if (body.isNotEmpty()) {
            try {
                val root = JSONObject(body)
                if (root.has("error")) {
                    val err = root.getJSONObject("error")
                    val msg = err.optString("message", "")
                    if (msg.isNotEmpty()) {
                        googleMessage = if (msg.length > 180) msg.substring(0, 180) + "..." else msg
                    }
                }
            } catch (e: Exception) {}
        }
        return if (googleMessage.isNotEmpty()) {
            "HTTP $code\n$classification - $googleMessage"
        } else {
            "HTTP $code\n$classification"
        }
    }
}
