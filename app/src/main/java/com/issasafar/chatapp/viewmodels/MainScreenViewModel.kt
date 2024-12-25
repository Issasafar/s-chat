package com.issasafar.chatapp.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.regex.Pattern

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {
    private val _chatStateHolder =
        MutableStateFlow(ChatStateHolder(connectionType = ConnectionType.SERVER, "", "", 0, ""))

    private val _deviceIp = MutableStateFlow("")
    val deviceIp : StateFlow<String> = _deviceIp
    val uiState: StateFlow<ChatStateHolder> = _chatStateHolder

    fun updateDeviceIp() {
        CoroutineScope(Dispatchers.IO).launch {
            val context = getApplication<Application>().applicationContext
            while (true) {
                if (isWifiEnabled(context) || isHotspotEnabled(context)) {
                    val ip = getIpAddress()
                    _deviceIp.emit(ip)
                } else {
                    _deviceIp.emit("127.0.0.1")
                }
                delay(1000)
            }
        }
    }


    @SuppressLint("ServiceCast")
    private fun isWifiEnabled(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    @SuppressLint("ServiceCast")
    private fun isHotspotEnabled(context: Context): Boolean {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        // Hotspot status detection (Android doesn't directly expose this information post Android O)
        return try {
            val method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
            method.isAccessible = true
            method.invoke(wifiManager) as Boolean
        } catch (e: Exception) {
            false // Handle errors gracefully
        }
    }

    private fun getIpAddress(): String {
        NetworkInterface.getNetworkInterfaces()?.toList()?.map { networkInterface ->
            networkInterface.inetAddresses?.toList()?.find {
                !it.isLoopbackAddress && it is Inet4Address
            }?.let { return it.hostAddress!! }
        }
        return ""
    }
    fun setConnection(connectionType: ConnectionType) {
        _chatStateHolder.update {
            it.copy(connectionType = connectionType)
        }
    }

    fun setUserName(userName: String) {
        _chatStateHolder.update {
            it.copy(userName = userName)
        }
    }

    fun updateMessage(message: String) {
        _chatStateHolder.update {
            it.copy(message = message)
        }
    }

    fun setTargetIp(ipAndPort: String) {
        val (ip, port) = ipAndPort.split(":")
        try {
            setIp(ip)
        } catch ( _: IllegalArgumentException) {

        }
        setPort(port = port.toInt())
    }

    private fun setIp(ip: String) {
        val ipPattern = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b")
        val matcher = ipPattern.matcher(ip)
        require(matcher.find()) { "Invalid ip address" }
        _chatStateHolder.update {
            it.copy(ip = ip)
        }
    }

    private fun setPort(port: Int) {
        _chatStateHolder.update {
            it.copy(port = port)
        }
    }

}