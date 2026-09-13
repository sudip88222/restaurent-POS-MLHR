package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.PosViewModel
import com.example.ui.components.AddEditMenuItemDialog
import com.example.ui.components.AddExpenseDialog
import com.example.ui.components.AddTableDialog
import com.example.ui.components.BluetoothPrinterDialog
import com.example.ui.components.EditBillDialog
import com.example.ui.components.EditTableDialog
import com.example.ui.components.QrPaymentDialog
import com.example.ui.components.RestaurantSettingsDialog
import com.example.ui.components.ScanMenuDialog
import com.example.ui.components.ThermalReceiptPreviewDialog
import com.example.ui.screens.BillHistoryScreen
import com.example.ui.screens.BillingScreen
import com.example.ui.screens.MenuManagementScreen
import com.example.ui.screens.OrderEntryScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.TablesScreen
import com.example.ui.theme.RestaurantPosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RestaurantPosTheme {
                PosApp()
            }
        }
    }
}

@Composable
fun PosApp(viewModel: PosViewModel = viewModel()) {
    val context = LocalContext.current
    val currentTab by viewModel.currentNavTab.collectAsState()
    val draftItems by viewModel.currentDraftItems.collectAsState()

    // Dialog states
    val previewBill by viewModel.previewBill.collectAsState()
    val billToEdit by viewModel.billToEdit.collectAsState()
    val showPrinterDialog by viewModel.showPrinterDialog.collectAsState()
    val showScanMenuDialog by viewModel.showScanMenuDialog.collectAsState()
    val isScanningMenu by viewModel.isScanningMenu.collectAsState()
    val scannedCandidates by viewModel.scannedCandidates.collectAsState()
    val showAddEditMenuItemDialog by viewModel.showAddEditMenuItemDialog.collectAsState()
    val itemToEdit by viewModel.itemToEdit.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val showAddTableDialog by viewModel.showAddTableDialog.collectAsState()
    val showEditTableDialog by viewModel.showEditTableDialog.collectAsState()
    val tableToEdit by viewModel.tableToEdit.collectAsState()
    val showRestaurantSettingsDialog by viewModel.showRestaurantSettingsDialog.collectAsState()
    val restaurantSettings by viewModel.restaurantSettings.collectAsState()
    val showQrPaymentDialog by viewModel.showQrPaymentDialog.collectAsState()
    val qrPaymentBill by viewModel.qrPaymentBill.collectAsState()
    val showAddExpenseDialog by viewModel.showAddExpenseDialog.collectAsState()

    val pairedPrinters by viewModel.pairedPrinters.collectAsState()
    val selectedPrinter by viewModel.selectedPrinter.collectAsState()
    val isPrinting by viewModel.isPrinting.collectAsState()

    // Bluetooth permission handling for Android 12+
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.refreshPairedPrinters()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val connectGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            val scanGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED

            if (!connectGranted || !scanGranted) {
                bluetoothPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    )
                )
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentTab != "billing") {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == "tables",
                        onClick = { viewModel.currentNavTab.value = "tables" },
                        icon = { Icon(Icons.Default.TableBar, contentDescription = "Tables") },
                        label = { Text("Tables", fontSize = 11.sp) }
                    )

                    NavigationBarItem(
                        selected = currentTab == "order",
                        onClick = { viewModel.currentNavTab.value = "order" },
                        icon = {
                            if (draftItems.isNotEmpty()) {
                                BadgedBox(badge = {
                                    Badge {
                                        Text("${draftItems.sumOf { it.quantity }}")
                                    }
                                }) {
                                    Icon(Icons.Default.Restaurant, contentDescription = "Order")
                                }
                            } else {
                                Icon(Icons.Default.Restaurant, contentDescription = "Order")
                            }
                        },
                        label = { Text("Order", fontSize = 11.sp) }
                    )

                    NavigationBarItem(
                        selected = currentTab == "bills",
                        onClick = { viewModel.currentNavTab.value = "bills" },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Bills") },
                        label = { Text("Bills", fontSize = 11.sp) }
                    )

                    NavigationBarItem(
                        selected = currentTab == "reports",
                        onClick = { viewModel.currentNavTab.value = "reports" },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                        label = { Text("Reports", fontSize = 11.sp) }
                    )

                    NavigationBarItem(
                        selected = currentTab == "menu",
                        onClick = { viewModel.currentNavTab.value = "menu" },
                        icon = { Icon(Icons.Default.MenuBook, contentDescription = "Menu") },
                        label = { Text("Menu", fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                "tables" -> TablesScreen(
                    viewModel = viewModel,
                    onOpenPrinterSettings = { viewModel.showPrinterDialog.value = true }
                )
                "order" -> OrderEntryScreen(
                    viewModel = viewModel,
                    onNavigateToBilling = { viewModel.currentNavTab.value = "billing" }
                )
                "billing" -> BillingScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.currentNavTab.value = "order" }
                )
                "bills" -> BillHistoryScreen(
                    viewModel = viewModel
                )
                "reports" -> ReportsScreen(
                    viewModel = viewModel
                )
                "menu" -> MenuManagementScreen(
                    viewModel = viewModel
                )
            }
        }
    }

    // --- Global Modals & Dialogs ---

    // 1. 58mm Thermal Receipt Preview & Printing
    previewBill?.let { bill ->
        ThermalReceiptPreviewDialog(
            bill = bill,
            settings = restaurantSettings,
            onDismiss = { viewModel.previewBill.value = null },
            onPrint = { b -> viewModel.printBill58mm(b) },
            onShareWhatsApp = { b -> viewModel.shareBillReceiptToWhatsApp(b) },
            onShowFullscreenQr = { b ->
                viewModel.qrPaymentBill.value = b
                viewModel.showQrPaymentDialog.value = true
            }
        )
    }

    // 2. Edit Previous Bill (Fix mistakes, void or delete)
    billToEdit?.let { bill ->
        EditBillDialog(
            bill = bill,
            onDismiss = { viewModel.billToEdit.value = null },
            onSaveUpdate = { updated -> viewModel.updatePreviousBill(updated) },
            onVoidBill = { billId, reason ->
                viewModel.voidPreviousBill(billId, reason)
                viewModel.billToEdit.value = null
            },
            onDeleteBill = { billId ->
                viewModel.deletePreviousBill(billId)
                viewModel.billToEdit.value = null
            }
        )
    }

    // 3. Bluetooth 58mm Printer Connection Settings
    if (showPrinterDialog) {
        BluetoothPrinterDialog(
            pairedPrinters = pairedPrinters,
            selectedPrinter = selectedPrinter,
            isPrinting = isPrinting,
            onDismiss = { viewModel.showPrinterDialog.value = false },
            onSelectPrinter = { device -> viewModel.selectedPrinter.value = device },
            onRefresh = { viewModel.refreshPairedPrinters() },
            onTestPrint = { device ->
                val sampleBill = com.example.data.local.BillEntity(
                    billNumber = "TEST-01",
                    tableId = 1,
                    tableName = "Test Table",
                    area = "HALL",
                    items = listOf(
                        com.example.model.OrderItem(
                            menuItemId = 1L,
                            name = "Test 58mm Print",
                            price = 100.0,
                            quantity = 1,
                            category = "Test",
                            roundNumber = 1
                        )
                    ),
                    subtotal = 100.0,
                    finalTotal = 105.0,
                    taxPercent = 5.0,
                    taxAmount = 5.0,
                    paymentMethod = "CASH"
                )
                viewModel.printBill58mm(sampleBill)
            }
        )
    }

    // 4. Scan Menu with AI Dialog
    if (showScanMenuDialog) {
        ScanMenuDialog(
            isScanning = isScanningMenu,
            candidates = scannedCandidates,
            onDismiss = {
                viewModel.showScanMenuDialog.value = false
                viewModel.scannedCandidates.value = emptyList()
            },
            onScanImage = { uri -> viewModel.scanMenuFromImageUri(uri) },
            onScanText = { text -> viewModel.scanMenuFromText(text) },
            onConfirmImport = { items -> viewModel.importScannedItems(items) }
        )
    }

    // 5. Add / Edit Menu Item Dialog
    if (showAddEditMenuItemDialog) {
        AddEditMenuItemDialog(
            itemToEdit = itemToEdit,
            categories = categories,
            onDismiss = {
                viewModel.showAddEditMenuItemDialog.value = false
                viewModel.itemToEdit.value = null
            },
            onSave = { id, name, category, price, isVeg, imageUrl, desc ->
                viewModel.saveMenuItem(id, name, category, price, isVeg, imageUrl, desc)
            }
        )
    }

    // 6. Add Table / Cabin Dialog
    if (showAddTableDialog) {
        AddTableDialog(
            onDismiss = { viewModel.showAddTableDialog.value = false },
            onAddTable = { name, area, cap ->
                viewModel.addTable(name, area, cap)
            }
        )
    }

    // 7. Add Expense Dialog
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { viewModel.showAddExpenseDialog.value = false },
            onAddExpense = { title, cat, amt, mode, notes ->
                viewModel.addExpense(title, cat, amt, mode, notes)
            }
        )
    }

    // 8. Edit / Remove Table or Cabin Dialog
    if (showEditTableDialog && tableToEdit != null) {
        EditTableDialog(
            table = tableToEdit!!,
            onDismiss = {
                viewModel.showEditTableDialog.value = false
                viewModel.tableToEdit.value = null
            },
            onSaveUpdate = { updatedTable ->
                viewModel.updateTable(updatedTable)
                viewModel.showEditTableDialog.value = false
                viewModel.tableToEdit.value = null
            },
            onDeleteTable = { tableToDelete ->
                viewModel.deleteTable(tableToDelete)
                viewModel.showEditTableDialog.value = false
                viewModel.tableToEdit.value = null
            }
        )
    }

    // 9. Restaurant Settings, Permanent GST, and UPI QR Setup Dialog
    if (showRestaurantSettingsDialog) {
        RestaurantSettingsDialog(
            initialSettings = restaurantSettings,
            onDismiss = { viewModel.showRestaurantSettingsDialog.value = false },
            onSave = { updatedSettings ->
                viewModel.updateRestaurantSettings(updatedSettings)
                viewModel.showRestaurantSettingsDialog.value = false
            }
        )
    }

    // 10. Large UPI QR Code Payment Modal (Customer Facing Scan)
    if (showQrPaymentDialog && qrPaymentBill != null) {
        QrPaymentDialog(
            bill = qrPaymentBill!!,
            settings = restaurantSettings,
            onDismiss = {
                viewModel.showQrPaymentDialog.value = false
                viewModel.qrPaymentBill.value = null
            }
        )
    }
}
