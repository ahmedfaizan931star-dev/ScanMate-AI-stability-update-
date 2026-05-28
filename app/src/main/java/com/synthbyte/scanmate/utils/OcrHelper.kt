package com.synthbyte.scanmate.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.roundToInt

private const val OCR_MAX_SIDE = 2048

data class OcrExtractionResult(
    val text: String,
    val confidencePercent: Int,
    val wordCount: Int,
    val qualityLabel: String
)

object OcrHelper {
    @Volatile
    private var recognizer: TextRecognizer? = null

    private fun getRecognizer(): TextRecognizer {
        return recognizer ?: synchronized(this) {
            recognizer ?: TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).also { recognizer = it }
        }
    }

    suspend fun extractTextFromBitmap(bitmap: Bitmap, rotationDegrees: Int = 0): String {
        return extractStatsFromBitmap(bitmap, rotationDegrees).text
    }

    suspend fun extractTextFromFile(context: Context, file: File): String {
        return extractTextWithStatsFromFile(context, file).text
    }

    suspend fun extractTextWithStatsFromFile(context: Context, file: File): OcrExtractionResult {
        val source = FileUtils.decodeSampledBitmap(file.absolutePath, OCR_MAX_SIDE, OCR_MAX_SIDE)
            ?: return buildStats("OCR failed: Could not decode image", 0)
        return try {
            val fixed = fixExifRotation(source, file)
            val prepared = preprocessForOcr(fixed)
            try {
                runTextRecognition(prepared, 0)
            } finally {
                if (prepared !== fixed && !prepared.isRecycled) runCatching { prepared.recycle() }
                if (fixed !== source && !fixed.isRecycled) runCatching { fixed.recycle() }
                if (!source.isRecycled) runCatching { source.recycle() }
            }
        } catch (e: Exception) {
            if (!source.isRecycled) runCatching { source.recycle() }
            buildStats("OCR failed: ${e.localizedMessage ?: "Unknown error"}", 0)
        }
    }

    suspend fun extractStatsFromBitmap(bitmap: Bitmap, rotationDegrees: Int = 0): OcrExtractionResult {
        val rotated = if (rotationDegrees != 0) rotate(bitmap, rotationDegrees.toFloat()) else bitmap
        val prepared = preprocessForOcr(rotated)
        return try {
            runTextRecognition(prepared, 0)
        } finally {
            if (prepared !== rotated && !prepared.isRecycled) runCatching { prepared.recycle() }
            if (rotated !== bitmap && !rotated.isRecycled) runCatching { rotated.recycle() }
        }
    }

    fun buildStats(text: String): OcrExtractionResult = buildStats(text, null)

    private fun buildStats(text: String, mlKitConfidence: Int?): OcrExtractionResult {
        val clean = DocumentIntelligence.cleanOcrText(text)
        val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
        val confidence = when {
            clean.isBlank() || clean.startsWith("OCR failed", ignoreCase = true) -> 0
            mlKitConfidence != null -> mlKitConfidence.coerceIn(0, 100)
            words.size >= 120 -> 82
            words.size >= 40 -> 74
            words.size >= 12 -> 62
            else -> 45
        }
        val label = when {
            confidence >= 88 -> "High confidence"
            confidence >= 72 -> "Good confidence"
            confidence >= 55 -> "Needs review"
            confidence > 0 -> "Low confidence"
            else -> "No OCR text"
        }
        return OcrExtractionResult(clean, confidence, words.size, label)
    }

    fun closeRecognizer() {
        synchronized(this) {
            runCatching { recognizer?.close() }
            recognizer = null
        }
    }

    private suspend fun runTextRecognition(bitmap: Bitmap, rotationDegrees: Int): OcrExtractionResult =
        suspendCancellableCoroutine { continuation ->
            try {
                val activeRecognizer = getRecognizer()
                activeRecognizer.process(InputImage.fromBitmap(bitmap, rotationDegrees))
                    .addOnSuccessListener { result ->
                        if (continuation.isActive) {
                            continuation.resume(buildStats(result.toSortedText(), result.symbolConfidencePercent()))
                        }
                    }
                    .addOnFailureListener { e ->
                        if (continuation.isActive) continuation.resume(buildStats("OCR failed: ${e.localizedMessage ?: "Unknown error"}", 0))
                    }
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(buildStats("OCR failed: ${e.localizedMessage ?: "Unknown error"}", 0))
            }
        }

    private fun Text.toSortedText(): String {
        return textBlocks
            .sortedWith(compareBy<Text.TextBlock> { it.boundingBox?.top ?: Int.MAX_VALUE }.thenBy { it.boundingBox?.left ?: Int.MAX_VALUE })
            .joinToString("\n") { block ->
                block.lines
                    .sortedWith(compareBy<Text.Line> { it.boundingBox?.top ?: Int.MAX_VALUE }.thenBy { it.boundingBox?.left ?: Int.MAX_VALUE })
                    .joinToString("\n") { line -> line.text }
            }
            .trim()
    }

    private fun Text.symbolConfidencePercent(): Int? {
        val values = textBlocks
            .flatMap { it.lines }
            .flatMap { it.elements }
            .flatMap { it.symbols }
            .mapNotNull { symbol ->
                val confidence = symbol.confidence
                if (confidence >= 0f) confidence else null
            }
        if (values.isEmpty()) return null
        return (values.average() * 100.0).roundToInt().coerceIn(0, 100)
    }

    private fun preprocessForOcr(source: Bitmap): Bitmap {
        val scaled = source.scaleDownToMax(OCR_MAX_SIDE)
        val gray = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(gray)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        if (scaled !== source && !scaled.isRecycled) runCatching { scaled.recycle() }
        val highContrast = FileUtils.applyFilter(gray, FilterType.HIGH_CONTRAST)
        if (!gray.isRecycled) runCatching { gray.recycle() }
        return highContrast
    }

    private fun fixExifRotation(bitmap: Bitmap, file: File): Bitmap {
        val degrees = runCatching {
            when (ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)
        return if (degrees == 0f) bitmap else rotate(bitmap, degrees)
    }

    private fun rotate(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun Bitmap.scaleDownToMax(maxSide: Int): Bitmap {
        val side = max(width, height)
        if (side <= maxSide) return this
        val ratio = maxSide.toFloat() / side.toFloat()
        val targetWidth = (width * ratio).roundToInt().coerceAtLeast(1)
        val targetHeight = (height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
    }
}
