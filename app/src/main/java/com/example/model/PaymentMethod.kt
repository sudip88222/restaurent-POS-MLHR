package com.example.model

enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    UPI("UPI / QR Code"),
    CARD("Debit / Credit Card"),
    SPLIT("Split Payment");

    companion object {
        fun fromString(value: String): PaymentMethod {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CASH
        }
    }
}
