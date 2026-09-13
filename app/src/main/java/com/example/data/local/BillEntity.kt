package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.OrderItem
import com.example.model.SplitPart

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billNumber: String,
    val orderId: Long? = null,
    val tableId: Long? = null,
    val tableName: String = "",
    val area: String = "",
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val taxPercent: Double = 5.0,
    val taxAmount: Double = 0.0,
    val discountPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val finalTotal: Double = 0.0,
    val paymentMethod: String = "CASH", // CASH, UPI, CARD, SPLIT
    val splitDetails: List<SplitPart> = emptyList(),
    val customerName: String = "",
    val customerPhone: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isPaid: Boolean = true,
    val isVoided: Boolean = false,
    val voidReason: String = ""
)
