package com.halangode.devicescanner

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Message

import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.math.BigInteger
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.UnknownHostException
import java.nio.ByteOrder
import java.util.HashMap

/**
 * Created by Harikumar Alangode on 19-Jun-17.
 */

class DeviceScannerUtil private constructor() {

    private var frontIpAddress: String? = null
    private val connectedDeviceMap = mutableMapOf<String, String>()
    private var isRunning: Boolean = false
    private var isCancelled: Boolean = false
    private var eventListener: IScanEvents? = null

    fun startDeviceScan(context: Context): DeviceScannerUtil {

        val ipAddress = getIpAddress(context)
        isCancelled = false
        isRunning = true
        scanHandler = object : Handler() {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                if (msg.what == 1) {
                    eventListener!!.onScanCompleted(connectedDeviceMap)
                    isRunning = false
                }
            }
        }

        if (isWifiConnected(context)) {

            val thread = Thread(Runnable {
                frontIpAddress = ipAddress.substring(0, ipAddress.lastIndexOf('.') + 1)

                try {
                    var inetAddress: InetAddress
                    for (i in 0..255) {
                        if (isCancelled) {
                            isRunning = false
                            return@Runnable
                        }
                        inetAddress = InetAddress.getByName(frontIpAddress!! + i.toString())
                        inetAddress.isReachable(Constants.DEVICE_SCAN_INTERVAL)
                        LogUtil.d("DeviceScannerUtil", "Pinging: " + inetAddress.hostAddress)

                    }

                    for (i in 1..255) {
                        if (isCancelled) {
                            isRunning = false
                            return@Runnable
                        }
                        inetAddress = InetAddress.getByName(frontIpAddress!! + i.toString())

                        LogUtil.d("DeviceScannerUtil", "Trying: " + inetAddress.hostAddress)
                        var macAddress: String? = getMacAddress(inetAddress)
                        if (macAddress == "") {
                            macAddress = getMacFromArpCache(inetAddress.hostName)
                        }

                        if (macAddress != "" && macAddress != "00:00:00:00:00:00") {
                            connectedDeviceMap[inetAddress.hostAddress] = macAddress.toString()
                            LogUtil.d(
                                "DeviceScannerUtil",
                                "Mac found: " + inetAddress.hostAddress + " -> " + macAddress
                            )
                        }
                    }

                } catch (exception: IOException) {
                    eventListener!!.onError(Error(exception.message))
                }

                scanHandler!!.sendEmptyMessage(1)
            })
            thread.start()
        }

        return deviceScannerUtil
    }

    fun setPingDuration(milliseconds: Int): DeviceScannerUtil {
        Constants.DEVICE_SCAN_INTERVAL = milliseconds
        return deviceScannerUtil
    }

    fun cancelScan() {
        isCancelled = true
    }


    fun setEnableLog(flag: Boolean): DeviceScannerUtil {
        Constants.isLogEnabled = flag
        return deviceScannerUtil
    }


    fun setEventListener(listener: IScanEvents): DeviceScannerUtil {
        eventListener = listener
        return deviceScannerUtil
    }


    private fun isWifiConnected(context: Context): Boolean {
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connManager.activeNetworkInfo

        return networkInfo != null && networkInfo.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
    }

    private fun getMacAddress(inetAddress: InetAddress): String {

        var sb = StringBuilder()
        try {

            val network = NetworkInterface.getByInetAddress(inetAddress)
            if (network != null) {
                val mac = network.hardwareAddress
                sb = StringBuilder()
                for (i in mac.indices) {
                    sb.append(String.format("%02X%s", mac[i], if (i < mac.size - 1) ":" else ""))
                }
            } else {
                return ""
            }

        } catch (e: SocketException) {
            e.printStackTrace()
        }

        return sb.toString().toUpperCase()

    }

    private fun getIpAddress(context: Context): String {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ip = if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN)
            Integer.reverseBytes(wifiInfo.ipAddress)
        else
            wifiInfo.ipAddress

        if(ip == 0) {
            eventListener?.onError(Error("Not connected to wifi"))
            return ""
        }
        try {
            val ipAddress = BigInteger.valueOf(ip.toLong()).toByteArray()
            return InetAddress.getByAddress(ipAddress).hostAddress

        } catch (e: UnknownHostException) {
            e.printStackTrace()
            eventListener!!.onError(Error(e.message + ": " + "not connected to Wifi"))
        }

        return ""
    }

    private fun getMacFromArpCache(ip: String?): String? {
        if (ip == null) {
            return null
        }

        var br: BufferedReader? = null
        try {
            br = BufferedReader(FileReader("/proc/net/arp"))
            var line = br.readLine()
            while (line != null) {
                val splitted = line.split(" +".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                if (splitted != null && splitted.size >= 4 && ip == splitted[0]) {
                    // Basic sanity check
                    val mac = splitted[3]
                    return if (mac.matches("..:..:..:..:..:..".toRegex())) {
                        mac.toUpperCase()
                    } else {
                        ""
                    }
                }
                line = br.readLine()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                br?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        return ""
    }

    companion object {
        private var scanHandler: Handler? = null
        private var deviceScannerUtil = DeviceScannerUtil()

        val instance: DeviceScannerUtil
            get() {
                deviceScannerUtil =  deviceScannerUtil ?: DeviceScannerUtil()
                return deviceScannerUtil
            }
    }
}
