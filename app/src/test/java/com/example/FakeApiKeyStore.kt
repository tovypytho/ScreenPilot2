package com.example

import android.content.Context
import com.example.data.ApiKeyStore

class FakeApiKeyStore : ApiKeyStore {
    private var geminiKey: String = ""

    override fun getGeminiApiKey(context: Context): String = geminiKey

    override fun storeGeminiApiKey(context: Context, apiKey: String): Result<Unit> {
        geminiKey = apiKey
        return Result.success(Unit)
    }

    override fun clearGeminiApiKey(context: Context) {
        geminiKey = ""
    }
}
