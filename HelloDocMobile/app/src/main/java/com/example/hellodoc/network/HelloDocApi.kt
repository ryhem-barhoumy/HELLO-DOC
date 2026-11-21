package com.example.hellodoc.network

import com.example.hellodoc.model.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface HelloDocApi {
    @GET("auth/csrf")
    suspend fun csrf(): Response<CsrfToken>

    @POST("auth/login")
    suspend fun login(
        @HeaderMap headers: Map<String, String>,
        @Body request: LoginRequest
    ): Response<SessionUser>

    @GET("auth/me")
    suspend fun me(): Response<SessionUser>

    @POST("auth/logout")
    suspend fun logout(@HeaderMap headers: Map<String, String>): Response<Unit>

    @POST("patients/registrations")
    suspend fun register(
        @HeaderMap headers: Map<String, String>,
        @Body request: RegistrationRequest
    ): Response<RegistrationResult>

    @POST("auth/password")
    suspend fun changePassword(
        @HeaderMap headers: Map<String, String>,
        @Body request: ChangePasswordRequest
    ): Response<Unit>

    // Profil patient
    @GET("patients/me")
    suspend fun patientProfile(): Response<PatientProfile>

    @PUT("patients/me")
    suspend fun updatePatient(
        @HeaderMap headers: Map<String, String>,
        @Body request: PatientUpdateRequest
    ): Response<PatientProfile>

    @GET("account/me/photo")
    suspend fun ownPhoto(): Response<ResponseBody>

    @Multipart
    @POST("account/me/photo")
    suspend fun uploadPhoto(
        @HeaderMap headers: Map<String, String>,
        @Part photo: MultipartBody.Part
    ): Response<Map<String, String>>

    @DELETE("account/me/photo")
    suspend fun deletePhoto(@HeaderMap headers: Map<String, String>): Response<Unit>

    // --- Recherche et disponibilité ---
    @GET("specialties")
    suspend fun specialties(): Response<List<Specialty>>

    @GET("doctors")
    suspend fun doctors(
        @Query("q") query: String?,
        @Query("specialty") specialty: String?,
        @Query("city") city: String?,
        @Query("availableOn") availableOn: String?,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<PageResponse<Doctor>>

    @GET("doctors/{id}")
    suspend fun doctor(@Path("id") id: String): Response<DoctorDetail>

    @GET("doctors/{id}/slots")
    suspend fun slots(
        @Path("id") id: String,
        @Query("date") date: String
    ): Response<List<TimeSlot>>

    @POST("appointments")
    suspend fun createAppointment(
        @HeaderMap headers: Map<String, String>,
        @Body body: AppointmentCreateRequest
    ): Response<Appointment>

    @GET("appointments/mine")
    suspend fun appointments(
        @Query("history") history: Boolean,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100
    ): Response<PageResponse<Appointment>>

    @GET("appointments/{id}/reschedule-options")
    suspend fun rescheduleOptions(
        @Path("id") id: String,
        @Query("date") date: String
    ): Response<List<TimeSlot>>

    @PUT("appointments/{id}/reschedule")
    suspend fun reschedule(
        @Path("id") id: String,
        @HeaderMap headers: Map<String, String>,
        @Body body: RescheduleRequest
    ): Response<Appointment>

    @POST("appointments/{id}/cancellation")
    suspend fun cancel(
        @Path("id") id: String,
        @HeaderMap headers: Map<String, String>,
        @Body body: DecisionRequest
    ): Response<Appointment>
}
