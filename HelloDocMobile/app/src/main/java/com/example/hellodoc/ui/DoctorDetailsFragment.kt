package com.example.hellodoc.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.hellodoc.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.hellodoc.databinding.FragmentDoctorDetailsBinding
import com.example.hellodoc.model.*
import com.example.hellodoc.viewmodel.*

class DoctorDetailsFragment : Fragment() {
    private val vm: DoctorsViewModel by viewModels { HelloDocViewModelFactory(requireActivity().application) }
    private val appointmentVm: AppointmentsViewModel by viewModels { HelloDocViewModelFactory(requireActivity().application) }
    private var _binding: FragmentDoctorDetailsBinding? = null
    private lateinit var doctorId: String
    private var services = emptyList<MedicalService>()
    private var slots = emptyList<TimeSlot>()
    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        _binding = FragmentDoctorDetailsBinding.inflate(inflater, parent, false)
        return requireNotNull(_binding).root
    }
    override fun onViewCreated(view: View, state: Bundle?) {
        val b = requireNotNull(_binding)
        doctorId = requireArguments().getString("doctorId") ?: return
        vm.detail.observe(viewLifecycleOwner) { value ->
            b.progress.isVisible = value.loading
            b.tvError.isVisible = !value.error.isNullOrBlank()
            b.tvError.text = value.error
            value.data?.let { doctor ->
                b.tvDoctorName.text = "Dr ${doctor.fullName.orEmpty()}"
                b.tvSpecialty.text = doctor.specialties?.joinToString(" • ") ?: doctor.specialty.orEmpty()
                b.tvLocation.text = listOfNotNull(doctor.city, doctor.location, doctor.address)
                    .filter { it.isNotBlank() }.distinct().joinToString(" · ")
                b.tvDescription.text = doctor.description?.ifBlank { "Consultations sur rendez-vous." }
                    ?: "Consultations sur rendez-vous."
                services = doctor.services.orEmpty().filter { it.name.isNotBlank() }
                b.spinnerServices.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item,
                    services.map { it.toString() }.ifEmpty { listOf("Aucun service disponible") })
                b.btnBook.isEnabled = slots.isNotEmpty() && services.any { !it.price.isNullOrBlank() }
            }
        }
        vm.slots.observe(viewLifecycleOwner) { value ->
            b.slotsProgress.isVisible = value.loading
            slots = value.data.orEmpty()
            b.tvSlotsNotice.text = value.error ?: if (slots.isEmpty() && !value.loading)
                "Aucun créneau libre à cette date." else "Choisissez un horaire disponible."
            b.spinnerSlots.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item,
                slots.map { formatClock(it.startsAt) }.ifEmpty { listOf("Aucun créneau") })
            b.btnBook.isEnabled = slots.isNotEmpty() && services.any { !it.price.isNullOrBlank() }
        }
        b.btnChooseDate.setOnClickListener { showDatePicker(requireContext()) { date ->
            b.tvDate.text = date
            vm.loadSlots(doctorId, date)
        } }
        b.btnBook.setOnClickListener {
            val svc = services.getOrNull(b.spinnerServices.selectedItemPosition) ?: run {
                requireContext().toast("Aucun service sélectionné."); return@setOnClickListener
            }
            val slot = slots.getOrNull(b.spinnerSlots.selectedItemPosition) ?: run {
                requireContext().toast("Choisissez un créneau libre."); return@setOnClickListener
            }
            if (svc.price.isNullOrBlank()) {
                requireContext().toast("Ce service ne possède pas de tarif."); return@setOnClickListener
            }
            val request = AppointmentCreateRequest(doctorId, svc.name, slot.startsAt,
                b.etReason.text?.toString()?.trim().orEmpty().takeIf { it.isNotBlank() }, svc.price)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirmer la demande ?")
                .setMessage("${svc.name} • ${formatInstant(slot.startsAt)}\nLe médecin validera ensuite votre demande.")
                .setNegativeButton("Retour", null)
                .setPositiveButton("Confirmer") { _, _ -> appointmentVm.book(request) }
                .show()
        }
        appointmentVm.busy.observe(viewLifecycleOwner) { b.btnBook.isEnabled = !it && slots.isNotEmpty() && services.any { s -> !s.price.isNullOrBlank() } }
        appointmentVm.error.observe(viewLifecycleOwner) { it?.let { message -> b.tvError.text = message; b.tvError.isVisible = true } }
        appointmentVm.booked.observe(viewLifecycleOwner) { event -> event.consume()?.let {
            requireContext().toast("Demande envoyée. Le médecin pourra la confirmer sur le site web.")
            findNavController().navigate(R.id.appointmentsFragment)
        } }
        b.tvDate.text = vm.selectedDate
        vm.loadDetail(doctorId)
        vm.loadSlots(doctorId, vm.selectedDate)
    }
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
