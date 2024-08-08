package com.devil7softwares.aescamera.list

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
    private var optionsMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFilesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActionBar()
        setupRecyclerView()

        val app = application as AESCameraApplication
        app.keyLiveData.observe(this) {
            refreshFileList()

            optionsMenu?.findItem(R.id.action_lock)?.isVisible = it != null
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
        optionsMenu?.findItem(R.id.action_select_all)?.title =
            if (selectedCount == adapter.itemCount) getString(R.string.deselect_all) else
                getString(R.string.select_all)

        if (selectedCount > 0) {
            optionsMenu?.findItem(R.id.action_delete)?.isVisible = true
            optionsMenu?.findItem(R.id.action_share)?.isVisible = true
            supportActionBar?.title = getString(R.string.selected_count, selectedCount)
        } else {
            optionsMenu?.findItem(R.id.action_delete)?.isVisible = false
            optionsMenu?.findItem(R.id.action_share)?.isVisible = false
            supportActionBar?.title = getString(R.string.files_list_title)
        }
    }

    private fun refreshFileList() {
        val app = application as AESCameraApplication
        val files =
            app.outputDirectory?.listFiles()
                ?.filter { it.isFile && !it.name.startsWith(".") && it.extension == "enc" }
                ?: emptyList()

        optionsMenu?.findItem(R.id.action_select_all)?.isVisible = files.isNotEmpty()

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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.file_list_menu, menu)
        optionsMenu = menu

        optionsMenu?.findItem(R.id.action_select_all)?.isVisible = adapter.itemCount != 0

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }

            R.id.action_select_all -> {
                if (adapter.getSelectedItems().size == adapter.itemCount) {
                    adapter.clearSelection()
                } else {
                    adapter.selectAll()
                }

                true
            }

            R.id.action_lock -> {
                val app = application as AESCameraApplication
                app.key = null
                true
            }

            R.id.action_delete -> {
                deleteSelectedItems()
                true
            }

            R.id.action_share -> {
                shareSelectedItems()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

//    override fun onSupportNavigateUp(): Boolean {
//        finish()
//        return true
//    }
}