package tn.rnu.isetr.hellodoc.repository

import com.google.gson.Gson
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import tn.rnu.isetr.hellodoc.model.*
import tn.rnu.isetr.hellodoc.network.*

class ApiFailure(val status: Int, override val message: String, val fields: Map<String, String> = emptyMap()) : Exception(message)
private data class ApiError(val message: String?, val fieldErrors: Map<String, String>?)

class AuthRepository(
    private val api: AuthApi = RetrofitClient.authApi,
    private val cookies: SessionCookieJar = RetrofitClient.cookies
) {
    private val mutations = Mutex()
    private val gson = Gson()
    private suspend fun csrfHeaders(): Map<String, String> {
        val response = api.csrf()
        val csrf = requireBody(response)
        // The JSON token is XOR-masked by Spring; its SPA handler expects the raw cookie in headers.
        val token = cookies.loadForRequest(response.raw().request().url())
            .firstOrNull { it.name() == "XSRF-TOKEN" }?.value()
            ?: throw ApiFailure(403, "Impossible d’initialiser la protection de session. Réessayez.")
        return mapOf(csrf.headerName to token)
    }
    suspend fun login(email: String, password: String): SessionUser = mutations.withLock {
        requireBody(api.login(csrfHeaders(), LoginRequest(email, password)))
    }
    suspend fun me(): SessionUser? {
        val response = api.me()
        if (response.code() == 401) { cookies.clear(); return null }
        return requireBody(response)
    }
    suspend fun logout() = mutations.withLock {
        val response = api.logout(csrfHeaders())
        if (!response.isSuccessful && response.code() != 401) throw failure(response)
        cookies.clear()
    }
    suspend fun specialties(): List<Specialty> = requireBody(api.specialties())
    suspend fun register(request: RegistrationRequest, document: ByteArray?, mime: String?): RegistrationResult = mutations.withLock {
        val headers = csrfHeaders()
        if (request.specialty == null) return@withLock requireBody(api.registerPatient(headers, request))
        require(document != null && document.isNotEmpty() && document.size <= 5 * 1024 * 1024) { "Joignez un justificatif de 5 Mo maximum." }
        require(mime in setOf("application/pdf", "image/jpeg", "image/png")) { "Choisissez un fichier PDF, JPEG ou PNG." }
        val suffix = when (mime) { "application/pdf" -> "pdf"; "image/png" -> "png"; else -> "jpg" }
        val json = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), gson.toJson(request))
        val file = MultipartBody.Part.createFormData("document", "justificatif.$suffix", RequestBody.create(MediaType.parse(mime!!), document))
        requireBody(api.registerDoctor(headers, json, file))
    }
    private fun <T> requireBody(response: Response<T>): T {
        if (!response.isSuccessful) throw failure(response)
        return response.body() ?: throw ApiFailure(response.code(), "Le serveur a renvoyé une réponse vide.")
    }
    private fun failure(response: Response<*>): ApiFailure {
        val error = runCatching { gson.fromJson(response.errorBody()?.string(), ApiError::class.java) }.getOrNull()
        val message = when {
            response.code() == 401 -> "Adresse e-mail ou mot de passe incorrect, ou session expirée."
            response.code() == 403 && error?.message?.contains("approval", true) == true -> "Votre inscription médecin est en attente de validation par l’administrateur."
            response.code() == 403 -> "L’accès a été refusé. Vérifiez votre compte puis réessayez."
            response.code() >= 500 -> "Le serveur est momentanément indisponible. Réessayez."
            else -> error?.message ?: "La demande a échoué (HTTP ${response.code()})."
        }
        return ApiFailure(response.code(), message, error?.fieldErrors.orEmpty())
    }
}
