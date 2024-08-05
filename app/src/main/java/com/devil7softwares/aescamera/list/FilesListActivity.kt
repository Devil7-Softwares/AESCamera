package com.devil7softwares.aescamera.list

import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import com.devil7softwares.aescamera.AESCameraApplication
import com.devil7softwares.aescamera.ProtectedBaseActivity
import com.devil7softwares.aescamera.R
import com.devil7softwares.aescamera.databinding.ActivityFilesListBinding

class FilesListActivity : ProtectedBaseActivity() {
    private lateinit var binding: ActivityFilesListBinding
    private lateinit var adapter: FileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        adapter = FileAdapter(this, app.thumbnailDirectory, app.key)
        binding.fileListRecyclerView.apply {
            layoutManager = GridLayoutManager(this@FilesListActivity, 3)
            adapter = this@FilesListActivity.adapter
        }
    }

    private fun refreshFileList() {
        val app = application as AESCameraApplication
        val files = app.outputDirectory?.listFiles()?.filter { it.isFile && !it.name.startsWith(".") } ?: emptyList()
        adapter.submitList(files)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}