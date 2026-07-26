package com.example.service

import java.net.URLEncoder

object GeminiUrlHelper {
    
    fun normalizeBaseUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("Invalid Gemini base URL.")
        }
        if (!trimmed.startsWith("https://")) {
            throw IllegalArgumentException("Invalid Gemini base URL.")
        }
        var normalized = trimmed
        while (normalized.endsWith("/")) {
            normalized = normalized.dropLast(1)
        }
        if (normalized.contains("?") || normalized.contains("#")) {
            throw IllegalArgumentException("Invalid Gemini base URL.")
        }
        if (normalized.contains("/models/")) {
            throw IllegalArgumentException("Invalid Gemini base URL.")
        }
        if (normalized.endsWith(":generateContent")) {
            throw IllegalArgumentException("Invalid Gemini base URL.")
        }
        // never append /v1beta twice, if /v1beta/v1beta is detected, reject
        if (normalized.contains("/v1beta/v1beta")) {
            throw IllegalArgumentException("Invalid Gemini base URL.")
        }
        return normalized
    }

    fun normalizeModel(model: String): String {
        val trimmed = model.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("Invalid Gemini model setting.")
        }
        val cleanModel = if (trimmed.startsWith("models/")) {
            trimmed.substring(7)
        } else {
            trimmed
        }
        if (cleanModel.contains("/") || cleanModel.contains(":") || cleanModel.contains("?") || cleanModel.contains("#") || cleanModel.contains("..")) {
            throw IllegalArgumentException("Invalid Gemini model setting.")
        }
        return cleanModel
    }

    fun buildEndpoint(baseUrl: String, model: String): String {
        val normBase = normalizeBaseUrl(baseUrl)
        val normModel = normalizeModel(model)
        // Never URL encode the colon because it's part of the method suffix (:generateContent), not part of the model value
        return "$normBase/models/$normModel:generateContent"
    }
}
