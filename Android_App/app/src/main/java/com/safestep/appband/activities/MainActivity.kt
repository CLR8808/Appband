package com.safestep.appband.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.safestep.appband.R
import com.safestep.appband.databinding.ActivityMainBinding

/**
 * Activity Principal que aloja la navegación por Tabs (Bottom Navigation) y Sub-pantallas.
 * Migrado desde shared/tabbar/tabbar.component.ts y app.routes.ts
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostMain) as NavHostFragment
        val navController = navHostFragment.navController

        // Vincular BottomNavigationView con Navigation Component
        binding.bottomNav.setupWithNavController(navController)

        // Ocultar BottomNav en pantallas secundarias de configuración
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.inicioFragment,
                R.id.mapaFragment,
                R.id.perfilFragment,
                R.id.eventosFragment -> {
                    binding.bottomNavContainer.visibility = View.VISIBLE
                }
                else -> {
                    binding.bottomNavContainer.visibility = View.GONE
                }
            }
        }
    }
}
