package tn.rnu.isetr.hellodoc.ui

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import tn.rnu.isetr.hellodoc.HelloDocApplication
import tn.rnu.isetr.hellodoc.network.LocalBackendAccess
import tn.rnu.isetr.hellodoc.viewmodel.AuthViewModel

/** Shared window and debug network permission handling, with no business logic in views. */
abstract class HelloDocActivity : AppCompatActivity() {
    val viewModel: AuthViewModel by viewModels { (application as HelloDocApplication).authViewModelFactory }
    private var afterPermission: (() -> Unit)? = null
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.setNetworkAccess(granted)
        if (granted) afterPermission?.invoke()
        else viewModel.showMessage("Autorisez le réseau local pour joindre HelloDoc. Vous pourrez réessayer avec le bouton de connexion.")
        afterPermission = null
    }
    protected fun showContent(view: View) {
        setContentView(view)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        ViewCompat.setOnApplyWindowInsetsListener(view) { root, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            root.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }
    fun withBackendAccess(action: () -> Unit) {
        if (LocalBackendAccess.isAllowed(this)) {
            viewModel.setNetworkAccess(true)
            action()
        } else {
            afterPermission = action
            permission.launch(LocalBackendAccess.PERMISSION)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setNetworkAccess(LocalBackendAccess.isAllowed(this))
    }
    override fun onResume() {
        super.onResume()
        viewModel.setNetworkAccess(LocalBackendAccess.isAllowed(this))
    }
}
