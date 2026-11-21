package com.example.hellodoc.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.hellodoc.databinding.FragmentAppointmentsBinding
import com.example.hellodoc.model.Appointment
import com.example.hellodoc.viewmodel.*

class AppointmentsFragment : Fragment() {
    private var _binding: FragmentAppointmentsBinding? = null
    private val vm: AppointmentsViewModel by viewModels { HelloDocViewModelFactory(requireActivity().application) }
    private var pendingRescheduleId: String? = null
    private var history = false
    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        _binding = FragmentAppointmentsBinding.inflate(inflater, parent, false)
        return requireNotNull(_binding).root
    }
    override fun onViewCreated(view: View, state: Bundle?) {
        val b = requireNotNull(_binding)
        val adapter = AppointmentAdapter({ a -> beginReschedule(a) }, { a -> confirmCancellation(a) })
        b.rvAppointments.layoutManager = LinearLayoutManager(requireContext())
        b.rvAppointments.adapter = adapter
        b.btnUpcoming.setOnClickListener { history = false; selectTab(); vm.load(false) }
        b.btnHistory.setOnClickListener { history = true; selectTab(); vm.load(true) }
        vm.appointments.observe(viewLifecycleOwner) { value ->
            b.progress.isVisible = value.loading
            if (!value.loading) {
                adapter.submit(value.data.orEmpty())
                b.tvEmpty.text = value.error ?: if (value.data.isNullOrEmpty())
                    "Aucun rendez-vous ${if (history) "dans l'historique" else "à venir"}." else ""
                b.tvEmpty.isVisible = b.tvEmpty.text.isNotEmpty()
            }
        }
        vm.options.observe(viewLifecycleOwner) { event -> event.consume()?.let { options ->
            if (options.isEmpty()) {
                requireContext().toast("Aucun créneau disponible à cette date.")
            } else {
                val choices = options.map { formatInstant(it.startsAt) }.toTypedArray()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Choisir le nouveau créneau")
                    .setItems(choices) { _, index -> pendingRescheduleId?.let { id ->
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Confirmer la modification ?")
                            .setMessage("Nouveau rendez-vous : ${choices[index]}")
                            .setNegativeButton("Retour", null)
                            .setPositiveButton("Confirmer") { _, _ -> vm.reschedule(id, options[index].startsAt) }
                            .show()
                    } }.setNegativeButton("Annuler", null).show()
            }
        } }
        vm.message.observe(viewLifecycleOwner) { event -> event.consume()?.let { requireContext().toast(it) } }
        vm.error.observe(viewLifecycleOwner) { error -> error?.let { requireContext().toast(it) } }
        vm.busy.observe(viewLifecycleOwner) { b.progress.isVisible = it || vm.appointments.value?.loading == true }
        selectTab()
        vm.load(history)
    }
    private fun selectTab() {
        val b = _binding ?: return
        b.btnUpcoming.isEnabled = history
        b.btnHistory.isEnabled = !history
    }
    private fun beginReschedule(a: Appointment) {
        pendingRescheduleId = a.id
        showDatePicker(requireContext()) { date -> vm.rescheduleOptions(a.id, date) }
    }
    private fun confirmCancellation(a: Appointment) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Annuler le rendez-vous ?")
            .setMessage("Dr ${a.doctorName.orEmpty()} — ${formatInstant(a.startsAt)}")
            .setNegativeButton("Garder", null)
            .setPositiveButton("Annuler le RDV") { _, _ -> vm.cancel(a.id) }
            .show()
    }
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
