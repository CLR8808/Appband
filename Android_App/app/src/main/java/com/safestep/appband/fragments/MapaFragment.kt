package com.safestep.appband.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.safestep.appband.databinding.FragmentMapaBinding
import com.safestep.appband.models.DeviceCoordinates
import com.safestep.appband.viewmodels.MapaViewModel
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Fragment de Ubicación en Tiempo Real del Dispositivo SafeBand y Teléfono con Google Maps SDK.
 */
class MapaFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapaViewModel by viewModels()
    private var googleMap: GoogleMap? = null
    private var deviceMarker: Marker? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            habilitarCapaUbicacionNativa()
            viewModel.obtenerUbicacionActual()
        }
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

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        verificarPermisosUbicacion()
        configurarListeners()
        observarEstado()
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
            if (coords != null && googleMap != null) {
                val latLng = LatLng(coords.lat, coords.lng)
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
            } else {
                viewModel.obtenerUbicacionActual()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = false
        googleMap?.uiSettings?.isCompassEnabled = true

        habilitarCapaUbicacionNativa()

        viewModel.currentLocation.value?.let { coords ->
            actualizarMapa(coords)
        }
    }

    @SuppressLint("MissingPermission")
    private fun habilitarCapaUbicacionNativa() {
        val fineLocation = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLocation && googleMap != null) {
            try {
                googleMap?.isMyLocationEnabled = true
                googleMap?.uiSettings?.isMyLocationButtonEnabled = false
            } catch (e: Exception) {
                e.printStackTrace()
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

        googleMap?.let { map ->
            val latLng = LatLng(coords.lat, coords.lng)

            if (deviceMarker == null) {
                val markerOptions = MarkerOptions()
                    .position(latLng)
                    .title("📍 SafeBand (Dispositivo Conectado)")
                    .snippet("Ubicación exacta GPS: ${coords.lat}, ${coords.lng}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))

                deviceMarker = map.addMarker(markerOptions)
            } else {
                deviceMarker?.position = latLng
                deviceMarker?.snippet = "Ubicación exacta GPS: ${coords.lat}, ${coords.lng}"
            }

            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
        }
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
        binding.mapView.onDestroy()
        _binding = null
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }
}
