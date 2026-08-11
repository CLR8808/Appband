package com.safestep.appband.repositories

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.safestep.appband.models.Dispositivo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.dataStoreDispositivos by preferencesDataStore(name = "safestep_dispositivos_v2")

/**
 * Repositorio para la lista y el estado activo de los dispositivos SafeBand.
 * Asocia de forma estricta los dispositivos con el ID único del usuario autenticado (FirebaseAuth UID),
 * tanto localmente en DataStore como en la nube en Firebase Firestore (usuarios/{uid}/dispositivos).
 */
class StorageRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private val gson = Gson()

    private fun getUserId(): String {
        return auth.currentUser?.uid ?: "guest"
    }

    private fun getKeyDispositivos(uid: String): androidx.datastore.preferences.core.Preferences.Key<String> {
        return stringPreferencesKey("dispositivos_lista_$uid")
    }

    private fun getKeyDispositivoActivo(uid: String): androidx.datastore.preferences.core.Preferences.Key<String> {
        return stringPreferencesKey("dispositivo_activo_$uid")
    }

    val dispositivosFlow: Flow<List<Dispositivo>> = context.dataStoreDispositivos.data.map { prefs ->
        val uid = getUserId()
        val json = prefs[getKeyDispositivos(uid)]
        if (json != null) {
            val type = object : TypeToken<List<Dispositivo>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    val dispositivoActivoFlow: Flow<Dispositivo?> = context.dataStoreDispositivos.data.map { prefs ->
        val uid = getUserId()
        val json = prefs[getKeyDispositivoActivo(uid)]
        if (json != null) {
            gson.fromJson(json, Dispositivo::class.java)
        } else {
            null
        }
    }

    suspend fun agregarDispositivoCompleto(nombre: String, macAddress: String = "", tipoConexion: String = "BLE"): Dispositivo {
        val uid = getUserId()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val fecha = dateFormat.format(Date())

        val nuevoDev = Dispositivo(
            nombre = nombre.ifBlank { "SafeBand" },
            macAddress = macAddress,
            tipoConexion = tipoConexion,
            fechaAgregado = fecha,
            estado = "Conectado"
        )

        // 1. Guardar localmente asociado al UID del usuario
        context.dataStoreDispositivos.edit { prefs ->
            val keyDisp = getKeyDispositivos(uid)
            val keyActivo = getKeyDispositivoActivo(uid)
            val jsonActual = prefs[keyDisp]

            val type = object : TypeToken<List<Dispositivo>>() {}.type
            val listaActual: MutableList<Dispositivo> = if (jsonActual != null) {
                gson.fromJson(jsonActual, type) ?: mutableListOf()
            } else {
                mutableListOf()
            }

            // Evitar duplicados por MAC o nombre para este usuario
            listaActual.removeAll { (macAddress.isNotBlank() && it.macAddress == macAddress) || it.nombre == nombre }
            listaActual.add(0, nuevoDev)

            prefs[keyDisp] = gson.toJson(listaActual)
            prefs[keyActivo] = gson.toJson(nuevoDev)
        }

        // 2. Guardar en Firestore asociado al UID del usuario
        if (uid != "guest") {
            try {
                val docId = if (macAddress.isNotBlank()) macAddress.replace(":", "_") else nombre.trim()
                val devMap = hashMapOf(
                    "nombre" to nuevoDev.nombre,
                    "macAddress" to nuevoDev.macAddress,
                    "tipoConexion" to nuevoDev.tipoConexion,
                    "fechaAgregado" to nuevoDev.fechaAgregado,
                    "estado" to nuevoDev.estado,
                    "userId" to uid
                )
                firestore.collection("usuarios")
                    .document(uid)
                    .collection("dispositivos")
                    .document(docId)
                    .set(devMap)
                    .await()
            } catch (e: Exception) {
                android.util.Log.e("StorageRepository", "Error guardando dispositivo en Firestore: ${e.message}")
            }
        }

        return nuevoDev
    }

    suspend fun agregarDispositivo(nombre: String) {
        agregarDispositivoCompleto(nombre)
    }

    suspend fun actualizarNombreDispositivoActivo(nuevoNombre: String) {
        if (nuevoNombre.isBlank()) return
        val uid = getUserId()
        context.dataStoreDispositivos.edit { prefs ->
            val keyDisp = getKeyDispositivos(uid)
            val keyActivo = getKeyDispositivoActivo(uid)
            val jsonActivo = prefs[keyActivo]

            if (jsonActivo != null) {
                val devActual = gson.fromJson(jsonActivo, Dispositivo::class.java)
                val devActualizado = devActual.copy(nombre = nuevoNombre.trim())
                prefs[keyActivo] = gson.toJson(devActualizado)

                val jsonLista = prefs[keyDisp]
                if (jsonLista != null) {
                    val type = object : TypeToken<List<Dispositivo>>() {}.type
                    val lista: MutableList<Dispositivo> = gson.fromJson(jsonLista, type) ?: mutableListOf()
                    val idx = lista.indexOfFirst { it.macAddress == devActual.macAddress || it.nombre == devActual.nombre }
                    if (idx != -1) {
                        lista[idx] = devActualizado
                    } else if (lista.isNotEmpty()) {
                        lista[0] = devActualizado
                    }
                    prefs[keyDisp] = gson.toJson(lista)
                }
            } else {
                val nuevoDev = Dispositivo(nombre = nuevoNombre.trim(), estado = "Conectado")
                prefs[keyActivo] = gson.toJson(nuevoDev)
            }
        }
    }

    suspend fun eliminarDispositivos() {
        val uid = getUserId()
        context.dataStoreDispositivos.edit { prefs ->
            prefs.remove(getKeyDispositivos(uid))
            prefs.remove(getKeyDispositivoActivo(uid))
        }
        if (uid != "guest") {
            try {
                val docs = firestore.collection("usuarios").document(uid).collection("dispositivos").get().await()
                for (doc in docs.documents) {
                    doc.reference.delete().await()
                }
            } catch (e: Exception) {
                android.util.Log.e("StorageRepository", "Error eliminando en Firestore: ${e.message}")
            }
        }
    }

    suspend fun desvincularDispositivo(nombre: String = "") {
        val uid = getUserId()
        var devAEliminar: Dispositivo? = null

        context.dataStoreDispositivos.edit { prefs ->
            val keyDisp = getKeyDispositivos(uid)
            val keyActivo = getKeyDispositivoActivo(uid)
            val jsonActual = prefs[keyDisp]

            val type = object : TypeToken<List<Dispositivo>>() {}.type
            val listaActual: MutableList<Dispositivo> = if (jsonActual != null) {
                gson.fromJson(jsonActual, type) ?: mutableListOf()
            } else {
                mutableListOf()
            }

            if (nombre.isNotBlank()) {
                devAEliminar = listaActual.find { it.nombre.equals(nombre, ignoreCase = true) }
                listaActual.removeAll { it.nombre.equals(nombre, ignoreCase = true) }
            } else {
                listaActual.clear()
            }

            if (listaActual.isEmpty()) {
                prefs.remove(keyDisp)
                prefs.remove(keyActivo)
            } else {
                prefs[keyDisp] = gson.toJson(listaActual)
                prefs[keyActivo] = gson.toJson(listaActual.first())
            }
        }

        // Eliminar en Firestore para este usuario
        if (uid != "guest") {
            try {
                if (devAEliminar != null) {
                    val docId = if (devAEliminar!!.macAddress.isNotBlank()) devAEliminar!!.macAddress.replace(":", "_") else devAEliminar!!.nombre.trim()
                    firestore.collection("usuarios")
                        .document(uid)
                        .collection("dispositivos")
                        .document(docId)
                        .delete()
                        .await()
                } else if (nombre.isBlank()) {
                    val docs = firestore.collection("usuarios").document(uid).collection("dispositivos").get().await()
                    for (doc in docs.documents) {
                        doc.reference.delete().await()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("StorageRepository", "Error desvinculando en Firestore: ${e.message}")
            }
        }
    }

    suspend fun saveLocal(keyName: String, value: String) {
        val prefKey = stringPreferencesKey("${keyName}_${getUserId()}")
        context.dataStoreDispositivos.edit { prefs ->
            prefs[prefKey] = value
        }
    }

    fun getLocalFlow(keyName: String): Flow<String?> {
        val prefKey = stringPreferencesKey("${keyName}_${getUserId()}")
        return context.dataStoreDispositivos.data.map { prefs ->
            prefs[prefKey]
        }
    }
}
