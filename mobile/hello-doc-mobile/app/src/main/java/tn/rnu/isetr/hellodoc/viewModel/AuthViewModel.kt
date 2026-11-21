package tn.rnu.isetr.hellodoc.viewmodel

import androidx.lifecycle.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import tn.rnu.isetr.hellodoc.model.*
import tn.rnu.isetr.hellodoc.repository.*
import java.io.IOException

class Event<T>(private val value: T) {
    private var handled = false
    fun consume(): T? = if (handled) null else value.also { handled = true }
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _user = MutableLiveData<SessionUser?>(null)
    val user: LiveData<SessionUser?> = _user
    private val _busy = MutableLiveData(false)
    val busy: LiveData<Boolean> = _busy
    private val _networkAccess = MutableLiveData(false)
    val networkAccess: LiveData<Boolean> = _networkAccess
    private val _message = MutableLiveData<String?>(null)
    val message: LiveData<String?> = _message
    private val _fieldErrors = MutableLiveData<Map<String, String>>(emptyMap())
    val fieldErrors: LiveData<Map<String, String>> = _fieldErrors
    private val _specialties = MutableLiveData<List<Specialty>>(emptyList())
    val specialties: LiveData<List<Specialty>> = _specialties
    private val _specialtiesLoading = MutableLiveData(false)
    val specialtiesLoading: LiveData<Boolean> = _specialtiesLoading
    private val _specialtiesError = MutableLiveData<String?>(null)
    val specialtiesError: LiveData<String?> = _specialtiesError
    private val _registered = MutableLiveData<Event<Boolean>>()
    val registered: LiveData<Event<Boolean>> = _registered
    private val _loggedIn = MutableLiveData<Event<Unit>>()
    val loggedIn: LiveData<Event<Unit>> = _loggedIn
    private val _sessionChecked = MutableLiveData(false)
    val sessionChecked: LiveData<Boolean> = _sessionChecked
    fun setNetworkAccess(allowed: Boolean) { _networkAccess.value = allowed }
    fun showMessage(text: String) { _message.value = text }

    fun clearMessage() { _message.value = null; _fieldErrors.value = emptyMap() }
    private fun perform(action: suspend () -> Unit) {
        if (_busy.value == true) return
        if (networkAccess.value != true) {
            _message.value = "Autorisez l’accès au réseau local depuis l’accueil pour joindre le serveur HelloDoc de votre PC."
            return
        }
        _busy.value = true
        clearMessage()
        viewModelScope.launch {
            try { action() }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                _message.value = when (error) {
                    is IOException -> "Connexion au serveur impossible. Vérifiez votre réseau et l’adresse du backend, puis réessayez."
                    else -> error.message ?: "La demande a échoué. Réessayez."
                }
                _fieldErrors.value = (error as? ApiFailure)?.fields.orEmpty()
            } finally { _busy.value = false }
        }
    }
    fun checkSession(force: Boolean = false) {
        if (_sessionChecked.value == true && !force) return
        perform { _user.value = repository.me(); _sessionChecked.value = true }
    }
    fun login(email: String, password: String) = perform {
        _user.value = repository.login(email, password)
        _sessionChecked.value = true
        _loggedIn.value = Event(Unit)
    }
    fun register(request: RegistrationRequest, document: ByteArray?, mime: String?) = perform {
        repository.register(request, document, mime)
        _registered.value = Event(request.specialty != null)
    }
    fun logout() = perform {
        repository.logout()
        _user.value = null
        _sessionChecked.value = true
    }
    fun loadSpecialties() {
        if (_specialtiesLoading.value == true) return
        if (networkAccess.value != true) {
            _specialtiesError.value = "L’accès au serveur local n’est pas autorisé. Revenez à l’accueil."
            return
        }
        _specialtiesLoading.value = true
        _specialtiesError.value = null
        viewModelScope.launch {
            try {
                _specialties.value = repository.specialties()
                if (specialties.value.isNullOrEmpty()) _specialtiesError.value = "Aucune spécialité disponible. Contactez le cabinet ou réessayez plus tard."
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { _specialtiesError.value = "Impossible de charger les spécialités. Vérifiez la connexion puis réessayez." }
            finally { _specialtiesLoading.value = false }
        }
    }
}
