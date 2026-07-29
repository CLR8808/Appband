package com.safestep.appband.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.safestep.appband.databinding.ActivityAuthBinding

/**
 * Activity contenedora del flujo de autenticación (Login, Registro, Verificación de Código).
 */
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
