package com.devil7softwares.aescamera

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

abstract class ProtectedBaseActivity : AppCompatActivity() {
    private var dialogOpen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPassword()
    }

    override fun onPause() {
        super.onPause()

        // Clear the key when the app is paused
        (application as AESCameraApplication).key = null
    }

    override fun onResume() {
        super.onResume()

        checkPassword()
    }

    private fun checkPassword() {
        val app = application as AESCameraApplication

        if (app.key == null) {
            showPasswordDialog()
        }
    }

    private fun showPasswordDialog() {
        if (dialogOpen) {
            return
        }

        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_key, null)
        val passwordEditText = dialogView.findViewById<EditText>(R.id.keyText)

        builder.setView(dialogView)
            .setTitle(getString(R.string.enter_encryption_key_title))
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                dialogOpen = false

                val password = passwordEditText.text.toString()
                if (password.isNotEmpty()) {
                    (application as AESCameraApplication).key = password
                } else {
                    showPasswordDialog()
                }
            }

        dialogOpen = true
        builder.create().show()
    }
}
