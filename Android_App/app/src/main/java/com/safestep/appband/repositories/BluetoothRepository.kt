package com.safestep.appband.repositories

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.util.Log
import com.safestep.appband.models.DatosTelemetriaBle
import com.safestep.appband.models.DispositivoBluetooth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

/**
 * Repositorio Singleton para Bluetooth Low Energy (BLE) GATT.
 * Conecta con la pulsera SafeBand vía Nordic UART Service (NUS),
 * igual que nRF Connect: connectGatt → discoverServices → write RX characteristic.
 */
class BluetoothRepository private constructor(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothRepo"

        // Nordic UART Service UUIDs (idénticos a los que muestra nRF Connect)
        private val NUS_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        private val NUS_RX_CHAR_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")  // Write (enviar datos AL ESP32)
        private val NUS_TX_CHAR_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")  // Notify (recibir datos DEL ESP32)
        private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")         // Client Characteristic Configuration Descriptor

        @Volatile
        private var INSTANCE: BluetoothRepository? = null

        fun getInstance(context: Context): BluetoothRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BluetoothRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val _dispositivosEncontrados = MutableStateFlow<List<DispositivoBluetooth>>(emptyList())
    val dispositivosEncontrados: StateFlow<List<DispositivoBluetooth>> = _dispositivosEncontrados

    sealed class EstadoConexion {
        object Desconectado : EstadoConexion()
        object Escaneando : EstadoConexion()
        data class Conectando(val nombre: String) : EstadoConexion()
        data class Conectado(val nombre: String, val mac: String) : EstadoConexion()
        data class Error(val mensaje: String) : EstadoConexion()
    }

    private val _estadoConexion = MutableStateFlow<EstadoConexion>(EstadoConexion.Desconectado)
    val estadoConexion: StateFlow<EstadoConexion> = _estadoConexion

    private val _telemetria = MutableStateFlow(DatosTelemetriaBle())
    val telemetria: StateFlow<DatosTelemetriaBle> = _telemetria

    private var bluetoothGatt: BluetoothGatt? = null
    private var nusRxCharacteristic: BluetoothGattCharacteristic? = null
    private var connectedDeviceName: String = ""
    private var connectedDeviceMac: String = ""
    private val scope = CoroutineScope(Dispatchers.IO)
    private var readJob: Job? = null

    // MTU negociado con el dispositivo BLE (nRF Connect negocia 512)
    private var negotiatedMtu = 23  // MTU por defecto BLE (20 bytes útiles = 23 - 3 ATT header)

    // Buffer para recibir datos fragmentados del ESP32 vía notificaciones TX
    private val rxBuffer = StringBuilder()

    private val dispositivosMap = mutableMapOf<String, DispositivoBluetooth>()

    // ═══════════════════════════════════════════════════
    // BLE GATT Callback (el corazón de la conexión BLE)
    // ═══════════════════════════════════════════════════
    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "✅ GATT conectado a ${gatt.device.address}. Negociando MTU...")
                    // Paso 1: Negociar MTU grande (como nRF Connect) para enviar payload completo en una sola escritura
                    gatt.requestMtu(512)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "❌ GATT desconectado de ${gatt.device.address}")
                    nusRxCharacteristic = null
                    bluetoothGatt = null
                    negotiatedMtu = 23
                    _estadoConexion.value = EstadoConexion.Desconectado
                    _telemetria.value = _telemetria.value.copy(conectado = false)
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                negotiatedMtu = mtu
                Log.i(TAG, "✅ MTU negociado: $mtu bytes (${mtu - 3} útiles)")
            } else {
                Log.w(TAG, "MTU negociación falló, usando default 23")
            }
            // Paso 2: Descubrir servicios después de negociar MTU
            gatt.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Error descubriendo servicios GATT: status=$status")
                _estadoConexion.value = EstadoConexion.Error("Error descubriendo servicios (status=$status)")
                return
            }

            val nusService = gatt.getService(NUS_SERVICE_UUID)
            if (nusService == null) {
                Log.e(TAG, "Nordic UART Service NO encontrado en el dispositivo")
                _estadoConexion.value = EstadoConexion.Error("Servicio NUS no encontrado en la pulsera")
                return
            }

            Log.i(TAG, "✅ Nordic UART Service encontrado!")

            // Obtener la Characteristic RX para escribir datos al ESP32
            nusRxCharacteristic = nusService.getCharacteristic(NUS_RX_CHAR_UUID)
            if (nusRxCharacteristic == null) {
                Log.e(TAG, "RX Characteristic no encontrada")
                _estadoConexion.value = EstadoConexion.Error("RX Characteristic no encontrada")
                return
            }
            Log.i(TAG, "✅ RX Characteristic lista para escritura")

            // Suscribirse a notificaciones de la TX Characteristic (datos del ESP32)
            val txChar = nusService.getCharacteristic(NUS_TX_CHAR_UUID)
            if (txChar != null) {
                gatt.setCharacteristicNotification(txChar, true)
                val descriptor = txChar.getDescriptor(CCCD_UUID)
                if (descriptor != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    } else {
                        @Suppress("DEPRECATION")
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        @Suppress("DEPRECATION")
                        gatt.writeDescriptor(descriptor)
                    }
                    Log.i(TAG, "✅ Notificaciones TX habilitadas")
                }
            }

            // ¡Conexión BLE GATT exitosa!
            _estadoConexion.value = EstadoConexion.Conectado(connectedDeviceName, connectedDeviceMac)
            _telemetria.value = _telemetria.value.copy(conectado = true)
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            // API < 33 callback
            if (characteristic.uuid == NUS_TX_CHAR_UUID) {
                @Suppress("DEPRECATION")
                val data = characteristic.value?.toString(Charsets.UTF_8) ?: return
                procesarDatosRecibidos(data)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            // API >= 33 callback
            if (characteristic.uuid == NUS_TX_CHAR_UUID) {
                val data = value.toString(Charsets.UTF_8)
                procesarDatosRecibidos(data)
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "✅ Escritura exitosa en RX Characteristic")
            } else {
                Log.e(TAG, "❌ Error escritura GATT: status=$status")
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // BLE Scan Callback
    // ═══════════════════════════════════════════════════
    private val bleScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val nombre = result.scanRecord?.deviceName ?: device.name

            // Solo mostrar dispositivos con nombre válido (igual que los ajustes de Bluetooth del teléfono)
            if (nombre.isNullOrBlank() || nombre.trim().isEmpty() || nombre.equals("null", ignoreCase = true)) {
                return
            }

            val mac = device.address
            val rssi = result.rssi

            dispositivosMap[mac] = DispositivoBluetooth(
                nombre = nombre.trim(),
                macAddress = mac,
                rssi = rssi,
                isSafeBand = true
            )

            _dispositivosEncontrados.value = dispositivosMap.values.toList()
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Error escaneo BLE: $errorCode")
        }
    }

    // ═══════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════

    fun isBluetoothHabilitado() = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun iniciarEscaneoBle() {
        if (!isBluetoothHabilitado()) return

        detenerEscaneoBle()
        dispositivosMap.clear()

        // 1. Cargar dispositivos con nombre vinculados/emparejados en el sistema Android
        try {
            bluetoothAdapter?.bondedDevices?.forEach { device ->
                val nombre = device.name
                if (!nombre.isNullOrBlank() && !nombre.equals("null", ignoreCase = true)) {
                    val mac = device.address
                    dispositivosMap[mac] = DispositivoBluetooth(
                        nombre = nombre.trim(),
                        macAddress = mac,
                        rssi = -50,
                        isSafeBand = true
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo bondedDevices: ${e.message}")
        }

        _dispositivosEncontrados.value = dispositivosMap.values.toList()

        // 2. Escaneo BLE activo sin filtro de UUID para descubrir la pulsera
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner != null) {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            try {
                scanner.startScan(null, settings, bleScanCallback)
            } catch (e: Exception) {
                Log.e(TAG, "Error startScan BLE: ${e.message}")
            }
        }

        // 3. Descubrimiento Clásico adicional para detectar cualquier dispositivo cercano
        try {
            bluetoothAdapter?.startDiscovery()
        } catch (e: Exception) {
            Log.e(TAG, "Error startDiscovery: ${e.message}")
        }

        _estadoConexion.value = EstadoConexion.Escaneando
    }

    @SuppressLint("MissingPermission")
    fun detenerEscaneoBle() {
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(bleScanCallback)
            bluetoothAdapter?.cancelDiscovery()
        } catch (e: Exception) {
            Log.e(TAG, "Error deteniendo escaneo: ${e.message}")
        }

        if (_estadoConexion.value == EstadoConexion.Escaneando) {
            _estadoConexion.value = EstadoConexion.Desconectado
        }
    }

    /**
     * Conectar al dispositivo BLE vía GATT (como nRF Connect).
     * Usa connectGatt() → discoverServices() → encuentra Nordic UART Service.
     */
    @SuppressLint("MissingPermission")
    fun conectarDispositivo(macAddress: String) {
        detenerEscaneoBle()
        val device = bluetoothAdapter?.getRemoteDevice(macAddress) ?: return
        val nombre = device.name ?: "SafeBand"

        connectedDeviceName = nombre
        connectedDeviceMac = macAddress

        _estadoConexion.value = EstadoConexion.Conectando(nombre)

        // Cerrar conexión GATT previa si existe
        try { bluetoothGatt?.close() } catch (e: Exception) {}
        bluetoothGatt = null
        nusRxCharacteristic = null

        // Conectar vía BLE GATT con transport LE (como nRF Connect)
        bluetoothGatt = device.connectGatt(
            context,
            false,           // autoConnect = false para conexión directa rápida
            gattCallback,
            BluetoothDevice.TRANSPORT_LE  // Forzar transporte BLE
        )

        Log.i(TAG, "Conectando GATT a $nombre ($macAddress)...")
    }

    /**
     * Enviar un comando/datos al ESP32 escribiendo en la RX Characteristic del Nordic UART Service.
     * Exactamente como lo hace nRF Connect cuando escribes en la RX Characteristic.
     */
    @SuppressLint("MissingPermission")
    fun enviarComando(comando: String): Boolean {
        val gatt = bluetoothGatt
        val rxChar = nusRxCharacteristic

        if (gatt == null || rxChar == null) {
            Log.e(TAG, "No se pudo enviar: GATT=$gatt, RX Characteristic=$rxChar")
            return false
        }

        val bytes = comando.toByteArray(Charsets.UTF_8)
        val maxPayload = negotiatedMtu - 3  // 3 bytes de ATT header

        Log.d(TAG, "Enviando ${bytes.size} bytes (MTU=$negotiatedMtu, max payload=$maxPayload)")

        // Con MTU negociado (512), el payload cabe en una sola escritura completa (como nRF Connect)
        val sent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(rxChar, bytes, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            rxChar.value = bytes
            @Suppress("DEPRECATION")
            rxChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(rxChar)
        }

        if (sent) {
            Log.d(TAG, "✅ Payload completo enviado al ESP32: $comando")
        } else {
            Log.e(TAG, "❌ Error enviando payload al ESP32")
        }

        return sent
    }

    @SuppressLint("MissingPermission")
    fun desconectar() {
        readJob?.cancel()
        detenerEscaneoBle()
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error desconectando GATT: ${e.message}")
        }
        bluetoothGatt = null
        nusRxCharacteristic = null
        _estadoConexion.value = EstadoConexion.Desconectado
        _telemetria.value = DatosTelemetriaBle(conectado = false)
    }

    // ═══════════════════════════════════════════════════
    // Procesamiento de datos recibidos del ESP32 vía TX Notify
    // ═══════════════════════════════════════════════════
    private fun procesarDatosRecibidos(data: String) {
        rxBuffer.append(data)
        val bufferStr = rxBuffer.toString()

        // Intentar parsear JSON completo del buffer
        if (bufferStr.contains("{") && bufferStr.contains("}")) {
            try {
                val jsonStr = bufferStr.substring(bufferStr.indexOf("{"), bufferStr.lastIndexOf("}") + 1)
                val json = JSONObject(jsonStr)
                _telemetria.value = _telemetria.value.copy(
                    temperatura = json.optDouble("temp", _telemetria.value.temperatura?.toDouble() ?: 0.0).toFloat(),
                    humedad = json.optDouble("hum", _telemetria.value.humedad?.toDouble() ?: 0.0).toFloat(),
                    conectado = true,
                    ultimoMensaje = "Datos actualizados"
                )
                rxBuffer.clear()
            } catch (e: Exception) {
                Log.e(TAG, "Error parseo datos BLE: ${e.message}")
                rxBuffer.clear()
            }
        }
    }
}
