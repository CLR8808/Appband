package com.safestep.appband.repositories

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.safestep.appband.models.DatosTelemetriaBle
import com.safestep.appband.models.DispositivoBluetooth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.util.UUID

/**
 * Repositorio nativo de conectividad Bluetooth LE real con la pulsera SafeBand / ESP32.
 * Maneja escaneo activo BLE LOW_LATENCY, parseo de bytes GAP raw, conexiones GATT (TRANSPORT_LE),
 * suscripción CCCD (0x2902) y envío de comandos/Wi-Fi sobre BLE.
 */
class BluetoothRepository(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothRepository"
        private const val SCAN_PERIOD_MS = 15000L
        private const val PUBLISH_INTERVAL_MS = 500L

        val CCCD_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val SERVICE_UUID: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val WIFI_CONFIG_CHAR_UUID: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        val VITAL_SIGNS_CHAR_UUID: UUID = UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter
    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val mainHandler = Handler(Looper.getMainLooper())

    // Estado del escaneo y dispositivos encontrados
    private val _dispositivosEncontrados = MutableStateFlow<List<DispositivoBluetooth>>(emptyList())
    val dispositivosEncontrados: StateFlow<List<DispositivoBluetooth>> = _dispositivosEncontrados

    // Estado de conexión BLE
    sealed class EstadoConexionBle {
        object Desconectado : EstadoConexionBle()
        object Escaneando : EstadoConexionBle()
        data class Conectando(val nombre: String) : EstadoConexionBle()
        data class Conectado(val dispositivo: DispositivoBluetooth, val serviciosCount: Int) : EstadoConexionBle()
        data class Error(val mensaje: String) : EstadoConexionBle()
    }

    private val _estadoConexion = MutableStateFlow<EstadoConexionBle>(EstadoConexionBle.Desconectado)
    val estadoConexion: StateFlow<EstadoConexionBle> = _estadoConexion

    private val _pulsoActual = MutableStateFlow<Int?>(null)
    val pulsoActual: StateFlow<Int?> = _pulsoActual

    // Telemetría continua en tiempo real recibida por BLE
    private val _telemetria = MutableStateFlow(DatosTelemetriaBle())
    val telemetria: StateFlow<DatosTelemetriaBle> = _telemetria

    private var bluetoothGatt: BluetoothGatt? = null
    private var isScanning = false
    private var isDirty = false
    private val dispositivosMap = mutableMapOf<String, DispositivoBluetooth>()

    private val publishRunnable = object : Runnable {
        override fun run() {
            if (isDirty) {
                synchronized(dispositivosMap) {
                    _dispositivosEncontrados.value = dispositivosMap.values.sortedWith(
                        compareByDescending<DispositivoBluetooth> { it.isSafeBand }
                            .thenByDescending { it.rssi }
                    )
                }
                isDirty = false
            }
            if (isScanning) {
                mainHandler.postDelayed(this, PUBLISH_INTERVAL_MS)
            }
        }
    }

    private val stopScanRunnable = Runnable {
        detenerEscaneoBle()
    }

    fun isBluetoothHabilitado(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun isUbicacionHabilitada(): Boolean {
        val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        val isNetworkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false
        return isGpsEnabled || isNetworkEnabled
    }

    @SuppressLint("MissingPermission")
    fun iniciarEscaneoBle() {
        if (!isBluetoothHabilitado()) {
            _estadoConexion.value = EstadoConexionBle.Error("El Bluetooth está desactivado en el teléfono.")
            return
        }

        if (!isUbicacionHabilitada()) {
            _estadoConexion.value = EstadoConexionBle.Error("Activa la Ubicación (GPS). Android la exige para conectar a dispositivos BLE.")
            return
        }

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            _estadoConexion.value = EstadoConexionBle.Error("El escáner BLE no está disponible")
            return
        }

        dispositivosMap.clear()
        _dispositivosEncontrados.value = emptyList()
        _estadoConexion.value = EstadoConexionBle.Escaneando
        isScanning = true
        isDirty = false

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        try {
            scanner.startScan(null, settings, scanCallback)
            Log.d(TAG, "Escaneo BLE iniciado")

            mainHandler.post(publishRunnable)
            mainHandler.postDelayed(stopScanRunnable, SCAN_PERIOD_MS)

        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar escaneo BLE: ${e.message}")
            _estadoConexion.value = EstadoConexionBle.Error("Error al iniciar escaneo: ${e.message}")
            isScanning = false
        }
    }

    @SuppressLint("MissingPermission")
    fun detenerEscaneoBle() {
        if (isScanning) {
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
                isScanning = false
                mainHandler.removeCallbacks(publishRunnable)
                mainHandler.removeCallbacks(stopScanRunnable)

                synchronized(dispositivosMap) {
                    _dispositivosEncontrados.value = dispositivosMap.values.sortedWith(
                        compareByDescending<DispositivoBluetooth> { it.isSafeBand }
                            .thenByDescending { it.rssi }
                    )
                }

                if (_estadoConexion.value is EstadoConexionBle.Escaneando) {
                    _estadoConexion.value = EstadoConexionBle.Desconectado
                }
                Log.d(TAG, "Escaneo BLE detenido")
            } catch (e: Exception) {
                Log.e(TAG, "Error al detener escaneo: ${e.message}")
            }
        }
    }

    private fun parseBleLocalName(bytes: ByteArray?): String? {
        if (bytes == null) return null
        var index = 0
        while (index < bytes.size) {
            val length = bytes[index].toInt() and 0xFF
            if (length == 0) break
            if (index + length >= bytes.size) break

            val type = bytes[index + 1].toInt() and 0xFF
            if (type == 0x09 || type == 0x08) {
                val nameBytes = bytes.copyOfRange(index + 2, index + 1 + length)
                val name = String(nameBytes, Charsets.UTF_8).trim()
                if (name.isNotBlank()) return name
            }
            index += length + 1
        }
        return null
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return
            val mac = device.address ?: return
            val rssi = result.rssi

            val devName = device.name
            val scanRecordName = result.scanRecord?.deviceName
            val rawBytesName = parseBleLocalName(result.scanRecord?.bytes)

            val rawName = devName ?: scanRecordName ?: rawBytesName ?: ""
            val nombre = if (rawName.isNotBlank()) rawName else "Dispositivo BLE (${mac.takeLast(5)})"

            val isSafeBand = nombre.contains("SafeBand", ignoreCase = true) ||
                    nombre.contains("ESP32", ignoreCase = true) ||
                    nombre.contains("Band", ignoreCase = true) ||
                    nombre.contains("Safe", ignoreCase = true)

            val item = DispositivoBluetooth(
                nombre = nombre,
                macAddress = mac,
                rssi = rssi,
                isSafeBand = isSafeBand
            )

            synchronized(dispositivosMap) {
                dispositivosMap[mac] = item
                isDirty = true
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Escaneo BLE falló con código: $errorCode")
            _estadoConexion.value = EstadoConexionBle.Error("Error de escaneo BLE ($errorCode)")
            isScanning = false
            mainHandler.removeCallbacks(publishRunnable)
            mainHandler.removeCallbacks(stopScanRunnable)
        }
    }

    @SuppressLint("MissingPermission")
    fun conectarDispositivo(macAddress: String) {
        detenerEscaneoBle()
        val device = bluetoothAdapter?.getRemoteDevice(macAddress)
        if (device == null) {
            _estadoConexion.value = EstadoConexionBle.Error("Dispositivo no encontrado ($macAddress)")
            return
        }

        val nombre = dispositivosMap[macAddress]?.nombre ?: "SafeBand"
        _estadoConexion.value = EstadoConexionBle.Conectando(nombre)

        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null

            bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }

            Log.d(TAG, "Iniciando conexión GATT REAL a $macAddress ($nombre)")
        } catch (e: Exception) {
            Log.e(TAG, "Error al conectar GATT: ${e.message}")
            _estadoConexion.value = EstadoConexionBle.Error("Error de conexión: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun desconectar() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            _estadoConexion.value = EstadoConexionBle.Desconectado
            _telemetria.value = DatosTelemetriaBle(conectado = false)
            Log.d(TAG, "Desconectado de Bluetooth BLE")
        } catch (e: Exception) {
            Log.e(TAG, "Error al desconectar: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun enviarComando(comando: String): Boolean {
        val gatt = bluetoothGatt ?: return false
        val services = gatt.services ?: return false

        var targetChar: BluetoothGattCharacteristic? = null
        for (service in services) {
            for (char in service.characteristics) {
                val props = char.properties
                if ((props and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) ||
                    (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0)) {
                    targetChar = char
                    break
                }
            }
            if (targetChar != null) break
        }

        if (targetChar == null) {
            Log.e(TAG, "No se encontró característica GATT con permiso de escritura")
            return false
        }

        targetChar.value = comando.toByteArray(Charsets.UTF_8)
        val success = gatt.writeCharacteristic(targetChar)
        Log.d(TAG, "Envío de comando BLE '$comando': $success")
        return success
    }

    fun enviarConfiguracionWifiOverBle(ssid: String, pass: String): Boolean {
        return enviarComando("WIFI:$ssid:$pass")
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Error de estado de conexión GATT (código $status)")
                _estadoConexion.value = EstadoConexionBle.Error("Error de conexión GATT (código $status). Reintenta.")
                try { gatt?.close() } catch (_: Exception) {}
                bluetoothGatt = null
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "GATT Conectado. Descubriendo servicios...")
                mainHandler.postDelayed({ gatt?.discoverServices() }, 350)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "GATT Desconectado")
                _estadoConexion.value = EstadoConexionBle.Desconectado
                _telemetria.value = _telemetria.value.copy(conectado = false)
                try { gatt?.close() } catch (_: Exception) {}
                bluetoothGatt = null
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                val numServicios = gatt.services?.size ?: 0
                val realName = gatt.device.name ?: "SafeBand BLE"
                val mac = gatt.device.address

                val dev = DispositivoBluetooth(
                    nombre = realName,
                    macAddress = mac,
                    conectado = true,
                    isSafeBand = true
                )

                _estadoConexion.value = EstadoConexionBle.Conectado(dev, numServicios)
                _telemetria.value = _telemetria.value.copy(
                    dispositivoNombre = realName,
                    macAddress = mac,
                    conectado = true,
                    serviciosCount = numServicios,
                    ultimoMensaje = "¡Conexión BLE 100% activa! Recibiendo telemetría..."
                )

                Log.d(TAG, "Conexión GATT 100% Real Establecida. Descubiertos $numServicios servicios.")

                for (service in gatt.services) {
                    for (char in service.characteristics) {
                        val props = char.properties
                        if ((props and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) ||
                            (props and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0)) {

                            gatt.setCharacteristicNotification(char, true)

                            val descriptor = char.getDescriptor(CCCD_DESCRIPTOR_UUID)
                            if (descriptor != null) {
                                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                gatt.writeDescriptor(descriptor)
                                Log.d(TAG, "Descriptor CCCD (0x2902) activado para ${char.uuid}")
                            }
                        }
                    }
                }
            } else {
                Log.e(TAG, "Fallo al descubrir servicios GATT ($status)")
                _estadoConexion.value = EstadoConexionBle.Error("Conectado pero falló lectura de servicios ($status)")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            val bytes = characteristic?.value ?: return
            val payloadText = bytes.toString(Charsets.UTF_8).trim()
            Log.d(TAG, "Datos BLE recibidos desde ${characteristic.uuid}: $payloadText")

            procesarPayloadBle(bytes, payloadText)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val bytes = characteristic?.value ?: return
                val payloadText = bytes.toString(Charsets.UTF_8).trim()
                procesarPayloadBle(bytes, payloadText)
            }
        }
    }

    private fun procesarPayloadBle(bytes: ByteArray, textPayload: String) {
        var bpmEncontrado: Int? = null
        var batEncontrado: Int? = null
        var alerta: String? = null

        try {
            if (textPayload.startsWith("{") && textPayload.endsWith("}")) {
                val json = JSONObject(textPayload)
                if (json.has("bpm")) bpmEncontrado = json.getInt("bpm")
                if (json.has("pulse")) bpmEncontrado = json.getInt("pulse")
                if (json.has("bateria")) batEncontrado = json.getInt("bateria")
                if (json.has("battery")) batEncontrado = json.getInt("battery")
                if (json.has("alerta")) alerta = json.getString("alerta")
            }
        } catch (_: Exception) {}

        if (bpmEncontrado == null && textPayload.contains("BPM", ignoreCase = true)) {
            bpmEncontrado = textPayload.replace("BPM:", "").replace("BPM", "").trim().toIntOrNull()
        }
        if (bpmEncontrado == null && textPayload.toIntOrNull() != null) {
            bpmEncontrado = textPayload.toInt()
        }

        if (bpmEncontrado == null && bytes.size >= 2) {
            val flags = bytes[0].toInt()
            val is16Bit = (flags and 0x01) != 0
            bpmEncontrado = if (is16Bit && bytes.size >= 3) {
                (bytes[1].toInt() and 0xFF) or ((bytes[2].toInt() and 0xFF) shl 8)
            } else {
                bytes[1].toInt() and 0xFF
            }
            if (bpmEncontrado <= 0 || bpmEncontrado > 250) bpmEncontrado = null
        }

        if (textPayload.contains("CAIDA", ignoreCase = true) || textPayload.contains("FALL", ignoreCase = true)) {
            alerta = "⚠️ ¡Caída Detectada!"
        } else if (textPayload.contains("SOS", ignoreCase = true) || textPayload.contains("EMERGENCIA", ignoreCase = true)) {
            alerta = "🚨 ¡Alerta de Emergencia SOS!"
        }

        if (bpmEncontrado != null) {
            _pulsoActual.value = bpmEncontrado
        }

        val actual = _telemetria.value
        _telemetria.value = actual.copy(
            pulsoBpm = bpmEncontrado ?: actual.pulsoBpm,
            bateriaPorcentaje = batEncontrado ?: actual.bateriaPorcentaje,
            alertaActiva = alerta ?: actual.alertaActiva,
            ultimoMensaje = textPayload.ifBlank { "Datos recibidos (${bytes.size} bytes)" },
            timestampMs = System.currentTimeMillis()
        )
    }
}
