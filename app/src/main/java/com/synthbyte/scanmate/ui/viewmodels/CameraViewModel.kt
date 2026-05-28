package com.synthbyte.scanmate.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.synthbyte.scanmate.data.DocDao
import com.synthbyte.scanmate.data.Document
import com.synthbyte.scanmate.data.Page
import com.synthbyte.scanmate.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CameraViewModel(private val dao: DocDao, private val context: Context) : ViewModel() {
    suspend fun importUris(uris: List<Uri>): List<File> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri -> FileUtils.copyUriToImageFile(context, uri) }
    }

    suspend fun saveScannedDocument(files: List<File>, defaultWorkspace: String): Long = withContext(Dispatchers.IO) {
        val validFiles = files.filter { it.exists() && it.length() > 0L }
        require(validFiles.isNotEmpty()) { "No valid scanned pages" }
        val now = System.currentTimeMillis()
        val id = dao.insertDocument(
            Document(
                title = "Scanned ${validFiles.size} page${if (validFiles.size == 1) "" else "s"}",
                timestamp = now,
                updatedAt = now,
                type = "SCAN",
                workspace = defaultWorkspace.ifBlank { "Inbox" }
            )
        )
        validFiles.forEachIndexed { index, file ->
            dao.insertPage(Page(documentId = id, imagePath = file.absolutePath, pageOrder = index))
        }
        id
    }
}

class CameraViewModelFactory(private val dao: DocDao, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CameraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CameraViewModel(dao, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
