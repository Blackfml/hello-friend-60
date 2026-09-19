package com.jarvis.ai.data.settings

import android.content.Context
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureApiKeyStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(apiKey: String) {
        if (apiKey.isBlank()) {
            clear()
            return
        }
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString(DATA, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    fun read(): String? {
        val data = prefs.getString(DATA, null) ?: return null
        val iv = prefs.getString(IV, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
            )
            String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrNull()
    }

    fun clear() {
        prefs.edit().remove(DATA).remove(IV).apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(ALIAS, null)?.let { return it as SecretKey }

        val generator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
        generator.init(256)
        return generator.generateKey()
    }

    private companion object {
        const val PREFS = "jarvis_secure_secrets"
        const val DATA = "gemini_api_key"
        const val IV = "gemini_api_key_iv"
        const val ALIAS = "jarvis_gemini_aes"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
