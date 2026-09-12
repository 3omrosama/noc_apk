package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.example.data.model.User
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class SecureSessionManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val userAdapter = moshi.adapter(User::class.java)

    private val _sessionExpiredEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpiredEvent: SharedFlow<Unit> = _sessionExpiredEvent.asSharedFlow()

    private val _isLoggedInState = MutableStateFlow(hasValidSession())
    val isLoggedInState: StateFlow<Boolean> = _isLoggedInState.asStateFlow()

    init {
        initKeystore()
    }

    private var secretKey: SecretKey? = null

    private fun initKeystore() {
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGen = KeyGenerator.getInstance(KEY_ALGORITHM, KEYSTORE_PROVIDER)
                val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                            android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGen.init(spec)
                keyGen.generateKey()
            }
            secretKey = (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
        } catch (_: Exception) {
            // Fallback for JVM unit tests / environments where AndroidKeyStore provider is unavailable
            val fallbackBytes = "InfraManagerNocSecureFallback256".toByteArray(StandardCharsets.UTF_8)
            secretKey = SecretKeySpec(fallbackBytes, "AES")
        }
    }

    private fun encrypt(plaintext: String): String {
        val key = secretKey ?: return plaintext
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Exception) {
            plaintext
        }
    }

    private fun decrypt(ciphertext: String): String? {
        val key = secretKey ?: return ciphertext
        return try {
            val combined = Base64.decode(ciphertext, Base64.NO_WRAP)
            if (combined.size < GCM_IV_LENGTH) return null
            val iv = ByteArray(GCM_IV_LENGTH)
            val encryptedBytes = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun saveServerUrl(url: String) {
        val normalized = UrlNormalizer.normalize(url)
        prefs.edit().putString(KEY_SERVER_URL, normalized).apply()
    }

    fun getServerUrl(): String? {
        return prefs.getString(KEY_SERVER_URL, null)
    }

    fun saveSession(token: String, refreshToken: String?, user: User?) {
        val encryptedToken = encrypt(token)
        val encryptedRefreshToken = refreshToken?.let { encrypt(it) }
        val userJson = user?.let { userAdapter.toJson(it) }

        prefs.edit()
            .putString(KEY_TOKEN, encryptedToken)
            .putString(KEY_REFRESH_TOKEN, encryptedRefreshToken)
            .putString(KEY_USER, userJson)
            .apply()

        _isLoggedInState.value = true
    }

    fun getToken(): String? {
        val raw = prefs.getString(KEY_TOKEN, null) ?: return null
        return decrypt(raw)
    }

    fun getRefreshToken(): String? {
        val raw = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        return decrypt(raw)
    }

    fun getUser(): User? {
        val json = prefs.getString(KEY_USER, null) ?: return null
        return try {
            userAdapter.fromJson(json)
        } catch (_: Exception) {
            null
        }
    }

    fun hasValidSession(): Boolean {
        return !getToken().isNullOrBlank()
    }

    fun onSessionExpired() {
        clearSession()
        _sessionExpiredEvent.tryEmit(Unit)
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER)
            .apply()
        _isLoggedInState.value = false
    }

    fun clearAll() {
        prefs.edit().clear().apply()
        _isLoggedInState.value = false
    }

    companion object {
        private const val PREFS_NAME = "inframanager_secure_session"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_REFRESH_TOKEN = "jwt_refresh_token"
        private const val KEY_USER = "user_json"

        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "InfraManagerSessionKey"
        private const val KEY_ALGORITHM = "AES"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }
}
