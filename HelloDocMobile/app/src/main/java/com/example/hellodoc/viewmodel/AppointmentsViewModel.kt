package com.example.hellodoc.viewmodel

import androidx.lifecycle.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.example.hellodoc.model.*
import com.example.hellodoc.repository.AppointmentRepository

class AppointmentsViewModel(private val repo: AppointmentRepository) : ViewModel() {
    private val _appointments = MutableLiveData(UiState<List<Appointment>>())
    val appointments: LiveData<UiState<List<Appointment>>> = _appointments
    private val _busy = MutableLiveData(false)
    val busy: LiveData<Boolean> = _busy
    private val _message = MutableLiveData<OneTime<String>>()
    val message: LiveData<OneTime<String>> = _message
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error
    private val _booked = MutableLiveData<OneTime<Unit>>()
    val booked: LiveData<OneTime<Unit>> = _booked
    private val _options = MutableLiveData<OneTime<List<TimeSlot>>>()
    val options: LiveData<OneTime<List<TimeSlot>>> = _options
    private var lastHistory = false

    fun load(history: Boolean = lastHistory) {
        lastHistory = history
        viewModelScope.launch {
            _appointments.value = UiState(loading = true)
            try { _appointments.value = UiState(data = repo.list(history)) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _appointments.value = UiState(error = friendlyError(e)) }
        }
    }
    fun book(body: AppointmentCreateRequest) = action {
        repo.book(body)
        _booked.value = OneTime(Unit)
        _message.value = OneTime("Demande de rendez-vous envoyée au médecin.")
    }
    fun rescheduleOptions(id: String, date: String) = action {
        _options.value = OneTime(repo.options(id, date))
    }
    fun reschedule(id: String, startAt: String) = action {
        repo.reschedule(id, startAt)
        _message.value = OneTime("Rendez-vous modifié.")
        load()
    }
    fun cancel(id: String) = action {
        repo.cancel(id)
        _message.value = OneTime("Rendez-vous annulé.")
        load()
    }
    private fun action(block: suspend () -> Unit) {
        if (_busy.value == true) return
        _busy.value = true; _error.value = null
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = friendlyError(e) }
            finally { _busy.value = false }
        }
    }
}
