package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String = "Groceries", // Groceries, Vegetables & Meat, Staff, Utility, Rent, Misc
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val paymentMode: String = "CASH"
)
