package com.synthbyte.scanmate.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synthbyte.scanmate.data.Document
import com.synthbyte.scanmate.data.Page
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeDocumentList(
    title: String,
    countLabel: String,
    documents: List<Document>,
    firstPageByDocument: Map<Long, Page>,
    selectedFilter: String,
    filters: List<String>,
    onFilterSelected: (String) -> Unit,
    onDocumentClick: (Document) -> Unit,
    onFavorite: (Document) -> Unit,
    onPin: (Document) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            filters.forEach { label ->
                FilterChip(selected = selectedFilter == label, onClick = { onFilterSelected(label) }, label = { Text(label) })
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title.uppercase(Locale.getDefault()), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
            Text(countLabel, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        if (documents.isEmpty()) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("No documents match this view", style = MaterialTheme.typography.titleSmall)
                    Text("Scan or import pages to start building your private document desk.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            documents.forEach { doc ->
                DocumentRow(
                    document = doc,
                    page = firstPageByDocument[doc.id],
                    onClick = { onDocumentClick(doc) },
                    onFavorite = { onFavorite(doc) },
                    onPin = { onPin(doc) }
                )
            }
        }
    }
}

@Composable
private fun DocumentRow(document: Document, page: Page?, onClick: () -> Unit, onFavorite: () -> Unit, onPin: () -> Unit) {
    val dateLabel = remember(document.updatedAt, document.timestamp) {
        SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(document.updatedAt.takeIf { it > 0 } ?: document.timestamp))
    }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (page != null) {
                AsyncImage(
                    model = page.imagePath,
                    contentDescription = document.title,
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.ImageNotSupported, null, modifier = Modifier.size(50.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(document.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!document.ocrText.isNullOrBlank()) MiniBadge("OCR")
                    if (document.type.equals("PDF", ignoreCase = true)) MiniBadge("PDF")
                    if (document.isPinned) MiniBadge("Pinned")
                }
                Text("${document.workspace.ifBlank { "Inbox" }} · $dateLabel", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onFavorite) {
                    Icon(if (document.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = if (document.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onPin) {
                    Icon(Icons.Default.PushPin, contentDescription = "Pin", tint = if (document.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MiniBadge(label: String) {
    Text(
        label,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.ExtraBold,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
