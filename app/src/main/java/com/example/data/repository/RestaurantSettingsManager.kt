package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.model.RestaurantSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RestaurantSettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pos_restaurant_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<RestaurantSettings> = _settings.asStateFlow()

    fun loadSettings(): RestaurantSettings {
        return RestaurantSettings(
            restaurantName = prefs.getString("name", "Royal Spice Restaurant") ?: "Royal Spice Restaurant",
            tagline = prefs.getString("tagline", "Authentic Multi-Cuisine & Bar") ?: "Authentic Multi-Cuisine & Bar",
            address = prefs.getString("address", "Main Street, Market Square") ?: "Main Street, Market Square",
            phone = prefs.getString("phone", "+91 98765 43210") ?: "+91 98765 43210",
            gstin = prefs.getString("gstin", "27AAAAA0000A1Z5") ?: "27AAAAA0000A1Z5",
            fssai = prefs.getString("fssai", "11521000000000") ?: "11521000000000",
            billFooter = prefs.getString("bill_footer", "Thank You! Visit Again 😊") ?: "Thank You! Visit Again 😊",
            isGstPermanentlyEnabled = prefs.getBoolean("gst_enabled_permanent", true),
            defaultTaxPercent = prefs.getFloat("default_tax_percent", 5.0f).toDouble(),
            upiId = prefs.getString("upi_id", "royalspice@upi") ?: "royalspice@upi",
            upiPayeeName = prefs.getString("upi_payee_name", "Royal Spice Restaurant") ?: "Royal Spice Restaurant",
            isUpiQrEnabledOnBill = prefs.getBoolean("upi_qr_enabled", true),
            isLargeQr = prefs.getBoolean("large_qr", true),
            openingCashFloat = prefs.getFloat("opening_cash_float", 0.0f).toDouble(),
            isAutoDriveBackupEnabled = prefs.getBoolean("auto_drive_backup", true),
            lastDriveBackupTimestamp = prefs.getLong("last_backup_ts", 0L),
            customCategories = (prefs.getStringSet("custom_categories", null) ?: setOf("Mocktails", "Chinese", "South Indian", "Tandoor", "Snacks")).toList().sorted()
        )
    }

    fun updateSettings(newSettings: RestaurantSettings) {
        prefs.edit().apply {
            putString("name", newSettings.restaurantName)
            putString("tagline", newSettings.tagline)
            putString("address", newSettings.address)
            putString("phone", newSettings.phone)
            putString("gstin", newSettings.gstin)
            putString("fssai", newSettings.fssai)
            putString("bill_footer", newSettings.billFooter)
            putBoolean("gst_enabled_permanent", newSettings.isGstPermanentlyEnabled)
            putFloat("default_tax_percent", newSettings.defaultTaxPercent.toFloat())
            putString("upi_id", newSettings.upiId)
            putString("upi_payee_name", newSettings.upiPayeeName)
            putBoolean("upi_qr_enabled", newSettings.isUpiQrEnabledOnBill)
            putBoolean("large_qr", newSettings.isLargeQr)
            putFloat("opening_cash_float", newSettings.openingCashFloat.toFloat())
            putBoolean("auto_drive_backup", newSettings.isAutoDriveBackupEnabled)
            putLong("last_backup_ts", newSettings.lastDriveBackupTimestamp)
            putStringSet("custom_categories", newSettings.customCategories.toSet())
            apply()
        }
        _settings.value = newSettings
    }

    fun setOpeningCashFloat(amount: Double) {
        prefs.edit().putFloat("opening_cash_float", amount.toFloat()).apply()
        _settings.value = _settings.value.copy(openingCashFloat = amount)
    }

    fun addCustomCategory(category: String) {
        val trimmed = category.trim()
        if (trimmed.isBlank()) return
        val current = _settings.value.customCategories.toMutableSet()
        current.add(trimmed)
        prefs.edit().putStringSet("custom_categories", current).apply()
        _settings.value = _settings.value.copy(customCategories = current.toList().sorted())
    }

    fun removeCustomCategory(category: String) {
        val current = _settings.value.customCategories.toMutableSet()
        current.remove(category)
        prefs.edit().putStringSet("custom_categories", current).apply()
        _settings.value = _settings.value.copy(customCategories = current.toList().sorted())
    }

    fun setLastBackupTimestamp(timestamp: Long) {
        prefs.edit().putLong("last_backup_ts", timestamp).apply()
        _settings.value = _settings.value.copy(lastDriveBackupTimestamp = timestamp)
    }

    fun setAutoDriveBackup(enabled: Boolean) {
        prefs.edit().putBoolean("auto_drive_backup", enabled).apply()
        _settings.value = _settings.value.copy(isAutoDriveBackupEnabled = enabled)
    }

    fun setGstPermanently(enabled: Boolean) {
        prefs.edit().putBoolean("gst_enabled_permanent", enabled).apply()
        _settings.value = _settings.value.copy(isGstPermanentlyEnabled = enabled)
    }
}
