package com.safestep.appband.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio de Autenticación de Firebase.
 * Migrado desde services/auth.ts
 */
class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    /**
     * Observable Flow con el estado del usuario autenticado actual.
     */
    val usuarioActualFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    val usuarioActual: FirebaseUser?
        get() = firebaseAuth.currentUser

    suspend fun iniciarSesion(correo: String, contrasena: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(correo, contrasena).await()
            val user = result.user ?: throw Exception("Usuario no encontrado")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun crearCuenta(correo: String, contrasena: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(correo, contrasena).await()
            val user = result.user ?: throw Exception("No se pudo crear la cuenta")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun cerrarSesion() {
        firebaseAuth.signOut()
    }
}
