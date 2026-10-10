package com.aurora.server

import android.os.Looper
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AuroraService {

    companion object {

        private const val TAG = "AURORA_DAEMON"
        private const val DAEMON_PORT = 52847
        private val START_TIME = System.currentTimeMillis()

        private val threadPool: ExecutorService = Executors.newFixedThreadPool(5)

        @JvmStatic
        fun main(args: Array<String>) {
            if (Looper.getMainLooper() == null) {
                Looper.prepareMainLooper()
            }

            println("info: aurora_starter exit with 0")
            println("aurora_PID_REPORT:" + android.os.Process.myPid())

            var serverSocket: ServerSocket? = null
            try {
                serverSocket = ServerSocket(DAEMON_PORT)
                println("Aurora Daemon started on Private Port: $DAEMON_PORT")
            } catch (e: Exception) {
                println("info: Aurora daemon already running. Exiting duplicate.")
                System.exit(0)
                return
            }

            while (true) {
                try {
                    val clientSocket = serverSocket!!.accept()
                    handleClientRequest(clientSocket)
                } catch (e: Exception) {
                    Log.e(TAG, "Socket accept error, but keeping daemon alive...", e)
                }
            }
        }

        private fun handleClientRequest(socket: Socket) {
            threadPool.execute {
                try {
                    socket.use { s ->
                        BufferedReader(InputStreamReader(s.inputStream)).use { `in` ->
                            PrintWriter(s.outputStream, true).use { out ->
                                val request = `in`.readLine()
                                if (request != null) {
                                    if (request == "PING") {
                                        out.println("PONG:" + android.os.Process.myPid() + ":" + START_TIME)
                                    } else if (request.startsWith("EXEC:")) {
                                        val command = request.substring(5)
                                        executeCommand(command, out)
                                    }
                                }
                            }
                        }
                    }
                } catch (ignored: Exception) {}
            }
        }

        private fun executeCommand(command: String, out: PrintWriter) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))

                var line: String?
                while (reader.readLine().also { line = it } != null) out.println(line)
                while (errorReader.readLine().also { line = it } != null) out.println("[ERROR] $line")

                process.waitFor()
                out.println("===EOF===")
            } catch (e: Exception) {
                out.println("[ERROR FATAL] " + e.message)
                out.println("===EOF===")
            }
        }
    }
}