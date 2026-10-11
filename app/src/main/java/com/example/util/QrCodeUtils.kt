package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object QrCodeUtils {
    private val qrCache = object : LinkedHashMap<String, ImageBitmap>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean {
            return size > 60
        }
    }

    fun generateQrBitMatrix(content: String, sizePx: Int = 256): com.google.zxing.common.BitMatrix {
        val safeContent = content.ifBlank { "https://akah-sop-register.local" }
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        return QRCodeWriter().encode(safeContent, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    }

    fun generateQrAndroidBitmap(content: String, sizePx: Int = 400): Bitmap {
        val bitMatrix = generateQrBitMatrix(content, sizePx)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        return bmp
    }

    suspend fun generateQrImageBitmap(content: String, sizePx: Int = 512): ImageBitmap =
        withContext(Dispatchers.Default) {
            val cacheKey = "$content|$sizePx"
            synchronized(qrCache) {
                qrCache[cacheKey]?.let { return@withContext it }
            }
            val bmp = generateQrAndroidBitmap(content, sizePx)
            val imgBmp = bmp.asImageBitmap()
            synchronized(qrCache) {
                qrCache[cacheKey] = imgBmp
            }
            imgBmp
        }

    suspend fun decodeQrFromUri(context: Context, uri: Uri): List<String> =
        suspendCancellableCoroutine { cont ->
            try {
                val image = InputImage.fromFilePath(context, uri)
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_ALL_FORMATS)
                    .build()
                val scanner = BarcodeScanning.getClient(options)
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        val values = barcodes.mapNotNull { it.rawValue?.trim() }.filter { it.isNotEmpty() }
                        if (cont.isActive) cont.resume(values)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(emptyList())
                    }
            } catch (_: Exception) {
                if (cont.isActive) cont.resume(emptyList())
            }
        }
}
