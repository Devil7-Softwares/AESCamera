package com.devil7softwares.aescamera

import android.app.Application

class AESCameraApplication : Application() {
    var key: String? = null
        set(value) {
            field = value

            if (value == null) {
                this.keyDigest = null
                return
            }

            // Generate MD5 digest of the key
            val md = java.security.MessageDigest.getInstance("MD5")
            val digest = md.digest(value.toByteArray())
            val sb = StringBuilder()
            for (b in digest) {
                sb.append(String.format("%02x", b))
            }
            this.keyDigest = sb.toString()
        }
    var keyDigest: String? = null
        private set
}
