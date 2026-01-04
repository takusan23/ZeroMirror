package io.github.takusan23.zeromirror.tool

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

object IpAddressTool {

    /**
     * IPアドレスをFlowで受け取る
     *
     * @param context Context
     * @return IPv4のIPアドレス
     * */
    fun collectIpAddress(context: Context) = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        // コールバック
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val linkProperties = connectivityManager.getLinkProperties(network)
                // IPv4アドレスを探す
                val address = linkProperties?.linkAddresses
                    ?.find { it.address?.hostAddress?.startsWith("192") == true }
                    ?.address?.hostAddress
                if (address != null) {
                    trySend(address)
                }
            }
        }
        connectivityManager.registerNetworkCallback(request, networkCallback)
        awaitClose { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }
}