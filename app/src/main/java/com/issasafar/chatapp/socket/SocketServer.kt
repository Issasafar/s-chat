package com.issasafar.chatapp.socket

import com.google.gson.Gson
import com.issasafar.chatapp.data.MessageUnit
import com.issasafar.chatapp.viewmodels.ConnectionType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

fun getIpAddress(): String {
    NetworkInterface.getNetworkInterfaces()?.toList()?.map { networkInterface ->
        networkInterface.inetAddresses?.toList()?.find {
            !it.isLoopbackAddress && it is Inet4Address
        }?.let { return it.hostAddress!! }
    }
    return ""
}
class SocketServer(private val port: Int) {

    private var isRunning = true
    private val _messagesFlow = MutableStateFlow("") // Emits new messages for the UI
    val messagesFlow: StateFlow<String> get() = _messagesFlow

    fun startServer(onClientConnected: (String) -> Unit, serverName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val serverSocket = ServerSocket(port)
                val serverIp = serverSocket.inetAddress.hostAddress
                println("Server started on port $port")

                while (isRunning) {
                    val clientSocket: Socket = serverSocket.accept()
                    println("Client connected: ${clientSocket.inetAddress.hostAddress}")
                    withContext(Dispatchers.Main) {
                        clientSocket.inetAddress.hostAddress?.let { onClientConnected(it) }
                    }
                    handleClient(clientSocket, serverName, serverIp!!)
                }

                serverSocket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleClient(clientSocket: Socket, serverName: String, serverIp: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                val writer = PrintWriter(clientSocket.getOutputStream(), true)
                // Read messages from the client
                var message: String? = null
                while (clientSocket.isConnected && reader.readLine()
                        .also { message = it } != null
                ) {
                    message?.let {
                        println("Received: $it")
                        _messagesFlow.emit(it) // Notify UI about the received message
                        val messageUnit = Gson().fromJson(it, MessageUnit::class.java)
                        val ip = getIpAddress()
                        writer.println(
                            Gson().toJson(
                                MessageUnit(
                                    owner = serverName,
                                    message = messageUnit.message,
                                    type =ConnectionType.SERVER,
                                    senderIp = ip,
                                    description = "Server '${ip}' received your message",
                                    isEchoMessage = true
                                )
                            )
                        )
                    }
                }

                clientSocket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopServer() {
        isRunning = false
    }
}
