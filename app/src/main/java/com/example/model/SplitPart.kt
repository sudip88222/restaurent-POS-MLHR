package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SplitPart(
    val partIndex: Int = 1,
    val title: String = "Person 1",
    val amount: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val isPaid: Boolean = false,
    val itemsSummary: String = ""
)
