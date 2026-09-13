package com.example.service

import android.content.Context
import com.example.data.local.BillEntity
import com.example.data.local.ExpenseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExcelReportExporter(private val context: Context) {

    suspend fun exportDailyReportToExcel(
        dateTimestamp: Long,
        bills: List<BillEntity>,
        expenses: List<ExpenseEntity>,
        restaurantName: String = "The Grand Bistro"
    ): File = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateString = dateFormat.format(Date(dateTimestamp))

        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()
        val csvFile = File(reportsDir, "Daily_Sales_Report_$dateString.csv")

        val activeBills = bills.filter { !it.isVoided }
        val grossSales = activeBills.sumOf { it.subtotal }
        val totalDiscounts = activeBills.sumOf { it.discountAmount }
        val totalTax = activeBills.sumOf { it.taxAmount }
        val netSales = activeBills.sumOf { it.finalTotal }
        val totalExpenses = expenses.sumOf { it.amount }
        val netProfit = netSales - totalExpenses

        FileOutputStream(csvFile).use { fos ->
            // Write UTF-8 BOM so Excel opens special characters and formatting correctly
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->

                // 1. Title & Executive Overview
                writer.appendLine("\"$restaurantName - DAILY ACCOUNTING & SALES REPORT\"")
                writer.appendLine("\"Report Date\",${escape(displayDateFormat.format(Date(dateTimestamp)))}")
                writer.appendLine("\"Generated At\",${escape(SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault()).format(Date()))}")
                writer.appendLine()

                writer.appendLine("\"KEY FINANCIAL METRICS\",\"AMOUNT (INR)\"")
                writer.appendLine("\"Total Completed Bills\",${activeBills.size}")
                writer.appendLine("\"Gross Food Sales\",${String.format(Locale.US, "%.2f", grossSales)}")
                writer.appendLine("\"Total Discounts\",${String.format(Locale.US, "%.2f", totalDiscounts)}")
                writer.appendLine("\"Net Sales\",${String.format(Locale.US, "%.2f", netSales)}")
                writer.appendLine("\"Total Taxes (GST)\",${String.format(Locale.US, "%.2f", totalTax)}")
                writer.appendLine("\"Total Operating Expenses\",${String.format(Locale.US, "%.2f", totalExpenses)}")
                writer.appendLine("\"NET OPERATING PROFIT\",${String.format(Locale.US, "%.2f", netProfit)}")
                writer.appendLine()

                // 2. Sales by Area
                writer.appendLine("\"SALES BY SEATING AREA\",\"BILLS COUNT\",\"REVENUE\"")
                val areaGroups = activeBills.groupBy { it.area }
                for ((area, group) in areaGroups) {
                    val count = group.size
                    val sum = group.sumOf { it.finalTotal }
                    writer.appendLine("${escape(area)},$count,${String.format(Locale.US, "%.2f", sum)}")
                }
                writer.appendLine()

                // 3. Sales by Payment Mode
                writer.appendLine("\"PAYMENT MODE BREAKDOWN\",\"COUNT\",\"COLLECTED AMOUNT\"")
                val payGroups = activeBills.groupBy { it.paymentMethod }
                for ((mode, group) in payGroups) {
                    val count = group.size
                    val sum = group.sumOf { it.finalTotal }
                    writer.appendLine("${escape(mode)},$count,${String.format(Locale.US, "%.2f", sum)}")
                }
                writer.appendLine()

                // 4. Detailed Bills Register
                writer.appendLine("\"--- DETAILED BILLS REGISTER ---\"")
                writer.appendLine("\"Bill No\",\"Date\",\"Time\",\"Area\",\"Table\",\"Customer\",\"Items Ordered\",\"Subtotal\",\"Discount (%)\",\"Discount (Amt)\",\"Tax (%)\",\"Tax (Amt)\",\"Final Total\",\"Payment Mode\",\"Status\"")

                for (bill in bills) {
                    val itemsSummary = bill.items.joinToString("; ") { "${it.name} x${it.quantity} (₹${String.format(Locale.US, "%.2f", it.totalAmount)})" }
                    val row = listOf(
                        bill.billNumber,
                        displayDateFormat.format(Date(bill.timestamp)),
                        timeFormat.format(Date(bill.timestamp)),
                        bill.area,
                        bill.tableName,
                        bill.customerName.ifBlank { "Guest" },
                        itemsSummary,
                        String.format(Locale.US, "%.2f", bill.subtotal),
                        String.format(Locale.US, "%.1f", bill.discountPercent),
                        String.format(Locale.US, "%.2f", bill.discountAmount),
                        String.format(Locale.US, "%.1f", bill.taxPercent),
                        String.format(Locale.US, "%.2f", bill.taxAmount),
                        String.format(Locale.US, "%.2f", bill.finalTotal),
                        bill.paymentMethod,
                        if (bill.isVoided) "VOIDED (${bill.voidReason})" else "PAID"
                    ).joinToString(",") { escape(it) }
                    writer.appendLine(row)
                }
                writer.appendLine()

                // 5. Item-wise Sales Analysis
                writer.appendLine("\"--- ITEM-WISE SALES PERFORMANCE ---\"")
                writer.appendLine("\"Item Name\",\"Category\",\"Quantity Sold\",\"Total Revenue\"")
                val itemSalesMap = mutableMapOf<String, Triple<String, Int, Double>>()
                for (b in activeBills) {
                    for (item in b.items) {
                        val current = itemSalesMap[item.name]
                        val newQty = (current?.second ?: 0) + item.quantity
                        val newRev = (current?.third ?: 0.0) + item.totalAmount
                        itemSalesMap[item.name] = Triple(item.category, newQty, newRev)
                    }
                }
                for ((name, data) in itemSalesMap.toList().sortedByDescending { it.second.third }) {
                    writer.appendLine("${escape(name)},${escape(data.first)},${data.second},${String.format(Locale.US, "%.2f", data.third)}")
                }
                writer.appendLine()

                // 6. Expenses Register
                writer.appendLine("\"--- DAILY EXPENSES REGISTER ---\"")
                writer.appendLine("\"Expense ID\",\"Date\",\"Title\",\"Category\",\"Payment Mode\",\"Amount\",\"Notes\"")
                for (exp in expenses) {
                    val row = listOf(
                        exp.id.toString(),
                        displayDateFormat.format(Date(exp.date)),
                        exp.title,
                        exp.category,
                        exp.paymentMode,
                        String.format(Locale.US, "%.2f", exp.amount),
                        exp.notes
                    ).joinToString(",") { escape(it) }
                    writer.appendLine(row)
                }

                writer.flush()
            }
        }

        csvFile
    }

    private fun escape(text: String): String {
        return "\"${text.replace("\"", "\"\"")}\""
    }
}
