package tn.rnu.isetr.hellodoc.ui.account

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import tn.rnu.isetr.hellodoc.R
import tn.rnu.isetr.hellodoc.databinding.ActivityFlowBinding
import tn.rnu.isetr.hellodoc.ui.HelloDocActivity

class AccountActivity : HelloDocActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityFlowBinding.inflate(layoutInflater)
        showContent(binding.root)
        binding.toolbar.title = getString(R.string.account_toolbar)
        binding.toolbar.navigationIcon = null
        if (savedInstanceState == null) supportFragmentManager.beginTransaction()
            .replace(R.id.flowHost, NavHostFragment.create(R.navigation.account_navigation)).commit()
    }
    override fun onResume() {
        super.onResume()
        withBackendAccess { viewModel.checkSession(true) }
    }
}
