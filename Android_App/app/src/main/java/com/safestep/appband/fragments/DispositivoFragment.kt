package com.safestep.appband.fragments

import android.app.AlertDialog
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
 * Fragment de Detalle e Información de SafeBand.
 * Muestra la telemetría en tiempo real (Pulso, Oxigenación, Batería, Conexión)
 * y permite desvincular el dispositivo de forma funcional.
 */
class DispositivoFragment : Fragment() {

    private var _binding: FragmentDispositivoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DispositivoViewModel by viewModels()
    private var nombreDispositivoActual: String = "Mi SafeBand V1"

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

        binding.btnVerMapa.setOnClickListener {
            findNavController().navigate(R.id.action_dispositivo_to_mapa)
        }

        binding.btnEditDeviceName.setOnClickListener {
            mostrarDialogoEditarNombre()
        }

        binding.btnDesvincular.setOnClickListener {
            confirmarDesvinculacion()
        }
    }

    private fun mostrarDialogoEditarNombre() {
        val input = android.widget.EditText(requireContext()).apply {
            setText(nombreDispositivoActual)
            setSelection(text.length)
            hint = "Nombre de la pulsera"
            setPadding(40, 30, 40, 30)
        }

        val container = android.widget.FrameLayout(requireContext()).apply {
            setPadding(40, 20, 40, 10)
            addView(input)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Editar nombre de SafeBand")
            .setMessage("Ingresa el nuevo nombre para tu pulsera:")
            .setView(container)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoNombre = input.text.toString().trim()
                if (nuevoNombre.isNotBlank()) {
                    nombreDispositivoActual = nuevoNombre
                    binding.tvDeviceName.text = nuevoNombre
                    viewModel.actualizarNombre(nuevoNombre)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarDesvinculacion() {
        AlertDialog.Builder(requireContext())
            .setTitle("Desvincular pulsera")
            .setMessage("¿Estás seguro de que deseas desvincular esta SafeBand? Esta acción removerá el dispositivo de tu cuenta.")
            .setPositiveButton("Desvincular") { _, _ ->
                viewModel.desvincularPulsera(nombreDispositivoActual) {
                    findNavController().popBackStack()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observeViewModel() {
        // Observar Dispositivo Activo desde DataStore
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dispositivoActivo.collectLatest { dev ->
                if (dev != null) {
                    nombreDispositivoActual = dev.nombre
                    binding.tvDeviceName.text = dev.nombre
                }
            }
        }

        // Observar datos del sensor desde Firestore (Se actualiza continuamente vía Wi-Fi o mantiene el último dato guardado)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.datosSensor.collectLatest { sensor ->
                if (sensor.hayDatos) {
                    // BPM: mostrar lectura continua o último valor guardado en Firestore
                    binding.tvHeartRate.text = if (sensor.bpm > 0) "${sensor.bpm} BPM" else "-- BPM"

                    // SpO2: mostrar lectura continua o último valor guardado en Firestore
                    binding.tvOxygen.text = if (sensor.spo2 > 0) "${sensor.spo2} %" else "-- %"

                    // Batería real leída desde Firestore
                    binding.tvBatteryLevel.text = if (sensor.bateriaPorcentaje > 0) "${sensor.bateriaPorcentaje} %" else "-- %"

                    // Estado de actualización y tiempo de última sincronización
                    val timeFormatted = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(sensor.ultimaActualizacion))

                    if (sensor.dedo) {
                        binding.tvLastUpdateTime.text = "Sensor activo ($timeFormatted)"
                        binding.tvLastSync.text = "En línea ($timeFormatted)"
                    } else {
                        binding.tvLastUpdateTime.text = "Último dato guardado ($timeFormatted)"
                        binding.tvLastSync.text = "Sin dedo detectado"
                    }

                    // Marcar como sincronizada por Wi-Fi / Firestore
                    binding.tvConnectionStatus.text = "Sincronizada (Wi-Fi)"
                    binding.tvConnectionStatus.setTextColor(resources.getColor(R.color.success_green, null))
                    binding.tvConnectionType.text = "Sincronizada"
                    binding.tvConnectionType.setTextColor(resources.getColor(R.color.text_primary, null))
                } else {
                    binding.tvHeartRate.text = "-- BPM"
                    binding.tvOxygen.text = "-- %"
                    binding.tvBatteryLevel.text = "-- %"
                    binding.tvLastUpdateTime.text = "Esperando base de datos..."
                    binding.tvLastSync.text = "--"
                }
            }
        }

        // Observar Promedios Diarios desde Base de Datos
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.promedioPulso.collectLatest { avg ->
                binding.tvHeartRateAverage.text = avg
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.promedioOxigenacion.collectLatest { avg ->
                binding.tvOxygenAverage.text = avg
            }
        }

        // Observar Estado de Conexión
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.estadoConexion.collectLatest { estado ->
                when (estado) {
                    is BluetoothRepository.EstadoConexion.Conectado -> {
                        binding.tvConnectionStatus.text = "Conectada"
                        binding.tvConnectionStatus.setTextColor(resources.getColor(R.color.success_green, null))
                        binding.tvConnectionType.text = "Conectada"
                        binding.tvConnectionType.setTextColor(resources.getColor(R.color.text_primary, null))
                    }
                    is BluetoothRepository.EstadoConexion.Conectando -> {
                        binding.tvConnectionStatus.text = "Conectando..."
                        binding.tvConnectionStatus.setTextColor(resources.getColor(R.color.primary_cyan, null))
                        binding.tvConnectionType.text = "Conectando"
                        binding.tvConnectionType.setTextColor(resources.getColor(R.color.primary_cyan, null))
                    }
                    is BluetoothRepository.EstadoConexion.Error -> {
                        binding.tvConnectionStatus.text = "Desconectada"
                        binding.tvConnectionStatus.setTextColor(resources.getColor(R.color.danger_red, null))
                        binding.tvConnectionType.text = "Desconectada"
                        binding.tvConnectionType.setTextColor(resources.getColor(R.color.text_secondary, null))
                    }
                    else -> {
                        binding.tvConnectionStatus.text = "Desconectada"
                        binding.tvConnectionStatus.setTextColor(resources.getColor(R.color.text_secondary, null))
                        binding.tvConnectionType.text = "Desconectada"
                        binding.tvConnectionType.setTextColor(resources.getColor(R.color.text_secondary, null))
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
