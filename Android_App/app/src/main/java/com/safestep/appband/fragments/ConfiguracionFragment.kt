package com.safestep.appband.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.safestep.appband.R
import com.safestep.appband.databinding.FragmentConfiguracionBinding
import com.safestep.appband.viewmodels.ConfiguracionViewModel
import kotlinx.coroutines.launch

/**
 * Fragment de Configuración de SafeBand.
 * Migrado desde components/configuracion/configuracion.component.ts
 */
class ConfiguracionFragment : Fragment() {

    private var _binding: FragmentConfiguracionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConfiguracionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfiguracionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.cardDesvincular.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Desvincular SafeBand")
                .setMessage("¿Seguro que quieres desvincular la SafeBand?")
                .setPositiveButton("Desvincular") { _, _ ->
                    viewModel.desvincular()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        observarEstado()
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.dispositivos.collect { lista ->
                        if (lista.isNotEmpty()) {
                            binding.tvDeviceNameValue.text = "${lista[0].nombre} >"
                            binding.tvStatusValue.text = "Activo y conectado"
                        } else {
                            binding.tvDeviceNameValue.text = "Sin dispositivo >"
                            binding.tvStatusValue.text = "Sin dispositivo"
                        }
                    }
                }

                launch {
                    viewModel.desvincularSuccessEvent.collect {
                        findNavController().navigate(R.id.inicioFragment)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
