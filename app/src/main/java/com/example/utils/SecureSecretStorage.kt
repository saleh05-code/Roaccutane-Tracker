package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed Android Keystore AES-256-GCM storage for sensitive user-entered secrets
 * (such as custom API keys). Secrets stored here are bound to the current device and are
 * never written in plaintext to SharedPreferences or synchronized to cloud backups.
 */
class SecureSecretStorage(context: Context) {

    private val appContext = context.applicationContext
    private val securePrefs: SharedPreferences =
        appContext.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSecret(key: String, value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            clearSecret(key)
            return
        }
        val encrypted = encrypt(trimmed)
        securePrefs.edit().putString(key, encrypted).apply()
    }

    fun getSecret(key: String): String {
        val stored = securePrefs.getString(key, null) ?: return ""
        return decrypt(stored)
    }

    fun clearSecret(key: String) {
        securePrefs.edit().remove(key).apply()
    }

    /**
     * Migrates any legacy plaintext secret from [legacyPrefs] into Keystore-encrypted
     * storage and permanently removes the plaintext entry from [legacyPrefs].
     */
    fun migrateFromLegacyPlaintext(legacyPrefs: SharedPreferences, key: String): String {
        val legacyValue = legacyPrefs.getString(key, null)?.trim().orEmpty()
        if (legacyValue.isNotEmpty()) {
            saveSecret(key, legacyValue)
            legacyPrefs.edit().remove(key).apply()
        }
        return getSecret(key)
    }

    private fun getOrCreateAesKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingEntry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existingEntry != null) {
            return existingEntry.secretKey
        }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plaintext: String): String {
        return try {
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateAesKey())
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(1 + iv.size + cipherBytes.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(cipherBytes, 0, combined, 1 + iv.size, cipherBytes.size)
            PREFIX_V1 + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Exception) {
            ""
        }
    }

    private fun decrypt(stored: String): String {
        if (!stored.startsWith(PREFIX_V1)) {
            return ""
        }
        return try {
            val combined = Base64.decode(stored.removePrefix(PREFIX_V1), Base64.NO_WRAP)
            if (combined.size < 2) return ""
            val ivLen = combined[0].toInt() and 0xFF
            if (combined.size <= 1 + ivLen) return ""
            val iv = combined.copyOfRange(1, 1 + ivLen)
            val cipherBytes = combined.copyOfRange(1 + ivLen, combined.size)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateAesKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "roaccutane_tracker_secret_aes_key"
        private const val SECURE_PREFS_NAME = "roaccutane_secure_secrets"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val PREFIX_V1 = "enc_v1:"
        const val KEY_CUSTOM_GEMINI_API = "custom_gemini_api_key"
    }
}
