@file:Suppress("DEPRECATION")
package com.aurora.adb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.Observer
import java.io.IOException
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket

import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

@RequiresApi(Build.VERSION_CODES.R)
class AdbMdns(context: Context, private val serviceType: String, private val observer: Observer<Int>) {

    private var registered = false
    private var running = false
    private var serviceName: String? = null
    private val listener = DiscoveryListener(this)
    private val nsdManager: NsdManager = context.getSystemService(NsdManager::class.java)
    private val isResolving = AtomicBoolean(false)
    private val pendingResolves = Collections.synchronizedList(mutableListOf<NsdServiceInfo>())

    fun start() {
        if (running) return
        running = true
        if (!registered) {
            try {
                nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service discovery", e)
            }
        }
    }

    fun stop() {
        if (!running) return
        running = false
        if (registered) {
            try {
                nsdManager.stopServiceDiscovery(listener)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop service discovery", e)
            }
        }
        pendingResolves.clear()
        isResolving.set(false)
    }

    private fun onDiscoveryStart() {
        registered = true
    }

    private fun onDiscoveryStop() {
        registered = false
    }

    private fun onServiceFound(info: NsdServiceInfo) {
        if (!running) return
        if (isResolving.compareAndSet(false, true)) {
            resolveInternal(info)
        } else {
            pendingResolves.add(info)
        }
    }

    private fun resolveInternal(info: NsdServiceInfo) {
        try {
            nsdManager.resolveService(info, object : NsdManager.ResolveListener {
                override fun onResolveFailed(nsdServiceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.w(TAG, "onResolveFailed: ${nsdServiceInfo.serviceName}, errorCode=$errorCode")
                    checkNextResolve()
                }

                override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                    this@AdbMdns.onServiceResolved(resolvedService)
                    checkNextResolve()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "resolveService exception", e)
            checkNextResolve()
        }
    }

    private fun checkNextResolve() {
        isResolving.set(false)
        if (running && pendingResolves.isNotEmpty()) {
            val next = try { pendingResolves.removeAt(0) } catch (e: Exception) { null }
            if (next != null && isResolving.compareAndSet(false, true)) {
                resolveInternal(next)
            }
        }
    }

    private fun onServiceLost(info: NsdServiceInfo) {
        if (info.serviceName == serviceName) {
            observer.onChanged(-1)
        }
    }
    
    private fun onServiceResolved(resolvedService: NsdServiceInfo) {
        if (!running) return
        val port = resolvedService.port
        if (port > 0) {
            val host = resolvedService.host
            val isLocal = try {
                if (host == null) true
                else if (host.isLoopbackAddress || host.isLinkLocalAddress || host.isAnyLocalAddress) true
                else {
                    NetworkInterface.getNetworkInterfaces()?.asSequence()?.any { iface ->
                        iface.inetAddresses.asSequence().any { addr ->
                            addr.hostAddress == host.hostAddress || addr.equals(host)
                        }
                    } ?: true
                }
            } catch (e: Exception) {
                true
            }

            if (isLocal) {
                Log.i(TAG, "Resolved valid local ADB Service '${resolvedService.serviceName}' on port $port")
                serviceName = resolvedService.serviceName
                observer.onChanged(port)
            }
        }
    }

    internal class DiscoveryListener(private val adbMdns: AdbMdns) : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String) {
            Log.v(TAG, "onDiscoveryStarted: $serviceType")
            adbMdns.onDiscoveryStart()
        }
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.w(TAG, "onStartDiscoveryFailed: $serviceType, $errorCode")
        }
        override fun onDiscoveryStopped(serviceType: String) {
            Log.v(TAG, "onDiscoveryStopped: $serviceType")
            adbMdns.onDiscoveryStop()
        }
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.w(TAG, "onStopDiscoveryFailed: $serviceType, $errorCode")
        }
        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            Log.v(TAG, "onServiceFound: ${serviceInfo.serviceName}")
            adbMdns.onServiceFound(serviceInfo)
        }
        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            Log.v(TAG, "onServiceLost: ${serviceInfo.serviceName}")
            adbMdns.onServiceLost(serviceInfo)
        }
    }

    companion object {
        const val TLS_CONNECT = "_adb-tls-connect._tcp"
        const val TLS_PAIRING = "_adb-tls-pairing._tcp"
        const val TAG = "AdbMdns"
    }
}
