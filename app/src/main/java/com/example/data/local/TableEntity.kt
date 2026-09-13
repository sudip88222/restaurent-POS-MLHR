package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tables")
data class TableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val area: String, // HALL, CABIN, TERRACE, TAKEAWAY
    val capacity: Int = 4,
    val isOccupied: Boolean = false,
    val currentOrderId: Long? = null,
    val occupiedSince: Long? = null
)
