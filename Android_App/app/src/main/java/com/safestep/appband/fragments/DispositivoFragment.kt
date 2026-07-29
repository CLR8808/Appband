package com.safestep.appband.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.safestep.appband.R
import com.safestep.appband.databinding.FragmentDispositivoBinding
import com.safestep.appband.viewmodels.DispositivoViewModel

/**
 * Fragment de Detalle de SafeBand.
 * Migrado desde components/dispositivo/dispositivo.component.ts
 */
class DispositivoFragment : Fragment() {

    private var _binding: FragmentDispositivoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DispositivoViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDispositivoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnEventos.setOnClickListener {
            findNavController().navigate(R.id.action_dispositivo_to_eventos)
        }

        binding.btnConfiguracion.setOnClickListener {
            findNavController().navigate(R.id.action_dispositivo_to_configuracion)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
