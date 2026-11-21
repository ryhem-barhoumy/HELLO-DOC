package tn.rnu.isetr.hellodoc.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputFilter
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tn.rnu.isetr.hellodoc.R
import tn.rnu.isetr.hellodoc.databinding.FragmentRegisterBinding
import tn.rnu.isetr.hellodoc.model.*
import tn.rnu.isetr.hellodoc.viewmodel.AuthViewModel
import java.io.ByteArrayOutputStream

class RegisterFragment : Fragment(R.layout.fragment_register) {
    private val viewModel: AuthViewModel by activityViewModels { (requireActivity().application as tn.rnu.isetr.hellodoc.HelloDocApplication).authViewModelFactory }
    private var binding: FragmentRegisterBinding? = null
    private var documentUri: Uri? = null
    private var documentName = "Aucun justificatif sélectionné"
    private var reading = false
    private val chooseDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { requireContext().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            documentUri = uri
            documentName = requireContext().contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else "Justificatif sélectionné"
            } ?: "Justificatif sélectionné"
            binding?.tvDocument?.text = documentName
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentRegisterBinding.bind(view)
        binding = b
        // A numeric keyboard alone does not validate paste, autofill or programmatic text changes.
        b.etPhone.filters = arrayOf(InputFilter { source, start, end, _, _, _ ->
            val text = source.subSequence(start, end)
            val digits = text.filter { it in '0'..'9' }
            if (digits.length == text.length) null else digits
        }, InputFilter.LengthFilter(15))
        documentUri = savedInstanceState?.getString("documentUri")?.let(Uri::parse)
        documentName = savedInstanceState?.getString("documentName") ?: documentName
        if (savedInstanceState == null && arguments?.getBoolean("doctor") == true) b.rbDoctor.isChecked = true
        b.layoutDoctor.isVisible = b.rbDoctor.isChecked
        b.tvDocument.text = documentName
        b.etPassword.isSaveEnabled = false
        b.etConfirmPassword.isSaveEnabled = false
        viewModel.clearMessage()
        fun doctor() = b.rbDoctor.isChecked
        fun updateEnabled() {
            val busy = viewModel.busy.value == true || reading
            b.btnRegister.isEnabled = !busy && (!doctor() || (!viewModel.specialties.value.isNullOrEmpty() && viewModel.specialtiesLoading.value != true))
            b.btnRegister.text = if (busy) "Envoi en cours…" else "Créer mon compte"
            b.progress.isVisible = busy
            b.btnDocument.isEnabled = !busy
            b.tvLogin.isEnabled = !busy
            b.rbDoctor.isEnabled = !busy
            b.rbPatient.isEnabled = !busy
            fields(b).values.forEach { it.isEnabled = !busy }
            b.spSpecialty.isEnabled = !busy
            b.cbConsent.isEnabled = !busy
        }
        b.rgRole.setOnCheckedChangeListener { _, _ ->
            b.layoutDoctor.isVisible = doctor()
            if (doctor() && viewModel.specialties.value.isNullOrEmpty()) loadSpecialties()
            updateEnabled()
        }
        b.btnDocument.setOnClickListener { chooseDocument.launch(arrayOf("application/pdf", "image/jpeg", "image/png")) }
        b.btnRetrySpecialties.setOnClickListener { loadSpecialties() }
        b.tvLogin.setOnClickListener {
            if (viewModel.busy.value != true && !reading) navigateToLogin()
        }
        b.btnRegister.setOnClickListener {
            if (reading || viewModel.busy.value == true) return@setOnClickListener
            val specialty = b.spSpecialty.selectedItem as? Specialty
            val request = RegistrationRequest(
                b.etFirstName.text.toString().trim(), b.etLastName.text.toString().trim(),
                b.etEmail.text.toString().trim(), b.etPhone.text.toString(),
                b.etAddress.text.toString().trim(), b.etPassword.text.toString(), b.cbConsent.isChecked,
                if (doctor()) specialty?.name ?: "" else null,
                if (doctor()) b.etLicenseNumber.text.toString().trim() else null,
                if (doctor()) b.etCity.text.toString().trim() else null
            )
            val errors = RegistrationValidation.errors(request, b.etConfirmPassword.text.toString(), doctor()).toMutableMap()
            if (doctor() && documentUri == null) errors["document"] = "Joignez votre attestation d’inscription à l’Ordre."
            fields(b).forEach { (key, field) -> field.error = errors[key] }
            b.cbConsent.error = errors["consent"]
            if (errors.isNotEmpty()) {
                b.tvError.text = errors.values.joinToString("\n")
                b.tvError.isVisible = true
                fields(b).entries.firstOrNull { errors.containsKey(it.key) }?.value?.requestFocus()
                return@setOnClickListener
            }
            if (!doctor()) {
                (requireActivity() as tn.rnu.isetr.hellodoc.ui.HelloDocActivity).withBackendAccess { viewModel.register(request, null, null) }
            } else {
                reading = true
                updateEnabled()
                val resolver = requireContext().contentResolver
                val uri = documentUri!!
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val (bytes, mime) = withContext(Dispatchers.IO) {
                            val mime = resolver.getType(uri)
                            require(mime in setOf("application/pdf", "image/jpeg", "image/png")) { "Choisissez un fichier PDF, JPEG ou PNG." }
                            val bytes = resolver.openInputStream(uri)?.use { input ->
                                val output = ByteArrayOutputStream()
                                val buffer = ByteArray(8192)
                                while (true) {
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    require(output.size() + count <= 5 * 1024 * 1024) { "Le justificatif doit faire 5 Mo maximum." }
                                    output.write(buffer, 0, count)
                                }
                                output.toByteArray()
                            } ?: error("Impossible de lire le justificatif. Sélectionnez-le à nouveau.")
                            require(bytes.isNotEmpty()) { "Le justificatif est vide." }
                            bytes to mime
                        }
                        (requireActivity() as tn.rnu.isetr.hellodoc.ui.HelloDocActivity).withBackendAccess { viewModel.register(request, bytes, mime) }
                    } catch (error: CancellationException) { throw error }
                    catch (error: Exception) {
                        b.tvError.text = error.message ?: "Impossible de lire le justificatif. Sélectionnez-le à nouveau."
                        b.tvError.isVisible = true
                    } finally { reading = false; updateEnabled() }
                }
            }
        }
        viewModel.busy.observe(viewLifecycleOwner) { updateEnabled() }
        viewModel.message.observe(viewLifecycleOwner) { b.tvError.text = it; b.tvError.isVisible = !it.isNullOrBlank() }
        viewModel.fieldErrors.observe(viewLifecycleOwner) { errors ->
            fields(b).forEach { (key, field) -> if (errors.containsKey(key)) field.error = errors[key] }
        }
        var restoredSpecialty = savedInstanceState?.getString("specialty")
        viewModel.specialties.observe(viewLifecycleOwner) { specialties ->
            val selectedName = (b.spSpecialty.selectedItem as? Specialty)?.name ?: restoredSpecialty
            val items: List<Any> = listOf("Choisir une spécialité") + specialties
            b.spSpecialty.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, items)
            b.spSpecialty.setSelection((items.indexOfFirst { it is Specialty && it.name == selectedName }).coerceAtLeast(0))
            restoredSpecialty = null
            updateEnabled()
        }
        viewModel.specialtiesLoading.observe(viewLifecycleOwner) {
            b.tvSpecialtiesStatus.text = if (it) "Chargement des spécialités…" else viewModel.specialtiesError.value.orEmpty()
            updateEnabled()
        }
        viewModel.specialtiesError.observe(viewLifecycleOwner) {
            b.tvSpecialtiesStatus.text = it.orEmpty()
            b.btnRetrySpecialties.isVisible = it != null
        }
        if (doctor()) loadSpecialties()
        viewModel.registered.observe(viewLifecycleOwner) { event ->
            event.consume()?.let { isDoctor ->
                b.etPassword.text?.clear()
                b.etConfirmPassword.text?.clear()
                val notice = if (isDoctor) "Demande d’inscription envoyée. Votre compte médecin est en attente de validation par l’administrateur. Les services et horaires seront renseignés après acceptation."
                    else "Compte patient créé. Vous pouvez maintenant vous connecter."
                navigateToLogin(notice)
            }
        }
    }
    private fun loadSpecialties() {
        (requireActivity() as tn.rnu.isetr.hellodoc.ui.HelloDocActivity).withBackendAccess { viewModel.loadSpecialties() }
    }
    private fun navigateToLogin(notice: String? = null) {
        startActivity(Intent(requireContext(), LoginActivity::class.java)
            .putExtra("notice", notice).putExtra("email", binding?.etEmail?.text?.toString()?.trim())
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        requireActivity().finish()
    }
    private fun fields(b: FragmentRegisterBinding): Map<String, EditText> = mapOf(
        "firstName" to b.etFirstName, "lastName" to b.etLastName, "email" to b.etEmail,
        "phone" to b.etPhone, "address" to b.etAddress, "password" to b.etPassword,
        "confirmation" to b.etConfirmPassword, "city" to b.etCity, "licenseNumber" to b.etLicenseNumber
    )
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("documentUri", documentUri?.toString())
        outState.putString("documentName", documentName)
        outState.putString("specialty", (binding?.spSpecialty?.selectedItem as? Specialty)?.name)
    }
    override fun onDestroyView() { binding = null; super.onDestroyView() }
}
