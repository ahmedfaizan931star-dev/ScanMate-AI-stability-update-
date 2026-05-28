package com.synthbyte.scanmate.ui.screens.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeToolChipRow(
    onPdf: () -> Unit,
    onOcr: () -> Unit,
    onQr: () -> Unit,
    onZip: () -> Unit,
    onTranslate: () -> Unit,
    onVault: () -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(onClick = onPdf, leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) }, label = { Text("PDF") })
        AssistChip(onClick = onOcr, leadingIcon = { Icon(Icons.Default.Description, null) }, label = { Text("OCR") })
        AssistChip(onClick = onQr, leadingIcon = { Icon(Icons.Default.QrCodeScanner, null) }, label = { Text("QR") })
        AssistChip(onClick = onZip, leadingIcon = { Icon(Icons.Default.FolderZip, null) }, label = { Text("ZIP") })
        AssistChip(onClick = onVault, leadingIcon = { Icon(Icons.Default.Lock, null) }, label = { Text("Vault") })
        AssistChip(onClick = onTranslate, leadingIcon = { Icon(Icons.Default.Translate, null) }, label = { Text("Translate") })
    }
}
