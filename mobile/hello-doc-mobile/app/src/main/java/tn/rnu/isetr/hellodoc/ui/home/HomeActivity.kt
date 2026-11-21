package tn.rnu.isetr.hellodoc.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import tn.rnu.isetr.hellodoc.databinding.ActivityHomeBinding
import tn.rnu.isetr.hellodoc.network.LocalBackendAccess
import tn.rnu.isetr.hellodoc.ui.HelloDocActivity
import tn.rnu.isetr.hellodoc.ui.account.AccountActivity
import tn.rnu.isetr.hellodoc.ui.auth.LoginActivity
import tn.rnu.isetr.hellodoc.ui.auth.RegisterActivity

class HomeActivity : HelloDocActivity() {
    private lateinit var binding: ActivityHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        showContent(binding.root)
        binding.btnWelcomeLogin.setOnClickListener { startActivity(Intent(this, LoginActivity::class.java)) }
        binding.btnWelcomeRegister.setOnClickListener { openRegistration(false) }
        binding.btnDoctor.setOnClickListener { openRegistration(true) }
        binding.btnNetworkAccess.setOnClickListener { withBackendAccess { viewModel.checkSession(true) } }
        binding.btnRetry.setOnClickListener { withBackendAccess { viewModel.checkSession(true) } }
        viewModel.message.observe(this) {
            binding.tvError.text = it
            binding.tvError.isVisible = !it.isNullOrBlank()
            binding.btnRetry.isVisible = !it.isNullOrBlank()
        }
        viewModel.busy.observe(this) { binding.progress.isVisible = it; binding.btnRetry.isEnabled = !it }
        viewModel.networkAccess.observe(this) { binding.layoutLocalNetwork.isVisible = !it }
        viewModel.user.observe(this) { user ->
            if (user != null) startActivity(Intent(this, AccountActivity::class.java))
        }
    }
    private fun openRegistration(doctor: Boolean) {
        startActivity(Intent(this, RegisterActivity::class.java).putExtra("doctor", doctor))
    }
    override fun onResume() {
        super.onResume()
        if (LocalBackendAccess.isAllowed(this)) viewModel.checkSession(true)
    }
}
