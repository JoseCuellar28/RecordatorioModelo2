package com.example.recordatoriomodelo2.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Servicio para monitorear el estado de conectividad de internet
 */
class ConnectivityService private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "ConnectivityService"
        
        @Volatile
        private var INSTANCE: ConnectivityService? = null
        
        fun getInstance(context: Context): ConnectivityService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConnectivityService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    // Estado de conectividad
    private val _isConnected = MutableStateFlow(checkInitialConnectivity())
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    // Callback para monitorear cambios de red
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Red disponible: $network")
            _isConnected.value = true
        }
        
        override fun onLost(network: Network) {
            Log.d(TAG, "Red perdida: $network")
            _isConnected.value = false
        }
        
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                             networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            Log.d(TAG, "Capacidades de red cambiadas. Tiene internet: $hasInternet")
            _isConnected.value = hasInternet
        }
    }
    
    init {
        startMonitoring()
    }
    
    /**
     * Verifica la conectividad inicial
     */
    private fun checkInitialConnectivity(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Inicia el monitoreo de conectividad
     */
    private fun startMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()
        
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            Log.d(TAG, "Monitoreo de conectividad iniciado")
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando monitoreo de conectividad", e)
        }
    }
    
    /**
     * Detiene el monitoreo de conectividad
     */
    fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            Log.d(TAG, "Monitoreo de conectividad detenido")
        } catch (e: Exception) {
            Log.e(TAG, "Error deteniendo monitoreo de conectividad", e)
        }
    }
    
    /**
     * Verifica si hay conexión a internet de forma síncrona
     */
    fun isCurrentlyConnected(): Boolean {
        return _isConnected.value
    }
}