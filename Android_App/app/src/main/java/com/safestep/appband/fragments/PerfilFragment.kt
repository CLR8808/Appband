package com.safestep.appband.fragments

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import com.safestep.appband.R
import com.safestep.appband.activities.AuthActivity
import com.safestep.appband.databinding.FragmentPerfilBinding
import com.safestep.appband.viewmodels.PerfilViewModel
import kotlinx.coroutines.launch

/**
 * Fragment de Perfil de Usuario y Registro de Paciente.
 * Soporta configuración real de notificaciones, ubicación y Wi-Fi,
 * además de botón de cierre de sesión rojo completo y selector de tabs sin superposición.
 */
class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PerfilViewModel by viewModels()

    private var isUpdatingSwitchesProgrammatically = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarOptionCards()
        observarEstado()
    }

    override fun onResume() {
        super.onResume()
        syncSystemPreferenceSwitches()
    }

    private fun syncSystemPreferenceSwitches() {
        if (_binding == null) return
        val context = requireContext()

        isUpdatingSwitchesProgrammatically = true

        // 1. Notificaciones
        val notifEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        binding.cardNotificaciones.switchToggle.isChecked = notifEnabled

        // 2. Ubicación
        val locationEnabled = isLocationServiceEnabled(context)
        binding.cardUbicacionPref.switchToggle.isChecked = locationEnabled

        // 3. Wi-Fi
        val wifiEnabled = isWifiServiceEnabled(context)
        binding.cardWifiPref.switchToggle.isChecked = wifiEnabled

        isUpdatingSwitchesProgrammatically = false
    }

    private fun isLocationServiceEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        val isNetEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        val hasPermission = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return (isGpsEnabled || isNetEnabled) && hasPermission
    }

    private fun isWifiServiceEnabled(context: Context): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        return wifiManager?.isWifiEnabled == true
    }

    private fun configurarOptionCards() {
        // Detalles de usuario
        binding.cardDetallesUsuario.tvTitle.text = "Detalles de usuario"
        binding.cardDetallesUsuario.ivIcon.setImageResource(R.drawable.ic_person)
        binding.cardDetallesUsuario.cvContainer.setOnClickListener {
            findNavController().navigate(R.id.action_perfil_to_detallesUsuario)
        }

        // Notificaciones Switch
        binding.cardNotificaciones.tvTitle.text = "Notificaciones"
        binding.cardNotificaciones.ivIcon.setImageResource(R.drawable.ic_mail)
        binding.cardNotificaciones.ivChevron.visibility = View.GONE
        binding.cardNotificaciones.switchToggle.visibility = View.VISIBLE

        binding.cardNotificaciones.switchToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitchesProgrammatically) return@setOnCheckedChangeListener
            val notifEnabled = NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()
            if (isChecked != notifEnabled) {
                try {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    startActivity(intent)
                }
            }
        }

        // Ubicación Switch
        binding.cardUbicacionPref.tvTitle.text = "Ubicación"
        binding.cardUbicacionPref.ivIcon.setImageResource(R.drawable.ic_map)
        binding.cardUbicacionPref.ivChevron.visibility = View.GONE
        binding.cardUbicacionPref.switchToggle.visibility = View.VISIBLE

        binding.cardUbicacionPref.switchToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitchesProgrammatically) return@setOnCheckedChangeListener
            val locEnabled = isLocationServiceEnabled(requireContext())
            if (isChecked != locEnabled) {
                if (isChecked && ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1001)
                } else {
                    try {
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        startActivity(intent)
                    } catch (e: Exception) {
                        // fallback
                    }
                }
            }
        }

        // Wi-Fi Switch
        binding.cardWifiPref.tvTitle.text = "Wi-Fi"
        binding.cardWifiPref.ivIcon.setImageResource(R.drawable.ic_wifi)
        binding.cardWifiPref.ivChevron.visibility = View.GONE
        binding.cardWifiPref.switchToggle.visibility = View.VISIBLE

        binding.cardWifiPref.switchToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitchesProgrammatically) return@setOnCheckedChangeListener
            val wifiEnabled = isWifiServiceEnabled(requireContext())
            if (isChecked != wifiEnabled) {
                try {
                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                    startActivity(intent)
                } catch (e: Exception) {
                    // fallback
                }
            }
        }

        // Soporte
        binding.cardSoporte.tvTitle.text = "Soporte"
        binding.cardSoporte.ivIcon.setImageResource(R.drawable.ic_call)
        binding.cardSoporte.cvContainer.setOnClickListener {
            findNavController().navigate(R.id.action_perfil_to_soporte)
        }

        // Cerrar Sesión (Boton Rojo Completo con texto blanco e icono blanco sin recuadro)
        binding.cardCerrarSesion.cvContainer.setCardBackgroundColor(Color.parseColor("#DC2626"))
        binding.cardCerrarSesion.containerIcon.background = null
        binding.cardCerrarSesion.containerIcon.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        binding.cardCerrarSesion.ivIcon.setImageResource(R.drawable.ic_lock)
        binding.cardCerrarSesion.ivIcon.imageTintList = ColorStateList.valueOf(Color.WHITE)
        binding.cardCerrarSesion.tvTitle.text = "Cerrar sesión"
        binding.cardCerrarSesion.tvTitle.setTextColor(Color.WHITE)
        binding.cardCerrarSesion.ivChevron.visibility = View.GONE

        binding.cardCerrarSesion.cvContainer.setOnClickListener {
            viewModel.cerrarSesion()
            val intent = Intent(requireActivity(), AuthActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.perfil.collect { perfil ->
                    if (perfil != null) {
                        binding.tvUserName.text = perfil.nombre.ifEmpty { "Usuario SafeStep" }
                        binding.tvUserEmail.text = perfil.correo.ifEmpty { "Sin correo registrado" }
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
