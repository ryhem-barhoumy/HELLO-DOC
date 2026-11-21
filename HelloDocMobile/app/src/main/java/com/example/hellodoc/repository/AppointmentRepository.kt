package com.example.hellodoc.repository

import com.example.hellodoc.model.*
import com.example.hellodoc.network.HelloDocApi

class AppointmentRepository(
    private val api: HelloDocApi,
    private val authRepository: AuthRepository
) {
    suspend fun list(history: Boolean): List<Appointment> =
        requireBody(api.appointments(history)).content

    suspend fun book(request: AppointmentCreateRequest): Appointment =
        requireBody(api.createAppointment(authRepository.csrfHeaders(), request))

    suspend fun options(id: String, date: String): List<TimeSlot> =
        requireBody(api.rescheduleOptions(id, date))

    suspend fun reschedule(id: String, startAt: String): Appointment =
        requireBody(
            api.reschedule(
                id,
                authRepository.csrfHeaders(),
                RescheduleRequest(startAt, null)
            )
        )

    suspend fun cancel(id: String): Appointment =
        requireBody(api.cancel(id, authRepository.csrfHeaders(), DecisionRequest(null)))
}
