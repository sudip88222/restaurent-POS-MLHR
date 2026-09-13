package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.OrderItem

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableId: Long,
    val tableName: String,
    val area: String,
    val items: List<OrderItem> = emptyList(),
    val status: String = "ACTIVE", // ACTIVE, BILLED, CANCELLED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val customerNotes: String = ""
) {
    val totalAmount: Double
        get() = items.sumOf { it.totalAmount }

    val totalItemsCount: Int
        get() = items.sumOf { it.quantity }
}
