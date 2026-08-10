package com.safestep.appband.repositories

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repositorio que escucha en tiempo real el documento "sensor/actual" de Firestore.
 * El ESP32 SafeBand envía datos vía Firebase.Firestore.patchDocument() a este documento.
 *
 * Estructura del documento en Firestore:
 *   sensor/actual
 *     ├── max30102 { bpm, spo2, bpmValido, spo2Valido, dedo, ir, red }
 *     ├── bateria  { voltaje, porcentaje }
 *     ├── gps      { fix, latitud, longitud, altitud, velocidad, satelites }
 *     ├── mpu6500  { acelerometro{x,y,z}, giroscopio{x,y,z}, resultante, g, caida }
 *     ├── alerta   { sos, caida }
 *     └── json     (string del JSON completo)
 */
class SensorFirebaseRepository {

    companion object {
        private const val TAG = "SensorFirebaseRepo"
        private const val COLECCION = "sensor"
        private const val DOCUMENTO = "actual"

        @Volatile
        private var INSTANCE: SensorFirebaseRepository? = null

        fun getInstance(): SensorFirebaseRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SensorFirebaseRepository().also { INSTANCE = it }
            }
        }
    }

    /**
     * Datos completos del sensor leídos desde Firestore.
     */
    data class DatosSensor(
        // MAX30102
        val bpm: Int = 0,
        val bpmValido: Boolean = false,
        val spo2: Int = 0,
        val spo2Valido: Boolean = false,
        val dedo: Boolean = false,
        val ir: Long = 0,
        val red: Long = 0,
        // Batería
        val bateriaPorcentaje: Int = 0,
        val bateriaVoltaje: Double = 0.0,
        // GPS
        val gpsFix: Boolean = false,
        val gpsLatitud: Double = 0.0,
        val gpsLongitud: Double = 0.0,
        val gpsAltitud: Double = 0.0,
        val gpsVelocidad: Double = 0.0,
        val gpsSatelites: Int = 0,
        // Alertas
        val alertaSOS: Boolean = false,
        val alertaCaida: Boolean = false,
        // Estado
        val hayDatos: Boolean = false,
        val ultimaActualizacion: Long = 0
    )

    private val _datosSensor = MutableStateFlow(DatosSensor())
    val datosSensor: StateFlow<DatosSensor> = _datosSensor

    private val _conectadoFirebase = MutableStateFlow(false)
    val conectadoFirebase: StateFlow<Boolean> = _conectadoFirebase

    private var listenerRegistration: ListenerRegistration? = null

    /**
     * Iniciar la escucha en tiempo real del documento "sensor/actual" en Firestore.
     * Cada vez que el ESP32 actualiza los valores (cada 10 segundos), se reciben aquí.
     */
    fun iniciarEscucha() {
        val firestore = FirebaseFirestore.getInstance()
        val docRef = firestore.collection(COLECCION).document(DOCUMENTO)

        // Remover listener anterior si existe
        listenerRegistration?.remove()

        listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "❌ Error escuchando Firestore: ${error.message}")
                _conectadoFirebase.value = false
                return@addSnapshotListener
            }

            if (snapshot == null || !snapshot.exists()) {
                Log.w(TAG, "⚠️ Documento sensor/actual no existe todavía")
                _conectadoFirebase.value = true
                return@addSnapshotListener
            }

            try {
                val datos = parsearDatos(snapshot)
                _datosSensor.value = datos
                _conectadoFirebase.value = true

                Log.d(TAG, "✅ Datos Firestore: BPM=${datos.bpm} (válido=${datos.bpmValido}), " +
                        "SpO2=${datos.spo2} (válido=${datos.spo2Valido}), Dedo=${datos.dedo}, " +
                        "Batería=${datos.bateriaPorcentaje}%, GPS fix=${datos.gpsFix}")
            } catch (e: Exception) {
                Log.e(TAG, "Error parseando datos de Firestore: ${e.message}")
            }
        }

        Log.i(TAG, "📡 Escuchando documento $COLECCION/$DOCUMENTO en Firestore...")
    }

    /**
     * Parsear el snapshot de Firestore a DatosSensor.
     * Los datos están organizados en maps anidados: max30102, bateria, gps, mpu6500, alerta.
     */
    @Suppress("UNCHECKED_CAST")
    private fun parsearDatos(snapshot: DocumentSnapshot): DatosSensor {
        // MAX30102
        val max30102 = snapshot.get("max30102") as? Map<String, Any> ?: emptyMap()
        val bpm = toLong(max30102["bpm"]).toInt()
        val spo2 = toLong(max30102["spo2"]).toInt()
        val bpmValido = max30102["bpmValido"] as? Boolean ?: false
        val spo2Valido = max30102["spo2Valido"] as? Boolean ?: false
        val dedo = max30102["dedo"] as? Boolean ?: false
        val ir = toLong(max30102["ir"])
        val red = toLong(max30102["red"])

        // Batería
        val bateria = snapshot.get("bateria") as? Map<String, Any> ?: emptyMap()
        val bateriaPorcentaje = toLong(bateria["porcentaje"]).toInt()
        val bateriaVoltaje = toDouble(bateria["voltaje"])

        // GPS
        val gps = snapshot.get("gps") as? Map<String, Any> ?: emptyMap()
        val gpsFix = gps["fix"] as? Boolean ?: false
        val gpsLatitud = toDouble(gps["latitud"])
        val gpsLongitud = toDouble(gps["longitud"])
        val gpsAltitud = toDouble(gps["altitud"])
        val gpsVelocidad = toDouble(gps["velocidad"])
        val gpsSatelites = toLong(gps["satelites"]).toInt()

        // Alertas
        val alerta = snapshot.get("alerta") as? Map<String, Any> ?: emptyMap()
        val alertaSOS = alerta["sos"] as? Boolean ?: false
        val alertaCaida = alerta["caida"] as? Boolean ?: false

        return DatosSensor(
            bpm = bpm,
            bpmValido = bpmValido,
            spo2 = spo2,
            spo2Valido = spo2Valido,
            dedo = dedo,
            ir = ir,
            red = red,
            bateriaPorcentaje = bateriaPorcentaje,
            bateriaVoltaje = bateriaVoltaje,
            gpsFix = gpsFix,
            gpsLatitud = gpsLatitud,
            gpsLongitud = gpsLongitud,
            gpsAltitud = gpsAltitud,
            gpsVelocidad = gpsVelocidad,
            gpsSatelites = gpsSatelites,
            alertaSOS = alertaSOS,
            alertaCaida = alertaCaida,
            hayDatos = true,
            ultimaActualizacion = System.currentTimeMillis()
        )
    }

    /** Helper: convierte cualquier Number a Long de forma segura */
    private fun toLong(value: Any?): Long {
        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            is Double -> value.toLong()
            is String -> value.toLongOrNull() ?: 0L
            else -> 0L
        }
    }

    /** Helper: convierte cualquier Number a Double de forma segura */
    private fun toDouble(value: Any?): Double {
        return when (value) {
            is Double -> value
            is Long -> value.toDouble()
            is Int -> value.toDouble()
            is Float -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    /**
     * Detener la escucha de datos del sensor.
     */
    fun detenerEscucha() {
        listenerRegistration?.remove()
        listenerRegistration = null
        _conectadoFirebase.value = false
    }
}
