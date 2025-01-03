package com.devil7softwares.aescamera.utils

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@Suppress("SpellCheckingInspection")
internal class EncryptionUtils {
    companion object {
        // Initialize the cipher with the given mode, password, salt, and IV
        fun initCipher(mode: Int, password: String, salt: ByteArray? = null, iv: ByteArray? = null): Cipher {
            val actualSalt = salt ?: ByteArray(16).apply { SecureRandom().nextBytes(this) }
            val actualIv = iv ?: ByteArray(16).apply { SecureRandom().nextBytes(this) }

            // Derive the key
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val spec = PBEKeySpec(password.toCharArray(), actualSalt, 65536, 256) // 256-bit key
            val tmp = factory.generateSecret(spec)
            val secretKey = SecretKeySpec(tmp.encoded, "AES")

            // Initialize the cipher
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(mode, secretKey, IvParameterSpec(actualIv))

            return cipher
        }

        fun encrypt(stream: ByteArrayOutputStream, password: String): ByteArray {
            return encrypt(stream.toByteArray(), password)
        }

        fun encrypt(byteArray: ByteArray, password: String): ByteArray {
            // Generate a random salt
            val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
            val cipher = initCipher(Cipher.ENCRYPT_MODE, password, salt)
            val encrypted = cipher.doFinal(byteArray)
            val iv = cipher.iv

            // Combine salt + iv + encrypted data
            return salt + iv + encrypted
        }

        fun decrypt(encryptedData: ByteArray, password: String): ByteArray {
            // Extract salt, iv, and encrypted content
            val salt = encryptedData.slice(0 until 16).toByteArray()
            val iv = encryptedData.slice(16 until 32).toByteArray()
            val encrypted = encryptedData.slice(32 until encryptedData.size).toByteArray()

            val cipher = initCipher(Cipher.DECRYPT_MODE, password, salt, iv)
            return cipher.doFinal(encrypted)
        }
    }
}