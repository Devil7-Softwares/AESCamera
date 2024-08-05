package com.devil7softwares.aescamera.list

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devil7softwares.aescamera.AESCameraApplication
import com.devil7softwares.aescamera.ProtectedBaseActivity
import com.devil7softwares.aescamera.R
import com.devil7softwares.aescamera.databinding.ActivityFilesListBinding
import com.devil7softwares.aescamera.databinding.ItemFileBinding
import com.devil7softwares.aescamera.utils.EncryptionUtils
import com.devil7softwares.aescamera.viewer.DecryptedImageViewerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FileAdapter(
    private val context: Context,
    private val thumbnailDirectory: File?,
    private var encryptionKey: String?
) : ListAdapter<File, FileAdapter.FileViewHolder>(FileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FileViewHolder(private val binding: ItemFileBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                val file = getItem(bindingAdapterPosition)
                val intent = Intent(context, DecryptedImageViewerActivity::class.java)
                intent.data = Uri.fromFile(file)
                context.startActivity(intent)
            }
        }

        fun bind(file: File) {
            binding.fileName.text = file.name
            binding.fileSize.text = formatFileSize(file.length())
            if (file.name.startsWith("IMG-") && thumbnailDirectory != null) {
                val thumbnailFile = File(thumbnailDirectory, file.name)
                if (thumbnailFile.exists()) {
                    loadDecryptedThumbnail(thumbnailFile)
                } else {
                    showIcon(context.getString(R.string.fa_file_image))
                }
            } else {
                showIcon(context.getString(R.string.fa_file))
            }
        }

        private fun showThumbnail(bitmap: Bitmap) {
            binding.fileIcon.visibility = View.GONE
            binding.fileThumbnail.visibility = View.VISIBLE
            binding.fileThumbnail.setImageBitmap(bitmap)
        }

        private fun showIcon(icon: String) {
            binding.fileIcon.visibility = View.VISIBLE
            binding.fileThumbnail.visibility = View.GONE
            binding.fileIcon.setIcon(icon)
        }

        private fun loadDecryptedThumbnail(thumbnailFile: File) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val key = encryptionKey ?: return@launch
                    val decryptedBytes = EncryptionUtils.decrypt(thumbnailFile.readBytes(), key)
                    val bitmap = BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
                    withContext(Dispatchers.Main) {
                        showThumbnail(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        showIcon(context.getString(R.string.fa_file_image))
                    }
                }
            }
        }

        private fun formatFileSize(size: Long): String {
            val kb = size / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1 -> String.format("%.2f MB", mb)
                kb >= 1 -> String.format("%.2f KB", kb)
                else -> String.format("%d bytes", size)
            }
        }
    }
}