package com.aurora.adb

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.net.ssl.SSLSocket

private const val TAG = "AdbPairClient"
private const val kCurrentKeyHeaderVersion = 1.toByte()
private const val kMinSupportedKeyHeaderVersion = 1.toByte()
private const val kMaxSupportedKeyHeaderVersion = 1.toByte()
private const val kMaxPeerInfoSize = 8192
private const val kMaxPayloadSize = kMaxPeerInfoSize * 2
private const val kExportedKeyLabel = "adb-label\u0000"
private const val kExportedKeySize = 64
private const val kPairingPacketHeaderSize = 6

private class PeerInfo(val type: Byte, data: ByteArray) {
    val data = ByteArray(kMaxPeerInfoSize - 1)
    init {
        data.copyInto(this.data, 0, 0, data.size.coerceAtMost(kMaxPeerInfoSize - 1))
    }
    enum class Type(val value: Byte) {
        ADB_RSA_PUB_KEY(0.toByte()),
        ADB_DEVICE_GUID(0.toByte()),
    }
    fun writeTo(buffer: ByteBuffer) {
        buffer.run {
            put(type)
            put(data)
        }
    }
    companion object {
        fun readFrom(buffer: ByteBuffer): PeerInfo {
            val type = buffer.get()
            val data = ByteArray(kMaxPeerInfoSize - 1)
            buffer.get(data)
            return PeerInfo(type, data)
        }
    }
}

private class PairingPacketHeader(val version: Byte, val type: Byte, val payload: Int) {
    enum class Type(val value: Byte) {
        SPAKE2_MSG(0.toByte()),
        PEER_INFO(1.toByte())
    }
    fun writeTo(buffer: ByteBuffer) {
        buffer.run {
            put(version)
            put(type)
            putInt(payload)
        }
    }
    companion object {
        fun readFrom(buffer: ByteBuffer): PairingPacketHeader? {
            val version = buffer.get()
            val type = buffer.get()
            val payload = buffer.int
            if (version < kMinSupportedKeyHeaderVersion || version > kMaxSupportedKeyHeaderVersion) return null
            if (type != Type.SPAKE2_MSG.value && type != Type.PEER_INFO.value) return null
            if (payload <= 0 || payload > kMaxPayloadSize) return null
            return PairingPacketHeader(version, type, payload)
        }
    }
}

// CATATAN: PairingContext versi Kotlin saya hapus agar aplikasi Anda 
// murni menggunakan file 'PairingContext.java' yang sudah Anda buat sebelumnya.

@RequiresApi(Build.VERSION_CODES.R)
class AdbPairingClient(private val host: String, private val port: Int, private val pairCode: String, private val key: AdbKey) : Closeable {
    private enum class State { Ready, ExchangingMsgs, ExchangingPeerInfo, Stopped }
    private lateinit var socket: Socket
    private lateinit var inputStream: DataInputStream
    private lateinit var outputStream: DataOutputStream
    private val peerInfo: PeerInfo = PeerInfo(PeerInfo.Type.ADB_RSA_PUB_KEY.value, key.adbPublicKey)
    
    // Kotlin akan membaca PairingContext.java Anda sebagai objek
    private lateinit var pairingContext: PairingContext 
    private var state: State = State.Ready

    fun start(): Boolean {
        setupTlsConnection()
        state = State.ExchangingMsgs
        if (!doExchangeMsgs()) { state = State.Stopped; return false }
        state = State.ExchangingPeerInfo
        if (!doExchangePeerInfo()) { state = State.Stopped; return false }
        state = State.Stopped
        return true
    }

    private fun setupTlsConnection() {
        socket = Socket(host, port)
        socket.tcpNoDelay = true
        val sslContext = key.sslContext
        val sslSocket = sslContext.socketFactory.createSocket(socket, host, port, true) as SSLSocket
        sslSocket.startHandshake()
        inputStream = DataInputStream(sslSocket.inputStream)
        outputStream = DataOutputStream(sslSocket.outputStream)
        
        val pairCodeBytes = pairCode.toByteArray()
        
        // =========================================================
        // PENGAMBILAN KUNCI ENKRIPSI
        // Karena blokir API sudah dihancurkan oleh HiddenApiBypass di SketchApplication,
        // kita bisa langsung memanggil kelas rahasia Conscrypt dengan Reflection bersih.
        // =========================================================
        try {
            val conscryptClass = Class.forName("com.android.org.conscrypt.Conscrypt")
            val exportMethod = conscryptClass.getMethod("exportKeyingMaterial", SSLSocket::class.java, String::class.java, ByteArray::class.java, Int::class.javaPrimitiveType)
            val keyMaterial = exportMethod.invoke(null, sslSocket, kExportedKeyLabel, null, kExportedKeySize) as ByteArray
            
            val passwordBytes = ByteArray(pairCode.length + keyMaterial.size)
            pairCodeBytes.copyInto(passwordBytes)
            keyMaterial.copyInto(passwordBytes, pairCodeBytes.size)
            
            // Memanggil PairingContext.java
            this.pairingContext = PairingContext(true, passwordBytes)
            
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengambil exportKeyingMaterial! Pastikan SketchApplication berjalan dengan benar.", e)
            throw e
        }
    }

    private fun createHeader(type: PairingPacketHeader.Type, payloadSize: Int): PairingPacketHeader {
        return PairingPacketHeader(kCurrentKeyHeaderVersion, type.value, payloadSize)
    }

    private fun readHeader(): PairingPacketHeader? {
        val bytes = ByteArray(kPairingPacketHeaderSize)
        inputStream.readFully(bytes)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        return PairingPacketHeader.readFrom(buffer)
    }

    private fun writeHeader(header: PairingPacketHeader, payload: ByteArray) {
        val buffer = ByteBuffer.allocate(kPairingPacketHeaderSize).order(ByteOrder.BIG_ENDIAN)
        header.writeTo(buffer)
        outputStream.write(buffer.array())
        outputStream.write(payload)
    }

    private fun doExchangeMsgs(): Boolean {
        // Otomatis memanggil getMsg() dari file Java Anda
        val msg = pairingContext.msg
        val size = msg.size
        val ourHeader = createHeader(PairingPacketHeader.Type.SPAKE2_MSG, size)
        writeHeader(ourHeader, msg)
        val theirHeader = readHeader() ?: return false
        if (theirHeader.type != PairingPacketHeader.Type.SPAKE2_MSG.value) return false
        val theirMessage = ByteArray(theirHeader.payload)
        inputStream.readFully(theirMessage)
        if (!pairingContext.initCipher(theirMessage)) return false
        return true
    }

    private fun doExchangePeerInfo(): Boolean {
        val buf = ByteBuffer.allocate(kMaxPeerInfoSize).order(ByteOrder.BIG_ENDIAN)
        peerInfo.writeTo(buf)
        val outbuf = pairingContext.encrypt(buf.array()) ?: return false
        val ourHeader = createHeader(PairingPacketHeader.Type.PEER_INFO, outbuf.size)
        writeHeader(ourHeader, outbuf)
        val theirHeader = readHeader() ?: return false
        if (theirHeader.type != PairingPacketHeader.Type.PEER_INFO.value) return false
        val theirMessage = ByteArray(theirHeader.payload)
        inputStream.readFully(theirMessage)
        val decrypted = pairingContext.decrypt(theirMessage) ?: throw AdbInvalidPairingCodeException()
        if (decrypted.size != kMaxPeerInfoSize) return false
        return true
    }

    override fun close() {
        try { inputStream.close() } catch (e: Throwable) {}
        try { outputStream.close() } catch (e: Throwable) {}
        try { socket.close() } catch (e: Exception) {}
        if (state != State.Ready) { pairingContext.destroy() }
    }
}
