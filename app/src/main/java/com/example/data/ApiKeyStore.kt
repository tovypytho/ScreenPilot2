package com.example.data

import android.content.Context

interface ApiKeyStore {
    fun getGeminiApiKey(context: Context): String
    fun storeGeminiApiKey(context: Context, apiKey: String): Result<Unit>
    fun clearGeminiApiKey(context: Context)
}
