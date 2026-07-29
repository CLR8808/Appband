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

private val Context.dataStoreDispositivos by preferencesDataStore(name = "safestep_dispositivos")

/**
 * Repositorio para la lista de dispositivos en almacenamiento local.
 * Migrado desde services/storage.ts
 */
class StorageRepository(private val context: Context) {

    private val gson = Gson()
    private val KEY_DISPOSITIVOS = stringPreferencesKey("dispositivos_lista")

    val dispositivosFlow: Flow<List<Dispositivo>> = context.dataStoreDispositivos.data.map { prefs ->
        val json = prefs[KEY_DISPOSITIVOS]
        if (json != null) {
            val type = object : TypeToken<List<Dispositivo>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    suspend fun agregarDispositivo(nombre: String) {
        context.dataStoreDispositivos.edit { prefs ->
            val jsonActual = prefs[KEY_DISPOSITIVOS]
            val type = object : TypeToken<List<Dispositivo>>() {}.type
            val listaActual: MutableList<Dispositivo> = if (jsonActual != null) {
                gson.fromJson(jsonActual, type) ?: mutableListOf()
            } else {
                mutableListOf()
            }

            listaActual.add(Dispositivo(nombre))
            prefs[KEY_DISPOSITIVOS] = gson.toJson(listaActual)
        }
    }

    suspend fun eliminarDispositivos() {
        context.dataStoreDispositivos.edit { prefs ->
            prefs.remove(KEY_DISPOSITIVOS)
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
