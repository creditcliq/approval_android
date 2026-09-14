package com.creditchek.approval_android.core.network

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.compose.ui.graphics.Color
import com.creditchek.approval_android.core.theme.ApprovalDanger
import com.creditchek.approval_android.core.theme.ApprovalSuccess
import com.creditchek.approval_android.core.theme.ApprovalWarning
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

enum class NetworkQuality(val label: String, val color: Color) {
    EXCELLENT("Excellent", ApprovalSuccess),
    MODERATE("Moderate", ApprovalWarning),
    UNSTABLE(
        "Unstable", ApprovalDanger
    );

    companion object {
        /**
         * Categorizes network based on hardware capabilities and link bandwidth
         */
        fun fromCapabilities(capabilities: NetworkCapabilities): NetworkQuality {
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            if (!hasInternet || !isValidated) {
                return UNSTABLE
            }
            val downstreamKbps = capabilities.linkDownstreamBandwidthKbps
            return when {
                downstreamKbps >= 10_000 -> EXCELLENT
                downstreamKbps >= 1_500 -> MODERATE
                downstreamKbps > 0 -> UNSTABLE
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> EXCELLENT
                else -> MODERATE
            }
        }

        fun fromBandwidthKbps(downstreamKbps: Int): NetworkQuality = when {
            downstreamKbps >= 10_000 -> EXCELLENT
            downstreamKbps >= 1_500 -> MODERATE
            else                     -> UNSTABLE
        }
    }
}



/**
 * 100% Local Hardware-Based Network Quality Monitor.
 * Reacts dynamically to Android ConnectivityManager callbacks.
 */
class NetworkQualityEstimator(context: Context) {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val _quality = MutableStateFlow(NetworkQuality.EXCELLENT)
    val quality: StateFlow<NetworkQuality> = _quality.asStateFlow()
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    init {
        // Initial snapshot evaluation on startup
        evaluateCurrentNetwork()
    }
    /**
     * Registers an OS kernel event listener for network speed/capability changes.
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun startMonitoring() {
        if (networkCallback != null || connectivityManager == null) return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                _quality.value = NetworkQuality.fromCapabilities(capabilities)
            }
            override fun onLost(network: Network) {
                _quality.value = NetworkQuality.UNSTABLE
            }
            override fun onAvailable(network: Network) {
                evaluateCurrentNetwork()
            }
        }
        networkCallback = callback
        try {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } catch (e: Exception) {
            evaluateCurrentNetwork()
        }
    }
    /**
     * Evaluates current network capabilities without waiting for an event.
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun evaluateCurrentNetwork() {
        val manager = connectivityManager ?: return
        val activeNetwork = manager.activeNetwork
        val capabilities = manager.getNetworkCapabilities(activeNetwork)
        if (capabilities == null) {
            _quality.value = NetworkQuality.UNSTABLE
            return
        }
        _quality.value = NetworkQuality.fromCapabilities(capabilities)
    }
    /**
     * Unregisters the OS listener when leaving the camera screen.
     */
    fun stopMonitoring() {
        networkCallback?.let { callback ->
            try {
                connectivityManager?.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
        networkCallback = null
    }
}