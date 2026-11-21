package com.example.hellodoc

import android.app.Application
import com.example.hellodoc.network.RetrofitClient
import com.example.hellodoc.repository.*

class HelloDocApp : Application() {
    lateinit var session: SessionPreferences
        private set
    val authRepository by lazy { AuthRepository(RetrofitClient.api, RetrofitClient.cookieJar, session) }
    val doctorRepository by lazy { DoctorRepository(RetrofitClient.api) }
    val appointmentRepository by lazy { AppointmentRepository(RetrofitClient.api, authRepository) }
    val patientRepository by lazy { PatientRepository(RetrofitClient.api, authRepository) }

    override fun onCreate() {
        super.onCreate()
        RetrofitClient.initialize(this)
        session = SessionPreferences(this)
    }
}
