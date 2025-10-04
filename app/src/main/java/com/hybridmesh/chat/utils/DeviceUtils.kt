package com.hybridmesh.chat.utils

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import java.net.NetworkInterface
import java.util.*

object DeviceUtils {
    
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
    }
    
    fun getDeviceName(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL} (HybridMesh)"
    }
    
    fun getMacAddress(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val macBytes = networkInterface.hardwareAddress
                if (macBytes != null && macBytes.size == 6) {
                    val mac = StringBuilder()
                    for (i in macBytes.indices) {
                        mac.append(String.format("%02X%s", macBytes[i], if (i < macBytes.size - 1) ":" else ""))
                    }
                    return mac.toString()
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
    
    fun getWifiMacAddress(context: Context): String? {
        return try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            wifiInfo.macAddress
        } catch (e: Exception) {
            null
        }
    }
    
    fun generateRandomDeviceName(): String {
        val adjectives = listOf("Swift", "Bright", "Quick", "Smart", "Fast", "Cool", "Sharp", "Bold")
        val nouns = listOf("Phoenix", "Eagle", "Tiger", "Wolf", "Hawk", "Lion", "Bear", "Fox")
        val random = Random()
        val adjective = adjectives[random.nextInt(adjectives.size)]
        val noun = nouns[random.nextInt(nouns.size)]
        return "$adjective$noun (HybridMesh)"
    }
}
