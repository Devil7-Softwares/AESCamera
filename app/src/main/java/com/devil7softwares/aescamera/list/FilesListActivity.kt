package com.devil7softwares.aescamera.list

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.view.ActionMode
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.devil7softwares.aescamera.AESCameraApplication
import com.devil7softwares.aescamera.ProtectedBaseActivity
import com.devil7softwares.aescamera.R
import com.devil7softwares.aescamera.databinding.ActivityFilesListBinding
import com.devil7softwares.aescamera.viewer.DecryptedImageViewerActivity
import java.io.File


class FilesListActivity : ProtectedBaseActivity() {
    private lateinit var binding: ActivityFilesListBinding
    private lateinit var adapter: FileAdapter
    private var actionMode: ActionMode? = null

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.context_menu, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_delete -> {
                    deleteSelectedItems()
                    true
                }

                R.id.action_share -> {
                    shareSelectedItems()
                    true
                }

                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        binding = ActivityFilesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActionBar()
        setupRecyclerView()

        val app = application as AESCameraApplication
        app.keyLiveData.observe(this) {
            refreshFileList()
        }
        refreshFileList()
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            title = getString(R.string.files_list_title)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        val app = application as AESCameraApplication
        adapter = FileAdapter(this, app.thumbnailDirectory, app.key, onItemClick = { file ->
            onItemClick(file)
        }, onSelectionChanged = { selectedCount ->
            onSelectionChanged(selectedCount)
        })
        binding.fileListRecyclerView.apply {
            layoutManager = GridLayoutManager(this@FilesListActivity, 3)
            adapter = this@FilesListActivity.adapter
        }
    }

    private fun onItemClick(file: File) {
        val intent = Intent(this, DecryptedImageViewerActivity::class.java)
        val uri = FileProvider.getUriForFile(this, application.packageName + ".provider", file)
        intent.data = uri
        startActivity(intent)
    }

    private fun onSelectionChanged(selectedCount: Int) {
        if (selectedCount > 0) {
            if (actionMode == null) {
                actionMode = startSupportActionMode(actionModeCallback)
            }
            actionMode?.title = getString(R.string.selected_count, selectedCount)
        } else {
            actionMode?.finish()
        }
    }

    private fun refreshFileList() {
        val app = application as AESCameraApplication
        val files =
            app.outputDirectory?.listFiles()?.filter { it.isFile && !it.name.startsWith(".") && it.extension == "enc" }
                ?: emptyList()
        adapter.encryptionKey = app.key
        adapter.submitList(files)
    }

    private fun deleteSelectedItems() {
        val selectedFiles = adapter.getSelectedItems()
        val app = application as AESCameraApplication

        for (file in selectedFiles) {
            val thumbnailFile = File(app.thumbnailDirectory, file.name)
            if (thumbnailFile.exists()) {
                thumbnailFile.delete()
            }

            if (file.exists()) {
                file.delete()
            }
        }

        refreshFileList()
        adapter.clearSelection()
    }

    private fun shareSelectedItems() {
        val selectedFiles = adapter.getSelectedItems()

        val intent = Intent()
        intent.setAction(Intent.ACTION_SEND_MULTIPLE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.setType("application/octet-stream")

        val files = ArrayList<Uri>()

        for (file in selectedFiles) {
            val uri = FileProvider.getUriForFile(this, application.packageName + ".provider", file)
            files.add(uri)
        }

        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}