package com.devil7softwares.aescamera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.devil7softwares.aescamera.databinding.ActivityMainBinding
import com.devil7softwares.aescamera.list.FilesListActivity
import com.devil7softwares.aescamera.utils.EncryptionUtils
import com.otaliastudios.cameraview.CameraException
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.gesture.Gesture
import com.otaliastudios.cameraview.gesture.GestureAction
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale


class MainActivity : ProtectedBaseActivity() {
    private lateinit var binding: ActivityMainBinding;

    private var thumbnailSize: Int = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide();

        // Check camera permissions if all permission granted
        // start camera else ask for the permission
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        binding.camera.setLifecycleOwner(this);

        val app = application as AESCameraApplication

        app.keyLiveData.observe(this) {
            binding.lockButton.visibility = if (it == null) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }
        }

        binding.camera.addCameraListener(object : com.otaliastudios.cameraview.CameraListener() {
            override fun onPictureShutter() {
                super.onPictureShutter()
                pictureShutter()
            }

            override fun onPictureTaken(pictureResult: PictureResult) {
                super.onPictureTaken(pictureResult)
                pictureTaken(pictureResult)
            }

            override fun onCameraError(exception: CameraException) {
                super.onCameraError(exception)
                cameraError(exception)
            }
        })

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

        thumbnailSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics).toInt()
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
        binding.camera.facing = if (binding.camera.facing == Facing.FRONT) {
            Facing.BACK
        } else {
            Facing.FRONT
        }
    }

    private fun takePhoto() {
        binding.camera.takePicture()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun cameraError(exception: CameraException) {
        val msg = "Photo capture failed: ${exception.message}"
        Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
        Log.e(TAG, msg, exception)
        enableControls()
    }

    private fun pictureShutter() {
        disableControls()
    }

    private fun pictureTaken(pictureResult: PictureResult) {
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
        EncryptionUtils.encrypt(pictureResult.data, key).let {
            encryptedFile.writeBytes(it)
        }

        // Create and encrypt thumbnail
        val thumbnailFile = File(thumbnailDirectory, fileName)

        // Create thumbnail
        val fullSizeImage = BitmapFactory.decodeByteArray(pictureResult.data, 0, pictureResult.data.size)

        val thumbnailWidth = thumbnailSize;
        val thumbnailHeight = fullSizeImage.height * thumbnailWidth / fullSizeImage.width;

        val thumbnail = Bitmap.createScaledBitmap(fullSizeImage, thumbnailWidth, thumbnailHeight, true)

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

    // checks the camera permission
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // If all permissions granted , then start Camera
            if (!allPermissionsGranted()) {
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
}