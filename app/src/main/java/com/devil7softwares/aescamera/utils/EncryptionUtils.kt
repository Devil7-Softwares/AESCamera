package com.devil7softwares.aescamera.utils

import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@Suppress("SpellCheckingInspection")
internal class EncryptionUtils {
    companion object {
        fun encrypt(stream: ByteArrayOutputStream, password: String): ByteArray {
            // Generate a random salt
            val salt = ByteArray(16)
            SecureRandom().nextBytes(salt)

            // Derive the key
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(password.toCharArray(), salt, 65536, 256) // 256-bit key
            val tmp = factory.generateSecret(spec)
            val secretKey = SecretKeySpec(tmp.encoded, "AES")

            // Generate a random IV
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)

            // Initialize the cipher
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)

            // Perform encryption
            val encrypted = cipher.doFinal(stream.toByteArray())

            // Combine salt + iv + encrypted data
            return salt + iv + encrypted
        }

        fun decrypt(encryptedData: ByteArray, password: String): ByteArray {
            // Extract salt, iv, and encrypted content
            val salt = encryptedData.slice(0 until 16).toByteArray()
            val iv = encryptedData.slice(16 until 32).toByteArray()
            val encrypted = encryptedData.slice(32 until encryptedData.size).toByteArray()

            // Derive the key
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(password.toCharArray(), salt, 65536, 256) // 256-bit key
            val tmp = factory.generateSecret(spec)
            val secretKey = SecretKeySpec(tmp.encoded, "AES")

            // Initialize the cipher for decryption
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

            // Perform decryption
            return cipher.doFinal(encrypted)
        }
    }
}