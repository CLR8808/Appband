package com.safestep.appband.activities

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.safestep.appband.databinding.ActivitySplashBinding
import com.safestep.appband.viewmodels.SplashViewModel
import kotlinx.coroutines.launch

/**
 * Pantalla de Splash Inicial (Logo animado en gradiente cian).
 * Redirige a MainActivity si la sesión está activa o a AuthActivity si se cerró sesión.
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Animación suave de aparición del logo
        val animFadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        binding.ivLogo.startAnimation(animFadeIn)

        // Observar destino de navegación
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationTarget.collect { targetClass ->
                    if (targetClass != null) {
                        val intent = Intent(this@SplashActivity, targetClass)
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
    }
}
