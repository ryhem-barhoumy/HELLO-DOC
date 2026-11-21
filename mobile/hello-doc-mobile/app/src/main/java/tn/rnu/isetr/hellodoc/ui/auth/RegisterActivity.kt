package tn.rnu.isetr.hellodoc.ui.auth

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import tn.rnu.isetr.hellodoc.R
import tn.rnu.isetr.hellodoc.databinding.ActivityFlowBinding
import tn.rnu.isetr.hellodoc.ui.HelloDocActivity

class RegisterActivity : HelloDocActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityFlowBinding.inflate(layoutInflater)
        showContent(binding.root)
        binding.toolbar.title = getString(R.string.register_toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
        if (savedInstanceState == null) supportFragmentManager.beginTransaction()
            .replace(R.id.flowHost, NavHostFragment.create(R.navigation.registration_navigation, Bundle().apply { putBoolean("doctor", intent.getBooleanExtra("doctor", false)) }))
            .commit()
    }
}
