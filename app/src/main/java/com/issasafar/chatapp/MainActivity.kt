package com.issasafar.chatapp

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.issasafar.chatapp.data.MessageUnit
import com.issasafar.chatapp.data.encrypt
import com.issasafar.chatapp.socket.SocketClient
import com.issasafar.chatapp.socket.SocketServer
import com.issasafar.chatapp.viewmodels.ConnectionType
import com.issasafar.chatapp.viewmodels.MainScreenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ClassCastException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        val mainScreenViewModel: MainScreenViewModel by viewModels()
        lateinit var client: SocketClient
        lateinit var server: SocketServer

        setContent {
            val messages = remember { mutableStateListOf<MessageUnit>() }
            val uiState by remember { mutableStateOf(mainScreenViewModel.uiState) }
            var clientConnected by remember { mutableStateOf(false) }
            var serverStarted by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf("") }
            var showErrorDialog by remember { mutableStateOf(false) }
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                mainScreenViewModel.updateDeviceIp()
            }
            val scope = rememberCoroutineScope()
            val deviceIp by mainScreenViewModel.deviceIp.collectAsState()
            val sendClicked: (String, Boolean, String) -> Unit =
                { messageStr, isEncrypted, password ->
                    val isClient = uiState.value.connectionType == ConnectionType.CLIENT
                    val messageUnit =
                        MessageUnit(
                            owner = uiState.value.userName,
                            message = messageStr,
                            type = uiState.value.connectionType,
                            senderIp = deviceIp,
                            description = "",
                            isEncrypted = isEncrypted
                        )

                    try {
                        if (isClient) {
                            // Send message only if connected
                            if (clientConnected) {
                                if (messageUnit.isEncrypted) {
                                    scope.launch {
                                        val newMsg =
                                            messageUnit.copy(
                                                message = messageUnit.message.encrypt(
                                                    password
                                                )
                                            )
                                        client.sendMessage(Gson().toJson(newMsg))
                                    }
                                    messages.add(
                                        messageUnit.copy(
                                            description = "Sent to server: " + uiState.value.ip,
                                            isEncrypted = false
                                        )
                                    )
                                } else {
                                    client.sendMessage(Gson().toJson(messageUnit))
                                    messages.add(messageUnit.copy(description = "Sent to server: " + uiState.value.ip))
                                }
                            } else {
                                error = "Client is not connected. Try reconnecting."
                                showErrorDialog = true
                            }
                        }
                    } catch (e: Exception) {
                        e.message?.let { err ->
                            error = err
                            showErrorDialog = true
                        }
                    }
                }

            val configButtonClicked: () -> Unit = {
                val isClient = uiState.value.connectionType == ConnectionType.CLIENT
                try {
                    if (isClient) {
                        // Initialize and connect client if not connected
                        if (!clientConnected) {
                            client = SocketClient(uiState.value.ip, uiState.value.port)
                            client.connect { isConnected ->
                                clientConnected = isConnected
                                if (!isConnected) {
                                    error = "Failed to connect to server"
                                    showErrorDialog = true
                                }
                            }

                            // Observe messages from the server
                            CoroutineScope(Dispatchers.Main).launch {
                                client.messagesFlow.collect { receivedMessage ->
                                    if (receivedMessage.isNotBlank()) {
                                        val receivedUnit = Gson().fromJson(
                                            receivedMessage,
                                            MessageUnit::class.java
                                        )
                                        messages.add(receivedUnit)
                                    }
                                }
                            }
                        }
                    } else {
                        // Start server if not running
                        if (!serverStarted) {
                            server = SocketServer(uiState.value.port)
                            server.startServer(
                                onClientConnected = { clientIp ->
                                    println("Client connected from IP: $clientIp")
                                },
                                serverName = uiState.value.userName
                            )
                            serverStarted = true

                            // Observe messages from clients
                            CoroutineScope(Dispatchers.Main).launch {
                                server.messagesFlow.collect { receivedMessage ->
                                    if (receivedMessage.isNotBlank()) {
                                        try {
                                            val receivedUnit = Gson().fromJson(
                                                receivedMessage,
                                                MessageUnit::class.java
                                            )
                                            messages.add(receivedUnit.copy(description = "Received from client: " + receivedUnit.senderIp))
                                        } catch (e: JsonSyntaxException) {
                                            e.printStackTrace()
                                        } catch (e: ClassCastException) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }

                        } else {
                            // Stop server if already running
                            server.stopServer()
                            serverStarted = false
                        }
                    }
                } catch (e: Exception) {
                    e.message?.let { err ->
                        error = err
                        showErrorDialog = true
                    }
                }
            }

            MaterialTheme {
                Scaffold(
                    content = { paddingValues ->
                        MainScreen(
                            modifier = Modifier
                                .padding(paddingValues = paddingValues),
                            onSendClick = sendClicked,
                            chatStateHolder = uiState.collectAsState(),
                            onChipSelected = {
                                mainScreenViewModel.setConnection(it)
                            },
                            onConfigureClicked = {

                                    nameIpPair ->
                                try {
                                    mainScreenViewModel.setUserName(nameIpPair.first)
                                    mainScreenViewModel.setTargetIp(nameIpPair.second)
                                    serverStarted = false
                                    clientConnected = false
                                    configButtonClicked()
                                } catch (e: Exception) {
                                    e.message?.let {
                                        error = it
                                        showErrorDialog = true
                                    }
                                }
                            },
                            messages = messages,
                            deviceIp = { deviceIp },
                            onDecryptionError = {
                                Toast.makeText(context, "Decryption error", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        )
                        if (showErrorDialog) {
                            AlertDialog(
                                onDismissRequest = { showErrorDialog = false },
                                title = { Text("Error") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        showErrorDialog = false
                                    }) {
                                        Text("OK")
                                    }
                                },
                                text = { Text(error) })
                        }
                    },
                )
            }
        }
    }
}
