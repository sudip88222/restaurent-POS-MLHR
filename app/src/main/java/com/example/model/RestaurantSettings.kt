package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RestaurantSettings(
    val restaurantName: String = "Royal Spice Restaurant",
    val tagline: String = "Authentic Multi-Cuisine & Bar",
    val address: String = "Main Street, Market Square",
    val phone: String = "+91 98765 43210",
    val gstin: String = "27AAAAA0000A1Z5",
    val fssai: String = "11521000000000",
    val billFooter: String = "Thank You! Visit Again 😊",
    val isGstPermanentlyEnabled: Boolean = true,
    val defaultTaxPercent: Double = 5.0,
    val upiId: String = "royalspice@upi",
    val upiPayeeName: String = "Royal Spice Restaurant",
    val isUpiQrEnabledOnBill: Boolean = true,
    val isLargeQr: Boolean = true,
    val openingCashFloat: Double = 0.0,
    val isAutoDriveBackupEnabled: Boolean = true,
    val lastDriveBackupTimestamp: Long = 0L,
    val customCategories: List<String> = listOf("Mocktails", "Chinese", "South Indian", "Tandoor", "Snacks")
)
