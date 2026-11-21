package com.example.hellodoc.viewmodel

import androidx.lifecycle.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.example.hellodoc.model.*
import com.example.hellodoc.repository.AuthRepository

class AuthViewModel(private val repo: AuthRepository) : ViewModel() {
    private val _busy = MutableLiveData(false)
    val busy: LiveData<Boolean> = _busy
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error
    private val _login = MutableLiveData<OneTime<SessionUser>>()
    val loginSuccess: LiveData<OneTime<SessionUser>> = _login
    private val _registered = MutableLiveData<OneTime<Unit>>()
    val registered: LiveData<OneTime<Unit>> = _registered
    private val _restored = MutableLiveData<OneTime<SessionUser?>>()
    val restored: LiveData<OneTime<SessionUser?>> = _restored
    private val _logout = MutableLiveData<OneTime<Unit>>()
    val logoutDone: LiveData<OneTime<Unit>> = _logout
    fun login(email: String, password: String) = runAction { _login.value = OneTime(repo.login(email, password)) }
    fun register(body: RegistrationRequest) = runAction { repo.register(body); _registered.value = OneTime(Unit) }
    fun restore() {
        if (_busy.value == true) return
        _busy.value = true
        viewModelScope.launch {
            try { _restored.value = OneTime(repo.restore()) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _restored.value = OneTime(null) }
            finally { _busy.value = false }
        }
    }
    private val _passwordChanged = MutableLiveData<OneTime<Unit>>()
    val passwordChanged: LiveData<OneTime<Unit>> = _passwordChanged

    fun changePassword(currentPassword: String, newPassword: String) = runAction {
        repo.changePassword(currentPassword, newPassword)
        _passwordChanged.value = OneTime(Unit)
    }

    fun logout() = runAction { repo.logout(); _logout.value = OneTime(Unit) }
    private fun runAction(block: suspend () -> Unit) {
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
