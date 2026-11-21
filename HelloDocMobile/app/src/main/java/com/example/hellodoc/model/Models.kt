package com.example.hellodoc.model

/** DTO Kotlin correspondant aux réponses JSON du backend Spring Boot Hello Doc. */

// Authentification et inscription du patient
data class CsrfToken(val headerName: String, val token: String)
data class LoginRequest(val email: String, val password: String)

data class SessionUser(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String,
    val status: String,
    val photoUrl: String?
) {
    val fullName: String get() = "$firstName $lastName".trim()
    val isPatient: Boolean get() = role in setOf("PATIENT", "USER") && status == "ACTIVE"
}

data class RegistrationRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val address: String,
    val password: String,
    val consent: Boolean = true
)

data class RegistrationResult(val id: String, val status: String)

data class Specialty(val id: String, val name: String) {
    override fun toString(): String = name
}

// Recherche paginée : Spring Boot renvoie un objet avec la propriété content.
data class PageResponse<T>(
    val content: List<T>,
    val number: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 0
)

data class Doctor(
    val id: String,
    val firstName: String?,
    val lastName: String?,
    val fullName: String?,
    val serviceTitle: String?,
    val specialty: String?,
    val city: String?,
    val location: String?,
    val address: String?,
    val status: String?,
    val nextAvailability: String?,
    val photoUrl: String?
) {
    fun displayName(): String = fullName?.takeIf { it.isNotBlank() }
        ?: "${firstName.orEmpty()} ${lastName.orEmpty()}".trim()
}

data class MedicalService(val name: String, val price: String?) {
    override fun toString(): String = "$name · ${price ?: "Tarif indisponible"}"
}

data class DoctorDetail(
    val id: String,
    val fullName: String?,
    val specialty: String?,
    val specialties: List<String>?,
    val city: String?,
    val location: String?,
    val address: String?,
    val phone: String?,
    val description: String?,
    val durationMinutes: Int?,
    val services: List<MedicalService>?,
    val nextAvailability: String?,
    val photoUrl: String?
)

data class TimeSlot(
    val id: String?,
    val startsAt: String,
    val endsAt: String,
    val status: String?,
    val periodLabel: String?
)

data class AppointmentCreateRequest(
    val doctorId: String,
    val serviceName: String,
    val startAt: String,
    val reason: String?,
    val expectedServicePrice: String?
)

data class Appointment(
    val id: String,
    val startsAt: String,
    val endsAt: String?,
    val status: String,
    val serviceName: String,
    val servicePrice: String?,
    val patientName: String?,
    val doctorName: String?,
    val location: String?,
    val reason: String?,
    val cancellationReason: String?
)

data class RescheduleRequest(val startAt: String, val reason: String?)
data class DecisionRequest(val reason: String?)

data class PatientProfile(
    val id: String,
    val firstName: String,
    val lastName: String,
    val fullName: String?,
    val email: String,
    val phone: String?,
    val address: String?,
    val photoUrl: String?
)

data class PatientUpdateRequest(
    val firstName: String,
    val lastName: String,
    val phone: String,
    val address: String,
    val currentPassword: String
)

/** Mot de passe modifié via /api/auth/password (backend réel). */
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)
