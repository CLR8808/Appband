package com.safestep.appband.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
 * Migrado desde components/perfil/perfil.component.ts
 */
class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PerfilViewModel by viewModels()

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
        configurarTabLayout()
        observarEstado()
    }

    private fun configurarOptionCards() {
        // Detalles de usuario
        binding.cardDetallesUsuario.tvTitle.text = "Detalles de usuario"
        binding.cardDetallesUsuario.ivIcon.setImageResource(R.drawable.ic_person)
        binding.cardDetallesUsuario.cvContainer.setOnClickListener {
            findNavController().navigate(R.id.action_perfil_to_detallesUsuario)
        }

        // Configuración
        binding.cardConfiguracion.tvTitle.text = "Configuración"
        binding.cardConfiguracion.ivIcon.setImageResource(R.drawable.ic_lock)
        binding.cardConfiguracion.cvContainer.setOnClickListener {
            findNavController().navigate(R.id.action_perfil_to_configuracion)
        }

        // Notificaciones Switch
        binding.cardNotificaciones.tvTitle.text = "Notificaciones"
        binding.cardNotificaciones.ivIcon.setImageResource(R.drawable.ic_mail)
        binding.cardNotificaciones.ivChevron.visibility = View.GONE
        binding.cardNotificaciones.switchToggle.visibility = View.VISIBLE
        binding.cardNotificaciones.switchToggle.isChecked = true

        // Ubicación Switch
        binding.cardUbicacionPref.tvTitle.text = "Ubicación"
        binding.cardUbicacionPref.ivIcon.setImageResource(R.drawable.ic_map)
        binding.cardUbicacionPref.ivChevron.visibility = View.GONE
        binding.cardUbicacionPref.switchToggle.visibility = View.VISIBLE
        binding.cardUbicacionPref.switchToggle.isChecked = true

        // Wi-Fi Switch
        binding.cardWifiPref.tvTitle.text = "Wi-Fi"
        binding.cardWifiPref.ivIcon.setImageResource(R.drawable.ic_mail)
        binding.cardWifiPref.ivChevron.visibility = View.GONE
        binding.cardWifiPref.switchToggle.visibility = View.VISIBLE
        binding.cardWifiPref.switchToggle.isChecked = true

        // Soporte
        binding.cardSoporte.tvTitle.text = "Soporte"
        binding.cardSoporte.ivIcon.setImageResource(R.drawable.ic_call)
        binding.cardSoporte.cvContainer.setOnClickListener {
            findNavController().navigate(R.id.action_perfil_to_soporte)
        }

        // Cerrar Sesión (Danger Card)
        binding.cardCerrarSesion.tvTitle.text = "Cerrar sesión"
        binding.cardCerrarSesion.tvTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger_red))
        binding.cardCerrarSesion.ivIcon.setImageResource(R.drawable.ic_lock)
        binding.cardCerrarSesion.ivIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.danger_red))
        binding.cardCerrarSesion.containerIcon.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.danger_red_bg))
        binding.cardCerrarSesion.cvContainer.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.danger_red_bg))
        binding.cardCerrarSesion.ivChevron.visibility = View.GONE

        binding.cardCerrarSesion.cvContainer.setOnClickListener {
            viewModel.cerrarSesion()
            val intent = Intent(requireActivity(), AuthActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun configurarTabLayout() {
        binding.tabLayoutPerfil.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 0) {
                    binding.containerPerfilTab.visibility = View.VISIBLE
                    binding.containerPacienteTab.visibility = View.GONE
                } else {
                    binding.containerPerfilTab.visibility = View.GONE
                    binding.containerPacienteTab.visibility = View.VISIBLE

                    // Cargar RegistroPacienteFragment si no está cargado
                    if (childFragmentManager.findFragmentById(R.id.containerPacienteTab) == null) {
                        childFragmentManager.beginTransaction()
                            .replace(R.id.containerPacienteTab, RegistroPacienteFragment())
                            .commit()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
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
