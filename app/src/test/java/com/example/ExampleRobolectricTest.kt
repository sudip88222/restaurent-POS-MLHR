package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BillEntity
import com.example.model.OrderItem
import com.example.model.PaymentMethod
import com.example.model.SplitPart
import com.example.service.ThermalPrinterService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Restaurant POS", appName)
    }

    @Test
    fun `test 58mm thermal printer receipt text layout`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val thermalPrinter = ThermalPrinterService(context)

        val bill = BillEntity(
            billNumber = "BILL-2026-001",
            tableId = 1,
            tableName = "Table 4",
            area = "HALL",
            items = listOf(
                OrderItem(menuItemId = 1, name = "Paneer Tikka", price = 240.0, quantity = 2, category = "Starters"),
                OrderItem(menuItemId = 2, name = "Butter Naan", price = 50.0, quantity = 3, category = "Breads")
            ),
            subtotal = 630.0,
            discountPercent = 10.0,
            discountAmount = 63.0,
            taxPercent = 5.0,
            taxAmount = 28.35,
            finalTotal = 595.35,
            paymentMethod = "CASH"
        )

        val receiptText = thermalPrinter.buildTextReceipt(bill)
        assertTrue(receiptText.contains("BILL-2026-001"))
        assertTrue(receiptText.contains("Table 4"))
        assertTrue(receiptText.contains("Paneer Tikka"))
        assertTrue(receiptText.contains("595.35"))

        // Verify ESC/POS byte sequence generation
        val escPosBytes = thermalPrinter.buildEscPosBytes(bill)
        assertTrue(escPosBytes.isNotEmpty())
    }

    @Test
    fun `test split bill calculation`() {
        val totalAmount = 600.0
        val people = 3
        val share = totalAmount / people
        assertEquals(200.0, share, 0.001)

        val splitParts = (1..people).map { i ->
            SplitPart(
                partIndex = i,
                title = "Person $i",
                amount = share,
                paymentMethod = if (i == 1) PaymentMethod.UPI else PaymentMethod.CASH,
                isPaid = true
            )
        }

        assertEquals(3, splitParts.size)
        assertEquals(600.0, splitParts.sumOf { it.amount }, 0.001)
    }
}
