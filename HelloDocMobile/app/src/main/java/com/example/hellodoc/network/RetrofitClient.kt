package com.example.hellodoc.network

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SessionCookieJar(context: Context) : CookieJar {

    private val cookies = mutableListOf<Cookie>()
    private val preferences = context.getSharedPreferences("hellodoc_secure_session", Context.MODE_PRIVATE)
    private val cookieUrl = RetrofitClient.BASE_URL.toHttpUrl()

    init {
        try {
            val encrypted = preferences.getString("cookies", null)
            if (encrypted != null) {
                val plain = decrypt(encrypted)
                cookies.addAll(plain.lineSequence().mapNotNull { Cookie.parse(cookieUrl, it) })
                cookies.removeAll { it.expiresAt <= System.currentTimeMillis() }
            }
        } catch (_: Exception) {
            preferences.edit().remove("cookies").apply()
            cookies.clear()
        }
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, received: List<Cookie>) {
        received.forEach { cookie ->
            cookies.removeAll { it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path }
            if (cookie.expiresAt > System.currentTimeMillis()) cookies.add(cookie)
        }
        persist()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        cookies.removeAll { it.expiresAt <= System.currentTimeMillis() }
        return cookies.filter { it.matches(url) }
    }

    @Synchronized
    fun csrfCookie(): String? = cookies.firstOrNull { it.name == "XSRF-TOKEN" }?.value

    @Synchronized
    fun clear() {
        cookies.clear()
        preferences.edit().remove("cookies").apply()
    }

    private fun persist() {
        try {
            val serialized = cookies.joinToString("\n") { it.toString() }
            preferences.edit().putString("cookies", encrypt(serialized)).apply()
        } catch (_: Exception) {
            // Si Keystore est indisponible, la session reste en mémoire jusqu'à la fermeture.
            preferences.edit().remove("cookies").apply()
        }
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = "hello_doc_cookie_key"
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build())
            generateKey()
        }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val payload = Base64.decode(value, Base64.NO_WRAP)
        require(payload.size > 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, payload.copyOfRange(0, 12)))
        return String(cipher.doFinal(payload.copyOfRange(12, payload.size)), Charsets.UTF_8)
    }
}

object RetrofitClient {
    const val BASE_URL = "http://10.213.108.180:8081/api/"

    private lateinit var sessionJar: SessionCookieJar

    fun initialize(context: Context) {
        if (!::sessionJar.isInitialized) sessionJar = SessionCookieJar(context.applicationContext)
    }

    val cookieJar: SessionCookieJar get() = sessionJar

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: HelloDocApi by lazy { retrofit.create(HelloDocApi::class.java) }
}
