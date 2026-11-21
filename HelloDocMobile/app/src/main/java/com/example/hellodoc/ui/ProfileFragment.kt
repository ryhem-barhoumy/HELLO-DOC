package com.example.hellodoc.ui

import android.content.Intent
import android.os.Bundle
import android.graphics.BitmapFactory
import androidx.activity.result.contract.ActivityResultContracts
import com.example.hellodoc.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.hellodoc.databinding.FragmentProfileBinding
import com.example.hellodoc.databinding.DialogChangePasswordBinding
import androidx.appcompat.app.AlertDialog
import com.example.hellodoc.model.PatientUpdateRequest
import com.example.hellodoc.viewmodel.*

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val type = requireContext().contentResolver.getType(uri) ?: ""
            val bytes = requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (output.size() <= 5 * 1024 * 1024) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
            if (bytes == null || bytes.size > 5 * 1024 * 1024 || type !in setOf("image/jpeg", "image/png"))
                requireContext().toast("Choisissez un JPG/PNG inférieur à 5 Mo.")
            else vm.uploadPhoto(bytes, type)
        }
    }
    private var passwordDialog: AlertDialog? = null
    private var passwordDialogBinding: DialogChangePasswordBinding? = null
    private val vm: ProfileViewModel by viewModels { HelloDocViewModelFactory(requireActivity().application) }
    private val authVm: AuthViewModel by viewModels { HelloDocViewModelFactory(requireActivity().application) }
    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, parent, false)
        return requireNotNull(_binding).root
    }
    override fun onViewCreated(view: View, state: Bundle?) {
        val b = requireNotNull(_binding)
        vm.photo.observe(viewLifecycleOwner) { imageBytes ->
            val bNow = _binding ?: return@observe
            val bitmap = imageBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            if (bitmap == null) bNow.imgPhoto.setImageResource(R.drawable.medical_symbol)
            else bNow.imgPhoto.setImageBitmap(bitmap)
        }
        vm.photoMessage.observe(viewLifecycleOwner) { event -> event.consume()?.let { requireContext().toast(it) } }
        b.btnChangePhoto.setOnClickListener { photoPicker.launch("image/*") }
        b.btnDeletePhoto.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Supprimer la photo ?")
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Supprimer") { _, _ -> vm.deletePhoto() }
                .show()
        }
        vm.profile.observe(viewLifecycleOwner) { value ->
            b.progress.isVisible = value.loading
            b.btnSave.isEnabled = !value.loading
            b.tvError.isVisible = !value.error.isNullOrBlank()
            b.tvError.text = value.error
            value.data?.let { user ->
                b.tvName.text = user.fullName ?: "${user.firstName} ${user.lastName}"
                if (!b.etFirstName.hasFocus()) b.etFirstName.setText(user.firstName)
                if (!b.etLastName.hasFocus()) b.etLastName.setText(user.lastName)
                b.etEmail.setText(user.email)
                if (!b.etPhone.hasFocus()) b.etPhone.setText(user.phone.orEmpty())
                if (!b.etAddress.hasFocus()) b.etAddress.setText(user.address.orEmpty())
            }
        }
        b.btnSave.setOnClickListener {
            val first = b.etFirstName.text.toString().trim()
            val last = b.etLastName.text.toString().trim()
            val phone = b.etPhone.text.toString().trim()
            val address = b.etAddress.text.toString().trim()
            val pass = b.etPassword.text.toString()
            if (first.isBlank() || last.isBlank() || pass.isBlank() ||
                (phone.isNotBlank() && !Regex("[0-9]{8,15}").matches(phone))) {
                requireContext().toast("Vérifiez les champs et saisissez votre mot de passe actuel.")
            } else vm.save(PatientUpdateRequest(first, last, phone, address, pass))
        }
        vm.saved.observe(viewLifecycleOwner) { event -> event.consume()?.let {
            b.etPassword.text?.clear()
            requireContext().toast("Profil mis à jour.")
        } }
        b.btnChangePassword.setOnClickListener { showChangePasswordDialog() }
        authVm.passwordChanged.observe(viewLifecycleOwner) { event ->
            event.consume()?.let {
                passwordDialog?.dismiss()
                requireContext().toast(getString(R.string.password_changed))
            }
        }
        authVm.busy.observe(viewLifecycleOwner) { busy ->
            passwordDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = !busy
        }
        b.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Se déconnecter ?")
                .setMessage("Vous devrez vous authentifier pour accéder à votre compte.")
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Déconnexion") { _, _ -> authVm.logout() }
                .show()
        }
        authVm.logoutDone.observe(viewLifecycleOwner) { event -> event.consume()?.let {
            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } }
        authVm.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                val dialogBinding = passwordDialogBinding
                if (dialogBinding != null) {
                    dialogBinding.tvPasswordError.text = error
                    dialogBinding.tvPasswordError.isVisible = true
                } else requireContext().toast(error)
            }
        }
        vm.load()
        vm.loadPhoto()
    }

    private fun showChangePasswordDialog() {
        if (passwordDialog != null) return
        val form = DialogChangePasswordBinding.inflate(layoutInflater)
        passwordDialogBinding = form
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.password_change_title)
            .setView(form.root)
            .setNegativeButton("Annuler", null)
            .setPositiveButton(R.string.password_save, null)
            .create()
        passwordDialog = dialog
        dialog.setOnDismissListener {
            passwordDialog = null
            passwordDialogBinding = null
        }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val current = form.etCurrentPassword.text?.toString().orEmpty()
                val newPassword = form.etNewPassword.text?.toString().orEmpty()
                val confirm = form.etConfirmPassword.text?.toString().orEmpty()
                when {
                    current.isBlank() || newPassword.length !in 8..72 || newPassword != confirm -> {
                        form.tvPasswordError.text = getString(R.string.password_check)
                        form.tvPasswordError.isVisible = true
                    }
                    newPassword == current -> {
                        form.tvPasswordError.text = "Choisissez un mot de passe différent de l'ancien."
                        form.tvPasswordError.isVisible = true
                    }
                    else -> {
                        form.tvPasswordError.isVisible = false
                        authVm.changePassword(current, newPassword)
                    }
                }
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        passwordDialog?.dismiss()
        _binding = null
        super.onDestroyView()
    }
}
