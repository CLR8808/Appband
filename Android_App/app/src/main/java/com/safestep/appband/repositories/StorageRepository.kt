package com.safestep.appband.repositories

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.safestep.appband.models.Dispositivo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.dataStoreDispositivos by preferencesDataStore(name = "safestep_dispositivos")

/**
 * Repositorio para la lista y el estado activo de los dispositivos SafeBand en almacenamiento local.
 */
class StorageRepository(private val context: Context) {

    private val gson = Gson()
    private val KEY_DISPOSITIVOS = stringPreferencesKey("dispositivos_lista")
    private val KEY_DISPOSITIVO_ACTIVO = stringPreferencesKey("dispositivo_activo_detalles")

    val dispositivosFlow: Flow<List<Dispositivo>> = context.dataStoreDispositivos.data.map { prefs ->
        val json = prefs[KEY_DISPOSITIVOS]
        if (json != null) {
            val type = object : TypeToken<List<Dispositivo>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    val dispositivoActivoFlow: Flow<Dispositivo?> = context.dataStoreDispositivos.data.map { prefs ->
        val json = prefs[KEY_DISPOSITIVO_ACTIVO]
        if (json != null) {
            gson.fromJson(json, Dispositivo::class.java)
        } else {
            null
        }
    }

    suspend fun agregarDispositivoCompleto(nombre: String, macAddress: String = "", tipoConexion: String = "BLE"): Dispositivo {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val fecha = dateFormat.format(Date())

        val nuevoDev = Dispositivo(
            nombre = nombre.ifBlank { "SafeBand" },
            macAddress = macAddress,
            tipoConexion = tipoConexion,
            fechaAgregado = fecha,
            estado = "Conectado"
        )

        context.dataStoreDispositivos.edit { prefs ->
            val jsonActual = prefs[KEY_DISPOSITIVOS]
            val type = object : TypeToken<List<Dispositivo>>() {}.type
            val listaActual: MutableList<Dispositivo> = if (jsonActual != null) {
                gson.fromJson(jsonActual, type) ?: mutableListOf()
            } else {
                mutableListOf()
            }

            // Evitar duplicados por MAC o nombre
            listaActual.removeAll { (macAddress.isNotBlank() && it.macAddress == macAddress) || it.nombre == nombre }
            listaActual.add(0, nuevoDev)

            prefs[KEY_DISPOSITIVOS] = gson.toJson(listaActual)
            prefs[KEY_DISPOSITIVO_ACTIVO] = gson.toJson(nuevoDev)
        }

        return nuevoDev
    }

    suspend fun agregarDispositivo(nombre: String) {
        agregarDispositivoCompleto(nombre)
    }

    suspend fun eliminarDispositivos() {
        context.dataStoreDispositivos.edit { prefs ->
            prefs.remove(KEY_DISPOSITIVOS)
            prefs.remove(KEY_DISPOSITIVO_ACTIVO)
        }
    }

    suspend fun saveLocal(keyName: String, value: String) {
        val prefKey = stringPreferencesKey(keyName)
        context.dataStoreDispositivos.edit { prefs ->
            prefs[prefKey] = value
        }
    }

    fun getLocalFlow(keyName: String): Flow<String?> {
        val prefKey = stringPreferencesKey(keyName)
        return context.dataStoreDispositivos.data.map { prefs ->
            prefs[prefKey]
        }
    }
}
