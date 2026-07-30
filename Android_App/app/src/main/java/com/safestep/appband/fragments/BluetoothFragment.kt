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
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.safestep.appband.R
import com.safestep.appband.models.DispositivoBluetooth
import com.safestep.appband.repositories.BluetoothRepository
import com.safestep.appband.viewmodels.BluetoothViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment ultra-optimizado para búsqueda, conexión real y modo simulación de prueba Bluetooth BLE.
 */
class BluetoothFragment : Fragment() {

    private val viewModel: BluetoothViewModel by viewModels()

    private lateinit var btnBack: ImageButton
    private lateinit var tvStatusTitle: TextView
    private lateinit var tvStatusDetail: TextView
    private lateinit var btnScanBle: MaterialButton
    private lateinit var containerDevicesList: LinearLayout
    private lateinit var tvEmptyState: TextView
    private lateinit var switchSimulacion: SwitchMaterial

    // Live Telemetry & Control Panel Views
    private lateinit var panelConnected: View
    private lateinit var tvLiveBpm: TextView
    private lateinit var tvLastMessage: TextView
    private lateinit var etBleWifiSsid: TextInputEditText
    private lateinit var etBleWifiPass: TextInputEditText
    private lateinit var btnSendBleWifi: MaterialButton
    private lateinit var btnTestBuzzer: MaterialButton
    private lateinit var btnTestPing: MaterialButton

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
        switchSimulacion = view.findViewById(R.id.switchSimulacion)

        // Panel de Telemetría
        panelConnected = view.findViewById(R.id.panelConnected)
        tvLiveBpm = view.findViewById(R.id.tvLiveBpm)
        tvLastMessage = view.findViewById(R.id.tvLastMessage)
        etBleWifiSsid = view.findViewById(R.id.etBleWifiSsid)
        etBleWifiPass = view.findViewById(R.id.etBleWifiPass)
        btnSendBleWifi = view.findViewById(R.id.btnSendBleWifi)
        btnTestBuzzer = view.findViewById(R.id.btnTestBuzzer)
        btnTestPing = view.findViewById(R.id.btnTestPing)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        btnScanBle.setOnClickListener {
            checkPermissionsAndScan()
        }

        switchSimulacion.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setModoSimulacion(isChecked)
            if (isChecked) {
                Toast.makeText(requireContext(), "🧪 Modo Simulación Activado. Puedes probar conectar a 'SafeBand ESP32 (SIMULADO)'", Toast.LENGTH_LONG).show()
            }
        }

        // Send Wi-Fi credentials over BLE
        btnSendBleWifi.setOnClickListener {
            val ssid = etBleWifiSsid.text?.toString()?.trim() ?: ""
            val pass = etBleWifiPass.text?.toString()?.trim() ?: ""

            if (ssid.isBlank()) {
                Toast.makeText(requireContext(), "Ingresa el nombre de tu red Wi-Fi (SSID)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sent = viewModel.enviarWifiConfig(ssid, pass)
            if (sent) {
                Toast.makeText(requireContext(), "📶 Credenciales Wi-Fi enviadas a la pulsera por BLE", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Fallo al enviar credenciales. Verifica la conexión BLE.", Toast.LENGTH_SHORT).show()
            }
        }

        // Test Buzzer / Alarma
        btnTestBuzzer.setOnClickListener {
            val sent = viewModel.enviarComando("BUZZER:1")
            if (sent) {
                Toast.makeText(requireContext(), "🔊 Comando de alarma enviado a la pulsera", Toast.LENGTH_SHORT).show()
            }
        }

        // Test Ping
        btnTestPing.setOnClickListener {
            val sent = viewModel.enviarComando("PING")
            if (sent) {
                Toast.makeText(requireContext(), "📡 Ping enviado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissionsAndScan() {
        if (viewModel.modoSimulacion.value) {
            viewModel.iniciarEscaneo()
            return
        }

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
                        panelConnected.visibility = View.GONE
                    }
                    is BluetoothRepository.EstadoConexionBle.Escaneando -> {
                        tvStatusTitle.text = "Estado: Buscando pulseras BLE..."
                        tvStatusDetail.text = "Buscando dispositivos cercanos. Mantén la pulsera cerca."
                        btnScanBle.isEnabled = false
                        btnScanBle.text = "Buscando..."
                        panelConnected.visibility = View.GONE
                    }
                    is BluetoothRepository.EstadoConexionBle.Conectando -> {
                        tvStatusTitle.text = "Estado: Conectando..."
                        tvStatusDetail.text = "Estableciendo conexión GATT con ${state.nombre}."
                        btnScanBle.isEnabled = false
                        panelConnected.visibility = View.GONE
                    }
                    is BluetoothRepository.EstadoConexionBle.Conectado -> {
                        tvStatusTitle.text = "Estado: ¡Conectado por Bluetooth!"
                        tvStatusDetail.text = "Conectado exitosamente a ${state.dispositivo.nombre} (${state.dispositivo.macAddress}) • ${state.serviciosCount} servicios GATT activos."
                        btnScanBle.isEnabled = true
                        btnScanBle.text = "Desconectar Pulsera"
                        btnScanBle.setOnClickListener { viewModel.desconectar() }

                        panelConnected.visibility = View.VISIBLE
                        Toast.makeText(requireContext(), "✅ Conexión BLE Establecida con ${state.dispositivo.nombre}", Toast.LENGTH_LONG).show()
                    }
                    is BluetoothRepository.EstadoConexionBle.Error -> {
                        tvStatusTitle.text = "Estado: Error"
                        tvStatusDetail.text = state.mensaje
                        btnScanBle.isEnabled = true
                        btnScanBle.text = "Reintentar Escaneo"
                        panelConnected.visibility = View.GONE
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

        // Observe Real-Time Continuous Telemetry
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.telemetria.collectLatest { telem ->
                if (telem.conectado) {
                    if (telem.pulsoBpm != null) {
                        tvLiveBpm.text = telem.pulsoBpm.toString()
                    }
                    tvLastMessage.text = telem.ultimoMensaje

                    if (telem.alertaActiva != null) {
                        Toast.makeText(requireContext(), telem.alertaActiva, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // Observe Modo Simulación
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.modoSimulacion.collectLatest { isSimulated ->
                if (switchSimulacion.isChecked != isSimulated) {
                    switchSimulacion.isChecked = isSimulated
                }
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
    }
}
