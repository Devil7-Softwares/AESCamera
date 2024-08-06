package com.devil7softwares.aescamera.viewer

import android.os.Bundle
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.devil7softwares.aescamera.AESCameraApplication
import com.devil7softwares.aescamera.ProtectedBaseActivity
import com.devil7softwares.aescamera.R
import com.devil7softwares.aescamera.databinding.ActivityDecryptedImageViewerBinding
import com.devil7softwares.aescamera.utils.EncryptionUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DecryptedImageViewerActivity : ProtectedBaseActivity() {

    private lateinit var binding: ActivityDecryptedImageViewerBinding
    private var fileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDecryptedImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.decrypted_image_viewer_title)

        fileUri = intent.data;

        if (fileUri == null) {
            fileUri = if (VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            } else {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            }
        }

        if (fileUri != null) {
            supportActionBar?.title =
                fileUri?.lastPathSegment ?: getString(R.string.decrypted_image_viewer_title)
            loadDecryptedImage()
        } else {
            Toast.makeText(this, getString(R.string.error_no_file_selected), Toast.LENGTH_SHORT).show()
            finish()
        }

        (application as AESCameraApplication).keyLiveData.observe(this) {
            loadDecryptedImage()
        }

        binding.decryptedImageViewResetKey.setOnClickListener {
            (application as AESCameraApplication).key = null
        }
    }

    private fun clearError() {
        binding.decryptedImageViewError.text = ""
        binding.decryptedImageViewError.visibility = View.GONE
        binding.decryptedImageViewResetKey.visibility = View.GONE
    }

    private fun showError(message: String, showResetKey: Boolean = false) {
        binding.decryptedImageViewError.text = message
        binding.decryptedImageViewError.visibility = View.VISIBLE
        binding.decryptedImageViewResetKey.visibility = if (showResetKey) View.VISIBLE else View.GONE
        binding.decryptedImageView.setImageResource(R.drawable.ic_blank)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun loadDecryptedImage() {
        val fileUri = this.fileUri;

        if (fileUri == null) {
            showError(getString(R.string.error_no_file_selected))
            return
        }

        clearError()

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val app = application as AESCameraApplication
                val key = app.key

                if (key == null) {
                    withContext(Dispatchers.Main) {
                        showError(getString(R.string.enter_key_message))
                    }
                    return@launch
                }

                val inputStream = contentResolver.openInputStream(fileUri)

                if (inputStream == null) {
                    withContext(Dispatchers.Main) {
                        showError(getString(R.string.file_open_failed_message))
                    }
                    return@launch
                }

                val encryptedBytes = inputStream.readBytes()
                inputStream.close()

                val decryptedBytes = runCatching {
                    EncryptionUtils.decrypt(encryptedBytes, key)
                }.getOrElse {
                    Log.e(TAG, "Error: ${it.message}", it)
                    withContext(Dispatchers.Main) {
                        showError(getString(R.string.image_decryption_failed_message), true)
                    }
                    return@launch
                }

                val bitmap = runCatching {
                    BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
                }.getOrElse {
                    Log.e(TAG, "Error: ${it.message}", it)
                    withContext(Dispatchers.Main) {
                        showError(getString(R.string.image_decode_failed_message))
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    binding.decryptedImageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@DecryptedImageViewerActivity, "Error: ${e.message}", Toast.LENGTH_LONG
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

    companion object {
        private const val TAG = "DecryptedImageViewer"
    }
}