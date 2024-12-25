package com.issasafar.chatapp.data

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import android.icu.text.SimpleDateFormat
import com.issasafar.chatapp.viewmodels.ConnectionType
import java.util.Date
import java.util.Locale


fun getTime(): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date())
}

data class MessageUnit(
    val owner: String,
    var message: String,
    val type: ConnectionType,
    val senderIp: String,
    val description: String,
    val isEchoMessage: Boolean = false,
    val time: String = getTime(),
    var isEncrypted: Boolean = false
) {
    val id: Int get() = hashCode()
}



private const val ALGORITHM = "AES/CBC/PKCS5Padding"
private const val SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
private const val KEY_LENGTH = 256
private const val ITERATION_COUNT = 65536

// Generate a secure salt (for production)
fun generateSalt(): ByteArray {
    val salt = ByteArray(16)
    SecureRandom().nextBytes(salt)
    return salt
}

// Generate a secure IV (for production)
fun generateIV(): ByteArray {
    val iv = ByteArray(16)
    SecureRandom().nextBytes(iv)
    return iv
}

val salt = ByteArray(16) { 1 } // Example salt
val iv = ByteArray(16) { 2 }  // Example IV
// Encrypt the string
fun String.encrypt(password: String, salt: ByteArray = generateSalt(), iv: ByteArray = generateIV()): String {
    try {
        val secretKeySpec = generateSecretKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, IvParameterSpec(iv))
        val encryptedBytes = cipher.doFinal(this.toByteArray(Charsets.UTF_8))
        val data = salt + iv + encryptedBytes // Combine salt, IV, and encrypted data
        return androidBase64Encode(data)
    } catch (e: Exception) {
        throw RuntimeException("Error encrypting string", e)
    }
}

// Decrypt the string
fun String.decrypt(password: String): String {
    try {
        val decodedData = androidBase64Decode(this)
        val salt = decodedData.copyOfRange(0, 16) // Extract salt
        val iv = decodedData.copyOfRange(16, 32) // Extract IV
        val encryptedBytes = decodedData.copyOfRange(32, decodedData.size) // Extract encrypted data
        val secretKeySpec = generateSecretKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, IvParameterSpec(iv))
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    } catch (e: Exception) {
        throw RuntimeException("Error decrypting string", e)
    }
}

// Generate SecretKeySpec using PBKDF2
private fun generateSecretKey(password: String, salt: ByteArray): SecretKeySpec {
    val factory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM)
    val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
    val secretKey = factory.generateSecret(spec)
    return SecretKeySpec(secretKey.encoded, "AES")
}

// Helper function for Base64 encoding (supports both JVM and Android)
private fun androidBase64Encode(data: ByteArray): String {
    return Base64.encodeToString(data, Base64.DEFAULT)
}

// Helper function for Base64 decoding (supports both JVM and Android)
private fun androidBase64Decode(data: String): ByteArray {
    return Base64.decode(data, Base64.DEFAULT)
}
