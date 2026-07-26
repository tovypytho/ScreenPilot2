package com.example.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeyStoreHelper : ApiKeyStore {
    private const val KEY_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS_GEMINI = "ScreenPilotGeminiKeyAlias"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFS_NAME = "secure_prefs"
    private const val ENCRYPTED_GEMINI_API_KEY = "encrypted_gemini_api_key"

    internal var getExistingGeminiSecretKeyProvider: () -> SecretKey? = {
        try {
            val keyStore = KeyStore.getInstance(KEY_PROVIDER).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS_GEMINI)) {
                keyStore.getKey(KEY_ALIAS_GEMINI, null) as? SecretKey
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    internal var getOrCreateGeminiSecretKeyProvider: () -> SecretKey? = {
        try {
            val keyStore = KeyStore.getInstance(KEY_PROVIDER).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS_GEMINI)) {
                keyStore.getKey(KEY_ALIAS_GEMINI, null) as? SecretKey
            } else {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_PROVIDER)
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS_GEMINI,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            null
        }
    }

    internal var cipherProvider: (String) -> Cipher = { transformation ->
        Cipher.getInstance(transformation)
    }

    internal var commitOverride: ((Context, String, String) -> Boolean)? = null

    override fun getGeminiApiKey(context: Context): String {
        return retrieveGeminiApiKey(context)
    }

    override fun storeGeminiApiKey(context: Context, apiKey: String): Result<Unit> {
        if (apiKey.isEmpty()) {
            clearGeminiApiKey(context)
            return Result.success(Unit)
        }

        return try {
            val secretKey = getOrCreateGeminiSecretKeyProvider() 
                ?: return Result.failure(Exception("Failed to obtain secure key for Gemini"))

            val cipher = cipherProvider(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))

            val ivString = Base64.encodeToString(iv, Base64.NO_WRAP)
            val encryptedString = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            val combined = "$ivString:$encryptedString"

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val success = if (commitOverride != null) {
                commitOverride!!(context, ENCRYPTED_GEMINI_API_KEY, combined)
            } else {
                prefs.edit().putString(ENCRYPTED_GEMINI_API_KEY, combined).commit()
            }

            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to persist secure storage preference for Gemini"))
            }
        } catch (e: Exception) {
            android.util.Log.e("KeyStoreHelper", "Encryption error for Gemini API key", e)
            Result.failure(Exception("Failed to encrypt Gemini API key securely: ${e.message}", e))
        }
    }

    fun retrieveGeminiApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val combined = prefs.getString(ENCRYPTED_GEMINI_API_KEY, null) ?: return ""

        val parts = combined.split(":")
        if (parts.size != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            clearGeminiApiKey(context)
            return ""
        }

        try {
            val secretKey = getExistingGeminiSecretKeyProvider() ?: throw Exception("Gemini secret key missing")
            
            val iv = Base64.decode(parts[0], Base64.NO_WRAP) ?: throw Exception("IV decode failed")
            val encryptedBytes = Base64.decode(parts[1], Base64.NO_WRAP) ?: throw Exception("Ciphertext decode failed")

            val cipher = cipherProvider(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            android.util.Log.e("KeyStoreHelper", "Decryption error for Gemini API key", e)
            clearGeminiApiKey(context)
            return ""
        }
    }

    override fun clearGeminiApiKey(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(ENCRYPTED_GEMINI_API_KEY)
            .apply()
    }

    fun storeSlotKey(context: Context, slotId: String, apiKey: String): Result<Unit> {
        if (apiKey.isEmpty()) {
            clearSlotKey(context, slotId)
            return Result.success(Unit)
        }

        return try {
            val secretKey = getOrCreateGeminiSecretKeyProvider() 
                ?: return Result.failure(Exception("Failed to obtain secure key for Gemini Slot"))

            val cipher = cipherProvider(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))

            val ivString = Base64.encodeToString(iv, Base64.NO_WRAP)
            val encryptedString = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            val combined = "$ivString:$encryptedString"

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val prefKey = "gemini_key_slot_$slotId"
            val success = if (commitOverride != null) {
                commitOverride!!(context, prefKey, combined)
            } else {
                prefs.edit().putString(prefKey, combined).commit()
            }

            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to persist secure storage preference for Gemini Slot $slotId"))
            }
        } catch (e: Exception) {
            android.util.Log.e("KeyStoreHelper", "Encryption error for Gemini API key Slot $slotId", e)
            Result.failure(Exception("Failed to encrypt Gemini API key for Slot $slotId securely: ${e.message}", e))
        }
    }

    fun retrieveSlotKey(context: Context, slotId: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prefKey = "gemini_key_slot_$slotId"
        val combined = prefs.getString(prefKey, null) ?: return ""

        val parts = combined.split(":")
        if (parts.size != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            clearSlotKey(context, slotId)
            return ""
        }

        try {
            val secretKey = getExistingGeminiSecretKeyProvider() ?: throw Exception("Gemini secret key missing")
            
            val iv = Base64.decode(parts[0], Base64.NO_WRAP) ?: throw Exception("IV decode failed")
            val encryptedBytes = Base64.decode(parts[1], Base64.NO_WRAP) ?: throw Exception("Ciphertext decode failed")

            val cipher = cipherProvider(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            android.util.Log.e("KeyStoreHelper", "Decryption error for Gemini API key Slot $slotId", e)
            clearSlotKey(context, slotId)
            return ""
        }
    }

    fun clearSlotKey(context: Context, slotId: String) {
        val prefKey = "gemini_key_slot_$slotId"
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(prefKey)
            .apply()
    }
}
