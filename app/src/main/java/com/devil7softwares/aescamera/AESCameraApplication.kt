package com.devil7softwares.aescamera

import android.app.Application
import java.io.File

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
        private set(value) {
            field = value

            val mediaDir = externalMediaDirs.firstOrNull()?.let {
                File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
            }

            if (value != null && mediaDir != null && mediaDir.exists()) {
                val keyDir = File(mediaDir, value) .apply { mkdirs() }

                if (keyDir.exists()) {
                    this.outputDirectory = keyDir
                } else {
                    this.outputDirectory = mediaDir
                }
            } else {
                this.outputDirectory = filesDir
            }
        }
    var outputDirectory: File? = null
        private set (value) {
            field = value

            if (value != null) {
                this.thumbnailDirectory = File(value, ".thumbs").apply { mkdirs() }
            }
        }
    var thumbnailDirectory: File? = null
        private set
}
