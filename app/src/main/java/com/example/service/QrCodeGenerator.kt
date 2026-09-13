package com.example.service

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.util.Locale

object QrCodeGenerator {

    /**
     * Builds the standard NPCI UPI payment deep link URI.
     */
    fun buildUpiPaymentUri(
        upiId: String,
        payeeName: String,
        amount: Double,
        billNumber: String = ""
    ): String {
        val encodedName = URLEncoder.encode(payeeName, "UTF-8")
        val noteText = if (billNumber.isNotBlank()) "Bill $billNumber" else "Restaurant Bill Payment"
        val encodedNote = URLEncoder.encode(noteText, "UTF-8")
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        return "upi://pay?pa=$upiId&pn=$encodedName&am=$formattedAmount&cu=INR&tn=$encodedNote"
    }

    /**
     * Generates an Android Bitmap for the given QR content.
     */
    fun generateQrBitmap(
        content: String,
        width: Int = 384,
        height: Int = 384
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mutableMapOf<EncodeHintType, Any>()
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
            hints[EncodeHintType.MARGIN] = 1 // Clean small margin

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints)

            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height
            val pixels = IntArray(matrixWidth * matrixHeight)

            for (y in 0 until matrixHeight) {
                val offset = y * matrixWidth
                for (x in 0 until matrixWidth) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }

            val bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, matrixWidth, 0, 0, matrixWidth, matrixHeight)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Converts a black-and-white Bitmap into ESC/POS Raster Bit Image bytes (GS v 0).
     * This is universally supported on 58mm thermal printers.
     */
    fun bitmapToEscPosRasterBytes(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height

        // Width in bytes (8 pixels per byte)
        val widthBytes = (width + 7) / 8

        val stream = ByteArrayOutputStream()

        // Center alignment
        stream.write(byteArrayOf(0x1B, 0x61, 0x01))

        // GS v 0 m xL xH yL yH
        // m = 0 (Normal mode)
        stream.write(byteArrayOf(
            0x1D, 0x76, 0x30, 0x00,
            (widthBytes and 0xFF).toByte(),
            ((widthBytes shr 8) and 0xFF).toByte(),
            (height and 0xFF).toByte(),
            ((height shr 8) and 0xFF).toByte()
        ))

        for (y in 0 until height) {
            for (xByte in 0 until widthBytes) {
                var slice = 0
                for (b in 0 until 8) {
                    val x = xByte * 8 + b
                    if (x < width) {
                        val pixel = bitmap.getPixel(x, y)
                        // Luminance check
                        val red = Color.red(pixel)
                        val green = Color.green(pixel)
                        val blue = Color.blue(pixel)
                        val gray = (red * 299 + green * 587 + blue * 114) / 1000
                        if (gray < 128) {
                            slice = slice or (1 shl (7 - b))
                        }
                    }
                }
                stream.write(slice)
            }
        }

        // Left alignment reset
        stream.write(byteArrayOf(0x1B, 0x61, 0x00))

        return stream.toByteArray()
    }
}
