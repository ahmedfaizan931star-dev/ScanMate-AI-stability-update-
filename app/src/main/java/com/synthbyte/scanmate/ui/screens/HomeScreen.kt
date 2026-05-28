package com.synthbyte.scanmate.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.synthbyte.scanmate.data.SettingsRepository
import com.synthbyte.scanmate.ui.screens.home.HomeBottomNavigation
import com.synthbyte.scanmate.ui.screens.home.HomeDocumentList
import com.synthbyte.scanmate.ui.screens.home.HomeHeaderZone
import com.synthbyte.scanmate.ui.screens.home.HomeHeroCard
import com.synthbyte.scanmate.ui.screens.home.HomeLoadingSkeleton
import com.synthbyte.scanmate.ui.screens.home.HomeNavItem
import com.synthbyte.scanmate.ui.screens.home.HomeToolChipRow
import com.synthbyte.scanmate.ui.viewmodels.rememberDocumentViewModel
import com.synthbyte.scanmate.utils.FileUtils
import com.synthbyte.scanmate.widgets.WidgetStateStore
import java.io.File
import java.util.Locale

private enum class DocumentFilterMode(val label: String, val sectionTitle: String) {
    ALL("All", "Recent files"),
    FAVORITES("Favorites", "Favorite documents"),
    PINNED("Pinned", "Pinned documents"),
    RECENT("Recent", "This week"),
    OCR("OCR", "OCR documents"),
    PDF("PDF", "PDF documents")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToDoc: (Long) -> Unit,
    onNavigateToQr: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToZip: () -> Unit,
    onNavigateToFiles: () -> Unit,
    onNavigateToPdfTools: () -> Unit,
    onNavigateToTranslate: () -> Unit,
    onNavigateToVault: () -> Unit
) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val defaultWorkspace by settingsRepository.defaultWorkspaceFlow.collectAsState(initial = "Inbox")
    val viewModel = rememberDocumentViewModel()
    val documents by viewModel.allDocuments.collectAsState(initial = emptyList())
    val firstPages by viewModel.allPages.collectAsState(initial = emptyList())
    val pinned by viewModel.pinnedDocuments.collectAsState(initial = emptyList())
    val pageCount by viewModel.pageCount.collectAsState(initial = 0)
    val snackbarHostState = remember { SnackbarHostState() }
    var query by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf(DocumentFilterMode.ALL) }
    val scope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 30)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.createDocumentFromUris(
                uris = uris,
                defaultWorkspace = defaultWorkspace,
                onCreated = { newDocId -> onNavigateToDoc(newDocId) },
                onError = { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
            )
        }
    }

    val recentForWidget = documents.firstOrNull()
    LaunchedEffect(recentForWidget?.id, recentForWidget?.title, recentForWidget?.workspace, recentForWidget?.category) {
        WidgetStateStore.publishRecentDocument(context, recentForWidget)
    }

    val firstPageByDocument = remember(firstPages) { firstPages.associateBy { it.documentId } }
    val visibleDocuments = remember(documents, query, filterMode) {
        val recentCutoff = System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L
        documents.filter { doc ->
            val matchesQuery = query.isBlank() ||
                doc.title.contains(query, ignoreCase = true) ||
                doc.ocrText.orEmpty().contains(query, ignoreCase = true) ||
                doc.category.contains(query, ignoreCase = true) ||
                doc.tags.contains(query, ignoreCase = true) ||
                doc.workspace.contains(query, ignoreCase = true)
            val matchesFilter = when (filterMode) {
                DocumentFilterMode.ALL -> true
                DocumentFilterMode.FAVORITES -> doc.isFavorite
                DocumentFilterMode.PINNED -> doc.isPinned
                DocumentFilterMode.RECENT -> doc.updatedAt >= recentCutoff || doc.timestamp >= recentCutoff
                DocumentFilterMode.OCR -> !doc.ocrText.isNullOrBlank()
                DocumentFilterMode.PDF -> doc.type.equals("PDF", ignoreCase = true)
            }
            matchesQuery && matchesFilter
        }.sortedWith(compareByDescending<com.synthbyte.scanmate.data.Document> { it.isPinned }.thenByDescending { it.updatedAt }.thenByDescending { it.timestamp })
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            HomeBottomNavigation(
                selected = "Home",
                onScan = onNavigateToCamera,
                items = listOf(
                    HomeNavItem("Home", Icons.Default.Home, {}),
                    HomeNavItem("Files", Icons.Default.Folder, onNavigateToFiles),
                    HomeNavItem("Tools", Icons.Default.Apps, onNavigateToQr),
                    HomeNavItem("AI", Icons.Default.AutoAwesome, onNavigateToAi)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HomeHeaderZone(
                    query = query,
                    onQueryChange = { query = it },
                    onNavigateToAi = onNavigateToAi,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
            item {
                HomeHeroCard(
                    documentCount = documents.size,
                    pageCount = pageCount,
                    pinnedCount = pinned.size,
                    onScan = onNavigateToCamera,
                    onImport = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    toolContent = {
                        HomeToolChipRow(
                            onPdf = onNavigateToPdfTools,
                            onOcr = onNavigateToCamera,
                            onQr = onNavigateToQr,
                            onZip = onNavigateToZip,
                            onTranslate = onNavigateToTranslate,
                            onVault = onNavigateToVault
                        )
                    }
                )
            }
            if (documents.isEmpty() && query.isBlank()) {
                item { HomeLoadingSkeleton(isEmptyState = true, onScan = onNavigateToCamera) }
            }
            item {
                HomeDocumentList(
                    title = filterMode.sectionTitle,
                    countLabel = "${visibleDocuments.size} shown",
                    documents = visibleDocuments,
                    firstPageByDocument = firstPageByDocument,
                    selectedFilter = filterMode.label,
                    filters = DocumentFilterMode.entries.map { it.label },
                    onFilterSelected = { label -> filterMode = DocumentFilterMode.entries.first { it.label == label } },
                    onDocumentClick = { onNavigateToDoc(it.id) },
                    onFavorite = { viewModel.toggleFavorite(it) },
                    onPin = { viewModel.togglePinned(it) }
                )
            }
        }
    }
}
