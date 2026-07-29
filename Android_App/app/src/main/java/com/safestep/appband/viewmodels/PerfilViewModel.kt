package com.safestep.appband.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safestep.appband.models.PerfilUsuario
import com.safestep.appband.repositories.AuthRepository
import com.safestep.appband.repositories.UsuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para PerfilFragment.
 * Migrado desde components/perfil/perfil.component.ts
 */
class PerfilViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val usuarioRepository = UsuarioRepository(application)

    private val _perfil = MutableStateFlow<PerfilUsuario?>(null)
    val perfil: StateFlow<PerfilUsuario?> = _perfil.asStateFlow()

    init {
        cargarPerfilUsuario()
    }

    private fun cargarPerfilUsuario() {
        val user = authRepository.usuarioActual
        if (user != null) {
            viewModelScope.launch {
                usuarioRepository.obtenerUsuarioFlow(user.uid).collect { perfilObtenido ->
                    _perfil.value = perfilObtenido ?: PerfilUsuario(
                        uid = user.uid,
                        nombre = user.displayName ?: "Usuario SafeStep",
                        correo = user.email ?: ""
                    )
                }
            }
        }
    }

    fun cerrarSesion() {
        authRepository.cerrarSesion()
    }
}
