package com.safestep.appband.repositories

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Repositorio Singleton para Bluetooth Clásico (SPP).
 * Permite compartir la conexión entre diferentes pantallas (Fragments/ViewModels).
 */
class BluetoothRepository private constructor(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothRepoClassic"
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

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

    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var readJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isReceiverRegistered = false

    private val dispositivosMap = mutableMapOf<String, DispositivoBluetooth>()

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothDevice.ACTION_FOUND) {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }

                device?.let {
                    val mac = it.address
                    val nombre = it.name ?: "Dispositivo Desconocido"
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()

                    dispositivosMap[mac] = DispositivoBluetooth(
                        nombre = nombre,
                        macAddress = mac,
                        rssi = rssi,
                        isSafeBand = true
                    )
                    _dispositivosEncontrados.value = dispositivosMap.values.toList()
                }
            }
        }
    }

    fun isBluetoothHabilitado() = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun iniciarEscaneoBle() {
        if (!isBluetoothHabilitado()) return
        detenerEscaneoBle()
        dispositivosMap.clear()

        bluetoothAdapter?.bondedDevices?.forEach { device ->
            dispositivosMap[device.address] = DispositivoBluetooth(
                nombre = device.name ?: "Vinculado",
                macAddress = device.address,
                rssi = 0,
                isSafeBand = true
            )
        }
        _dispositivosEncontrados.value = dispositivosMap.values.toList()

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(receiver, filter)
        isReceiverRegistered = true

        bluetoothAdapter?.startDiscovery()
        _estadoConexion.value = EstadoConexion.Escaneando
    }

    @SuppressLint("MissingPermission")
    fun detenerEscaneoBle() {
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter.cancelDiscovery()
        }
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Log.e(TAG, "Error al desregistrar: ${e.message}")
            }
            isReceiverRegistered = false
        }
    }

    @SuppressLint("MissingPermission")
    fun conectarDispositivo(macAddress: String) {
        detenerEscaneoBle()
        val device = bluetoothAdapter?.getRemoteDevice(macAddress) ?: return

        scope.launch {
            try {
                _estadoConexion.value = EstadoConexion.Conectando(device.name ?: macAddress)
                bluetoothSocket?.close()
                bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                bluetoothSocket?.connect()

                outputStream = bluetoothSocket?.outputStream
                inputStream = bluetoothSocket?.inputStream

                _estadoConexion.value = EstadoConexion.Conectado(device.name ?: "SafeBand", macAddress)
                iniciarLectura()
            } catch (e: IOException) {
                _estadoConexion.value = EstadoConexion.Error("Error: ${e.message}")
                desconectar()
            }
        }
    }

    private fun iniciarLectura() {
        readJob?.cancel()
        readJob = scope.launch {
            val buffer = ByteArray(1024)
            while (true) {
                try {
                    val bytes = inputStream?.read(buffer) ?: -1
                    if (bytes > 0) {
                        procesarDatosRecibidos(String(buffer, 0, bytes))
                    }
                } catch (e: IOException) {
                    _estadoConexion.value = EstadoConexion.Desconectado
                    break
                }
            }
        }
    }

    private fun procesarDatosRecibidos(data: String) {
        try {
            if (data.contains("{")) {
                val json = JSONObject(data.substring(data.indexOf("{"), data.lastIndexOf("}") + 1))
                _telemetria.value = _telemetria.value.copy(
                    temperatura = json.optDouble("temp", _telemetria.value.temperatura?.toDouble() ?: 0.0).toFloat(),
                    humedad = json.optDouble("hum", _telemetria.value.humedad?.toDouble() ?: 0.0).toFloat(),
                    conectado = true,
                    ultimoMensaje = "Datos actualizados"
                )
            } else {
                val temp = "T:([0-9.]+)".toRegex().find(data)?.groupValues?.get(1)?.toFloatOrNull()
                val hum = "H:([0-9.]+)".toRegex().find(data)?.groupValues?.get(1)?.toFloatOrNull()
                if (temp != null || hum != null) {
                    _telemetria.value = _telemetria.value.copy(
                        temperatura = temp ?: _telemetria.value.temperatura,
                        humedad = hum ?: _telemetria.value.humedad,
                        conectado = true,
                        ultimoMensaje = "Lectura DHT11 OK"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parseo: ${e.message}")
        }
    }

    fun enviarComando(comando: String): Boolean {
        return try {
            outputStream?.write(comando.toByteArray())
            true
        } catch (e: IOException) {
            false
        }
    }

    fun desconectar() {
        readJob?.cancel()
        detenerEscaneoBle()
        try { bluetoothSocket?.close() } catch (e: Exception) {}
        bluetoothSocket = null
        _estadoConexion.value = EstadoConexion.Desconectado
        _telemetria.value = DatosTelemetriaBle(conectado = false)
    }
}
