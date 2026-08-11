package com.safestep.appband.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.safestep.appband.R
import com.safestep.appband.databinding.FragmentDetalleIncidenteBinding
import com.safestep.appband.models.Evento
import com.safestep.appband.repositories.EventosRepository
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker

/**
 * Fragment de Detalle de Incidente.
 * Muestra la información completa del incidente desde la base de datos con mini mapa embebido.
 */
class DetalleIncidenteFragment : Fragment() {

    private var _binding: FragmentDetalleIncidenteBinding? = null
    private val binding get() = _binding!!

    private val repository = EventosRepository()
    private var selectedEvento: Evento? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx: Context = requireActivity().applicationContext
        val prefs = ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        Configuration.getInstance().load(ctx, prefs)
        Configuration.getInstance().userAgentValue = requireActivity().packageName

        selectedEvento = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("evento", Evento::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable("evento") as? Evento
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleIncidenteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        loadIncidentData()
        setupMiniMap()
    }

    private fun loadIncidentData() {
        val evento = selectedEvento ?: return

        val isCritical = evento.tipo.equals("CRITICO", ignoreCase = true)

        // Tarjeta de Prioridad
        binding.priorityCard.setBackgroundResource(
            if (isCritical) R.drawable.bg_incident_critical
            else R.drawable.bg_incident_warning
        )

        // Etiqueta de Prioridad
        binding.tvPriorityLabel.text = if (isCritical) "Prioridad alta" else "Advertencia"
        binding.tvPriorityLabel.setTextColor(
            android.graphics.Color.parseColor(if (isCritical) "#DC2626" else "#D97706")
        )

        // Título y Descripción
        binding.tvIncidentTitle.text = evento.titulo.ifEmpty { "Incidente detectado" }
        binding.tvIncidentDescription.text = evento.descripcion.ifEmpty {
            "Se ha registrado una anomalía en los patrones de movimiento de la SafeBand."
        }

        // Validar coordenadas de la alerta
        val rawLat = evento.latitud
        val rawLng = evento.longitud
        val tieneGpsValido = esCoordenadaValida(rawLat, rawLng)

        val latFinal = if (tieneGpsValido) rawLat else 23.264864
        val lngFinal = if (tieneGpsValido) rawLng else -106.430832

        // Ubicación en texto
        binding.tvLocationName.text = if (evento.ubicacionTexto.isNotBlank() && !evento.ubicacionTexto.contains("Guang Xi", ignoreCase = true)) {
            evento.ubicacionTexto
        } else {
            "Mazatlán, Sinaloa"
        }

        if (tieneGpsValido) {
            viewLifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val geocoder = android.location.Geocoder(requireContext(), java.util.Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latFinal, lngFinal, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val country = address.countryName ?: ""
                        // Evitar nombres incoherentes si el sensor envió datos no calibrados
                        if (!country.contains("China", ignoreCase = true)) {
                            val street = address.thoroughfare ?: address.subLocality ?: ""
                            val locality = address.locality ?: address.subAdminArea ?: "Mazatlán"
                            val adminArea = address.adminArea ?: "Sinaloa"
                            val fullText = listOf(street, locality, adminArea).filter { it.isNotBlank() }.joinToString(", ")
                            if (fullText.isNotBlank()) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    _binding?.tvLocationName?.text = fullText
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DetalleIncidente", "Error en Geocoder: ${e.message}")
                }
            }
        }

        // Hora y Fecha
        binding.tvHora.text = evento.horaTexto.ifEmpty { evento.tiempoTexto.ifEmpty { "--:--" } }
        binding.tvFecha.text = evento.fechaTexto.ifEmpty { "--/--/----" }

        // Frecuencia Cardíaca y Estado Pulsera
        binding.tvHeartRate.text = evento.frecuenciaCardiaca.ifEmpty { "85 BPM" }
        binding.tvEstadoPulsera.text = evento.estadoPulsera.ifEmpty { "Activa" }

        // Estado del botón
        if (evento.revisado) {
            binding.btnMarcarRevisado.text = "Incidente revisado ✓"
            binding.btnMarcarRevisado.isEnabled = false
            binding.btnMarcarRevisado.alpha = 0.7f
        } else {
            binding.btnMarcarRevisado.text = "Marcar como revisado"
            binding.btnMarcarRevisado.isEnabled = true
            binding.btnMarcarRevisado.alpha = 1.0f
        }
    }

    private fun esCoordenadaValida(lat: Double, lng: Double): Boolean {
        if (lat == 0.0 && lng == 0.0) return false
        if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) return false
        if (kotlin.math.abs(lat) < 0.1 && kotlin.math.abs(lng) < 0.1) return false
        return true
    }

    private fun setupMiniMap() {
        val evento = selectedEvento
        val rawLat = evento?.latitud ?: 0.0
        val rawLng = evento?.longitud ?: 0.0

        val tieneGpsValido = esCoordenadaValida(rawLat, rawLng)
        val lat = if (tieneGpsValido) rawLat else 23.264864
        val lng = if (tieneGpsValido) rawLng else -106.430832

        binding.miniMapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.miniMapView.setMultiTouchControls(true)
        binding.miniMapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        val controller = binding.miniMapView.controller
        controller.setZoom(16.5)

        val point = GeoPoint(lat, lng)
        controller.setCenter(point)

        val marker = Marker(binding.miniMapView)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = evento?.titulo?.ifEmpty { "Ubicación del incidente" } ?: "Ubicación del incidente"

        val pinDrawable = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_location_pin)
        if (pinDrawable != null) {
            marker.icon = pinDrawable
        }

        binding.miniMapView.overlays.clear()
        binding.miniMapView.overlays.add(marker)
        binding.miniMapView.invalidate()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnMarcarRevisado.setOnClickListener {
            val evento = selectedEvento
            if (evento != null && evento.id.isNotEmpty()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.marcarComoRevisado(evento.id)
                    Toast.makeText(requireContext(), "Incidente marcado como revisado", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            } else {
                findNavController().popBackStack()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.miniMapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.miniMapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
