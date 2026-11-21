package com.example.hellodoc.ui

import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.hellodoc.databinding.ActivityLoginBinding
import com.example.hellodoc.viewmodel.*

class LoginActivity : AppCompatActivity() {
    private val requestLan = registerForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (!allowed) toast("Autorisez l'accès au réseau local pour contacter votre PC.")
    }
    private val vm: AuthViewModel by viewModels { HelloDocViewModelFactory(application) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.btnLogin.setOnClickListener {
            val email = b.etEmail.text.toString().trim()
            val pwd = b.etPassword.text.toString()
            b.emailLayout.error = if (Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email)) null else "E-mail invalide"
            b.passwordLayout.error = if (pwd.isNotEmpty()) null else "Mot de passe obligatoire"
            if (b.emailLayout.error == null && b.passwordLayout.error == null) vm.login(email, pwd)
        }
        b.btnRegister.setOnClickListener { startActivity(Intent(this, RegisterActivity::class.java)) }
        vm.busy.observe(this) { busy ->
            b.progress.isVisible = busy
            b.btnLogin.isEnabled = !busy
            b.btnRegister.isEnabled = !busy
        }
        vm.error.observe(this) { error ->
            b.tvError.isVisible = !error.isNullOrBlank()
            b.tvError.text = error
        }
        vm.loginSuccess.observe(this) { event -> event.consume()?.let { openPatient() } }
        vm.restored.observe(this) { event -> event.consume()?.let { openPatient() } }
        if (Build.VERSION.SDK_INT >= 37 && ContextCompat.checkSelfPermission(
                this, "android.permission.ACCESS_LOCAL_NETWORK") != PackageManager.PERMISSION_GRANTED) {
            requestLan.launch("android.permission.ACCESS_LOCAL_NETWORK")
        }
        if (savedInstanceState == null) vm.restore()
    }
    private fun openPatient() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}
