package com.safestep.appband.repositories

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.safestep.appband.models.PerfilUsuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private val Context.dataStoreUser by preferencesDataStore(name = "safestep_perfil_usuario")

/**
 * Repositorio de Perfil de Usuario.
 * Maneja Firestore (colección usuarios/) con caché local en DataStore.
 * Migrado desde services/usuario.ts
 */
class UsuarioRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val gson = Gson()
    private val KEY_PERFIL = stringPreferencesKey("perfil_cache")

    suspend fun guardarUsuario(usuario: PerfilUsuario): Result<Unit> {
        return try {
            // Guardar localmente en DataStore
            guardarEnCache(usuario)

            // Guardar en Firestore
            val mapaUsuario = hashMapOf(
                "uid" to usuario.uid,
                "nombre" to usuario.nombre,
                "correo" to usuario.correo,
                "genero" to usuario.genero,
                "telefono" to usuario.telefono,
                "fechaNacimiento" to usuario.fechaNacimiento,
                "fechaCreacion" to FieldValue.serverTimestamp()
            )
            firestore.collection("usuarios")
                .document(usuario.uid)
                .set(mapaUsuario)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizarUsuario(
        uid: String,
        datos: Map<String, Any>
    ): Result<Unit> {
        return try {
            firestore.collection("usuarios")
                .document(uid)
                .set(datos, SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun obtenerUsuarioFlow(uid: String): Flow<PerfilUsuario?> = flow {
        // Emitir primero desde Firestore si se puede, o capturar
        try {
            val doc = firestore.collection("usuarios").document(uid).get().await()
            if (doc.exists()) {
                val perfil = doc.toObject(PerfilUsuario::class.java)
                if (perfil != null) {
                    guardarEnCache(perfil)
                    emit(perfil)
                    return@flow
                }
            }
        } catch (e: Exception) {
            // Fallback a caché
        }

        // Leer caché local
        context.dataStoreUser.data.map { prefs ->
            val json = prefs[KEY_PERFIL]
            if (json != null) {
                gson.fromJson(json, PerfilUsuario::class.java)
            } else null
        }.collect { perfilLocal ->
            emit(perfilLocal)
        }
    }

    private suspend fun guardarEnCache(usuario: PerfilUsuario) {
        val json = gson.toJson(usuario)
        context.dataStoreUser.edit { prefs ->
            prefs[KEY_PERFIL] = json
        }
    }
}
