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
import com.google.android.material.textfield.TextInputEditText
import com.safestep.appband.R
import com.safestep.appband.models.DispositivoBluetooth
import com.safestep.appband.repositories.BluetoothRepository
import com.safestep.appband.viewmodels.BluetoothViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment para búsqueda, conexión y telemetría usando Bluetooth Clásico (SPP).
 * Actualizado para mostrar datos de sensor DHT11 (Temperatura y Humedad).
 */
class BluetoothFragment : Fragment() {

    private val viewModel: BluetoothViewModel by viewModels()

    private lateinit var btnBack: ImageButton
    private lateinit var tvStatusTitle: TextView
    private lateinit var tvStatusDetail: TextView
    private lateinit var btnScanBle: MaterialButton
    private lateinit var containerDevicesList: LinearLayout
    private lateinit var tvEmptyState: TextView

    // Live Telemetry & Control Panel Views
    private lateinit var panelConnected: View
    private lateinit var btnSaveAndContinue: MaterialButton
    private lateinit var etBleWifiSsid: TextInputEditText
    private lateinit var etBleWifiPass: TextInputEditText
    private lateinit var btnSendBleWifi: MaterialButton
    private lateinit var btnTestBuzzer: MaterialButton
    private lateinit var btnTestPing: MaterialButton

    private val deviceViewsMap = mutableMapOf<String, View>()
    private var lastConnectedMac = ""
    private var lastConnectedName = ""

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

        // Panel de Telemetría
        panelConnected = view.findViewById(R.id.panelConnected)
        btnSaveAndContinue = view.findViewById(R.id.btnSaveAndContinue)
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

        btnSaveAndContinue.setOnClickListener {
            val name = lastConnectedName.ifBlank { "SafeBand" }
            val mac = lastConnectedMac
            viewModel.guardarDispositivoConectado(name, mac) {
                Toast.makeText(requireContext(), "✅ Dispositivo $name guardado", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.anadidaFragment)
            }
        }

        btnSendBleWifi.setOnClickListener {
            val ssid = etBleWifiSsid.text?.toString()?.trim() ?: ""
            val pass = etBleWifiPass.text?.toString()?.trim() ?: ""

            if (ssid.isBlank()) {
                Toast.makeText(requireContext(), "Ingresa el nombre de la red", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sent = viewModel.enviarWifiConfig(ssid, pass)
            if (sent) {
                Toast.makeText(requireContext(), "📶 Configuración enviada por Bluetooth", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Fallo al enviar. Verifica la conexión.", Toast.LENGTH_SHORT).show()
            }
        }

        btnTestBuzzer.setOnClickListener {
            viewModel.enviarComando("BUZZER:1\n")
        }

        btnTestPing.setOnClickListener {
            viewModel.enviarComando("PING\n")
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
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH)
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN)
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.estadoConexion.collectLatest { state ->
                when (state) {
                    is BluetoothRepository.EstadoConexion.Desconectado -> {
                        tvStatusTitle.text = "Estado: Desconectado"
                        tvStatusDetail.text = "Busca tu pulsera vinculada o cercana."
                        btnScanBle.isEnabled = true
                        btnScanBle.text = "🔍 Buscar Pulsera"
                        panelConnected.visibility = View.GONE
                    }
                    is BluetoothRepository.EstadoConexion.Escaneando -> {
                        tvStatusTitle.text = "Estado: Buscando..."
                        tvStatusDetail.text = "Asegúrate de que la pulsera sea visible."
                        btnScanBle.isEnabled = false
                        btnScanBle.text = "Buscando..."
                        panelConnected.visibility = View.GONE
                    }
                    is BluetoothRepository.EstadoConexion.Conectando -> {
                        tvStatusTitle.text = "Estado: Conectando..."
                        tvStatusDetail.text = "Iniciando puerto serie con ${state.nombre}."
                        btnScanBle.isEnabled = false
                        panelConnected.visibility = View.GONE
                    }
                    is BluetoothRepository.EstadoConexion.Conectado -> {
                        lastConnectedName = state.nombre
                        lastConnectedMac = state.mac

                        tvStatusTitle.text = "Estado: ¡Conectado!"
                        tvStatusDetail.text = "Conectado a ${state.nombre} (${state.mac})"
                        btnScanBle.isEnabled = true
                        btnScanBle.text = "Desconectar"
                        btnScanBle.setOnClickListener { viewModel.desconectar() }

                        panelConnected.visibility = View.VISIBLE
                        viewModel.guardarDispositivoConectado(lastConnectedName, lastConnectedMac)
                    }
                    is BluetoothRepository.EstadoConexion.Error -> {
                        tvStatusTitle.text = "Estado: Error"
                        tvStatusDetail.text = state.mensaje
                        btnScanBle.isEnabled = true
                        btnScanBle.text = "Reintentar"
                        panelConnected.visibility = View.GONE
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dispositivosEncontrados.collectLatest { devices ->
                renderDevicesList(devices)
            }
        }
    }

    private fun renderDevicesList(devices: List<DispositivoBluetooth>) {
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

            cardView.findViewById<TextView>(R.id.tvDeviceName)?.text = device.nombre
            cardView.findViewById<TextView>(R.id.tvMacAddress)?.text = "MAC: $mac"
        }
    }
}
