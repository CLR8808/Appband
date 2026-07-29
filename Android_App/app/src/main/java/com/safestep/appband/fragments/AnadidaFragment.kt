package com.safestep.appband.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.safestep.appband.R
import com.safestep.appband.databinding.FragmentAnadidaBinding
import com.safestep.appband.viewmodels.AnadidaViewModel

/**
 * Fragment de Éxito al Vincular SafeBand.
 * Migrado desde components/anadida/anadida.component.ts
 */
class AnadidaFragment : Fragment() {

    private var _binding: FragmentAnadidaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnadidaViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnadidaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnVerDispositivos.setOnClickListener {
            findNavController().navigate(R.id.action_anadida_to_inicio)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
