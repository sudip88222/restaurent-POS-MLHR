package com.example.ui

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.BillEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.MenuItemEntity
import com.example.data.local.OrderEntity
import com.example.data.local.TableEntity
import com.example.data.repository.PosRepository
import com.example.model.OrderItem
import com.example.model.PaymentMethod
import com.example.model.RestaurantSettings
import com.example.model.SeatingArea
import com.example.model.SplitPart
import com.example.data.repository.RestaurantSettingsManager
import com.example.service.BluetoothPrinterDevice
import com.example.service.ExcelReportExporter
import com.example.service.GeminiMenuScanner
import com.example.service.PdfReportGenerator
import com.example.service.ShareHelper
import com.example.service.SyncManager
import com.example.service.SyncState
import com.example.service.ThermalPrinterService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.service.GoogleDriveBackupManager
import com.example.service.RestoreResult
import java.util.Calendar

class PosViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val repository = PosRepository(db)

    private val geminiScanner = GeminiMenuScanner(application)
    val thermalPrinter = ThermalPrinterService(application)
    private val pdfGenerator = PdfReportGenerator(application)
    private val excelExporter = ExcelReportExporter(application)
    private val syncManager = SyncManager(application, repository)
    val settingsManager = RestaurantSettingsManager(application)
    val googleDriveBackupManager = GoogleDriveBackupManager(application, repository, settingsManager)
    val restaurantSettings: StateFlow<RestaurantSettings> = settingsManager.settings

    // --- Google Drive Backup / Restore State ---
    val showDriveBackupDialog = MutableStateFlow(false)
    val isDriveBackupRunning = MutableStateFlow(false)
    val showCashInHandDialog = MutableStateFlow(false)

    // --- Navigation Tabs ---
    val currentNavTab = MutableStateFlow("tables") // "tables", "order", "bills", "reports", "menu"

    // --- Seating Area Filter & Table Selection ---
    val selectedArea = MutableStateFlow<SeatingArea?>(null) // null means ALL
    val selectedTable = MutableStateFlow<TableEntity?>(null)

    // --- Menu Search & Category Filter ---
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null) // null = ALL

    // --- Draft Order for Fast-Paced Entry ---
    val currentDraftItems = MutableStateFlow<List<OrderItem>>(emptyList())
    val currentOrderNotes = MutableStateFlow("")
    val currentRoundNumber = MutableStateFlow(1)

    // --- Dialog and Sheet States ---
    val showSplitBillDialog = MutableStateFlow(false)
    val showAddEditMenuItemDialog = MutableStateFlow(false)
    val itemToEdit = MutableStateFlow<MenuItemEntity?>(null)

    val showScanMenuDialog = MutableStateFlow(false)
    val isScanningMenu = MutableStateFlow(false)
    val scannedCandidates = MutableStateFlow<List<MenuItemEntity>>(emptyList())

    val showAddTableDialog = MutableStateFlow(false)
    val showEditTableDialog = MutableStateFlow(false)
    val tableToEdit = MutableStateFlow<TableEntity?>(null)

    val showRestaurantSettingsDialog = MutableStateFlow(false)
    val showQrPaymentDialog = MutableStateFlow(false)
    val qrPaymentBill = MutableStateFlow<BillEntity?>(null)

    val showAddExpenseDialog = MutableStateFlow(false)
    val showPrinterDialog = MutableStateFlow(false)

    val previewBill = MutableStateFlow<BillEntity?>(null)
    val billToEdit = MutableStateFlow<BillEntity?>(null)

    // --- Bluetooth Printer Selection ---
    val pairedPrinters = MutableStateFlow<List<BluetoothPrinterDevice>>(emptyList())
    val selectedPrinter = MutableStateFlow<BluetoothPrinterDevice?>(null)
    val isPrinting = MutableStateFlow(false)

    // --- Sync State ---
    val syncState: StateFlow<SyncState> = syncManager.syncState

    // --- Room Database Flows ---
    val allMenuItems = repository.allMenuItems.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allTables = repository.allTables.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val activeOrders = repository.activeOrders.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allBills = repository.allBills.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allExpenses = repository.allExpenses.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Combined categories: default categories + database categories + user's custom categories
    val allCategories: StateFlow<List<String>> = combine(
        repository.allCategories,
        restaurantSettings
    ) { dbCategories, settings ->
        val defaults = listOf("Starters", "Main Course", "Rice & Biryani", "Breads", "Fast Food", "Beverages", "Desserts")
        (defaults + dbCategories + settings.customCategories)
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Frequency calculation for most used items based on all settled bills
    val itemUsageMap: StateFlow<Map<String, Int>> = allBills.map { bills ->
        val map = mutableMapOf<String, Int>()
        for (b in bills) {
            if (!b.isVoided) {
                for (item in b.items) {
                    map[item.name] = (map[item.name] ?: 0) + item.quantity
                }
            }
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Filtered menu items for high speed order taking with most used items always at top
    val filteredMenuItems = combine(
        allMenuItems,
        selectedCategory,
        searchQuery,
        itemUsageMap
    ) { items, category, query, usageMap ->
        items.filter { item ->
            val matchesCategory = when {
                category == null || category == "All" -> true
                category == "🔥 Most Used" -> (usageMap[item.name] ?: 0) > 0
                else -> item.category.equals(category, ignoreCase = true)
            }
            val matchesQuery = query.isBlank() ||
                    item.name.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }.sortedWith(
            // Most used items appear first on top!
            compareByDescending<MenuItemEntity> { usageMap[it.name] ?: 0 }
                .thenBy { it.name }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered tables by area
    val filteredTables = combine(allTables, selectedArea) { tables, area ->
        if (area == null) tables else tables.filter { it.area.equals(area.name, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
            refreshPairedPrinters()
        }
    }

    fun refreshPairedPrinters() {
        val list = thermalPrinter.getPairedPrinters()
        pairedPrinters.value = list
        if (selectedPrinter.value == null && list.isNotEmpty()) {
            selectedPrinter.value = list.first()
        }
    }

    // --- Table Actions ---
    fun onSelectTable(table: TableEntity) {
        selectedTable.value = table
        viewModelScope.launch {
            val activeOrder = repository.getActiveOrderByTable(table.id)
            if (activeOrder != null) {
                currentDraftItems.value = activeOrder.items
                currentOrderNotes.value = activeOrder.customerNotes
                val maxRound = activeOrder.items.maxOfOrNull { it.roundNumber } ?: 1
                currentRoundNumber.value = maxRound + 1
            } else {
                currentDraftItems.value = emptyList()
                currentOrderNotes.value = ""
                currentRoundNumber.value = 1
            }
            currentNavTab.value = "order"
        }
    }

    fun addQuickItemToDraft(menuItem: MenuItemEntity) {
        val currentList = currentDraftItems.value.toMutableList()
        val index = currentList.indexOfFirst {
            it.menuItemId == menuItem.id && it.roundNumber == currentRoundNumber.value
        }
        if (index != -1) {
            val existing = currentList[index]
            currentList[index] = existing.copy(quantity = existing.quantity + 1)
        } else {
            currentList.add(
                OrderItem(
                    menuItemId = menuItem.id,
                    name = menuItem.name,
                    price = menuItem.price,
                    quantity = 1,
                    category = menuItem.category,
                    roundNumber = currentRoundNumber.value
                )
            )
        }
        currentDraftItems.value = currentList
    }

    fun updateDraftItemQty(item: OrderItem, delta: Int) {
        val currentList = currentDraftItems.value.toMutableList()
        val index = currentList.indexOf(item)
        if (index != -1) {
            val newQty = item.quantity + delta
            if (newQty <= 0) {
                currentList.removeAt(index)
            } else {
                currentList[index] = item.copy(quantity = newQty)
            }
            currentDraftItems.value = currentList
        }
    }

    fun removeDraftItem(item: OrderItem) {
        currentDraftItems.value = currentDraftItems.value.filter { it != item }
    }

    fun saveDraftOrderOrSendKot() {
        val table = selectedTable.value ?: return
        val items = currentDraftItems.value
        if (items.isEmpty()) {
            Toast.makeText(getApplication(), "Add at least one item", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            repository.addOrUpdateTableOrder(table, items, currentOrderNotes.value)
            Toast.makeText(getApplication(), "Order & KOT punched for ${table.name}!", Toast.LENGTH_SHORT).show()
            currentNavTab.value = "tables"
        }
    }

    fun cancelActiveOrder() {
        val table = selectedTable.value ?: return
        viewModelScope.launch {
            val activeOrder = repository.getActiveOrderByTable(table.id)
            if (activeOrder != null) {
                repository.cancelOrder(activeOrder.id)
                Toast.makeText(getApplication(), "Order cancelled for ${table.name}", Toast.LENGTH_SHORT).show()
            }
            currentDraftItems.value = emptyList()
            selectedTable.value = null
            currentNavTab.value = "tables"
        }
    }

    // --- Billing and Checkout ---
    fun checkoutBill(
        taxPercent: Double = 5.0,
        discountPercent: Double = 0.0,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        splitDetails: List<SplitPart> = emptyList(),
        customerName: String = "",
        customerPhone: String = "",
        onCompleted: (BillEntity) -> Unit = {}
    ) {
        val table = selectedTable.value
        val items = currentDraftItems.value
        if (items.isEmpty()) {
            Toast.makeText(getApplication(), "Cannot checkout empty order", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            val activeOrder = if (table != null) repository.getActiveOrderByTable(table.id) else null
            val createdBill = repository.createBill(
                order = activeOrder,
                tableId = table?.id,
                tableName = table?.name ?: "Direct Takeaway",
                area = table?.area ?: SeatingArea.TAKEAWAY.name,
                items = items,
                taxPercent = taxPercent,
                discountPercent = discountPercent,
                paymentMethod = paymentMethod,
                splitDetails = splitDetails,
                customerName = customerName,
                customerPhone = customerPhone
            )
            Toast.makeText(getApplication(), "Bill #${createdBill.billNumber} Generated!", Toast.LENGTH_SHORT).show()

            // Reset selection and open receipt preview
            selectedTable.value = null
            currentDraftItems.value = emptyList()
            previewBill.value = createdBill

            // Automatic background backup if enabled
            if (restaurantSettings.value.isAutoDriveBackupEnabled) {
                try {
                    googleDriveBackupManager.createFullBackupJson(isAutoBackup = true)
                } catch (e: Exception) {
                    // silent fallback
                }
            }

            onCompleted(createdBill)
        }
    }

    // --- Bill History Actions (Edit / Void / Delete) ---
    fun updatePreviousBill(bill: BillEntity) {
        viewModelScope.launch {
            repository.updateBill(bill)
            Toast.makeText(getApplication(), "Bill #${bill.billNumber} updated!", Toast.LENGTH_SHORT).show()
            billToEdit.value = null
        }
    }

    fun voidPreviousBill(billId: Long, reason: String) {
        viewModelScope.launch {
            repository.voidBill(billId, reason)
            Toast.makeText(getApplication(), "Bill voided: $reason", Toast.LENGTH_SHORT).show()
        }
    }

    fun deletePreviousBill(billId: Long) {
        viewModelScope.launch {
            repository.deleteBill(billId)
            Toast.makeText(getApplication(), "Bill deleted from records", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Expense Management ---
    fun addExpense(
        title: String,
        category: String,
        amount: Double,
        paymentMode: String = "CASH",
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.addExpense(
                ExpenseEntity(
                    title = title,
                    category = category,
                    amount = amount,
                    paymentMode = paymentMode,
                    notes = notes,
                    date = System.currentTimeMillis()
                )
            )
            Toast.makeText(getApplication(), "Expense added: ₹$amount", Toast.LENGTH_SHORT).show()
            showAddExpenseDialog.value = false
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteExpense(id)
            Toast.makeText(getApplication(), "Expense deleted", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Table Management ---
    fun addTable(name: String, area: SeatingArea, capacity: Int) {
        viewModelScope.launch {
            repository.addTable(name, area, capacity)
            Toast.makeText(getApplication(), "Added $name to ${area.displayName}", Toast.LENGTH_SHORT).show()
            showAddTableDialog.value = false
        }
    }

    fun updateTable(table: TableEntity) {
        viewModelScope.launch {
            repository.updateTable(table)
            Toast.makeText(getApplication(), "Updated ${table.name}", Toast.LENGTH_SHORT).show()
            showEditTableDialog.value = false
            tableToEdit.value = null
        }
    }

    fun deleteTable(table: TableEntity) {
        viewModelScope.launch {
            repository.deleteTable(table)
            Toast.makeText(getApplication(), "Deleted ${table.name}", Toast.LENGTH_SHORT).show()
            if (selectedTable.value?.id == table.id) {
                selectedTable.value = null
                currentDraftItems.value = emptyList()
            }
        }
    }

    // --- Restaurant Settings & GST Permanent Switch ---
    fun updateRestaurantSettings(newSettings: RestaurantSettings) {
        settingsManager.updateSettings(newSettings)
        Toast.makeText(getApplication(), "Restaurant details saved!", Toast.LENGTH_SHORT).show()
        showRestaurantSettingsDialog.value = false
    }

    fun setGstPermanently(enabled: Boolean) {
        settingsManager.setGstPermanently(enabled)
        val msg = if (enabled) "GST / Tax enabled permanently (5%)" else "GST / Tax turned OFF permanently (0%)"
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }

    fun openQrPayment(bill: BillEntity) {
        qrPaymentBill.value = bill
        showQrPaymentDialog.value = true
    }

    // --- Menu Item Management ---
    fun saveMenuItem(
        id: Long = 0,
        name: String,
        category: String,
        price: Double,
        isVeg: Boolean,
        imageUrl: String,
        description: String
    ) {
        viewModelScope.launch {
            val entity = MenuItemEntity(
                id = id,
                name = name,
                category = category,
                price = price,
                isVeg = isVeg,
                imageUrl = imageUrl,
                description = description,
                isAvailable = true
            )
            if (id == 0L) {
                repository.addMenuItem(entity)
                Toast.makeText(getApplication(), "Item '$name' added to menu", Toast.LENGTH_SHORT).show()
            } else {
                repository.updateMenuItem(entity)
                Toast.makeText(getApplication(), "Item '$name' updated", Toast.LENGTH_SHORT).show()
            }
            if (category.isNotBlank()) {
                settingsManager.addCustomCategory(category)
            }
            showAddEditMenuItemDialog.value = false
            itemToEdit.value = null
        }
    }

    fun deleteMenuItem(item: MenuItemEntity) {
        viewModelScope.launch {
            repository.deleteMenuItem(item)
            Toast.makeText(getApplication(), "'${item.name}' deleted", Toast.LENGTH_SHORT).show()
        }
    }

    // --- AI Menu Scanning (Photo / PDF text) ---
    fun scanMenuFromImageUri(uri: Uri) {
        viewModelScope.launch {
            isScanningMenu.value = true
            try {
                val extracted = geminiScanner.scanMenuFromImageUri(uri)
                scannedCandidates.value = extracted
                if (extracted.isEmpty()) {
                    Toast.makeText(getApplication(), "No menu items recognized. Try clearer image.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(getApplication(), "Extracted ${extracted.size} menu items!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Scan error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isScanningMenu.value = false
            }
        }
    }

    fun scanMenuFromText(text: String) {
        viewModelScope.launch {
            isScanningMenu.value = true
            try {
                val extracted = geminiScanner.scanMenuFromText(text)
                scannedCandidates.value = extracted
                Toast.makeText(getApplication(), "Extracted ${extracted.size} items from text!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Scan error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isScanningMenu.value = false
            }
        }
    }

    fun importScannedItems(items: List<MenuItemEntity>) {
        viewModelScope.launch {
            repository.addMenuItems(items)
            Toast.makeText(getApplication(), "Successfully imported ${items.size} menu items!", Toast.LENGTH_LONG).show()
            scannedCandidates.value = emptyList()
            showScanMenuDialog.value = false
        }
    }

    // --- 58mm Bluetooth Printing ---
    fun printBill58mm(bill: BillEntity) {
        val printer = selectedPrinter.value
        if (printer == null) {
            Toast.makeText(getApplication(), "No Bluetooth printer selected. Please connect POS-58.", Toast.LENGTH_SHORT).show()
            showPrinterDialog.value = true
            return
        }

        viewModelScope.launch {
            isPrinting.value = true
            val bytes = thermalPrinter.buildEscPosBytes(bill, restaurantSettings.value)
            val result = thermalPrinter.printToBluetoothDevice(printer.address, bytes)
            isPrinting.value = false
            result.onSuccess {
                Toast.makeText(getApplication(), "Print job sent to ${printer.name}!", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(getApplication(), "Print note: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- Reports Sharing & Exporting ---
    fun shareDailyPdfReport(preferWhatsApp: Boolean = false) {
        viewModelScope.launch {
            val today = System.currentTimeMillis()
            val bills = repository.getDailyBills(today)
            val expenses = repository.getDailyExpenses(today)
            val file = pdfGenerator.generateDailyReportPdf(
                dateTimestamp = today,
                bills = bills,
                expenses = expenses,
                restaurantName = restaurantSettings.value.restaurantName
            )
            ShareHelper.shareFile(
                context = getApplication(),
                file = file,
                mimeType = "application/pdf",
                subject = "${restaurantSettings.value.restaurantName} - Daily Sales & Expense Report",
                textMessage = "Attached is the Daily Restaurant POS Performance Report for ${restaurantSettings.value.restaurantName}.",
                preferWhatsApp = preferWhatsApp
            )
        }
    }

    fun exportDailyExcelReport(preferWhatsApp: Boolean = false) {
        viewModelScope.launch {
            val today = System.currentTimeMillis()
            val bills = repository.getDailyBills(today)
            val expenses = repository.getDailyExpenses(today)
            val file = excelExporter.exportDailyReportToExcel(
                dateTimestamp = today,
                bills = bills,
                expenses = expenses,
                restaurantName = restaurantSettings.value.restaurantName
            )
            ShareHelper.shareFile(
                context = getApplication(),
                file = file,
                mimeType = "text/csv",
                subject = "${restaurantSettings.value.restaurantName} - Daily Sales & Expenses Excel Sheet",
                textMessage = "Attached is the Daily Accounting Excel (.csv) export for ${restaurantSettings.value.restaurantName}.",
                preferWhatsApp = preferWhatsApp
            )
        }
    }

    fun shareBillReceiptToWhatsApp(bill: BillEntity) {
        val textReceipt = thermalPrinter.buildTextReceipt(bill, restaurantSettings.value)
        ShareHelper.shareTextToWhatsApp(getApplication(), textReceipt)
    }

    fun triggerSync() {
        syncManager.triggerAutoSync()
    }

    // --- Google Drive Backup & Restore ---
    fun backupToGoogleDrive() {
        viewModelScope.launch {
            isDriveBackupRunning.value = true
            try {
                val backupFile = googleDriveBackupManager.createFullBackupJson(isAutoBackup = false)
                googleDriveBackupManager.shareToGoogleDrive(backupFile)
                Toast.makeText(getApplication(), "Backup created! Select 'Save to Drive' to complete.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isDriveBackupRunning.value = false
            }
        }
    }

    fun restoreFromGoogleDriveJson(jsonString: String, onFinished: (RestoreResult) -> Unit = {}) {
        viewModelScope.launch {
            isDriveBackupRunning.value = true
            try {
                val result = googleDriveBackupManager.restoreFromBackupJson(jsonString)
                if (result.success) {
                    Toast.makeText(getApplication(), "Restored ${result.menuItemsRestored} items, ${result.billsRestored} bills!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(getApplication(), result.message, Toast.LENGTH_LONG).show()
                }
                onFinished(result)
            } catch (e: Exception) {
                val fail = RestoreResult(false, "Restore error: ${e.message}")
                Toast.makeText(getApplication(), fail.message, Toast.LENGTH_LONG).show()
                onFinished(fail)
            } finally {
                isDriveBackupRunning.value = false
            }
        }
    }

    fun setAutoDriveBackup(enabled: Boolean) {
        settingsManager.setAutoDriveBackup(enabled)
        val msg = if (enabled) "Automatic Google Drive & local backup enabled" else "Automatic backup paused"
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }

    // --- Cash in Hand / Drawer Management ---
    fun setOpeningCashFloat(amount: Double) {
        settingsManager.setOpeningCashFloat(amount)
        Toast.makeText(getApplication(), "Opening cash float updated: ₹$amount", Toast.LENGTH_SHORT).show()
    }

    // --- Custom Category Management ---
    fun addCustomCategory(category: String) {
        settingsManager.addCustomCategory(category)
        Toast.makeText(getApplication(), "Category '$category' added!", Toast.LENGTH_SHORT).show()
    }

    fun removeCustomCategory(category: String) {
        settingsManager.removeCustomCategory(category)
        Toast.makeText(getApplication(), "Category '$category' removed", Toast.LENGTH_SHORT).show()
    }
}
