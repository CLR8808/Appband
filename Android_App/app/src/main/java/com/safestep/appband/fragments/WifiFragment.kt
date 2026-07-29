package com.safestep.appband.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.safestep.appband.adapters.WifiAdapter
import com.safestep.appband.databinding.FragmentWifiBinding
import com.safestep.appband.models.WifiNetwork
import com.safestep.appband.viewmodels.WifiViewModel
import kotlinx.coroutines.launch

/**
 * Fragment de Escaneo y Configuración Wi-Fi.
 * Migrado desde components/wifi/wifi.component.ts
 */
class WifiFragment : Fragment() {

    private var _binding: FragmentWifiBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WifiViewModel by viewModels()
    private lateinit var adapter: WifiAdapter

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            viewModel.iniciarEscaneo()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWifiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarRecyclerView()
        configurarListeners()
        verificarPermisosYEscaneo()
        observarEstado()
    }

    private fun configurarRecyclerView() {
        adapter = WifiAdapter { net ->
            viewModel.selectNetwork(net)
            val action = WifiFragmentDirections.actionWifiToPassword(net.ssid)
            findNavController().navigate(action)
        }
        binding.rvWifiNetworks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWifiNetworks.adapter = adapter
    }

    private fun configurarListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnRefresh.setOnClickListener {
            viewModel.iniciarEscaneo()
        }

        binding.btnManualNetwork.setOnClickListener {
            mostrarDialogoRedManual()
        }
    }

    private fun verificarPermisosYEscaneo() {
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
            viewModel.iniciarEscaneo()
        }
    }

    private fun mostrarDialogoRedManual() {
        val input = EditText(requireContext())
        input.hint = "Nombre de la red Wi-Fi (SSID)"

        AlertDialog.Builder(requireContext())
            .setTitle("Agregar Red Oculta")
            .setView(input)
            .setPositiveButton("Siguiente") { _, _ ->
                val ssid = input.text.toString().trim()
                if (ssid.isNotEmpty()) {
                    val manualNet = WifiNetwork(ssid = ssid)
                    viewModel.selectNetwork(manualNet)
                    val action = WifiFragmentDirections.actionWifiToPassword(ssid)
                    findNavController().navigate(action)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isScanning.collect { scanning ->
                        binding.containerScanning.visibility = if (scanning) View.VISIBLE else View.GONE
                        binding.btnRefresh.isEnabled = !scanning
                    }
                }

                launch {
                    viewModel.networks.collect { lista ->
                        adapter.submitList(lista)
                        binding.tvCountBadge.text = "${lista.size} encontradas"
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
