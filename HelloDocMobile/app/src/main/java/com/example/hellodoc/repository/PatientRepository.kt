package com.example.hellodoc.repository

import com.example.hellodoc.model.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.hellodoc.network.HelloDocApi

class PatientRepository(private val api: HelloDocApi, private val auth: AuthRepository) {
    suspend fun photo(): ByteArray? {
        val response = api.ownPhoto()
        if (response.code() == 404) return null
        return requireBody(response).bytes()
    }
    suspend fun uploadPhoto(bytes: ByteArray, mime: String) {
        if (mime !in setOf("image/jpeg", "image/png") || bytes.size > 5 * 1024 * 1024) {
            throw ApiException(400, "Choisissez une photo JPG ou PNG de moins de 5 Mo.")
        }
        val part = MultipartBody.Part.createFormData("photo", if (mime == "image/png") "photo.png" else "photo.jpg",
            bytes.toRequestBody(mime.toMediaType()))
        requireBody(api.uploadPhoto(auth.csrfHeaders(), part))
    }
    suspend fun deletePhoto() {
        val response = api.deletePhoto(auth.csrfHeaders())
        if (!response.isSuccessful) throw parseFailure(response)
    }
    suspend fun get() = requireBody(api.patientProfile())
    suspend fun update(request: PatientUpdateRequest) = requireBody(api.updatePatient(auth.csrfHeaders(), request))
}
