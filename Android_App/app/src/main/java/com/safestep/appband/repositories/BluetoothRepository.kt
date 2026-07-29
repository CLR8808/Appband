package com.safestep.appband.repositories

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.util.Log
import com.safestep.appband.models.DispositivoBluetooth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Repositorio para la gestión de conectividad Bluetooth LE con la pulsera SafeBand / ESP32.
 * Permite escaneo de dispositivos BLE, conexión GATT, envío de credenciales Wi-Fi sobre BLE,
 * y recepción de telemetría de signos vitales (pulso).
 */
class BluetoothRepository(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothRepository"

        // UUIDs estándar de servicio y características para SafeBand / ESP32 BLE
        val SERVICE_UUID: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val WIFI_CONFIG_CHAR_UUID: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        val VITAL_SIGNS_CHAR_UUID: UUID = UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    // Estado del escaneo y dispositivos encontrados
    private val _dispositivosEncontrados = MutableStateFlow<List<DispositivoBluetooth>>(emptyList())
    val dispositivosEncontrados: StateFlow<List<DispositivoBluetooth>> = _dispositivosEncontrados

    // Estado de conexión BLE
    sealed class EstadoConexionBle {
        object Desconectado : EstadoConexionBle()
        object Escaneando : EstadoConexionBle()
        data class Conectando(val nombre: String) : EstadoConexionBle()
        data class Conectado(val dispositivo: DispositivoBluetooth) : EstadoConexionBle()
        data class Error(val mensaje: String) : EstadoConexionBle()
    }

    private val _estadoConexion = MutableStateFlow<EstadoConexionBle>(EstadoConexionBle.Desconectado)
    val estadoConexion: StateFlow<EstadoConexionBle> = _estadoConexion

    // Pulso actual recibido por BLE
    private val _pulsoActual = MutableStateFlow<Int?>(null)
    val pulsoActual: StateFlow<Int?> = _pulsoActual

    private var bluetoothGatt: BluetoothGatt? = null
    private var isScanning = false
    private val dispositivosMap = mutableMapOf<String, DispositivoBluetooth>()

    fun isBluetoothHabilitado(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    fun iniciarEscaneoBle() {
        if (!isBluetoothHabilitado()) {
            _estadoConexion.value = EstadoConexionBle.Error("El Bluetooth no está activado")
            return
        }

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            _estadoConexion.value = EstadoConexionBle.Error("Escáner BLE no disponible en este dispositivo")
            return
        }

        dispositivosMap.clear()
        _dispositivosEncontrados.value = emptyList()
        _estadoConexion.value = EstadoConexionBle.Escaneando
        isScanning = true

        try {
            scanner.startScan(scanCallback)
            Log.d(TAG, "Escaneo BLE iniciado")
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar escaneo BLE: ${e.message}")
            _estadoConexion.value = EstadoConexionBle.Error("Error al iniciar escaneo: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun detenerEscaneoBle() {
        if (isScanning) {
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
                isScanning = false
                if (_estadoConexion.value is EstadoConexionBle.Escaneando) {
                    _estadoConexion.value = EstadoConexionBle.Desconectado
                }
                Log.d(TAG, "Escaneo BLE detenido")
            } catch (e: Exception) {
                Log.e(TAG, "Error al detener escaneo: ${e.message}")
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                val nombre = device.name ?: "SafeBand BLE (${device.address.takeLast(5)})"
                val mac = device.address
                val rssi = result.rssi
                val isSafeBand = nombre.contains("SafeBand", ignoreCase = true) ||
                        nombre.contains("ESP32", ignoreCase = true) ||
                        nombre.contains("Band", ignoreCase = true)

                val item = DispositivoBluetooth(
                    nombre = nombre,
                    macAddress = mac,
                    rssi = rssi,
                    isSafeBand = isSafeBand
                )
                dispositivosMap[mac] = item
                _dispositivosEncontrados.value = dispositivosMap.values.sortedByDescending { it.rssi }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Escaneo BLE falló con código: $errorCode")
            _estadoConexion.value = EstadoConexionBle.Error("Error de escaneo BLE ($errorCode)")
            isScanning = false
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
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
            Log.d(TAG, "Conectando GATT a $macAddress")
        } catch (e: Exception) {
            Log.e(TAG, "Error al conectar GATT: ${e.message}")
            _estadoConexion.value = EstadoConexionBle.Error("Error al conectar: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun desconectar() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            _estadoConexion.value = EstadoConexionBle.Desconectado
            _pulsoActual.value = null
            Log.d(TAG, "Desconectado de Bluetooth BLE")
        } catch (e: Exception) {
            Log.e(TAG, "Error al desconectar: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun enviarConfiguracionWifiOverBle(ssid: String, pass: String): Boolean {
        val gatt = bluetoothGatt ?: return false
        val service = gatt.getService(SERVICE_UUID) ?: return false
        val characteristic = service.getCharacteristic(WIFI_CONFIG_CHAR_UUID) ?: return false

        val payload = "WIFI:$ssid:$pass"
        characteristic.value = payload.toByteArray(Charsets.UTF_8)
        return gatt.writeCharacteristic(characteristic)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "GATT Conectado. Descubriendo servicios...")
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "GATT Desconectado")
                _estadoConexion.value = EstadoConexionBle.Desconectado
                bluetoothGatt?.close()
                bluetoothGatt = null
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                val dev = DispositivoBluetooth(
                    nombre = gatt.device.name ?: "SafeBand BLE",
                    macAddress = gatt.device.address,
                    conectado = true,
                    isSafeBand = true
                )
                _estadoConexion.value = EstadoConexionBle.Conectado(dev)
                Log.d(TAG, "Servicios descubiertos exitosamente. Conexión completa.")

                // Suscribir a notificaciones de pulso/telemetría si existe la característica
                val service = gatt.getService(SERVICE_UUID)
                val charVitals = service?.getCharacteristic(VITAL_SIGNS_CHAR_UUID)
                if (charVitals != null) {
                    gatt.setCharacteristicNotification(charVitals, true)
                }
            } else {
                _estadoConexion.value = EstadoConexionBle.Error("Fallo al descubrir servicios BLE")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            if (characteristic?.uuid == VITAL_SIGNS_CHAR_UUID) {
                val data = characteristic.value?.toString(Charsets.UTF_8) ?: return
                // Formato esperado: "BPM:78" o "78"
                val bpm = data.replace("BPM:", "").trim().toIntOrNull()
                if (bpm != null) {
                    _pulsoActual.value = bpm
                    Log.d(TAG, "BPM recibido por BLE: $bpm")
                }
            }
        }
    }
}
