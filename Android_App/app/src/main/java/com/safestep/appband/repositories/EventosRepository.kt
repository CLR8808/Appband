package com.safestep.appband.repositories

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.safestep.appband.models.Evento
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repositorio de Eventos / Incidentes.
 * Escucha en tiempo real los incidentes y alertas generados por la pulsera SafeBand en la colección "sensor" (y "eventos" / "incidentes").
 */
class EventosRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    /**
     * Escucha en tiempo real la lista de incidentes desde Firestore.
     * Lee primero de la colección "sensor" (donde el ESP32 envía registro_<millis>_<hex>),
     * y como fallback consulta "eventos" o "incidentes".
     */
    fun obtenerIncidentesFlow(): Flow<List<Evento>> = callbackFlow {
        val listener = firestore.collection("sensor")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.isEmpty) {
                    android.util.Log.w("EventosRepo", "Colección 'sensor' sin alertas directas. Probando 'eventos'...")
                    cargarColeccionFallback("eventos") { listaEventos ->
                        if (listaEventos.isNotEmpty()) {
                            trySend(listaEventos)
                        } else {
                            cargarColeccionFallback("incidentes") { listaIncidentes ->
                                trySend(listaIncidentes)
                            }
                        }
                    }
                    return@addSnapshotListener
                }

                android.util.Log.d("EventosRepo", "Documentos leídos en 'sensor': ${snapshot.size()}")

                // Filtrar solo los registros que representen alertas o prealertas
                val listaAlertas = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    
                    // Verificar si el documento es una alerta o un evento relevante
                    val estado = data["estado"]?.toString() ?: ""
                    val sos = data["sos"] as? Boolean ?: false
                    val caida = data["caida"] as? Boolean ?: false
                    val nivelRiesgo = (data["nivelRiesgo"] as? Number)?.toInt() ?: 0
                    val movimientoBrusco = data["movimientoBrusco"] as? Boolean ?: false
                    val tipoRaw = data["tipo"]?.toString()

                    val esAlerta = sos || caida || movimientoBrusco || 
                            estado == "ALERTA" || estado == "PREALERTA" || 
                            nivelRiesgo > 2 || tipoRaw != null

                    if (esAlerta) {
                        mapearDocSensorAEvento(doc.id, data)
                    } else {
                        null
                    }
                }.sortedByDescending { it.timestamp }

                if (listaAlertas.isNotEmpty()) {
                    trySend(listaAlertas)
                } else {
                    // Si en 'sensor' solo hay el documento 'actual' o registros normales, buscar en 'eventos'
                    cargarColeccionFallback("eventos") { lista ->
                        trySend(lista)
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    private fun cargarColeccionFallback(coleccionNombre: String, onResult: (List<Evento>) -> Unit) {
        firestore.collection(coleccionNombre)
            .get()
            .addOnSuccessListener { snapshot ->
                val lista = snapshot.documents.mapNotNull { doc ->
                    mapearDocSensorAEvento(doc.id, doc.data)
                }.sortedByDescending { it.timestamp }
                onResult(lista)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    suspend fun marcarComoRevisado(incidenteId: String): Result<Unit> {
        return try {
            firestore.collection("sensor")
                .document(incidenteId)
                .update("revisado", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            try {
                firestore.collection("eventos")
                    .document(incidenteId)
                    .update("revisado", true)
                    .await()
                Result.success(Unit)
            } catch (e2: Exception) {
                Result.failure(e2)
            }
        }
    }

    /**
     * Mapea un documento de Firestore (de la colección 'sensor', 'eventos' o 'incidentes') a la clase Evento de la app.
     */
    private fun mapearDocSensorAEvento(id: String, data: Map<String, Any>?): Evento? {
        if (data == null) return null

        val sos = data["sos"] as? Boolean ?: false
        val caida = data["caida"] as? Boolean ?: false
        val movimientoBrusco = data["movimientoBrusco"] as? Boolean ?: false
        val personaQuieta = data["personaQuieta"] as? Boolean ?: false
        val estado = data["estado"]?.toString() ?: "NORMAL"
        val nivelRiesgo = (data["nivelRiesgo"] as? Number)?.toInt() ?: 0
        val bpmNum = (data["bpm"] as? Number)?.toInt() ?: 0
        val spo2Num = (data["spo2"] as? Number)?.toInt() ?: 0

        // Determinar Severidad (CRITICO vs ADVERTENCIA)
        val tipo = when {
            data.containsKey("tipo") -> data["tipo"].toString().uppercase()
            sos || (caida && personaQuieta) || estado == "ALERTA" || nivelRiesgo >= 6 -> "CRITICO"
            else -> "ADVERTENCIA"
        }

        // Determinar Título
        val titulo = when {
            data.containsKey("titulo") -> data["titulo"].toString()
            data.containsKey("nombre") -> data["nombre"].toString()
            sos -> "🚨 BOTÓN SOS PRESIONADO"
            caida && personaQuieta -> "🚨 CAÍDA E INMOVILIDAD DETECTADA"
            caida -> "⚠️ CAÍDA DETECTADA"
            bpmNum > 120 -> "⚠️ FRECUENCIA CARDÍACA ALTA"
            bpmNum in 1..44 -> "⚠️ FRECUENCIA CARDÍACA BAJA"
            spo2Num in 1..91 -> "⚠️ OXIGENACIÓN BAJA (SpO2)"
            movimientoBrusco -> "⚠️ MOVIMIENTO BRUSCO DETECTADO"
            estado == "PREALERTA" -> "⚠️ PREALERTA EN SAFEBAND"
            else -> "⚠️ ALERTA EN SAFEBAND"
        }

        // Determinar Descripción
        val descripcion = when {
            data.containsKey("descripcion") -> data["descripcion"].toString()
            data.containsKey("mensaje") -> data["mensaje"].toString()
            sos -> "El paciente ha presionado manualmente el botón de pánico SOS en su pulsera SafeBand."
            caida && personaQuieta -> "Se detectó un impacto de caída y el paciente permanece sin movimiento (Nivel de riesgo: $nivelRiesgo)."
            caida -> "El acelerómetro MPU6500 de la pulsera registró un impacto compatible con una caída (Nivel de riesgo: $nivelRiesgo)."
            bpmNum > 120 -> "La pulsera registró un pulso de $bpmNum BPM, superando el umbral normal."
            spo2Num in 1..91 -> "Se detectó una disminución en el nivel de oxígeno a $spo2Num%."
            movimientoBrusco -> "Se detectó un movimiento repentino o agitación inusual en la pulsera."
            else -> "Se ha registrado un evento con nivel de riesgo $nivelRiesgo en la pulsera SafeBand."
        }

        // Timestamp
        val tsRaw = data["timestamp"] ?: data["fechaRegistro"] ?: data["creado_en"] ?: data["tiempoESP32"]
        val timestamp = when (tsRaw) {
            is Timestamp -> tsRaw.toDate().time
            is Long -> if (tsRaw > 1000000000000L) tsRaw else System.currentTimeMillis()
            is Number -> {
                val num = tsRaw.toLong()
                if (num > 1000000000000L) num else System.currentTimeMillis()
            }
            else -> System.currentTimeMillis()
        }

        val dateObj = Date(timestamp)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val tiempoTexto = data["tiempoTexto"]?.toString()
            ?: calcularTiempoRelativo(timestamp)

        // Ubicación / GPS
        val lat = (data["latitud"] as? Number)?.toDouble()
            ?: (data["lat"] as? Number)?.toDouble()
            ?: 23.264864

        val lng = (data["longitud"] as? Number)?.toDouble()
            ?: (data["lng"] as? Number)?.toDouble()
            ?: -106.430832

        val ubicacionTexto = data["ubicacionTexto"]?.toString()
            ?: data["ubicacion"]?.toString()
            ?: "Mazatlán, Sinaloa"

        // Frecuencia Cardíaca
        val bpmTexto = if (bpmNum > 0) "$bpmNum BPM" else (data["frecuenciaCardiaca"]?.toString() ?: "85 BPM")

        val revisado = data["revisado"] as? Boolean ?: false

        return Evento(
            id = id,
            tipo = tipo,
            titulo = titulo,
            descripcion = descripcion,
            tiempoTexto = tiempoTexto,
            timestamp = timestamp,
            latitud = lat,
            longitud = lng,
            ubicacionTexto = ubicacionTexto,
            frecuenciaCardiaca = bpmTexto,
            estadoPulsera = if (estado != "NORMAL") estado else "Activa",
            fechaTexto = dateFormat.format(dateObj),
            horaTexto = timeFormat.format(dateObj),
            revisado = revisado
        )
    }

    private fun calcularTiempoRelativo(timestamp: Long): String {
        val diffMs = System.currentTimeMillis() - timestamp
        val mins = diffMs / (1000 * 60)
        val hours = mins / 60
        val days = hours / 24

        return when {
            mins < 1 -> "Hace un momento"
            mins < 60 -> "Hace $mins min"
            hours < 24 -> "Hace $hours h"
            else -> "Hace $days d"
        }
    }
}
