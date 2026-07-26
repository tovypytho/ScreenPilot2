package com.example.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

object ConnectionTester {

    suspend fun testGeminiConnection(
        apiKey: String,
        baseUrl: String,
        model: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.trim().isEmpty()) {
            return@withContext Result.failure(Exception("Gemini API key is not configured."))
        }

        try {
            val body = GeminiProviderClient.executeTextRequest(
                apiKey = apiKey,
                baseUrl = baseUrl,
                model = model,
                prompt = "Read the text: \"Hello, world!\" and return the number 1."
            )

            try {
                val root = JSONObject(body)
                val candidates = root.getJSONArray("candidates")
                val candidate = candidates.getJSONObject(0)
                val content = candidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                val part = parts.getJSONObject(0)
                val text = part.getString("text").trim()

                if (text.contains("1")) {
                    Result.success("Success: Gemini is connected. Response text: '$text'")
                } else {
                    Result.failure(Exception("API returned unexpected content: '$text' (Expected response to contain '1')"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Failed to parse response JSON: ${e.message}"))
            }
        } catch (e: UnknownHostException) {
            Result.failure(Exception("DNS lookup failed."))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("Connection timed out."))
        } catch (e: SSLException) {
            Result.failure(Exception("TLS connection failed."))
        } catch (e: ConnectException) {
            Result.failure(Exception("Internet connection unavailable."))
        } catch (e: ApiException) {
            Result.failure(Exception(e.message))
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message ?: "Connection reset"}"))
        } catch (e: Exception) {
            Result.failure(Exception("Unexpected error: ${e.message}"))
        }
    }
}
