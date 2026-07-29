package com.safestep.appband.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Modelo de datos Kotlin para el perfil de usuario autenticado en Firebase / Firestore.
 * Migrado desde interfaces/usuario.ts
 */
data class PerfilUsuario(
    val uid: String = "",
    val nombre: String = "",
    val correo: String = "",
    val genero: String = "",
    val telefono: String = "",
    val fechaNacimiento: String = "",
    @ServerTimestamp
    val fechaCreacion: Date? = null
)
