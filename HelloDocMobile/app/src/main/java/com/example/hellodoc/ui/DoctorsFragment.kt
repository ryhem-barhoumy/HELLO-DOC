package com.example.hellodoc.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hellodoc.R
import com.example.hellodoc.databinding.FragmentDoctorsBinding
import com.example.hellodoc.viewmodel.*

class DoctorsFragment : Fragment() {
    private var _binding: FragmentDoctorsBinding? = null
    private val vm: DoctorsViewModel by viewModels { HelloDocViewModelFactory(requireActivity().application) }
    private val handler = Handler(Looper.getMainLooper())
    private var delayedSearch: Runnable? = null
    private var selectedDate: String? = null
    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        _binding = FragmentDoctorsBinding.inflate(inflater, parent, false)
        return requireNotNull(_binding).root
    }
    override fun onViewCreated(view: View, state: Bundle?) {
        val b = requireNotNull(_binding)
        val adapter = DoctorAdapter { doctor ->
            findNavController().navigate(R.id.action_doctorsFragment_to_doctorDetailsFragment,
                bundleOf("doctorId" to doctor.id))
        }
        b.rvDoctors.layoutManager = LinearLayoutManager(requireContext())
        b.rvDoctors.adapter = adapter
        b.spinnerSpecialty.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item,
            listOf("Toutes les spécialités"))
        b.spinnerSpecialty.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, position: Int, id: Long) = scheduleSearch()
        }
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = scheduleSearch()
            override fun afterTextChanged(s: Editable?) {}
        }
        b.etQuery.addTextChangedListener(watcher)
        b.etCity.addTextChangedListener(watcher)
        b.btnDate.setOnClickListener { showDatePicker(requireContext()) {
            selectedDate = it
            b.btnDate.text = "Disponibles : $it"
            scheduleSearch()
        } }
        b.btnClear.setOnClickListener {
            selectedDate = null; b.etQuery.text?.clear(); b.etCity.text?.clear()
            b.spinnerSpecialty.setSelection(0); b.btnDate.text = "Choisir une date"
            scheduleSearch()
        }
        vm.specialties.observe(viewLifecycleOwner) { specialties ->
            val names = listOf("Toutes les spécialités") + specialties.map { it.name }
            b.spinnerSpecialty.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
        }
        vm.doctors.observe(viewLifecycleOwner) { stateValue ->
            b.progress.isVisible = stateValue.loading
            if (!stateValue.loading) {
                adapter.submit(stateValue.data.orEmpty())
                b.tvCount.text = if (stateValue.error != null) "" else "${stateValue.data.orEmpty().size} résultat(s)"
                b.tvMessage.text = stateValue.error ?: if (stateValue.data.isNullOrEmpty())
                    "Aucun médecin trouvé. Modifiez les filtres." else ""
                b.tvMessage.isVisible = b.tvMessage.text.isNotEmpty()
            }
        }
        vm.loadSpecialties()
        scheduleSearch()
    }
    private fun scheduleSearch() {
        val b = _binding ?: return
        delayedSearch?.let { handler.removeCallbacks(it) }
        delayedSearch = Runnable {
            vm.search(b.etQuery.text.toString(),
                b.spinnerSpecialty.selectedItem?.toString()?.takeUnless { it == "Toutes les spécialités" },
                b.etCity.text.toString(), selectedDate)
        }.also { handler.postDelayed(it, 350) }
    }
    override fun onDestroyView() {
        delayedSearch?.let { handler.removeCallbacks(it) }
        _binding = null
        super.onDestroyView()
    }
}
