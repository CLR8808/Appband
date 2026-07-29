package com.safestep.appband.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.safestep.appband.R
import com.safestep.appband.databinding.FragmentPrepararDispositivoBinding
import com.safestep.appband.viewmodels.PrepararDispositivoViewModel

/**
 * Fragment de Preparar Dispositivo.
 * Migrado desde components/preparar-dispositivo/preparar-dispositivo.component.ts
 */
class PrepararDispositivoFragment : Fragment() {

    private var _binding: FragmentPrepararDispositivoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PrepararDispositivoViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrepararDispositivoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnNext.setOnClickListener {
            findNavController().navigate(R.id.action_prepararDispositivo_to_wifi)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
