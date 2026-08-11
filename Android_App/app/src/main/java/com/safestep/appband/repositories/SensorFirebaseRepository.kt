package com.safestep.appband.repositories

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repositorio que escucha en tiempo real el registro más reciente en la colección "sensor" de Firestore.
 * El ESP32 SafeBand sube periódicamente un nuevo documento (cada 30 segundos) con la telemetría del sensor.
 *
 * Estructura de documentos soportada (plana o anidada):
 *   sensor/registro_<millis>_<hex>
 *     ├── bateria: 54
 *     ├── bpm: 0
 *     ├── spo2: 94
 *     ├── fecha: "2026-08-10"
 *     ├── hora: "21:07:41"
 *     ├── fechaHora: "2026-08-10 21:07:41"
 *     ├── gpsFix: true
 *     ├── latitud: 23.2875
 *     ├── longitud: -106.417094
 *     ├── max30102Conectado: true
 *     └── caida: false
 */
class SensorFirebaseRepository {

    companion object {
        private const val TAG = "SensorFirebaseRepo"
        private const val COLECCION = "sensor"

        @Volatile
        private var INSTANCE: SensorFirebaseRepository? = null

        fun getInstance(): SensorFirebaseRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SensorFirebaseRepository().also { 
                    INSTANCE = it 
                    it.iniciarEscucha()
                }
            }
        }
    }

    /**
     * Datos completos del sensor leídos desde el registro más reciente en Firestore.
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
        // Fechas y hora desde el registro
        val horaTexto: String = "",
        val fechaTexto: String = "",
        val documentId: String = "",
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
     * Iniciar la escucha en tiempo real del registro más reciente en la colección "sensor".
     * Cada vez que el ESP32 o la base de datos registra un nuevo documento (cada 30 segundos),
     * este listener captura el documento superior y actualiza la app al instante.
     */
    fun iniciarEscucha() {
        val firestore = FirebaseFirestore.getInstance()

        // Remover listener anterior si existe
        listenerRegistration?.remove()

        // Consulta en tiempo real: Ordenar por ID de documento de manera descendente (los más recientes primero) y limitar a 1
        val query = firestore.collection(COLECCION)
            .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
            .limit(1)

        listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "❌ Error escuchando registros de Firestore: ${error.message}")
                _conectadoFirebase.value = false
                return@addSnapshotListener
            }

            if (snapshot == null || snapshot.isEmpty) {
                Log.w(TAG, "⚠️ La colección '$COLECCION' aún no contiene registros")
                _conectadoFirebase.value = true
                return@addSnapshotListener
            }

            val docMasReciente = snapshot.documents.firstOrNull()
            if (docMasReciente != null && docMasReciente.exists()) {
                try {
                    val datos = parsearDatos(docMasReciente)
                    _datosSensor.value = datos
                    _conectadoFirebase.value = true

                    Log.d(TAG, "✅ Registro más reciente en vivo (${docMasReciente.id}): Hora=${datos.horaTexto}, " +
                            "BPM=${datos.bpm}, SpO2=${datos.spo2}, Batería=${datos.bateriaPorcentaje}%")
                } catch (e: Exception) {
                    Log.e(TAG, "Error parseando datos de Firestore: ${e.message}")
                }
            }
        }

        Log.i(TAG, "📡 Escuchando registros más recientes en la colección '$COLECCION' de Firestore...")
    }

    /**
     * Parsear el snapshot del documento más reciente en Firestore a DatosSensor.
     */
    @Suppress("UNCHECKED_CAST")
    private fun parsearDatos(snapshot: DocumentSnapshot): DatosSensor {
        // MAX30102
        val max30102Map = snapshot.get("max30102") as? Map<String, Any> ?: emptyMap()
        val bpmVal = snapshot.get("bpm")
        val bpm = when (bpmVal) {
            is Number -> bpmVal.toInt()
            is String -> bpmVal.toIntOrNull() ?: 0
            else -> toLong(max30102Map["bpm"]).toInt()
        }

        val spo2Val = snapshot.get("spo2")
        val spo2 = when (spo2Val) {
            is Number -> spo2Val.toInt()
            is String -> spo2Val.toIntOrNull() ?: 0
            else -> toLong(max30102Map["spo2"]).toInt()
        }

        // Batería
        val bateriaVal = snapshot.get("bateria")
        val bateriaMap = snapshot.get("bateria") as? Map<String, Any> ?: emptyMap()
        val bateriaPorcentaje = when (bateriaVal) {
            is Number -> bateriaVal.toInt()
            is String -> bateriaVal.toIntOrNull() ?: 0
            else -> toLong(bateriaMap["porcentaje"]).toInt()
        }

        val bateriaVoltaje = if (snapshot.contains("voltajeBateria")) toDouble(snapshot.get("voltajeBateria")) else toDouble(bateriaMap["voltaje"])

        // Hora y Fecha
        val horaTexto = snapshot.getString("hora") ?: snapshot.getString("fechaHora") ?: ""
        val fechaTexto = snapshot.getString("fecha") ?: ""

        // GPS
        val gpsMap = snapshot.get("gps") as? Map<String, Any> ?: emptyMap()
        val gpsFix = snapshot.getBoolean("gpsFix") ?: (gpsMap["fix"] as? Boolean ?: false)
        val gpsLatitud = if (snapshot.contains("latitud")) toDouble(snapshot.get("latitud")) else toDouble(gpsMap["latitud"])
        val gpsLongitud = if (snapshot.contains("longitud")) toDouble(snapshot.get("longitud")) else toDouble(gpsMap["longitud"])

        // Alertas
        val alertaMap = snapshot.get("alerta") as? Map<String, Any> ?: emptyMap()
        val alertaSOS = snapshot.getBoolean("sos") ?: (alertaMap["sos"] as? Boolean ?: false)
        val alertaCaida = snapshot.getBoolean("caida") ?: (alertaMap["caida"] as? Boolean ?: false)

        val max30102Conectado = snapshot.getBoolean("max30102Conectado") ?: true

        return DatosSensor(
            bpm = bpm,
            bpmValido = bpm > 0,
            spo2 = spo2,
            spo2Valido = spo2 > 0,
            dedo = max30102Conectado,
            bateriaPorcentaje = bateriaPorcentaje,
            bateriaVoltaje = bateriaVoltaje,
            gpsFix = gpsFix,
            gpsLatitud = gpsLatitud,
            gpsLongitud = gpsLongitud,
            alertaSOS = alertaSOS,
            alertaCaida = alertaCaida,
            horaTexto = horaTexto,
            fechaTexto = fechaTexto,
            documentId = snapshot.id,
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
