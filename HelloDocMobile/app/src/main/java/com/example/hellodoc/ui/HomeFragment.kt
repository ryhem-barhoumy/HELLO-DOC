package com.example.hellodoc.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.hellodoc.R
import com.example.hellodoc.databinding.FragmentHomeBinding

/** Page d'accueil du patient : interface XML simple, comme les Fragments du TP5. */
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, parent, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Le FAB de recherche remplace les deux anciens boutons de l'accueil.
        binding.fabSearchDoctor.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_doctors)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
