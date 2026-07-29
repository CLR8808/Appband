package com.safestep.appband.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safestep.appband.models.WifiNetwork
import com.safestep.appband.repositories.WifiRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para WifiFragment.
 * Migrado desde components/wifi/wifi.component.ts
 */
class WifiViewModel(application: Application) : AndroidViewModel(application) {

    private val wifiRepository = WifiRepository(application)

    val networks: StateFlow<List<WifiNetwork>> = wifiRepository.networks
    val isScanning: StateFlow<Boolean> = wifiRepository.isScanning

    init {
        iniciarEscaneo()
    }

    fun iniciarEscaneo() {
        viewModelScope.launch {
            wifiRepository.escaneoRedes()
        }
    }

    fun selectNetwork(network: WifiNetwork) {
        wifiRepository.selectNetwork(network)
    }
}
