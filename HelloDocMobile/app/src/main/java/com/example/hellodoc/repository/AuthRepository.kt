package com.example.hellodoc.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.example.hellodoc.model.*
import com.example.hellodoc.network.HelloDocApi
import com.example.hellodoc.network.SessionCookieJar

class AuthRepository(private val api: HelloDocApi, private val jar: SessionCookieJar,
                     private val preferences: SessionPreferences) {
    private val mutex = Mutex()
    suspend fun csrfHeaders(): Map<String, String> {
        val response = api.csrf()
        val csrf = requireBody(response)
        val rawCookie = jar.csrfCookie()
            ?: throw ApiException(403, "Cookie de sécurité introuvable. Réessayez.")
        return mapOf(csrf.headerName to rawCookie)
    }
    suspend fun login(email: String, password: String): SessionUser = mutex.withLock {
        val user = requireBody(api.login(csrfHeaders(), LoginRequest(email.trim(), password)))
        if (!user.isPatient) {
            try { api.logout(csrfHeaders()) } finally { jar.clear() }
            throw ApiException(403, "Cette application est réservée aux patients.")
        }
        preferences.hasLoggedIn = true
        user
    }
    suspend fun register(body: RegistrationRequest): RegistrationResult = mutex.withLock {
        requireBody(api.register(csrfHeaders(), body))
    }
    suspend fun restore(): SessionUser? {
        if (!preferences.hasLoggedIn) return null
        val response = api.me()
        if (response.code() == 401 || response.code() == 403) {
            jar.clear(); preferences.clear(); return null
        }
        val user = requireBody(response)
        if (!user.isPatient) { jar.clear(); preferences.clear(); return null }
        return user
    }
    suspend fun changePassword(currentPassword: String, newPassword: String) = mutex.withLock {
        val response = api.changePassword(
            csrfHeaders(), ChangePasswordRequest(currentPassword, newPassword)
        )
        if (!response.isSuccessful) throw parseFailure(response)
    }

    suspend fun logout() = mutex.withLock {
        try {
            val response = api.logout(csrfHeaders())
            if (!response.isSuccessful && response.code() != 401) {
            }
        } catch (_: Exception) {
        } finally {
            jar.clear()
            preferences.clear()
        }
    }
}
