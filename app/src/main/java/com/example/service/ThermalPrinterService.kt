package com.example.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.example.data.local.BillEntity
import com.example.model.OrderItem
import com.example.model.SplitPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class BluetoothPrinterDevice(
    val name: String,
    val address: String,
    val isBonded: Boolean
)

class ThermalPrinterService(private val context: Context) {
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val lineLength = 32 // 58mm thermal printer standard is 32 columns

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(): List<BluetoothPrinterDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return try {
            adapter.bondedDevices?.map {
                BluetoothPrinterDevice(
                    name = it.name ?: "Unknown Printer",
                    address = it.address,
                    isBonded = true
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e("ThermalPrinterService", "Failed to list bluetooth devices: ${e.message}")
            emptyList()
        }
    }

    /**
     * Builds standard ESC/POS byte sequence for 58mm thermal printer
     */
    fun buildEscPosBytes(
        bill: BillEntity,
        settings: com.example.model.RestaurantSettings = com.example.model.RestaurantSettings()
    ): ByteArray {
        val stream = ByteArrayOutputStream()

        // Initialize printer
        stream.write(byteArrayOf(0x1B, 0x40))

        // Center Align
        stream.write(byteArrayOf(0x1B, 0x61, 0x01))

        // Double Height + Bold for Restaurant Name
        stream.write(byteArrayOf(0x1D, 0x21, 0x11)) // Double size
        stream.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold ON
        stream.write("${settings.restaurantName}\n".toByteArray(Charsets.UTF_8))

        // Normal Size
        stream.write(byteArrayOf(0x1D, 0x21, 0x00))
        stream.write(byteArrayOf(0x1B, 0x45, 0x00)) // Bold OFF
        if (settings.tagline.isNotBlank()) {
            stream.write("${settings.tagline}\n".toByteArray(Charsets.UTF_8))
        }
        stream.write("${settings.address}\n".toByteArray(Charsets.UTF_8))
        stream.write("Tel: ${settings.phone}\n".toByteArray(Charsets.UTF_8))
        if (settings.gstin.isNotBlank()) {
            stream.write("GSTIN: ${settings.gstin}\n".toByteArray(Charsets.UTF_8))
        }
        if (settings.fssai.isNotBlank()) {
            stream.write("FSSAI: ${settings.fssai}\n".toByteArray(Charsets.UTF_8))
        }

        // Separator
        stream.write("${"-".repeat(lineLength)}\n".toByteArray(Charsets.UTF_8))

        // Left Align for Bill Info
        stream.write(byteArrayOf(0x1B, 0x61, 0x00))
        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(bill.timestamp))

        stream.write("Bill No : ${bill.billNumber}\n".toByteArray(Charsets.UTF_8))
        stream.write("Date    : $formattedDate\n".toByteArray(Charsets.UTF_8))
        stream.write("Area    : ${bill.area} | Table: ${bill.tableName}\n".toByteArray(Charsets.UTF_8))
        if (bill.customerName.isNotBlank()) {
            stream.write("Customer: ${bill.customerName}\n".toByteArray(Charsets.UTF_8))
        }

        // Table Header
        stream.write("${"-".repeat(lineLength)}\n".toByteArray(Charsets.UTF_8))
        stream.write((formatColumns("ITEM", "QTY", "AMOUNT") + "\n").toByteArray(Charsets.UTF_8))
        stream.write("${"-".repeat(lineLength)}\n".toByteArray(Charsets.UTF_8))

        // Items
        for (item in bill.items) {
            val itemName = if (item.name.length > 16) item.name.take(15) + "." else item.name
            val qtyStr = "x${item.quantity}"
            val amtStr = String.format(Locale.US, "%.2f", item.totalAmount)
            stream.write((formatColumns(itemName, qtyStr, amtStr) + "\n").toByteArray(Charsets.UTF_8))
        }

        // Totals Section
        stream.write("${"-".repeat(lineLength)}\n".toByteArray(Charsets.UTF_8))
        stream.write((formatTotalRow("Subtotal", bill.subtotal) + "\n").toByteArray(Charsets.UTF_8))

        if (bill.discountAmount > 0) {
            val discLabel = "Discount (${String.format(Locale.US, "%.0f", bill.discountPercent)}%)"
            stream.write((formatTotalRow(discLabel, -bill.discountAmount) + "\n").toByteArray(Charsets.UTF_8))
        }

        if (bill.taxAmount > 0) {
            val taxLabel = "Tax (${String.format(Locale.US, "%.1f", bill.taxPercent)}%)"
            stream.write((formatTotalRow(taxLabel, bill.taxAmount) + "\n").toByteArray(Charsets.UTF_8))
        }

        stream.write("${"=".repeat(lineLength)}\n".toByteArray(Charsets.UTF_8))

        // Grand Total in Bold
        stream.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold ON
        stream.write((formatTotalRow("GRAND TOTAL", bill.finalTotal) + "\n").toByteArray(Charsets.UTF_8))
        stream.write(byteArrayOf(0x1B, 0x45, 0x00)) // Bold OFF
        stream.write("${"=".repeat(lineLength)}\n".toByteArray(Charsets.UTF_8))

        // Payment Info
        stream.write("Payment: ${bill.paymentMethod}\n".toByteArray(Charsets.UTF_8))
        if (bill.splitDetails.isNotEmpty()) {
            stream.write("Split Details:\n".toByteArray(Charsets.UTF_8))
            for (split in bill.splitDetails) {
                val splitLine = "  ${split.title}: ${String.format(Locale.US, "%.2f", split.amount)} (${split.paymentMethod.displayName})"
                stream.write("$splitLine\n".toByteArray(Charsets.UTF_8))
            }
        }

        // --- Large UPI Payment QR Code Section ---
        if (settings.isUpiQrEnabledOnBill && settings.upiId.isNotBlank()) {
            stream.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
            stream.write("\n${"-".repeat(lineLength)}\n".toByteArray(Charsets.UTF_8))
            stream.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold ON
            stream.write("SCAN & PAY VIA ANY UPI APP\n".toByteArray(Charsets.UTF_8))
            stream.write("GPay / PhonePe / Paytm / BHIM\n".toByteArray(Charsets.UTF_8))
            stream.write(byteArrayOf(0x1B, 0x45, 0x00)) // Bold OFF

            val upiUri = QrCodeGenerator.buildUpiPaymentUri(
                upiId = settings.upiId,
                payeeName = settings.upiPayeeName.ifBlank { settings.restaurantName },
                amount = bill.finalTotal,
                billNumber = bill.billNumber
            )

            val qrDim = if (settings.isLargeQr) 320 else 240
            val qrBitmap = QrCodeGenerator.generateQrBitmap(upiUri, qrDim, qrDim)
            if (qrBitmap != null) {
                val rasterBytes = QrCodeGenerator.bitmapToEscPosRasterBytes(qrBitmap)
                stream.write(rasterBytes)
            }

            stream.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
            stream.write("UPI ID: ${settings.upiId}\n".toByteArray(Charsets.UTF_8))
            stream.write("Amount: INR ${String.format(Locale.US, "%.2f", bill.finalTotal)}\n".toByteArray(Charsets.UTF_8))
            stream.write("${"-".repeat(lineLength)}\n".toByteArray(Charsets.UTF_8))
        }

        // Footer centered
        stream.write(byteArrayOf(0x1B, 0x61, 0x01))
        val footerMsg = if (settings.billFooter.isNotBlank()) settings.billFooter else "*** THANK YOU! VISIT AGAIN ***"
        stream.write("\n$footerMsg\n".toByteArray(Charsets.UTF_8))
        stream.write("Powered by ${settings.restaurantName}\n\n\n\n".toByteArray(Charsets.UTF_8))

        // Paper Feed & Cut
        stream.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))

        return stream.toByteArray()
    }

    /**
     * Formats receipt as a clean 32-character monospace string for UI preview & WhatsApp text
     */
    fun buildTextReceipt(
        bill: BillEntity,
        settings: com.example.model.RestaurantSettings = com.example.model.RestaurantSettings()
    ): String {
        val sb = StringBuilder()
        val separator = "-".repeat(lineLength)
        val doubleSeparator = "=".repeat(lineLength)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())

        sb.appendLine(centerText(settings.restaurantName, lineLength))
        if (settings.tagline.isNotBlank()) {
            sb.appendLine(centerText(settings.tagline, lineLength))
        }
        sb.appendLine(centerText(settings.address, lineLength))
        sb.appendLine(centerText("Tel: ${settings.phone}", lineLength))
        if (settings.gstin.isNotBlank()) {
            sb.appendLine(centerText("GSTIN: ${settings.gstin}", lineLength))
        }
        if (settings.fssai.isNotBlank()) {
            sb.appendLine(centerText("FSSAI: ${settings.fssai}", lineLength))
        }
        sb.appendLine(separator)

        sb.appendLine("Bill No : ${bill.billNumber}")
        sb.appendLine("Date    : ${dateFormat.format(Date(bill.timestamp))}")
        sb.appendLine("Area    : ${bill.area} | Table: ${bill.tableName}")
        if (bill.customerName.isNotBlank()) {
            sb.appendLine("Customer: ${bill.customerName}")
        }
        sb.appendLine(separator)

        sb.appendLine(formatColumns("ITEM", "QTY", "AMOUNT"))
        sb.appendLine(separator)

        for (item in bill.items) {
            val itemName = if (item.name.length > 16) item.name.take(15) + "." else item.name
            val qtyStr = "x${item.quantity}"
            val amtStr = String.format(Locale.US, "%.2f", item.totalAmount)
            sb.appendLine(formatColumns(itemName, qtyStr, amtStr))
        }

        sb.appendLine(separator)
        sb.appendLine(formatTotalRow("Subtotal", bill.subtotal))
        if (bill.discountAmount > 0) {
            sb.appendLine(formatTotalRow("Discount (${String.format(Locale.US, "%.0f", bill.discountPercent)}%)", -bill.discountAmount))
        }
        if (bill.taxAmount > 0) {
            sb.appendLine(formatTotalRow("Tax (${String.format(Locale.US, "%.1f", bill.taxPercent)}%)", bill.taxAmount))
        }
        sb.appendLine(doubleSeparator)
        sb.appendLine(formatTotalRow("GRAND TOTAL", bill.finalTotal))
        sb.appendLine(doubleSeparator)
        sb.appendLine("Payment : ${bill.paymentMethod}")

        if (bill.splitDetails.isNotEmpty()) {
            sb.appendLine("Split Payments:")
            for (split in bill.splitDetails) {
                sb.appendLine("  ${split.title}: ${String.format(Locale.US, "%.2f", split.amount)} [${split.paymentMethod.displayName}]")
            }
        }

        if (settings.isUpiQrEnabledOnBill && settings.upiId.isNotBlank()) {
            sb.appendLine(separator)
            sb.appendLine(centerText("SCAN & PAY VIA ANY UPI APP", lineLength))
            sb.appendLine(centerText("UPI ID: ${settings.upiId}", lineLength))
            sb.appendLine(centerText("Amount: INR ${String.format(Locale.US, "%.2f", bill.finalTotal)}", lineLength))
            sb.appendLine(separator)
        }

        sb.appendLine()
        val footerMsg = if (settings.billFooter.isNotBlank()) settings.billFooter else "*** THANK YOU! VISIT AGAIN ***"
        sb.appendLine(centerText(footerMsg, lineLength))
        sb.appendLine(centerText("Powered by ${settings.restaurantName}", lineLength))

        return sb.toString()
    }

    /**
     * Sends ESC/POS print commands to target Bluetooth printer via RFCOMM socket
     */
    @SuppressLint("MissingPermission")
    suspend fun printToBluetoothDevice(
        deviceAddress: String,
        printBytes: ByteArray
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: return@withContext Result.failure(Exception("Bluetooth not supported on this device"))

        var socket: BluetoothSocket? = null
        var outStream: OutputStream? = null

        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
            adapter.cancelDiscovery()

            socket = device.createRfcommSocketToServiceRecord(sppUuid)
            socket.connect()

            outStream = socket.outputStream
            outStream.write(printBytes)
            outStream.flush()

            // Short pause to ensure buffer transmission
            Thread.sleep(500)
            Result.success(true)
        } catch (e: Exception) {
            Log.e("ThermalPrinterService", "Error printing: ${e.message}", e)
            Result.failure(Exception("Printer connection failed: ${e.localizedMessage ?: "Unknown error"}. Ensure printer is ON and paired."))
        } finally {
            try { outStream?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    private fun formatColumns(col1: String, col2: String, col3: String): String {
        // Total width = 32 chars. col1 = 16 chars, col2 = 5 chars, col3 = 11 chars
        val p1 = col1.padEnd(16).take(16)
        val p2 = col2.padStart(5).take(5)
        val p3 = col3.padStart(11).take(11)
        return "$p1$p2$p3"
    }

    private fun formatTotalRow(label: String, amount: Double): String {
        val amtStr = String.format(Locale.US, "%.2f", amount)
        val spacing = (lineLength - label.length - amtStr.length).coerceAtLeast(1)
        return "$label${" ".repeat(spacing)}$amtStr"
    }

    private fun centerText(text: String, width: Int): String {
        if (text.length >= width) return text.take(width)
        val padding = (width - text.length) / 2
        return " ".repeat(padding) + text
    }
}
