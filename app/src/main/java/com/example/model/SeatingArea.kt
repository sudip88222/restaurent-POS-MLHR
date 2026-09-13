package com.example.model

enum class SeatingArea(val displayName: String) {
    HALL("Dining Hall"),
    CABIN("Private Cabin"),
    TERRACE("Terrace / Garden"),
    TAKEAWAY("Counter / Takeaway");

    companion object {
        fun fromString(value: String): SeatingArea {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: HALL
        }
    }
}
