package com.safestep.appband.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.safestep.appband.R
import com.safestep.appband.databinding.FragmentDispositivoBinding
import com.safestep.appband.repositories.BluetoothRepository
import com.safestep.appband.viewmodels.DispositivoViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment de Detalle de SafeBand.
 * Muestra la telemetría en tiempo real del sensor DHT11.
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

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
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

    private fun observeViewModel() {
        // Observar Telemetría (Temperatura y Humedad)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.telemetria.collectLatest { telem ->
                if (telem.conectado) {
                    binding.tvMainTemp.text = telem.temperatura?.let { "$it °C" } ?: "-- °C"
                    binding.tvMainHum.text = telem.humedad?.let { "$it %" } ?: "-- %"
                } else {
                    binding.tvMainTemp.text = "-- °C"
                    binding.tvMainHum.text = "-- %"
                }
            }
        }

        // Observar Estado de Conexión
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.estadoConexion.collectLatest { estado ->
                when (estado) {
                    is BluetoothRepository.EstadoConexion.Conectado -> {
                        binding.tvConnectionStatus.text = "Conectado a ${estado.nombre}"
                        binding.tvConnectionStatus.setTextColor(resources.getColor(R.color.success_green, null))
                    }
                    is BluetoothRepository.EstadoConexion.Conectando -> {
                        binding.tvConnectionStatus.text = "Conectando..."
                        binding.tvConnectionStatus.setTextColor(resources.getColor(R.color.primary_cyan, null))
                    }
                    is BluetoothRepository.EstadoConexion.Error -> {
                        binding.tvConnectionStatus.text = "Error: ${estado.mensaje}"
                        binding.tvConnectionStatus.setTextColor(resources.getColor(R.color.danger_red, null))
                    }
                    else -> {
                        binding.tvConnectionStatus.text = "Desconectado"
                        binding.tvConnectionStatus.setTextColor(resources.getColor(R.color.text_secondary, null))
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
