package com.example.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.local.BillEntity
import com.example.data.local.ExpenseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportGenerator(private val context: Context) {

    suspend fun generateDailyReportPdf(
        dateTimestamp: Long,
        bills: List<BillEntity>,
        expenses: List<ExpenseEntity>,
        restaurantName: String = "The Grand Bistro POS"
    ): File = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateString = dateFormat.format(Date(dateTimestamp))
        val displayDate = displayDateFormat.format(Date(dateTimestamp))

        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()
        val pdfFile = File(reportsDir, "Daily_Report_$dateString.pdf")

        // Standard A4 dimensions in PostScript points: 595 x 842 pt
        val pageWidth = 595
        val pageHeight = 842
        val document = PdfDocument()

        val activeBills = bills.filter { !it.isVoided }
        val grossSales = activeBills.sumOf { it.subtotal }
        val totalDiscounts = activeBills.sumOf { it.discountAmount }
        val totalTax = activeBills.sumOf { it.taxAmount }
        val netSales = activeBills.sumOf { it.finalTotal }
        val totalExpenses = expenses.sumOf { it.amount }
        val netProfit = netSales - totalExpenses

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Header Banner
        paint.color = Color.rgb(194, 65, 12) // Terracotta #C2410C
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 20f
        canvas.drawText(restaurantName.uppercase(), 36f, 42f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 12f
        canvas.drawText("DAILY BUSINESS & EXPENSE PERFORMANCE REPORT", 36f, 62f, paint)
        canvas.drawText("Date: $displayDate | Generated: ${SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())}", 36f, 80f, paint)

        var y = 120f

        // 2. Summary KPI Metric Cards (6 cards in 2 rows of 3)
        val cardWidth = 160f
        val cardHeight = 55f
        val startX = 36f
        val spacing = 15f

        data class Metric(val title: String, val value: String, val bgR: Int, val bgG: Int, val bgB: Int)
        val metrics = listOf(
            Metric("TOTAL ORDERS", "${activeBills.size} Bills", 241, 245, 249),
            Metric("NET SALES", "₹${String.format(Locale.US, "%.2f", netSales)}", 220, 252, 231),
            Metric("TOTAL EXPENSES", "₹${String.format(Locale.US, "%.2f", totalExpenses)}", 254, 226, 226),
            Metric("DISCOUNTS", "₹${String.format(Locale.US, "%.2f", totalDiscounts)}", 254, 243, 199),
            Metric("TAXES (GST)", "₹${String.format(Locale.US, "%.2f", totalTax)}", 219, 234, 254),
            Metric("NET PROFIT", "₹${String.format(Locale.US, "%.2f", netProfit)}", if (netProfit >= 0) 220 else 254, if (netProfit >= 0) 252 else 226, if (netProfit >= 0) 231 else 226)
        )

        for (i in metrics.indices) {
            val row = i / 3
            val col = i % 3
            val cx = startX + col * (cardWidth + spacing)
            val cy = y + row * (cardHeight + 10f)

            paint.color = Color.rgb(metrics[i].bgR, metrics[i].bgG, metrics[i].bgB)
            canvas.drawRoundRect(cx, cy, cx + cardWidth, cy + cardHeight, 8f, 8f, paint)

            paint.color = Color.rgb(71, 85, 105)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9f
            canvas.drawText(metrics[i].title, cx + 12f, cy + 20f, paint)

            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 15f
            canvas.drawText(metrics[i].value, cx + 12f, cy + 42f, paint)
        }

        y += 140f

        // 3. Payment Mode & Area Breakdown (Two side-by-side columns)
        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("SALES BY AREA", 36f, y, paint)
        canvas.drawText("SALES BY PAYMENT MODE", 300f, y, paint)

        y += 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f

        val areaSales = activeBills.groupBy { it.area }.mapValues { entry -> entry.value.sumOf { it.finalTotal } }
        var areaY = y
        if (areaSales.isEmpty()) {
            paint.color = Color.GRAY
            canvas.drawText("No orders recorded today", 36f, areaY, paint)
        } else {
            for ((area, total) in areaSales) {
                paint.color = Color.rgb(51, 65, 85)
                canvas.drawText("• $area: ₹${String.format(Locale.US, "%.2f", total)}", 36f, areaY, paint)
                areaY += 16f
            }
        }

        val paymentSales = activeBills.groupBy { it.paymentMethod }.mapValues { entry -> entry.value.sumOf { it.finalTotal } }
        var payY = y
        if (paymentSales.isEmpty()) {
            paint.color = Color.GRAY
            canvas.drawText("No transactions", 300f, payY, paint)
        } else {
            for ((pay, total) in paymentSales) {
                paint.color = Color.rgb(51, 65, 85)
                canvas.drawText("• $pay: ₹${String.format(Locale.US, "%.2f", total)}", 300f, payY, paint)
                payY += 16f
            }
        }

        y = maxOf(areaY, payY) + 20f

        // 4. Detailed Bills Summary Table (Top recent bills)
        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("TRANSACTIONS / BILLS (TOTAL: ${activeBills.size})", 36f, y, paint)

        y += 14f
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawRect(36f, y, pageWidth - 36f, y + 20f, paint)

        paint.color = Color.rgb(51, 65, 85)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        canvas.drawText("BILL #", 42f, y + 14f, paint)
        canvas.drawText("TIME", 110f, y + 14f, paint)
        canvas.drawText("TABLE / AREA", 170f, y + 14f, paint)
        canvas.drawText("ITEMS", 280f, y + 14f, paint)
        canvas.drawText("PAYMENT", 410f, y + 14f, paint)
        canvas.drawText("TOTAL", 480f, y + 14f, paint)

        y += 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f

        val displayBills = activeBills.take(12)
        for (bill in displayBills) {
            paint.color = Color.rgb(15, 23, 42)
            canvas.drawText(bill.billNumber, 42f, y + 12f, paint)
            canvas.drawText(timeFormat.format(Date(bill.timestamp)), 110f, y + 12f, paint)
            canvas.drawText("${bill.tableName} (${bill.area})", 170f, y + 12f, paint)

            val itemsCount = "${bill.items.sumOf { it.quantity }} items"
            canvas.drawText(itemsCount, 280f, y + 12f, paint)
            canvas.drawText(bill.paymentMethod, 410f, y + 12f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("₹${String.format(Locale.US, "%.2f", bill.finalTotal)}", 480f, y + 12f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            paint.color = Color.rgb(241, 245, 249)
            canvas.drawLine(36f, y + 16f, pageWidth - 36f, y + 16f, paint)
            y += 18f
        }

        if (activeBills.size > 12) {
            paint.color = Color.GRAY
            canvas.drawText("+ ${activeBills.size - 12} more bills listed in attached Excel export.", 42f, y + 12f, paint)
            y += 18f
        }

        y += 12f

        // 5. Daily Expenses Table
        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("DAILY EXPENSES (TOTAL: ₹${String.format(Locale.US, "%.2f", totalExpenses)})", 36f, y, paint)

        y += 14f
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawRect(36f, y, pageWidth - 36f, y + 20f, paint)

        paint.color = Color.rgb(51, 65, 85)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        canvas.drawText("TITLE", 42f, y + 14f, paint)
        canvas.drawText("CATEGORY", 180f, y + 14f, paint)
        canvas.drawText("MODE", 320f, y + 14f, paint)
        canvas.drawText("AMOUNT", 480f, y + 14f, paint)

        y += 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f

        if (expenses.isEmpty()) {
            paint.color = Color.GRAY
            canvas.drawText("No expenses logged for this date.", 42f, y + 12f, paint)
            y += 18f
        } else {
            for (expense in expenses.take(6)) {
                paint.color = Color.rgb(15, 23, 42)
                canvas.drawText(expense.title.take(24), 42f, y + 12f, paint)
                canvas.drawText(expense.category, 180f, y + 12f, paint)
                canvas.drawText(expense.paymentMode, 320f, y + 12f, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = Color.rgb(185, 28, 28)
                canvas.drawText("₹${String.format(Locale.US, "%.2f", expense.amount)}", 480f, y + 12f, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                paint.color = Color.rgb(241, 245, 249)
                canvas.drawLine(36f, y + 16f, pageWidth - 36f, y + 16f, paint)
                y += 18f
            }
        }

        // 6. Page Footer
        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 8f
        canvas.drawText("Restaurant POS System • Offline-First Architecture • Official Accounting Summary", 36f, pageHeight - 20f, paint)

        document.finishPage(page)

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        pdfFile
    }
}
