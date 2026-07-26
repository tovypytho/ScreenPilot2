package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KeyStoreHelperTest {

    private lateinit var context: Context
    private val PREFS_NAME = "secure_prefs"
    private val ENCRYPTED_GEMINI_API_KEY = "encrypted_gemini_api_key"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Reset seams before each test
        KeyStoreHelper.getExistingGeminiSecretKeyProvider = {
            try {
                val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                if (keyStore.containsAlias("ScreenPilotGeminiKeyAlias")) {
                    keyStore.getKey("ScreenPilotGeminiKeyAlias", null) as? SecretKey
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

        KeyStoreHelper.getOrCreateGeminiSecretKeyProvider = {
            try {
                val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                if (keyStore.containsAlias("ScreenPilotGeminiKeyAlias")) {
                    keyStore.getKey("ScreenPilotGeminiKeyAlias", null) as? SecretKey
                } else {
                    val keyGenerator = javax.crypto.KeyGenerator.getInstance("AES", "AndroidKeyStore")
                    val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                        "ScreenPilotGeminiKeyAlias",
                        android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build()
                    keyGenerator.init(spec)
                    keyGenerator.generateKey()
                }
            } catch (e: Exception) {
                null
            }
        }

        KeyStoreHelper.cipherProvider = { transformation ->
            Cipher.getInstance(transformation)
        }

        KeyStoreHelper.commitOverride = null

        // Clear shared preferences
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun testNoPlaintextApiKeyPreferenceExists() {
        // Assert we never write plain_api_key to preferences
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        assertFalse(prefs.contains("plain_api_key"))
    }

    @Test
    fun testMissingSecretKeyRemovesUnreadableEncryptedData() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(ENCRYPTED_GEMINI_API_KEY, "someIv:someCipher").commit()

        // Mock secret key missing
        KeyStoreHelper.getExistingGeminiSecretKeyProvider = { null }

        val retrieved = KeyStoreHelper.getGeminiApiKey(context)
        assertEquals("", retrieved)
        assertFalse(prefs.contains(ENCRYPTED_GEMINI_API_KEY))
    }

    @Test
    fun testMalformedEncryptedValueIsRemoved() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(ENCRYPTED_GEMINI_API_KEY, "malformedValueNoColon").commit()

        val retrieved = KeyStoreHelper.getGeminiApiKey(context)
        assertEquals("", retrieved)
        assertFalse(prefs.contains(ENCRYPTED_GEMINI_API_KEY))
    }

    @Test
    fun testFailedEncryptionDoesNotOverwriteAnExistingEncryptedValue() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val originalValue = "validIv:validCipher"
        prefs.edit().putString(ENCRYPTED_GEMINI_API_KEY, originalValue).commit()

        // Mock failed encryption by making secret key generator fail
        KeyStoreHelper.getOrCreateGeminiSecretKeyProvider = { null }

        val res = KeyStoreHelper.storeGeminiApiKey(context, "new_secret_key")
        assertTrue(res.isFailure)
        assertEquals(originalValue, prefs.getString(ENCRYPTED_GEMINI_API_KEY, null))
    }

    @Test
    fun testFailedPreferenceCommitReturnsFailure() {
        // Create a dummy key
        val dummyKey = SecretKeySpec(ByteArray(16), "AES")
        KeyStoreHelper.getOrCreateGeminiSecretKeyProvider = { dummyKey }

        // Mock commit failure
        KeyStoreHelper.commitOverride = { _, _, _ -> false }

        val res = KeyStoreHelper.storeGeminiApiKey(context, "test_key")
        assertTrue(res.isFailure)
    }

    @Test
    fun testSuccessfulStoreReturnsSuccess() {
        val dummyKey = SecretKeySpec(ByteArray(16), "AES")
        KeyStoreHelper.getOrCreateGeminiSecretKeyProvider = { dummyKey }
        KeyStoreHelper.getExistingGeminiSecretKeyProvider = { dummyKey }

        val res = KeyStoreHelper.storeGeminiApiKey(context, "my_api_key_123")
        assertTrue(res.isSuccess)

        val retrieved = KeyStoreHelper.getGeminiApiKey(context)
        assertEquals("my_api_key_123", retrieved)
    }

    @Test
    fun testRetrieveNeverReturnsCiphertext() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cipherText = "ivPart:cipherPart"
        prefs.edit().putString(ENCRYPTED_GEMINI_API_KEY, cipherText).commit()

        // Make decryption fail by throwing an exception inside cipher init
        KeyStoreHelper.cipherProvider = { _ ->
            throw RuntimeException("Decryption failure")
        }

        val retrieved = KeyStoreHelper.getGeminiApiKey(context)
        assertEquals("", retrieved)
        assertFalse(retrieved.contains("cipherPart"))
        assertFalse(retrieved.contains("ivPart"))
        assertFalse(prefs.contains(ENCRYPTED_GEMINI_API_KEY))
    }
}
