package com.devil7softwares.aescamera.list

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.RecyclerView
import com.devil7softwares.aescamera.R
import com.devil7softwares.aescamera.utils.EncryptionUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.LinkedList

class ThumbnailLoader(
    private val context: Context,
    private val thumbnailDirectory: File?,
    private val encryptionKey: String?
) {
    private val handler = Handler(Looper.getMainLooper())
    private val loadingQueue = LinkedList<Pair<FileAdapter.FileViewHolder, File>>()
    private var isLoading = false
    private var currentJob: Job? = null
    private var isPaused = false
    private var currentLoadingItem: Pair<FileAdapter.FileViewHolder, File>? = null

    fun loadThumbnail(holder: FileAdapter.FileViewHolder, file: File) {
        if (file.name.startsWith("IMG-") && thumbnailDirectory != null) {
            val thumbnailFile = File(thumbnailDirectory, file.name)
            if (thumbnailFile.exists()) {
                queueThumbnailLoad(holder, thumbnailFile)
            }
        }
    }

    private fun queueThumbnailLoad(holder: FileAdapter.FileViewHolder, thumbnailFile: File) {
        loadingQueue.removeAll { it.first == holder }
        loadingQueue.offer(holder to thumbnailFile)
        if (!isLoading && !isPaused) {
            loadNextThumbnail()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun loadNextThumbnail() {
        if (loadingQueue.isEmpty() || isPaused) {
            isLoading = false
            currentLoadingItem = null
            return
        }

        isLoading = true
        currentLoadingItem = loadingQueue.poll()
        val (holder, thumbnailFile) = currentLoadingItem!!

        currentJob = GlobalScope.launch(Dispatchers.IO) {
            try {
                val key = encryptionKey ?: return@launch
                val decryptedBytes = EncryptionUtils.decrypt(thumbnailFile.readBytes(), key)
                val bitmap = BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
                withContext(Dispatchers.Main) {
                    if (!isPaused && holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        holder.showThumbnail(bitmap)
                    }
                    isLoading = false
                    currentLoadingItem = null
                    loadNextThumbnail()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    if (!isPaused && holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        holder.showIcon(context.getString(R.string.fa_file_image))
                    }
                    isLoading = false
                    currentLoadingItem = null
                    loadNextThumbnail()
                }
            }
        }
    }

    fun pauseLoading() {
        isPaused = true
        currentJob?.cancel()
        isLoading = false
    }

    fun resumeLoading() {
        isPaused = false
        // Re-queue the interrupted item if it exists
        currentLoadingItem?.let { interruptedItem ->
            loadingQueue.addFirst(interruptedItem)  // Changed to addFirst
        }
        currentLoadingItem = null
        if (!isLoading) {
            loadNextThumbnail()
        }
    }

    fun cancelLoading() {
        loadingQueue.clear()
        currentJob?.cancel()
        isLoading = false
        isPaused = false
        currentLoadingItem = null
    }
}