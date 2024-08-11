package com.devil7softwares.aescamera.utils

import java.text.SimpleDateFormat
import java.util.Locale

class CommonUtils {
    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

        fun getImageFileName(): String {
            return "IMG-${SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())}.enc"
        }
    }
}