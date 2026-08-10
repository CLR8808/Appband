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
 * Carga en tiempo real el historial de incidentes desde Firebase Firestore ("incidentes" o "eventos").
 */
class EventosRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    /**
     * Escucha en tiempo real la lista de incidentes desde Firestore.
     * Colección principal: "eventos". Fallback: "incidentes".
     */
    fun obtenerIncidentesFlow(): Flow<List<Evento>> = callbackFlow {
        val listener = firestore.collection("eventos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("EventosRepo", "Error en 'eventos': ${error.message}")
                    // Si falla con "eventos", intentamos "incidentes"
                    cargarIncidentesFallback { listaFallback ->
                        trySend(listaFallback)
                    }
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    android.util.Log.w("EventosRepo", "Colección 'eventos' vacía o null. Docs: ${snapshot?.size() ?: 0}")
                    // Colección vacía, intentamos "incidentes" como fallback
                    cargarIncidentesFallback { listaFallback ->
                        trySend(listaFallback)
                    }
                    return@addSnapshotListener
                }

                android.util.Log.d("EventosRepo", "Documentos encontrados en 'eventos': ${snapshot.size()}")

                val lista = snapshot.documents.mapNotNull { doc ->
                    android.util.Log.d("EventosRepo", "Doc ID: ${doc.id}, Data: ${doc.data}")
                    mapearDocAEvento(doc.id, doc.data)
                }.sortedByDescending { it.timestamp }

                android.util.Log.d("EventosRepo", "Eventos mapeados: ${lista.size}")
                trySend(lista)
            }

        awaitClose { listener.remove() }
    }

    private fun cargarIncidentesFallback(onResult: (List<Evento>) -> Unit) {
        firestore.collection("incidentes")
            .get()
            .addOnSuccessListener { snapshot ->
                android.util.Log.d("EventosRepo", "Fallback 'incidentes': ${snapshot.size()} docs")
                val lista = snapshot.documents.mapNotNull { doc ->
                    mapearDocAEvento(doc.id, doc.data)
                }.sortedByDescending { it.timestamp }
                onResult(lista)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("EventosRepo", "Fallback falló: ${e.message}")
                onResult(emptyList())
            }
    }

    suspend fun marcarComoRevisado(incidenteId: String): Result<Unit> {
        return try {
            firestore.collection("incidentes")
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

    private fun mapearDocAEvento(id: String, data: Map<String, Any>?): Evento? {
        if (data == null) return null

        val tipo = data["tipo"]?.toString() ?: "ADVERTENCIA"
        val titulo = data["titulo"]?.toString()
            ?: data["nombre"]?.toString()
            ?: "Incidente detectado"
        val descripcion = data["descripcion"]?.toString()
            ?: data["mensaje"]?.toString()
            ?: "Se ha registrado una anomalía en los patrones de movimiento de la SafeBand."

        val tsRaw = data["timestamp"] ?: data["fechaRegistro"] ?: data["creado_en"]
        val timestamp = when (tsRaw) {
            is Timestamp -> tsRaw.toDate().time
            is Long -> tsRaw
            is Number -> tsRaw.toLong()
            else -> System.currentTimeMillis()
        }

        val dateObj = Date(timestamp)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val tiempoTexto = data["tiempoTexto"]?.toString()
            ?: calcularTiempoRelativo(timestamp)

        val lat = (data["latitud"] as? Number)?.toDouble()
            ?: (data["lat"] as? Number)?.toDouble()
            ?: 23.264864

        val lng = (data["longitud"] as? Number)?.toDouble()
            ?: (data["lng"] as? Number)?.toDouble()
            ?: -106.430832

        val ubicacionTexto = data["ubicacionTexto"]?.toString()
            ?: data["ubicacion"]?.toString()
            ?: "Mazatlán, Sinaloa"

        val bpm = data["frecuenciaCardiaca"]?.toString()
            ?: data["ritmoCardiaco"]?.toString()
            ?: data["bpm"]?.toString()
            ?: "85 BPM"

        val estado = data["estadoPulsera"]?.toString()
            ?: data["estado"]?.toString()
            ?: "Activa"

        val revisado = data["revisado"] as? Boolean ?: false

        return Evento(
            id = id,
            tipo = tipo.uppercase(),
            titulo = titulo,
            descripcion = descripcion,
            tiempoTexto = tiempoTexto,
            timestamp = timestamp,
            latitud = lat,
            longitud = lng,
            ubicacionTexto = ubicacionTexto,
            frecuenciaCardiaca = if (bpm.contains("BPM", true)) bpm else "$bpm BPM",
            estadoPulsera = estado,
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
