package com.example.hellodoc.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.hellodoc.databinding.ActivityRegisterBinding
import com.example.hellodoc.model.RegistrationRequest
import com.example.hellodoc.viewmodel.*

class RegisterActivity : AppCompatActivity() {
    private val vm: AuthViewModel by viewModels { HelloDocViewModelFactory(application) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.btnBack.setOnClickListener { finish() }
        b.btnRegister.setOnClickListener {
            val first = b.etFirstName.text.toString().trim()
            val last = b.etLastName.text.toString().trim()
            val email = b.etEmail.text.toString().trim()
            val phone = b.etPhone.text.toString().trim()
            val address = b.etAddress.text.toString().trim()
            val password = b.etPassword.text.toString()
            val confirm = b.etConfirm.text.toString()
            b.firstLayout.error = if (first.length >= 2) null else "Minimum 2 caractères"
            b.lastLayout.error = if (last.length >= 2) null else "Minimum 2 caractères"
            b.emailLayout.error = if (Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email)) null else "E-mail invalide"
            b.phoneLayout.error = if (Regex("[0-9]{8,15}").matches(phone)) null else "8 à 15 chiffres"
            b.addressLayout.error = if (address.isNotBlank()) null else "Adresse obligatoire"
            b.passwordLayout.error = if (password.length in 8..72) null else "8 à 72 caractères"
            b.confirmLayout.error = if (password == confirm) null else "Les mots de passe sont différents"
            if (!b.checkConsent.isChecked) { toast("Acceptez le traitement des données pour continuer."); return@setOnClickListener }
            val valid = listOf(b.firstLayout, b.lastLayout, b.emailLayout, b.phoneLayout,
                b.addressLayout, b.passwordLayout, b.confirmLayout).all { it.error == null }
            if (valid) vm.register(RegistrationRequest(first, last, email, phone, address, password))
        }
        vm.busy.observe(this) { busy -> b.progress.isVisible = busy; b.btnRegister.isEnabled = !busy }
        vm.error.observe(this) { error -> b.tvError.isVisible = !error.isNullOrBlank(); b.tvError.text = error }
        vm.registered.observe(this) { event -> event.consume()?.let {
            toast("Compte patient créé. Connectez-vous maintenant.")
            finish()
        } }
    }
}
