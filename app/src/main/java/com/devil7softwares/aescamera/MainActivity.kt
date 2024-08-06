package com.devil7softwares.aescamera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.devil7softwares.aescamera.databinding.ActivityMainBinding
import com.devil7softwares.aescamera.list.FilesListActivity
import com.devil7softwares.aescamera.utils.EncryptionUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ProtectedBaseActivity() {
    private lateinit var binding: ActivityMainBinding;

    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide();

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Check camera permissions if all permission granted
        // start camera else ask for the permission
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        val app = application as AESCameraApplication

        app.keyLiveData.observe(this) {
            binding.lockButton.visibility = if (it == null) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        binding.cameraCaptureButton.setOnClickListener {
            takePhoto()
        }
        binding.cameraFlipButton.setOnClickListener {
            flipCamera()
        }
        binding.openFilesListButton.setOnClickListener {
            openFilesList()
        }
        binding.lockButton.setOnClickListener {
            lock()
        }
    }

    private fun disableControls() {
        binding.cameraCaptureButton.isEnabled = false
        binding.cameraFlipButton.isEnabled = false
        binding.openFilesListButton.isEnabled = false
    }

    private fun enableControls() {
        binding.cameraCaptureButton.isEnabled = true
        binding.cameraFlipButton.isEnabled = true
        binding.openFilesListButton.isEnabled = true
    }

    private fun lock() {
        val app = application as AESCameraApplication
        app.key = null
        Toast.makeText(this, "Locked", Toast.LENGTH_SHORT).show()
    }

    private  fun openFilesList() {
        val intent = Intent(this, FilesListActivity::class.java)
        startActivity(intent)
    }

    private fun flipCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera()
    }

    private fun takePhoto() {
        disableControls()
        val imageCapture = imageCapture ?: return
        val outputStream = ByteArrayOutputStream()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputStream).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    val msg = "Photo capture failed: ${exc.message}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
                    Log.e(TAG, msg, exc)
                    enableControls()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val app = application as AESCameraApplication
                    val key = app.key
                    val outputDirectory = app.outputDirectory
                    val thumbnailDirectory = app.thumbnailDirectory
                    if (key == null) {
                        Toast.makeText(this@MainActivity, "Key not found", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val timestamp = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())

                    val fileName = "IMG-$timestamp.enc"

                    // Create main encrypted file
                    val encryptedFile = File(outputDirectory, fileName)
                    EncryptionUtils.encrypt(outputStream, key).let {
                        encryptedFile.writeBytes(it)
                    }

                    // Create and encrypt thumbnail
                    val thumbnailFile = File(thumbnailDirectory, fileName)

                    // Create thumbnail
                    val fullSizeImage = BitmapFactory.decodeByteArray(outputStream.toByteArray(), 0, outputStream.size())
                    val thumbnailSize = 200 // Adjust this value for desired thumbnail size
                    val thumbnail = Bitmap.createScaledBitmap(fullSizeImage, thumbnailSize, thumbnailSize, true)

                    val thumbnailOutputStream = ByteArrayOutputStream()
                    thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, thumbnailOutputStream)

                    // Encrypt and save thumbnail
                    EncryptionUtils.encrypt(thumbnailOutputStream, key).let {
                        thumbnailFile.writeBytes(it)
                    }

                    val msg = "Photo and thumbnail saved: ${encryptedFile.toURI()}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
                    Log.d(TAG, msg)
                    enableControls()
                }
            })
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = binding.viewFinder.surfaceProvider
                }

            imageCapture = ImageCapture.Builder().build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // checks the camera permission
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // If all permissions granted , then start Camera
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                // If permissions are not granted,
                // present a toast to notify the user that
                // the permissions were not granted.
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "AESCamera"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 20
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}