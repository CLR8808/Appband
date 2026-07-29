package com.safestep.appband.repositories

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.safestep.appband.models.RegistroPaciente
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private val Context.dataStorePaciente by preferencesDataStore(name = "safestep_paciente")

/**
 * Repositorio de Pacientes.
 * CRUD en Firebase Firestore (pacientes/{uid}) con caché local en DataStore.
 * Migrado desde services/paciente.ts
 */
class PacienteRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val gson = Gson()
    private val KEY_PACIENTE = stringPreferencesKey("paciente_cache")

    suspend fun guardarPaciente(paciente: RegistroPaciente): Result<Unit> {
        return try {
            guardarEnCache(paciente)

            val docRef = firestore.collection("pacientes").document(paciente.uid)
            val docSnapshot = docRef.get().await()

            val datosMapa = hashMapOf(
                "uid" to paciente.uid,
                "datosPaciente" to paciente.datosPaciente,
                "infoMedica" to paciente.infoMedica,
                "umbralesAlerta" to paciente.umbralesAlerta,
                "datosTutor" to paciente.datosTutor,
                "contactosEmergencia" to paciente.contactosEmergencia,
                "configuracionAlertas" to paciente.configuracionAlertas,
                "ultimaActualizacion" to FieldValue.serverTimestamp()
            )

            if (!docSnapshot.exists()) {
                datosMapa["fechaRegistro"] = FieldValue.serverTimestamp()
            }

            docRef.set(datosMapa, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun obtenerPacienteFlow(uid: String): Flow<RegistroPaciente?> = flow {
        try {
            val doc = firestore.collection("pacientes").document(uid).get().await()
            if (doc.exists()) {
                val paciente = doc.toObject(RegistroPaciente::class.java)
                if (paciente != null) {
                    guardarEnCache(paciente)
                    emit(paciente)
                    return@flow
                }
            }
        } catch (e: Exception) {
            // Fallback a caché
        }

        context.dataStorePaciente.data.map { prefs ->
            val json = prefs[KEY_PACIENTE]
            if (json != null) {
                gson.fromJson(json, RegistroPaciente::class.java)
            } else null
        }.collect { pacienteLocal ->
            emit(pacienteLocal)
        }
    }

    private suspend fun guardarEnCache(paciente: RegistroPaciente) {
        val json = gson.toJson(paciente)
        context.dataStorePaciente.edit { prefs ->
            prefs[KEY_PACIENTE] = json
        }
    }
}
