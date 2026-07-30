package com.safestep.appband.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.safestep.appband.databinding.FragmentMapaBinding
import com.safestep.appband.models.DeviceCoordinates
import com.safestep.appband.viewmodels.MapaViewModel
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Locale

/**
 * Fragment de Ubicación en Tiempo Real del Dispositivo SafeBand con mapa HD (OsmDroid / OpenStreetMap).
 */
class MapaFragment : Fragment() {

    private var _binding: FragmentMapaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapaViewModel by viewModels()
    private var deviceMarker: Marker? = null
    private var myLocationOverlay: MyLocationNewOverlay? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            habilitarUbicacionOverlay()
            viewModel.obtenerUbicacionActual()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx: Context = requireActivity().applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = requireActivity().packageName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarMapaOsm()
        verificarPermisosUbicacion()
        configurarListeners()
        observarEstado()
    }

    private fun configurarMapaOsm() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        binding.mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        val controller = binding.mapView.controller
        controller.setZoom(17.5)

        // Ubicación por defecto inicial (23.264864, -106.430832 Mazatlán)
        val initialPoint = GeoPoint(23.264864, -106.430832)
        controller.setCenter(initialPoint)

        habilitarUbicacionOverlay()
    }

    private fun habilitarUbicacionOverlay() {
        val fineLocation = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLocation && _binding != null) {
            if (myLocationOverlay == null) {
                myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), binding.mapView)
                myLocationOverlay?.enableMyLocation()
                myLocationOverlay?.enableFollowLocation()
                binding.mapView.overlays.add(myLocationOverlay)
            }
        }
    }

    private fun verificarPermisosUbicacion() {
        val fineLocation = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocation) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            viewModel.obtenerUbicacionActual()
        }
    }

    private fun configurarListeners() {
        binding.fabCenterMap.setOnClickListener {
            val coords = viewModel.currentLocation.value
            if (coords != null) {
                val point = GeoPoint(coords.lat, coords.lng)
                binding.mapView.controller.animateTo(point, 18.0, 1000L)
            } else {
                viewModel.obtenerUbicacionActual()
            }
        }
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentLocation.collect { coords ->
                    if (coords != null) {
                        actualizarMapa(coords)
                    }
                }
            }
        }
    }

    private fun actualizarMapa(coords: DeviceCoordinates) {
        binding.tvLat.text = String.format(Locale.getDefault(), "%.6f", coords.lat)
        binding.tvLng.text = String.format(Locale.getDefault(), "%.6f", coords.lng)
        binding.tvAccuracy.text = String.format(
            Locale.getDefault(),
            "±%.0f m",
            coords.accuracy ?: 0f
        )

        val geoPoint = GeoPoint(coords.lat, coords.lng)

        if (deviceMarker == null) {
            deviceMarker = Marker(binding.mapView).apply {
                position = geoPoint
                title = "📍 SafeBand (Dispositivo Conectado)"
                snippet = "GPS Lat: ${coords.lat}, Lng: ${coords.lng}"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            binding.mapView.overlays.add(deviceMarker)
        } else {
            deviceMarker?.position = geoPoint
            deviceMarker?.snippet = "GPS Lat: ${coords.lat}, Lng: ${coords.lng}"
        }

        binding.mapView.controller.animateTo(geoPoint, 17.5, 800L)
        binding.mapView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
