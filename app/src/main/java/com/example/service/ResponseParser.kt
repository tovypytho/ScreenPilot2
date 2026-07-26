package com.example.service

import org.json.JSONObject
import java.util.Locale

sealed interface ParsedAnswer {
    val confidence: Double?

    data class MultipleChoice(
        val answerIndex: Int,
        override val confidence: Double?
    ) : ParsedAnswer

    data class FreeResponse(
        val answerText: String,
        override val confidence: Double?
    ) : ParsedAnswer

    data class Unclear(
        override val confidence: Double?
    ) : ParsedAnswer
}

object ResponseParser {
    private const val MAX_FREE_RESPONSE_CHARS = 500

    fun extractGeminiText(responseJson: String): String {
        val root = JSONObject(responseJson)
        val candidates = root.optJSONArray("candidates")
            ?: throw IllegalArgumentException("Blocked response or empty generation (no candidates).")
        if (candidates.length() == 0) {
            throw IllegalArgumentException("Blocked response or empty generation (0 candidates).")
        }
        val candidate = candidates.getJSONObject(0)
        val content = candidate.optJSONObject("content")
            ?: throw IllegalArgumentException("Blocked response or empty generation (no content).")
        val parts = content.optJSONArray("parts")
            ?: throw IllegalArgumentException("Blocked response or empty generation (no parts).")
        if (parts.length() == 0) {
            throw IllegalArgumentException("Blocked response or empty generation (0 parts).")
        }
        val part = parts.getJSONObject(0)
        val text = part.optString("text", "")
        if (text.isEmpty()) {
            throw IllegalArgumentException("Blocked response or empty generation (empty text).")
        }
        return text
    }

    fun parse(rawContent: String): ParsedAnswer {
        val trimmed = rawContent.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("Failed to parse response: empty content")
        }

        // Backward-compatible fallback for legacy MC-only responses.
        if (trimmed.length == 1 && trimmed[0] in '1'..'5') {
            return ParsedAnswer.MultipleChoice(trimmed[0].digitToInt(), null)
        }

        val jsonText = stripMarkdownFence(trimmed)

        try {
            val json = JSONObject(jsonText)
            val confidence = parseConfidence(json)
            val questionType = json.optString("question_type", "")
                .trim()
                .uppercase(Locale.US)

            // Backward compatibility for the previous structured schema that only
            // returned answer_index (+ optional confidence).
            if (questionType.isEmpty() && json.has("answer_index")) {
                return ParsedAnswer.MultipleChoice(
                    answerIndex = parseAnswerIndex(json),
                    confidence = confidence
                )
            }

            return when (questionType) {
                "MULTIPLE_CHOICE" -> ParsedAnswer.MultipleChoice(
                    answerIndex = parseAnswerIndex(json),
                    confidence = confidence
                )

                "FREE_RESPONSE" -> ParsedAnswer.FreeResponse(
                    answerText = parseFreeResponseText(json),
                    confidence = confidence
                )

                "UNCLEAR" -> ParsedAnswer.Unclear(confidence = confidence)

                else -> throw IllegalArgumentException(
                    "question_type must be MULTIPLE_CHOICE, FREE_RESPONSE, or UNCLEAR"
                )
            }
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Failed to parse response: ${e.message}", e)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse response: ${e.message}", e)
        }
    }

    private fun stripMarkdownFence(value: String): String {
        if (!value.startsWith("```")) return value

        var result = value
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")

        if (result.endsWith("```")) {
            result = result.removeSuffix("```")
        }
        return result.trim()
    }

    private fun parseAnswerIndex(json: JSONObject): Int {
        val rawIndex = json.opt("answer_index")
        if (rawIndex == null || rawIndex == JSONObject.NULL) {
            throw IllegalArgumentException("answer_index is missing or null")
        }
        if (rawIndex !is Number) {
            throw IllegalArgumentException("answer_index must be a JSON numeric value")
        }

        val doubleVal = rawIndex.toDouble()
        if (!doubleVal.isFinite() || doubleVal % 1.0 != 0.0) {
            throw IllegalArgumentException("answer_index must be a finite integral number")
        }

        val answerIndex = doubleVal.toInt()
        if (answerIndex !in 1..5) {
            throw IllegalArgumentException("answer_index $answerIndex is not within the valid 1-5 range")
        }
        return answerIndex
    }

    private fun parseFreeResponseText(json: JSONObject): String {
        val rawText = json.opt("answer_text")
        if (rawText == null || rawText == JSONObject.NULL || rawText !is String) {
            throw IllegalArgumentException("answer_text is missing or is not a JSON string")
        }

        val normalized = rawText
            .trim()
            .replace(Regex("\\s+"), " ")

        if (normalized.isEmpty()) {
            throw IllegalArgumentException("answer_text is empty")
        }

        // Keep notification/history payloads bounded even if the provider ignores
        // the concise-answer instruction. Truncation is safer than turning a valid
        // provider answer into a key-failover condition.
        return if (normalized.length <= MAX_FREE_RESPONSE_CHARS) {
            normalized
        } else {
            normalized.take(MAX_FREE_RESPONSE_CHARS - 1).trimEnd() + "…"
        }
    }

    private fun parseConfidence(json: JSONObject): Double? {
        if (!json.has("confidence") || json.isNull("confidence")) return null
        val raw = json.opt("confidence")
        if (raw !is Number) return null
        val value = raw.toDouble()
        return value.takeIf { it.isFinite() && it in 0.0..1.0 }
    }
}
