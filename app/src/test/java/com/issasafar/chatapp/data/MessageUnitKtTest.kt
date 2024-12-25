package com.issasafar.chatapp.data

import org.junit.Assert.assertEquals
import org.junit.Test


class MessageUnitKtTest {

    @Test
    fun encryptTest() {
       val msg = "hello";
        val salt = ByteArray(16) { 1 } // Example salt
        val iv = ByteArray(16) { 2 }  // Example IV
        val encrypted = msg.encrypt("password")
        val decrypted = encrypted.decrypt("password")
        assertEquals(msg, decrypted)
    }

    @Test
    fun decryptTest() {

    }
}