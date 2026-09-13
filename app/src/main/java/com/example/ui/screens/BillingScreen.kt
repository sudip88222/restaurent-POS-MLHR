package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PaymentMethod
import com.example.model.SplitPart
import com.example.service.QrCodeGenerator
import com.example.ui.PosViewModel
import com.example.ui.components.SplitBillDialog
import java.util.Locale

@Composable
fun BillingScreen(
    viewModel: PosViewModel,
    onBack: () -> Unit
) {
    val selectedTable by viewModel.selectedTable.collectAsState()
    val draftItems by viewModel.currentDraftItems.collectAsState()
    val restaurantSettings by viewModel.restaurantSettings.collectAsState()

    var discountPercent by remember { mutableDoubleStateOf(0.0) }
    var applyTax by remember(restaurantSettings.isGstPermanentlyEnabled) {
        mutableStateOf(restaurantSettings.isGstPermanentlyEnabled)
    }
    val taxPercent = if (applyTax) restaurantSettings.defaultTaxPercent else 0.0

    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var splitParts by remember { mutableStateOf<List<SplitPart>>(emptyList()) }
    var showSplitDialog by remember { mutableStateOf(false) }

    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var showSettleOptionsDialog by remember { mutableStateOf(false) }

    val subtotal = draftItems.sumOf { it.totalAmount }
    val discountAmount = (subtotal * discountPercent) / 100.0
    val taxableAmount = (subtotal - discountAmount).coerceAtLeast(0.0)
    val taxAmount = (taxableAmount * taxPercent) / 100.0
    val finalTotal = taxableAmount + taxAmount

    // Dynamic QR for UPI Payment on screen
    val upiQrBitmap = remember(finalTotal, restaurantSettings, paymentMethod) {
        if (paymentMethod == PaymentMethod.UPI && restaurantSettings.upiId.isNotBlank()) {
            val uri = QrCodeGenerator.buildUpiPaymentUri(
                upiId = restaurantSettings.upiId,
                payeeName = restaurantSettings.upiPayeeName.ifBlank { restaurantSettings.restaurantName },
                amount = finalTotal,
                billNumber = ""
            )
            val size = if (restaurantSettings.isLargeQr) 360 else 260
            QrCodeGenerator.generateQrBitmap(uri, size, size)
        } else {
            null
        }
    }

    if (showSplitDialog) {
        SplitBillDialog(
            totalBillAmount = finalTotal,
            items = draftItems,
            onDismiss = { showSplitDialog = false },
            onConfirmSplit = { parts ->
                splitParts = parts
                paymentMethod = PaymentMethod.SPLIT
                showSplitDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Checkout & Settle Bill",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${selectedTable?.name ?: "Direct Takeaway"} • ${selectedTable?.area ?: "Counter"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Itemized Bill Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ordered Items (${draftItems.sumOf { it.quantity }})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    draftItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.name} x${item.quantity}",
                                fontSize = 13.5.sp,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", item.totalAmount)}",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }

            // Discounts and Tax Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Discount Selector
                    Text(
                        text = "Apply Discount",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.0, 5.0, 10.0, 15.0, 20.0).forEach { disc ->
                            val isSelected = discountPercent == disc
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF1F5F9))
                                    .clickable { discountPercent = disc }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (disc == 0.0) "None" else "${disc.toInt()}%",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF334155)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tax Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (applyTax) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                            .clickable { applyTax = !applyTax }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Restaurant GST (${restaurantSettings.defaultTaxPercent}%)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (applyTax) Color(0xFF1E3A8A) else Color(0xFF334155)
                            )
                            Text(
                                text = if (applyTax) "CGST ${(restaurantSettings.defaultTaxPercent / 2.0)}% + SGST ${(restaurantSettings.defaultTaxPercent / 2.0)}%" else "GST is OFF (0% Tax)",
                                fontSize = 11.sp,
                                color = if (applyTax) Color(0xFF2563EB) else Color(0xFF64748B)
                            )
                        }
                        Checkbox(
                            checked = applyTax,
                            onCheckedChange = { applyTax = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            // Payment Mode Selector (Cash, UPI, Card, Split Bill)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Select Payment Method",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentOptionChip(
                            title = "Cash",
                            icon = Icons.Default.Money,
                            isSelected = paymentMethod == PaymentMethod.CASH,
                            onClick = { paymentMethod = PaymentMethod.CASH },
                            modifier = Modifier.weight(1f)
                        )
                        PaymentOptionChip(
                            title = "UPI / QR",
                            icon = Icons.Default.QrCode,
                            isSelected = paymentMethod == PaymentMethod.UPI,
                            onClick = { paymentMethod = PaymentMethod.UPI },
                            modifier = Modifier.weight(1f)
                        )
                        PaymentOptionChip(
                            title = "Card",
                            icon = Icons.Default.CreditCard,
                            isSelected = paymentMethod == PaymentMethod.CARD,
                            onClick = { paymentMethod = PaymentMethod.CARD },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // UPI QR Section on Screen
                    if (paymentMethod == PaymentMethod.UPI && upiQrBitmap != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF0FDF4),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF86EFAC))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "SCAN UPI QR TO PAY ₹${String.format(Locale.US, "%.2f", finalTotal)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF15803D)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                                ) {
                                    Box(modifier = Modifier.padding(10.dp)) {
                                        Image(
                                            bitmap = upiQrBitmap.asImageBitmap(),
                                            contentDescription = "UPI Payment QR Code",
                                            modifier = Modifier.size(if (restaurantSettings.isLargeQr) 200.dp else 160.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "UPI: ${restaurantSettings.upiId}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Payee: ${restaurantSettings.upiPayeeName.ifBlank { restaurantSettings.restaurantName }}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "Customer can scan from this screen or from 58mm printed bill",
                                    fontSize = 10.sp,
                                    color = Color(0xFF166534)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Split Bill Option
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.5.dp,
                                if (paymentMethod == PaymentMethod.SPLIT) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                showSplitDialog = true
                            },
                        color = if (paymentMethod == PaymentMethod.SPLIT) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(Color(0xFFFEF3C7), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CallSplit,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Split Bill Among Customers",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    )
                                    Text(
                                        text = if (splitParts.isNotEmpty()) "${splitParts.size} parts configured" else "Tap to separate payments",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            if (splitParts.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Text(
                                        text = "Split Active",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Customer Details (Optional for SMS / WhatsApp Receipt)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Customer Info (Optional)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Name") },
                            placeholder = { Text("e.g. John Doe") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text("Phone") },
                            placeholder = { Text("+91...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            // Final Bill Computation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", color = Color(0xFF64748B), fontSize = 13.sp)
                        Text("₹${String.format(Locale.US, "%.2f", subtotal)}", color = Color(0xFF1E293B), fontSize = 13.sp)
                    }
                    if (discountAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Discount (${discountPercent.toInt()}%)", color = Color(0xFF16A34A), fontSize = 13.sp)
                            Text("-₹${String.format(Locale.US, "%.2f", discountAmount)}", color = Color(0xFF16A34A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (taxAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GST (${taxPercent}%)", color = Color(0xFF64748B), fontSize = 13.sp)
                            Text("₹${String.format(Locale.US, "%.2f", taxAmount)}", color = Color(0xFF1E293B), fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFCBD5E1)))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("GRAND TOTAL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "₹${String.format(Locale.US, "%.2f", finalTotal)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Bottom Action Bar: Two distinct options - Print Bill or Save Bill
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Settle and Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Option 1: Save the Bill only (No print)
                    OutlinedButton(
                        onClick = {
                            viewModel.checkoutBill(
                                taxPercent = taxPercent,
                                discountPercent = discountPercent,
                                paymentMethod = paymentMethod,
                                splitDetails = splitParts,
                                customerName = customerName,
                                customerPhone = customerPhone
                            ) { _ ->
                                viewModel.currentNavTab.value = "tables"
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Save the Bill",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Without Printing",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    // Option 2: Print the Bill & Settle
                    Button(
                        onClick = {
                            viewModel.checkoutBill(
                                taxPercent = taxPercent,
                                discountPercent = discountPercent,
                                paymentMethod = paymentMethod,
                                splitDetails = splitParts,
                                customerName = customerName,
                                customerPhone = customerPhone
                            ) { createdBill ->
                                if (viewModel.selectedPrinter.value != null) {
                                    viewModel.printBill58mm(createdBill)
                                }
                                viewModel.currentNavTab.value = "tables"
                            }
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Print the Bill",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = "58mm Thermal Print",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        // Bill & Settle Choice Dialog (if user taps direct prompt)
        if (showSettleOptionsDialog) {
            AlertDialog(
                onDismissRequest = { showSettleOptionsDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bill & Settle Options", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Grand Total: ₹${String.format(Locale.US, "%.2f", finalTotal)} • ${paymentMethod.name}",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Please choose how you would like to settle this bill:",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSettleOptionsDialog = false
                            viewModel.checkoutBill(
                                taxPercent = taxPercent,
                                discountPercent = discountPercent,
                                paymentMethod = paymentMethod,
                                splitDetails = splitParts,
                                customerName = customerName,
                                customerPhone = customerPhone
                            ) { createdBill ->
                                if (viewModel.selectedPrinter.value != null) {
                                    viewModel.printBill58mm(createdBill)
                                }
                                viewModel.currentNavTab.value = "tables"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print the Bill")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showSettleOptionsDialog = false
                            viewModel.checkoutBill(
                                taxPercent = taxPercent,
                                discountPercent = discountPercent,
                                paymentMethod = paymentMethod,
                                splitDetails = splitParts,
                                customerName = customerName,
                                customerPhone = customerPhone
                            ) { _ ->
                                viewModel.currentNavTab.value = "tables"
                            }
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save the Bill")
                    }
                }
            )
        }
    }
}

@Composable
private fun PaymentOptionChip(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.5.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.White
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF334155)
            )
        }
    }
}
