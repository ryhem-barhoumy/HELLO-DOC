package com.example.hellodoc.repository

import com.google.gson.JsonParser
import retrofit2.Response

class ApiException(val status: Int, override val message: String) : Exception(message)

fun <T> requireBody(response: Response<T>): T {
    if (!response.isSuccessful) throw parseFailure(response)
    return response.body() ?: throw ApiException(response.code(), "Le serveur a renvoyé une réponse vide.")
}

fun parseFailure(response: Response<*>): ApiException {
    val status = response.code()
    val serverMessage = try {
        val raw = response.errorBody()?.string().orEmpty()
        val json = JsonParser.parseString(raw).asJsonObject
        json.get("message")?.takeUnless { it.isJsonNull }?.asString
    } catch (_: Exception) { null }
    val fallback = when (status) {
        400 -> "Vérifiez les informations saisies."
        401 -> "Session expirée ou identifiants incorrects."
        403 -> "Accès refusé. Vérifiez votre compte."
        404 -> "Information introuvable."
        409 -> "Ce créneau n'est plus disponible ou un conflit est survenu."
        in 500..599 -> "Erreur du serveur. Réessayez plus tard."
        else -> "La demande a échoué (HTTP $status)."
    }
    return ApiException(status, serverMessage ?: fallback)
}
