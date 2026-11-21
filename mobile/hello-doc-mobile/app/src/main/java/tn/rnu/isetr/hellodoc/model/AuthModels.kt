package tn.rnu.isetr.hellodoc.model

data class SessionUser(
    val id: String, val firstName: String, val lastName: String,
    val email: String, val role: String, val status: String
) {
    val displayName: String get() = "$firstName $lastName".trim()
    val spaceLabel: String get() = when (role) {
        "DOCTOR" -> "Espace médecin"
        "USER", "PATIENT" -> "Espace patient"
        "ADMIN" -> "Compte administrateur"
        else -> "Mon compte"
    }
}

data class CsrfToken(val headerName: String, val token: String)
data class RegistrationResult(val id: String, val status: String)
data class Specialty(val id: String, val name: String) {
    override fun toString(): String = name
}
data class RegistrationRequest(
    val firstName: String, val lastName: String, val email: String,
    val phone: String, val address: String, val password: String,
    val consent: Boolean,
    val specialty: String? = null, val licenseNumber: String? = null,
    val city: String? = null
)

/** Also checked by the server; shared by the form and JVM tests. */
object RegistrationValidation {
    fun validEmail(email: String) = email.length <= 254 && Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email)
    fun errors(request: RegistrationRequest, confirmation: String, doctor: Boolean): Map<String, String> = buildMap {
        if (request.firstName.length !in 2..100) put("firstName", "Saisissez un prénom de 2 à 100 caractères.")
        if (request.lastName.length !in 2..100) put("lastName", "Saisissez un nom de 2 à 100 caractères.")
        if (!validEmail(request.email)) put("email", "Saisissez une adresse e-mail valide.")
        if (!Regex("[0-9]{8,15}").matches(request.phone)) put("phone", "Saisissez entre 8 et 15 chiffres, sans lettres.")
        if (request.address.isBlank() || request.address.length > 300) put("address", "Saisissez votre adresse (300 caractères maximum).")
        if (request.password.length !in 8..72) put("password", "Utilisez entre 8 et 72 caractères.")
        if (request.password != confirmation) put("confirmation", "Les mots de passe ne correspondent pas.")
        if (!request.consent) put("consent", "Votre consentement est nécessaire.")
        if (doctor) {
            if (request.specialty.isNullOrBlank() || request.specialty.length > 120) put("specialty", "Choisissez une spécialité.")
            if (request.licenseNumber.isNullOrBlank() || request.licenseNumber.length > 100) put("licenseNumber", "Saisissez votre numéro d’inscription à l’Ordre.")
            if (request.city.isNullOrBlank() || request.city.length > 120) put("city", "Saisissez votre ville.")
        }
    }
}
