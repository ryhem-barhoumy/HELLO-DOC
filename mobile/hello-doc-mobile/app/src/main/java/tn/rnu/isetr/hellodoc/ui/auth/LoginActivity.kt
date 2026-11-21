package tn.rnu.isetr.hellodoc.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import tn.rnu.isetr.hellodoc.databinding.ActivityLoginBinding
import tn.rnu.isetr.hellodoc.model.RegistrationValidation
import tn.rnu.isetr.hellodoc.ui.HelloDocActivity
import tn.rnu.isetr.hellodoc.ui.account.AccountActivity

class LoginActivity : HelloDocActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityLoginBinding.inflate(layoutInflater)
        showContent(b.root)
        b.btnBack.setOnClickListener { finish() }
        b.etPassword.isSaveEnabled = false
        b.tvNotice.text = intent.getStringExtra("notice")
        b.tvNotice.isVisible = !b.tvNotice.text.isNullOrBlank()
        b.etEmail.setText(savedInstanceState?.getString("email") ?: intent.getStringExtra("email").orEmpty())
        fun submit() {
            val email = b.etEmail.text.toString().trim()
            val password = b.etPassword.text.toString()
            b.emailLayout.error = if (RegistrationValidation.validEmail(email)) null else "Saisissez une adresse e-mail valide."
            b.passwordLayout.error = if (password.isNotEmpty()) null else "Saisissez votre mot de passe."
            if (b.emailLayout.error == null && b.passwordLayout.error == null) withBackendAccess { viewModel.login(email, password) }
        }
        b.btnLogin.setOnClickListener { submit() }
        b.etPassword.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) { submit(); true } else false
        }
        b.tvRegister.setOnClickListener { if (viewModel.busy.value != true) startActivity(Intent(this, RegisterActivity::class.java)) }
        viewModel.busy.observe(this) {
            b.btnLogin.isEnabled = !it
            b.btnLogin.text = if (it) "Connexion…" else "Se connecter"
            b.progress.isVisible = it
            b.etEmail.isEnabled = !it
            b.etPassword.isEnabled = !it
            b.tvRegister.isEnabled = !it
        }
        viewModel.message.observe(this) { b.tvError.text = it; b.tvError.isVisible = !it.isNullOrBlank() }
        viewModel.loggedIn.observe(this) { event -> event.consume()?.let {
            b.etPassword.text?.clear()
            startActivity(Intent(this, AccountActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        } }
    }
}
