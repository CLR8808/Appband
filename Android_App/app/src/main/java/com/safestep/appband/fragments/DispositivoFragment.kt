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

    override fun onResume() {
        super.onResume()
        viewModel.recargarDatos()
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

        // Observar datos del sensor desde el último registro en Firestore en tiempo real (se actualiza automáticamente cada 30s)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.datosSensor.collectLatest { sensor ->
                if (sensor.hayDatos) {
                    // BPM: mostrar lectura exacta del último registro (0 BPM si el sensor marca 0)
                    binding.tvHeartRate.text = if (sensor.bpm > 0) "${sensor.bpm} BPM" else "0 BPM"

                    // SpO2: mostrar oxigenación exacta del último registro
                    binding.tvOxygen.text = if (sensor.spo2 > 0) "${sensor.spo2} %" else "-- %"

                    // Batería real leída desde el registro más reciente en Firestore
                    binding.tvBatteryLevel.text = "${sensor.bateriaPorcentaje} %"

                    // Hora exacta del registro capturado desde Firestore
                    val horaMostrar = if (sensor.horaTexto.isNotBlank()) {
                        sensor.horaTexto
                    } else {
                        java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(sensor.ultimaActualizacion))
                    }

                    binding.tvLastUpdateTime.text = "Sensor activo ($horaMostrar)"
                    binding.tvLastSync.text = "En línea ($horaMostrar)"

                    // Evaluar si la pulsera sigue activa o han pasado más de 2 minutos sin un registro nuevo
                    val tiempoTranscurrido = System.currentTimeMillis() - sensor.ultimaActualizacion
                    val estaActiva = tiempoTranscurrido < 120_000 // 2 minutos

                    if (estaActiva) {
                        binding.tvConnectionStatus.text = "Sincronizada (Wi-Fi)"
                        binding.tvConnectionStatus.setTextColor(resources.getColor(R.color.success_green, null))
                        binding.tvConnectionType.text = "Sincronizada"
                        binding.tvConnectionType.setTextColor(resources.getColor(R.color.text_primary, null))
                    } else {
                        binding.tvConnectionStatus.text = "Desconectada"
                        binding.tvConnectionStatus.setTextColor(resources.getColor(R.color.danger_red, null))
                        binding.tvConnectionType.text = "Desconectada"
                        binding.tvConnectionType.setTextColor(resources.getColor(R.color.text_secondary, null))
                    }
                } else {
                    binding.tvHeartRate.text = "-- BPM"
                    binding.tvOxygen.text = "-- %"
                    binding.tvBatteryLevel.text = "-- %"
                    binding.tvLastUpdateTime.text = "Esperando registros..."
                    binding.tvLastSync.text = "--"
                    binding.tvConnectionStatus.text = "Desconectada"
                    binding.tvConnectionStatus.setTextColor(resources.getColor(R.color.text_secondary, null))
                }
            }
        }

        // Observar Promedios Diarios calculados en tiempo real desde Firestore
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.promediosDiarios.collectLatest { promedios ->
                if (promedios.promedioBpm > 0) {
                    binding.tvHeartRateAverage.text = "${promedios.promedioBpm} BPM"
                }
                if (promedios.promedioSpo2 > 0) {
                    binding.tvOxygenAverage.text = "${promedios.promedioSpo2} %"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
