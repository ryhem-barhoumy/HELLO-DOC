package tn.rnu.isetr.hellodoc.ui.account

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import tn.rnu.isetr.hellodoc.HelloDocApplication
import tn.rnu.isetr.hellodoc.R
import tn.rnu.isetr.hellodoc.databinding.FragmentAccountBinding
import tn.rnu.isetr.hellodoc.ui.HelloDocActivity
import tn.rnu.isetr.hellodoc.ui.home.HomeActivity
import tn.rnu.isetr.hellodoc.viewmodel.AuthViewModel

class AccountFragment : Fragment(R.layout.fragment_account) {
    private val viewModel: AuthViewModel by activityViewModels { (requireActivity().application as HelloDocApplication).authViewModelFactory }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentAccountBinding.bind(view)
        var leaving = false
        fun home() {
            if (leaving) return
            leaving = true
            startActivity(Intent(requireContext(), HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
        b.btnLogout.setOnClickListener { (requireActivity() as HelloDocActivity).withBackendAccess { viewModel.logout() } }
        b.btnRetry.setOnClickListener { (requireActivity() as HelloDocActivity).withBackendAccess { viewModel.checkSession(true) } }
        viewModel.sessionChecked.observe(viewLifecycleOwner) { if (it && viewModel.user.value == null) home() }
        viewModel.user.observe(viewLifecycleOwner) { user ->
            b.layoutAccount.isVisible = user != null
            if (user != null) {
                b.tvAccountName.text = user.displayName
                b.tvAccountEmail.text = user.email
                b.tvAccountRole.text = user.spaceLabel
                b.tvInitials.text = "${user.firstName.take(1)}${user.lastName.take(1)}".uppercase()
                b.tvAccountStatus.text = if (user.status == "ACTIVE") getString(R.string.account_active) else user.status
                b.tvScope.text = getString(if (user.role == "DOCTOR") R.string.account_doctor_help else if (user.role == "ADMIN") R.string.account_admin_help else R.string.account_patient_help)
            } else if (viewModel.sessionChecked.value == true) home()
        }
        viewModel.busy.observe(viewLifecycleOwner) {
            b.progress.isVisible = it
            b.btnLogout.isEnabled = !it
            b.btnRetry.isEnabled = !it
        }
        viewModel.message.observe(viewLifecycleOwner) {
            b.tvError.text = it
            b.tvError.isVisible = !it.isNullOrBlank()
            b.btnRetry.isVisible = !it.isNullOrBlank()
        }
    }
}
