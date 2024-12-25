package com.issasafar.chatapp.viewmodels

enum class ConnectionType {
    SERVER,
    CLIENT
}
data class ChatStateHolder (
    var connectionType: ConnectionType,
    var userName: String,
    var ip: String,
    var port: Int,
    var message: String,
    var error: String? = null
)