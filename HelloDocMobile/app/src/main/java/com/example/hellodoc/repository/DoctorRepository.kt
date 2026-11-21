package com.example.hellodoc.repository

import com.example.hellodoc.model.Doctor
import com.example.hellodoc.model.DoctorDetail
import com.example.hellodoc.model.Specialty
import com.example.hellodoc.model.TimeSlot
import com.example.hellodoc.network.HelloDocApi

class DoctorRepository(private val api: HelloDocApi) {

    suspend fun specialties(): List<Specialty> = requireBody(api.specialties())

    suspend fun search(
        name: String?, specialty: String?, city: String?, date: String?
    ): List<Doctor> {
        val response = api.doctors(
            name?.takeIf { it.isNotBlank() },
            specialty?.takeIf { it.isNotBlank() },
            city?.takeIf { it.isNotBlank() },
            date?.takeIf { it.isNotBlank() }
        )
        return requireBody(response).content
    }

    suspend fun detail(id: String): DoctorDetail = requireBody(api.doctor(id))

    suspend fun slots(id: String, date: String): List<TimeSlot> =
        requireBody(api.slots(id, date))
}
