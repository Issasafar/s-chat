package com.issasafar.chatapp.socket

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.PrintWriter
import java.net.Socket

class SocketClient(private val serverIp: String, private val serverPort: Int) {

    private var clientSocket: Socket? = null
    private var writer: PrintWriter? = null
    private val _messagesFlow = MutableStateFlow("") // Emits new messages for the UI
    val messagesFlow: StateFlow<String> get() = _messagesFlow

    fun connect(onConnectionStatusChanged: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                clientSocket = Socket(serverIp, serverPort)
                writer = PrintWriter(clientSocket!!.getOutputStream(), true)
                withContext(Dispatchers.Main) { onConnectionStatusChanged(true) }
                println("Connected to server at $serverIp:$serverPort")

                // Start listening for messages from the server
                listenForMessages(clientSocket!!)
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onConnectionStatusChanged(false) }
            }
        }
    }

    private fun listenForMessages(socket: Socket) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reader = socket.getInputStream().bufferedReader()
                var message: String? = null
                while (socket.isConnected && reader.readLine().also { message = it } != null) {
                    message?.let {
                        _messagesFlow.emit(it) // Notify UI about the received message
                        println("Server says: $it")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendMessage(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            writer?.println(message)
        }
    }

    fun disconnect() {
        try {
            clientSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
