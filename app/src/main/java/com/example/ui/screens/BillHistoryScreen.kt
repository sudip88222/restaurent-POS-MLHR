package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BillEntity
import com.example.ui.PosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BillHistoryScreen(
    viewModel: PosViewModel
) {
    val bills by viewModel.allBills.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredBills = remember(bills, searchQuery) {
        if (searchQuery.isBlank()) {
            bills
        } else {
            bills.filter {
                it.billNumber.contains(searchQuery, ignoreCase = true) ||
                        it.tableName.contains(searchQuery, ignoreCase = true) ||
                        it.customerName.contains(searchQuery, ignoreCase = true) ||
                        it.paymentMethod.contains(searchQuery, ignoreCase = true)
            }
        }
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
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Previous Bills & Invoices",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Edit, reprint or void past transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Bill #, Table or Customer...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )
            }
        }

        if (filteredBills.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No bills found in history",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredBills) { bill ->
                    BillHistoryItemCard(
                        bill = bill,
                        onViewReceipt = { viewModel.previewBill.value = bill },
                        onShowUpiQr = {
                            viewModel.qrPaymentBill.value = bill
                            viewModel.showQrPaymentDialog.value = true
                        },
                        onPrint58mm = { viewModel.printBill58mm(bill) },
                        onShareWhatsApp = { viewModel.shareBillReceiptToWhatsApp(bill) },
                        onEdit = { viewModel.billToEdit.value = bill }
                    )
                }
            }
        }
    }
}

@Composable
private fun BillHistoryItemCard(
    bill: BillEntity,
    onViewReceipt: () -> Unit,
    onShowUpiQr: () -> Unit,
    onPrint58mm: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onEdit: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val formattedTime = dateFormat.format(Date(bill.timestamp))
    val isVoided = bill.isVoided

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isVoided) Color(0xFFFEF2F2) else Color.White),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isVoided) Color(0xFFFECACA) else Color(0xFFE2E8F0)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Bill #, Status, and Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${bill.billNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isVoided) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEE2E2)
                            ) {
                                Text(
                                    text = "VOIDED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = bill.paymentMethod,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "${bill.tableName} (${bill.area}) • $formattedTime",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Text(
                    text = "₹${String.format(Locale.US, "%.2f", bill.finalTotal)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = if (isVoided) Color(0xFF991B1B) else MaterialTheme.colorScheme.primary
                )
            }

            if (isVoided) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Reason: ${bill.voidReason}",
                    fontSize = 11.sp,
                    color = Color(0xFFDC2626),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Items Summary preview
            val itemsPreview = bill.items.joinToString(", ") { "${it.name} x${it.quantity}" }
            Text(
                text = itemsPreview,
                fontSize = 12.sp,
                color = Color(0xFF475569),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action row: UPI QR, View, Print 58mm, WhatsApp, Edit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Direct UPI QR scan modal
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFCCFBF1),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onShowUpiQr() }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF0F766E))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("UPI QR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier
                        .weight(0.9f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onViewReceipt() }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF475569))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPrint58mm() }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF2563EB))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print 58mm", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF0FDF4),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onShareWhatsApp() }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF16A34A))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .weight(0.9f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEdit() }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF64748B))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    }
                }
            }
        }
    }
}
