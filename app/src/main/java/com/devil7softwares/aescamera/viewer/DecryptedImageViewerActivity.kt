package com.devil7softwares.aescamera.viewer

import android.annotation.TargetApi
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.view.MenuItem
import android.widget.Toast
import com.devil7softwares.aescamera.AESCameraApplication
import com.devil7softwares.aescamera.databinding.ActivityDecryptedImageViewerBinding
import com.devil7softwares.aescamera.utils.EncryptionUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DecryptedImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDecryptedImageViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDecryptedImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        var fileUri = intent.data;

        if (fileUri == null) {
            fileUri = if (VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            } else {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            }
        }

        if (fileUri != null) {
            loadDecryptedImage(fileUri)
        } else {
            Toast.makeText(this, "Error: No file selected", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun loadDecryptedImage(fileUri: Uri) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(fileUri)
                val encryptedBytes =
                    inputStream?.readBytes() ?: throw Exception("Unable to read file")
                inputStream.close()

                val app = application as AESCameraApplication
                val key = app.key ?: throw Exception("Encryption key not available")

                val decryptedBytes = EncryptionUtils.decrypt(encryptedBytes, key)
                val bitmap = BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)

                withContext(Dispatchers.Main) {
                    binding.decryptedImageView.setImageBitmap(bitmap)
                    supportActionBar?.title = fileUri.lastPathSegment ?: "Decrypted Image"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@DecryptedImageViewerActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}