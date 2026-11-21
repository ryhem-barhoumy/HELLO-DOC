package tn.rnu.isetr.hellodoc.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import tn.rnu.isetr.hellodoc.model.*

interface AuthApi {
    @GET("auth/csrf") suspend fun csrf(): Response<CsrfToken>
    @POST("auth/login") suspend fun login(@HeaderMap headers: Map<String, String>, @Body request: LoginRequest): Response<SessionUser>
    @GET("auth/me") suspend fun me(): Response<SessionUser>
    @POST("auth/logout") suspend fun logout(@HeaderMap headers: Map<String, String>): Response<Unit>
    @GET("specialties") suspend fun specialties(): Response<List<Specialty>>
    @POST("patients/registrations") suspend fun registerPatient(@HeaderMap headers: Map<String, String>, @Body request: RegistrationRequest): Response<RegistrationResult>
    @Multipart @POST("doctors/registrations") suspend fun registerDoctor(
        @HeaderMap headers: Map<String, String>, @Part("registration") registration: RequestBody,
        @Part document: MultipartBody.Part
    ): Response<RegistrationResult>
}
