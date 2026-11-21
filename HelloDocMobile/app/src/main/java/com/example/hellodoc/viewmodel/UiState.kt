package com.example.hellodoc.viewmodel

import androidx.lifecycle.*
import android.app.Application
import com.example.hellodoc.HelloDocApp

// États observables via LiveData, comme au TP6/TP7.
data class UiState<T>(val loading: Boolean = false, val data: T? = null, val error: String? = null)
class OneTime<T>(private val item: T) {
    private var handled = false
    fun consume(): T? = if (handled) null else { handled = true; item }
}
fun friendlyError(e: Exception): String = when (e) {
    is java.io.IOException -> "Connexion impossible. Vérifiez le Wi-Fi, l'IP du PC et Spring Boot."
    else -> e.message ?: "Une erreur est survenue."
}

class HelloDocViewModelFactory(application: Application) : ViewModelProvider.Factory {
    private val app = application as HelloDocApp
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        AuthViewModel::class.java -> AuthViewModel(app.authRepository)
        DoctorsViewModel::class.java -> DoctorsViewModel(app.doctorRepository)
        AppointmentsViewModel::class.java -> AppointmentsViewModel(app.appointmentRepository)
        ProfileViewModel::class.java -> ProfileViewModel(app.patientRepository)
        else -> throw IllegalArgumentException("ViewModel inconnu: ${modelClass.name}")
    } as T
}
