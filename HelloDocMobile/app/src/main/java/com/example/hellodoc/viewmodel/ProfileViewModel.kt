package com.example.hellodoc.viewmodel

import androidx.lifecycle.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.example.hellodoc.model.*
import com.example.hellodoc.repository.PatientRepository

class ProfileViewModel(private val repo: PatientRepository) : ViewModel() {
    private val _profile = MutableLiveData(UiState<PatientProfile>())
    val profile: LiveData<UiState<PatientProfile>> = _profile
    private val _saved = MutableLiveData<OneTime<Unit>>()
    val saved: LiveData<OneTime<Unit>> = _saved
    private val _photo = MutableLiveData<ByteArray?>()
    val photo: LiveData<ByteArray?> = _photo
    private val _photoMessage = MutableLiveData<OneTime<String>>()
    val photoMessage: LiveData<OneTime<String>> = _photoMessage
    fun loadPhoto() = viewModelScope.launch {
        try { _photo.value = repo.photo() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { _photo.value = null }
    }
    fun uploadPhoto(bytes: ByteArray, mime: String) = viewModelScope.launch {
        try {
            repo.uploadPhoto(bytes, mime)
            loadPhoto()
            _photoMessage.value = OneTime("Photo mise à jour.")
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { _photoMessage.value = OneTime(friendlyError(e)) }
    }
    fun deletePhoto() = viewModelScope.launch {
        try {
            repo.deletePhoto()
            _photo.value = null
            _photoMessage.value = OneTime("Photo supprimée.")
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { _photoMessage.value = OneTime(friendlyError(e)) }
    }
    fun load() = viewModelScope.launch {
        _profile.value = UiState(loading = true, data = _profile.value?.data)
        try { _profile.value = UiState(data = repo.get()) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { _profile.value = UiState(error = friendlyError(e)) }
    }
    fun save(request: PatientUpdateRequest) = viewModelScope.launch {
        _profile.value = UiState(loading = true, data = _profile.value?.data)
        try {
            val updated = repo.update(request)
            _profile.value = UiState(data = updated)
            _saved.value = OneTime(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { _profile.value = UiState(data = _profile.value?.data, error = friendlyError(e)) }
    }
}
