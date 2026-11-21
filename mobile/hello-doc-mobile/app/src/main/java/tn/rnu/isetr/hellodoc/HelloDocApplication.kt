package tn.rnu.isetr.hellodoc

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import tn.rnu.isetr.hellodoc.repository.AuthRepository
import tn.rnu.isetr.hellodoc.viewmodel.AuthViewModel

/** Application container: the current HelloDoc API is the only source of accounts. */
class HelloDocApplication : Application() {
    private val authRepository by lazy { AuthRepository() }
    val authViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass == AuthViewModel::class.java)
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authRepository) as T
        }
    }
}
