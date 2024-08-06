package com.devil7softwares.aescamera

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import java.io.File

class AESCameraApplication : Application() {
    var keyLiveData: MutableLiveData<String?> = MutableLiveData()
    var key: String? = null
        set(value) {
            field = value
            keyLiveData.value = value

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
                val keyDir = File(mediaDir, value).apply { mkdirs() }

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
        private set(value) {
            field = value

            if (value != null) {
                this.thumbnailDirectory = File(value, ".thumbs").apply { mkdirs() }
            }
        }
    var thumbnailDirectory: File? = null
        private set

    private var activeActivities = 0
    private val handler = Handler(Looper.getMainLooper())
    private val clearKeyDelay = 5000L

    fun onActivityStarted() {
        activeActivities++
        handler.removeCallbacksAndMessages(null)
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                activeActivities++
                handler.removeCallbacks(clearKeyRunnable)
            }

            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                onActivityStopped()
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun onActivityStopped() {
        activeActivities--
        if (activeActivities == 0) {
            handler.postDelayed(clearKeyRunnable, clearKeyDelay)
        }
    }

    private val clearKeyRunnable = Runnable {
        key = null
    }
}
