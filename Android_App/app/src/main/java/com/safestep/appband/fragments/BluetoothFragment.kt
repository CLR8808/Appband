package com.safestep.appband.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.safestep.appband.R
import com.safestep.appband.models.DispositivoBluetooth
import com.safestep.appband.repositories.BluetoothRepository
import com.safestep.appband.viewmodels.BluetoothViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment ultra-optimizado para búsqueda y conexión Bluetooth BLE con la pulsera SafeBand.
 * Evita congelamientos usando reciclaje directo de vistas en contenedor vertical.
 */
class BluetoothFragment : Fragment() {

    private val viewModel: BluetoothViewModel by viewModels()

    private lateinit var btnBack: ImageButton
    private lateinit var tvStatusTitle: TextView
    private lateinit var tvStatusDetail: TextView
    private lateinit var btnScanBle: MaterialButton
    private lateinit var containerDevicesList: LinearLayout
    private lateinit var tvEmptyState: TextView

    // Cache de vistas por dirección MAC para evitar reinstanciar layouts innecesariamente
    private val deviceViewsMap = mutableMapOf<String, View>()

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            viewModel.iniciarEscaneo()
        } else {
            Toast.makeText(
                requireContext(),
                "Se requieren permisos de Bluetooth y Ubicación para buscar la pulsera.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bluetooth, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupListeners()
        observeViewModel()
    }

    private fun bindViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        tvStatusTitle = view.findViewById(R.id.tvStatusTitle)
        tvStatusDetail = view.findViewById(R.id.tvStatusDetail)
        btnScanBle = view.findViewById(R.id.btnScanBle)
        containerDevicesList = view.findViewById(R.id.containerDevicesList)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        btnScanBle.setOnClickListener {
            checkPermissionsAndScan()
        }
    }

    private fun checkPermissionsAndScan() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            deviceViewsMap.clear()
            viewModel.iniciarEscaneo()
        }
    }

    private fun observeViewModel() {
        // Observe Connection State
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.estadoConexion.collectLatest { state ->
                when (state) {
                    is BluetoothRepository.EstadoConexionBle.Desconectado -> {
                        tvStatusTitle.text = "Estado: Desconectado"
                        tvStatusDetail.text = "Presiona 'Buscar Pulsera' para escanear."
                        btnScanBle.isEnabled = true
                        btnScanBle.text = "🔍 Buscar Pulsera SafeBand"
                    }
                    is BluetoothRepository.EstadoConexionBle.Escaneando -> {
                        tvStatusTitle.text = "Estado: Buscando pulseras BLE..."
                        tvStatusDetail.text = "Buscando dispositivos cercanos. Mantén la pulsera cerca."
                        btnScanBle.isEnabled = false
                        btnScanBle.text = "Buscando..."
                    }
                    is BluetoothRepository.EstadoConexionBle.Conectando -> {
                        tvStatusTitle.text = "Estado: Conectando..."
                        tvStatusDetail.text = "Estableciendo conexión GATT con ${state.nombre}."
                        btnScanBle.isEnabled = false
                    }
                    is BluetoothRepository.EstadoConexionBle.Conectado -> {
                        tvStatusTitle.text = "Estado: ¡Conectado por Bluetooth!"
                        tvStatusDetail.text = "Conectado exitosamente a ${state.dispositivo.nombre} (${state.dispositivo.macAddress}) • ${state.serviciosCount} servicios GATT activos."
                        btnScanBle.isEnabled = true
                        btnScanBle.text = "Desconectar Pulsera"
                        btnScanBle.setOnClickListener { viewModel.desconectar() }

                        Toast.makeText(requireContext(), "✅ Conexión BLE Real Establecida con ${state.dispositivo.nombre}", Toast.LENGTH_LONG).show()
                    }
                    is BluetoothRepository.EstadoConexionBle.Error -> {
                        tvStatusTitle.text = "Estado: Error"
                        tvStatusDetail.text = state.mensaje
                        btnScanBle.isEnabled = true
                        btnScanBle.text = "Reintentar Escaneo"
                    }
                }
            }
        }

        // Observe Devices Found List
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dispositivosEncontrados.collectLatest { devices ->
                renderDevicesListOptimized(devices)
            }
        }
    }

    private fun renderDevicesListOptimized(devices: List<DispositivoBluetooth>) {
        if (devices.isEmpty()) {
            containerDevicesList.removeAllViews()
            deviceViewsMap.clear()
            containerDevicesList.addView(tvEmptyState)
            return
        }

        // Quitar estado vacío si hay elementos
        if (containerDevicesList.indexOfChild(tvEmptyState) != -1) {
            containerDevicesList.removeView(tvEmptyState)
        }

        val inflater = LayoutInflater.from(requireContext())

        devices.forEach { device ->
            val mac = device.macAddress
            val cardView = deviceViewsMap.getOrPut(mac) {
                inflater.inflate(R.layout.item_bluetooth_device, containerDevicesList, false).also { newView ->
                    containerDevicesList.addView(newView)
                    newView.findViewById<MaterialButton>(R.id.btnConnectBt)?.setOnClickListener {
                        viewModel.conectar(mac)
                    }
                }
            }

            val tvName = cardView.findViewById<TextView>(R.id.tvDeviceName)
            val tvMac = cardView.findViewById<TextView>(R.id.tvMacAddress)

            tvName?.text = device.nombre
            tvMac?.text = "MAC: $mac  •  Señal: ${device.rssi} dBm"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.detenerEscaneo()
    }
}
