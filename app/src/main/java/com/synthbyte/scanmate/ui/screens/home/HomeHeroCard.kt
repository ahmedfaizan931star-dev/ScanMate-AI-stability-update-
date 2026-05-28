package com.synthbyte.scanmate.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Composable
fun HomeHeroCard(
    documentCount: Int,
    pageCount: Int,
    pinnedCount: Int,
    onScan: () -> Unit,
    onImport: () -> Unit,
    toolContent: @Composable () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "DOCUMENT WORKSPACE",
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 10.sp,
                letterSpacing = 0.12.em,
                fontWeight = FontWeight.ExtraBold
            )
            Text("Build a clean\ndocument", color = Color.White, fontSize = 22.sp, lineHeight = 25.sp, fontWeight = FontWeight.ExtraBold)
            Text("Scan, verify OCR, export PDF or DOCX — all offline and private.", color = Color.White.copy(alpha = 0.76f), fontSize = 12.sp, lineHeight = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                HeroStep("1", "Scan", Modifier.weight(1f))
                HeroStep("2", "OCR", Modifier.weight(1f))
                HeroStep("3", "Export", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onScan,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.CameraAlt, null)
                    Text(" Scan now", fontWeight = FontWeight.ExtraBold)
                }
                OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.PhotoLibrary, null, tint = Color.White)
                    Text(" Import", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            toolContent()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                HeroStat(documentCount.toString(), "Docs", Modifier.weight(1f))
                HeroStat(pageCount.toString(), "Pages", Modifier.weight(1f))
                HeroStat(pinnedCount.toString(), "Pinned", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroStep(number: String, label: String, modifier: Modifier) {
    Row(
        modifier = modifier.background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp)).padding(7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(number, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        Text("  $label", color = Color.White.copy(alpha = 0.90f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HeroStat(value: String, label: String, modifier: Modifier) {
    Column(modifier = modifier.background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(10.dp)).padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Text(label, color = Color.White.copy(alpha = 0.64f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}
