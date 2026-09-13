package com.example.service

import android.content.Context
import android.widget.Toast
import com.example.data.local.BillEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.MenuItemEntity
import com.example.data.local.OrderEntity
import com.example.data.local.TableEntity
import com.example.data.repository.PosRepository
import com.example.data.repository.RestaurantSettingsManager
import com.example.model.RestaurantSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RestoreResult(
    val success: Boolean,
    val message: String,
    val menuItemsRestored: Int = 0,
    val tablesRestored: Int = 0,
    val billsRestored: Int = 0,
    val expensesRestored: Int = 0
)

class GoogleDriveBackupManager(
    private val context: Context,
    private val repository: PosRepository,
    private val settingsManager: RestaurantSettingsManager
) {

    suspend fun createFullBackupJson(isAutoBackup: Boolean = false): File = withContext(Dispatchers.IO) {
        val menuList = repository.allMenuItems.first()
        val tablesList = repository.allTables.first()
        val ordersList = repository.activeOrders.first()
        val billsList = repository.allBills.first()
        val expensesList = repository.allExpenses.first()
        val currentSettings = settingsManager.settings.value

        val root = JSONObject()
        root.put("appName", "Restaurant POS")
        root.put("backupVersion", 2)
        root.put("backupType", if (isAutoBackup) "AUTOMATIC_GOOGLE_DRIVE" else "MANUAL_EXPORT")
        root.put("timestamp", System.currentTimeMillis())
        val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        root.put("dateFormatted", dateFormatted)
        root.put("restaurantName", currentSettings.restaurantName)

        // Settings JSON
        val settingsObj = JSONObject().apply {
            put("restaurantName", currentSettings.restaurantName)
            put("tagline", currentSettings.tagline)
            put("address", currentSettings.address)
            put("phone", currentSettings.phone)
            put("gstin", currentSettings.gstin)
            put("fssai", currentSettings.fssai)
            put("billFooter", currentSettings.billFooter)
            put("isGstPermanentlyEnabled", currentSettings.isGstPermanentlyEnabled)
            put("defaultTaxPercent", currentSettings.defaultTaxPercent)
            put("upiId", currentSettings.upiId)
            put("upiPayeeName", currentSettings.upiPayeeName)
            put("isUpiQrEnabledOnBill", currentSettings.isUpiQrEnabledOnBill)
            put("isLargeQr", currentSettings.isLargeQr)
            put("openingCashFloat", currentSettings.openingCashFloat)
            put("isAutoDriveBackupEnabled", currentSettings.isAutoDriveBackupEnabled)

            val catArray = JSONArray()
            currentSettings.customCategories.forEach { catArray.put(it) }
            put("customCategories", catArray)
        }
        root.put("settings", settingsObj)

        // Menu Items Array
        val menuArray = JSONArray()
        menuList.forEach { m ->
            val obj = JSONObject().apply {
                put("id", m.id)
                put("name", m.name)
                put("category", m.category)
                put("price", m.price)
                put("isVeg", m.isVeg)
                put("imageUrl", m.imageUrl)
                put("description", m.description)
                put("isAvailable", m.isAvailable)
            }
            menuArray.put(obj)
        }
        root.put("menuItems", menuArray)

        // Tables Array
        val tablesArray = JSONArray()
        tablesList.forEach { t ->
            val obj = JSONObject().apply {
                put("id", t.id)
                put("name", t.name)
                put("area", t.area)
                put("capacity", t.capacity)
                put("status", t.status)
                put("currentOrderId", t.currentOrderId ?: JSONObject.NULL)
                put("activeRound", t.activeRound)
            }
            tablesArray.put(obj)
        }
        root.put("tables", tablesArray)

        val converters = com.example.data.local.Converters()

        // Bills Array
        val billsArray = JSONArray()
        billsList.forEach { b ->
            val obj = JSONObject().apply {
                put("id", b.id)
                put("billNumber", b.billNumber)
                put("orderId", b.orderId ?: JSONObject.NULL)
                put("tableId", b.tableId ?: JSONObject.NULL)
                put("tableName", b.tableName)
                put("area", b.area)
                put("itemsJson", converters.fromOrderItemList(b.items))
                put("subtotal", b.subtotal)
                put("taxPercent", b.taxPercent)
                put("taxAmount", b.taxAmount)
                put("discountPercent", b.discountPercent)
                put("discountAmount", b.discountAmount)
                put("finalTotal", b.finalTotal)
                put("paymentMethod", b.paymentMethod)
                put("splitDetailsJson", converters.fromSplitPartList(b.splitDetails))
                put("customerName", b.customerName)
                put("customerPhone", b.customerPhone)
                put("timestamp", b.timestamp)
                put("isPaid", b.isPaid)
                put("isVoided", b.isVoided)
                put("voidReason", b.voidReason)
            }
            billsArray.put(obj)
        }
        root.put("bills", billsArray)

        // Expenses Array
        val expensesArray = JSONArray()
        expensesList.forEach { e ->
            val obj = JSONObject().apply {
                put("id", e.id)
                put("title", e.title)
                put("amount", e.amount)
                put("category", e.category)
                put("notes", e.notes)
                put("timestamp", e.timestamp)
                put("dateString", e.dateString)
            }
            expensesArray.put(obj)
        }
        root.put("expenses", expensesArray)

        // Save backup file
        val backupDir = File(context.cacheDir, "drive_backup")
        if (!backupDir.exists()) backupDir.mkdirs()

        val fileName = if (isAutoBackup) {
            "GoogleDrive_POS_AutoBackup_Latest.json"
        } else {
            val tsString = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            "RestaurantPOS_Backup_$tsString.json"
        }

        val backupFile = File(backupDir, fileName)
        backupFile.writeText(root.toString(2))

        // Also save persistent internal copy for automated system restore
        val internalDir = File(context.filesDir, "drive_backup")
        if (!internalDir.exists()) internalDir.mkdirs()
        File(internalDir, "drive_autobackup.json").writeText(root.toString(2))

        settingsManager.setLastBackupTimestamp(System.currentTimeMillis())
        backupFile
    }

    fun shareToGoogleDrive(backupFile: File) {
        ShareHelper.shareFile(
            context = context,
            file = backupFile,
            mimeType = "application/json",
            subject = "Restaurant POS Backup (Google Drive)",
            textMessage = "Google Drive Database Backup for ${settingsManager.settings.value.restaurantName} created on ${SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault()).format(Date())}. Select 'Save to Drive' to backup directly to your Google Drive account.",
            preferWhatsApp = false
        )
    }

    suspend fun restoreFromBackupJson(jsonContent: String): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonContent)
            if (!root.has("menuItems") && !root.has("tables") && !root.has("bills")) {
                return@withContext RestoreResult(
                    success = false,
                    message = "Invalid backup format. Missing core POS database sections."
                )
            }

            var menuCount = 0
            var tableCount = 0
            var billCount = 0
            var expenseCount = 0

            // 1. Restore Settings & Custom Categories
            if (root.has("settings")) {
                val sObj = root.getJSONObject("settings")
                val customCats = mutableListOf<String>()
                if (sObj.has("customCategories")) {
                    val catArr = sObj.getJSONArray("customCategories")
                    for (i in 0 until catArr.length()) {
                        customCats.add(catArr.getString(i))
                    }
                }
                val restoredSettings = RestaurantSettings(
                    restaurantName = sObj.optString("restaurantName", "Royal Spice Restaurant"),
                    tagline = sObj.optString("tagline", "Authentic Multi-Cuisine & Bar"),
                    address = sObj.optString("address", "Main Street, Market Square"),
                    phone = sObj.optString("phone", "+91 98765 43210"),
                    gstin = sObj.optString("gstin", "27AAAAA0000A1Z5"),
                    fssai = sObj.optString("fssai", "11521000000000"),
                    billFooter = sObj.optString("billFooter", "Thank You! Visit Again 😊"),
                    isGstPermanentlyEnabled = sObj.optBoolean("isGstPermanentlyEnabled", true),
                    defaultTaxPercent = sObj.optDouble("defaultTaxPercent", 5.0),
                    upiId = sObj.optString("upiId", "royalspice@upi"),
                    upiPayeeName = sObj.optString("upiPayeeName", "Royal Spice Restaurant"),
                    isUpiQrEnabledOnBill = sObj.optBoolean("isUpiQrEnabledOnBill", true),
                    isLargeQr = sObj.optBoolean("isLargeQr", true),
                    openingCashFloat = sObj.optDouble("openingCashFloat", 0.0),
                    isAutoDriveBackupEnabled = sObj.optBoolean("isAutoDriveBackupEnabled", true),
                    customCategories = if (customCats.isNotEmpty()) customCats else listOf("Mocktails", "Chinese", "South Indian", "Tandoor", "Snacks")
                )
                settingsManager.updateSettings(restoredSettings)
            }

            // 2. Restore Menu Items
            if (root.has("menuItems")) {
                val menuArr = root.getJSONArray("menuItems")
                val menuList = mutableListOf<MenuItemEntity>()
                for (i in 0 until menuArr.length()) {
                    val m = menuArr.getJSONObject(i)
                    menuList.add(
                        MenuItemEntity(
                            id = m.optLong("id", 0L),
                            name = m.getString("name"),
                            category = m.getString("category"),
                            price = m.getDouble("price"),
                            isVeg = m.optBoolean("isVeg", true),
                            imageUrl = m.optString("imageUrl", "icon:curry"),
                            description = m.optString("description", ""),
                            isAvailable = m.optBoolean("isAvailable", true)
                        )
                    )
                }
                if (menuList.isNotEmpty()) {
                    repository.addMenuItems(menuList)
                    menuCount = menuList.size
                }
            }

            // 3. Restore Tables
            if (root.has("tables")) {
                val tableArr = root.getJSONArray("tables")
                for (i in 0 until tableArr.length()) {
                    val t = tableArr.getJSONObject(i)
                    val tableEntity = TableEntity(
                        id = t.optLong("id", 0L),
                        name = t.getString("name"),
                        area = t.getString("area"),
                        capacity = t.optInt("capacity", 4),
                        status = t.optString("status", "AVAILABLE"),
                        currentOrderId = if (t.isNull("currentOrderId")) null else t.optLong("currentOrderId"),
                        activeRound = t.optInt("activeRound", 1)
                    )
                    repository.updateTable(tableEntity)
                    tableCount++
                }
            }

            // 4. Restore Bills
            if (root.has("bills")) {
                val converters = com.example.data.local.Converters()
                val billArr = root.getJSONArray("bills")
                for (i in 0 until billArr.length()) {
                    val b = billArr.getJSONObject(i)
                    val rawItems = if (b.has("itemsJson")) b.getString("itemsJson") else "[]"
                    val rawSplit = if (b.has("splitDetailsJson") && !b.isNull("splitDetailsJson")) b.getString("splitDetailsJson") else null

                    val billEntity = BillEntity(
                        id = b.optLong("id", 0L),
                        billNumber = b.getString("billNumber"),
                        orderId = if (b.isNull("orderId")) null else b.optLong("orderId"),
                        tableId = if (b.isNull("tableId")) null else b.optLong("tableId"),
                        tableName = b.optString("tableName", ""),
                        area = b.optString("area", "Dine In"),
                        items = converters.toOrderItemList(rawItems),
                        subtotal = b.getDouble("subtotal"),
                        taxPercent = b.optDouble("taxPercent", 0.0),
                        taxAmount = b.optDouble("taxAmount", 0.0),
                        discountPercent = b.optDouble("discountPercent", 0.0),
                        discountAmount = b.optDouble("discountAmount", 0.0),
                        finalTotal = b.getDouble("finalTotal"),
                        paymentMethod = b.optString("paymentMethod", "CASH"),
                        splitDetails = converters.toSplitPartList(rawSplit),
                        customerName = b.optString("customerName", ""),
                        customerPhone = b.optString("customerPhone", ""),
                        timestamp = b.optLong("timestamp", System.currentTimeMillis()),
                        isPaid = b.optBoolean("isPaid", true),
                        isVoided = b.optBoolean("isVoided", false),
                        voidReason = b.optString("voidReason", "")
                    )
                    repository.restoreBill(billEntity)
                    billCount++
                }
            }

            // 5. Restore Expenses
            if (root.has("expenses")) {
                val expArr = root.getJSONArray("expenses")
                for (i in 0 until expArr.length()) {
                    val e = expArr.getJSONObject(i)
                    val expEntity = ExpenseEntity(
                        id = e.optLong("id", 0L),
                        title = e.getString("title"),
                        amount = e.getDouble("amount"),
                        category = e.optString("category", "General"),
                        notes = e.optString("notes", ""),
                        timestamp = e.optLong("timestamp", System.currentTimeMillis()),
                        dateString = e.optString("dateString", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
                    )
                    repository.addExpense(expEntity.title, expEntity.amount, expEntity.category, expEntity.notes)
                    expenseCount++
                }
            }

            RestoreResult(
                success = true,
                message = "Data restored successfully from Google Drive backup!",
                menuItemsRestored = menuCount,
                tablesRestored = tableCount,
                billsRestored = billCount,
                expensesRestored = expenseCount
            )
        } catch (e: Exception) {
            RestoreResult(
                success = false,
                message = "Restore failed: ${e.localizedMessage ?: e.message}"
            )
        }
    }
}
