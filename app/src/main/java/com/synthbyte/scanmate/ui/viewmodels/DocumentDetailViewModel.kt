package com.synthbyte.scanmate.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.synthbyte.scanmate.data.DocDao
import com.synthbyte.scanmate.data.DocumentWithPages
import com.synthbyte.scanmate.data.Page
import com.synthbyte.scanmate.utils.DocumentIntelligence
import com.synthbyte.scanmate.utils.FileUtils
import com.synthbyte.scanmate.utils.OcrHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DocumentDetailViewModel(
    private val dao: DocDao,
    private val context: Context,
    private val docId: Long
) : ViewModel() {
    val documentWithPages: Flow<DocumentWithPages?> = dao.getDocumentWithPages(docId)

    fun setFavorite(favorite: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        dao.setFavorite(docId, favorite)
    }

    fun setPinned(pinned: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        dao.setPinned(docId, pinned)
    }

    fun rename(title: String) = viewModelScope.launch(Dispatchers.IO) {
        dao.renameDocument(docId, title.trim().ifBlank { "Untitled Scan" })
    }

    fun updateMeta(category: String, tags: String, workspace: String) = viewModelScope.launch(Dispatchers.IO) {
        dao.updateCategoryTags(
            id = docId,
            category = category.trim().ifBlank { "General" },
            tags = tags.trim(),
            workspace = workspace.trim().ifBlank { "Inbox" }
        )
    }

    fun delete(onDeleted: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        dao.deleteDocumentById(docId)
        withContext(Dispatchers.Main) { onDeleted() }
    }

    fun updateOcr(text: String) = viewModelScope.launch(Dispatchers.IO) {
        dao.updateOcrText(docId, text)
    }

    suspend fun updateOcrAndMetadata(text: String, currentWorkspace: String) = withContext(Dispatchers.IO) {
        dao.updateOcrText(docId, text)
        val suggested = DocumentIntelligence.suggestedCategory(text)
        val keywords = DocumentIntelligence.keywordList(text, 8).joinToString(", ")
        dao.updateCategoryTags(docId, suggested, keywords, currentWorkspace.ifBlank { "Inbox" })
    }

    fun updatePageImage(pageId: Long, imagePath: String) = viewModelScope.launch(Dispatchers.IO) {
        dao.updatePageImage(pageId, imagePath)
    }

    suspend fun deletePage(pageId: Long) = withContext(Dispatchers.IO) {
        dao.deletePageById(pageId)
        renumberPagesInternal()
    }

    suspend fun duplicatePage(page: Page) = withContext(Dispatchers.IO) {
        val copied = FileUtils.duplicateImageFile(context, page.imagePath) ?: return@withContext
        val pages = dao.getPagesForDocumentOnce(docId).sortedBy { it.pageOrder }
        val insertIndex = pages.indexOfFirst { it.id == page.id }.takeIf { it >= 0 }?.plus(1) ?: pages.size
        pages.forEachIndexed { index, existing ->
            val order = if (index >= insertIndex) index + 1 else index
            dao.updatePageOrder(existing.id, order)
        }
        dao.insertPage(Page(documentId = docId, imagePath = copied.absolutePath, pageOrder = insertIndex))
        renumberPagesInternal()
    }

    suspend fun movePage(page: Page, direction: Int) = withContext(Dispatchers.IO) {
        val pages = dao.getPagesForDocumentOnce(docId).sortedBy { it.pageOrder }.toMutableList()
        val index = pages.indexOfFirst { it.id == page.id }
        if (index < 0) return@withContext
        val newIndex = (index + direction).coerceIn(0, pages.lastIndex)
        if (index == newIndex) return@withContext
        val current = pages.removeAt(index)
        pages.add(newIndex, current)
        pages.forEachIndexed { order, existing -> dao.updatePageOrder(existing.id, order) }
    }

    suspend fun reorderPages(pages: List<Page>) = withContext(Dispatchers.IO) {
        pages.forEachIndexed { order, page -> dao.updatePageOrder(page.id, order) }
    }

    private suspend fun renumberPagesInternal() {
        dao.getPagesForDocumentOnce(docId).sortedBy { it.pageOrder }.forEachIndexed { index, page ->
            dao.updatePageOrder(page.id, index)
        }
    }

    override fun onCleared() {
        OcrHelper.closeRecognizer()
        super.onCleared()
    }
}

class DocumentDetailViewModelFactory(
    private val dao: DocDao,
    private val context: Context,
    private val docId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DocumentDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DocumentDetailViewModel(dao, context, docId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
