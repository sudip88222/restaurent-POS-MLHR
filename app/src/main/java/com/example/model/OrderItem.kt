package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OrderItem(
    val menuItemId: Long = 0,
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val category: String = "Main",
    val notes: String = "",
    val roundNumber: Int = 1,
    val addedTimestamp: Long = System.currentTimeMillis()
) {
    val totalAmount: Double
        get() = price * quantity
}
