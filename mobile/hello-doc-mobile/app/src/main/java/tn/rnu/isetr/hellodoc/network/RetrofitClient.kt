package tn.rnu.isetr.hellodoc.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tn.rnu.isetr.hellodoc.BuildConfig
import java.util.concurrent.TimeUnit

/** Session cookies stay in memory, never in backups or a fake JWT preference. */
class SessionCookieJar : CookieJar {
    private val cookies = mutableListOf<Cookie>()
    @Synchronized override fun saveFromResponse(url: HttpUrl, received: List<Cookie>) {
        received.forEach { cookie ->
            cookies.removeAll { it.name() == cookie.name() && it.domain() == cookie.domain() && it.path() == cookie.path() }
            if (cookie.expiresAt() > System.currentTimeMillis()) cookies.add(cookie)
        }
    }
    @Synchronized override fun loadForRequest(url: HttpUrl): List<Cookie> {
        cookies.removeAll { it.expiresAt() <= System.currentTimeMillis() }
        return cookies.filter { it.matches(url) }
    }
    @Synchronized fun clear() = cookies.clear()
}

object RetrofitClient {
    val cookies = SessionCookieJar()
    val authApi: AuthApi by lazy { createApi(BuildConfig.API_BASE_URL, cookies) }
    fun createApi(baseUrl: String, cookieJar: SessionCookieJar): AuthApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(OkHttpClient.Builder().cookieJar(cookieJar)
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS).retryOnConnectionFailure(false).build())
        .addConverterFactory(GsonConverterFactory.create()).build().create(AuthApi::class.java)
}
