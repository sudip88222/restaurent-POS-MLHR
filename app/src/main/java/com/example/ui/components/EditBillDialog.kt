package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.window.Dialog
import com.example.data.local.BillEntity
import com.example.model.PaymentMethod
import java.util.Locale

@Composable
fun EditBillDialog(
    bill: BillEntity,
    onDismiss: () -> Unit,
    onSaveUpdate: (BillEntity) -> Unit,
    onVoidBill: (billId: Long, reason: String) -> Unit,
    onDeleteBill: (billId: Long) -> Unit
) {
    var selectedPaymentMethod by remember {
        mutableStateOf(
            PaymentMethod.entries.find { it.name.equals(bill.paymentMethod, ignoreCase = true) } ?: PaymentMethod.CASH
        )
    }
    var discountPercent by remember { mutableDoubleStateOf(bill.discountPercent) }
    var customerName by remember { mutableStateOf(bill.customerName) }
    var customerPhone by remember { mutableStateOf(bill.customerPhone) }

    var showVoidConfirm by remember { mutableStateOf(false) }
    var voidReason by remember { mutableStateOf("Billing error") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Recompute recalculations
    val subtotal = bill.subtotal
    val discountAmount = (subtotal * discountPercent) / 100.0
    val taxableAmount = (subtotal - discountAmount).coerceAtLeast(0.0)
    val taxAmount = (taxableAmount * bill.taxPercent) / 100.0
    val updatedTotal = taxableAmount + taxAmount

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Edit Bill #${bill.billNumber}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${bill.tableName} • Subtotal: ₹${String.format(Locale.US, "%.2f", subtotal)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Payment Method
                Text(
                    text = "Payment Method:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(PaymentMethod.CASH, PaymentMethod.UPI, PaymentMethod.CARD).forEach { mode ->
                        val isSelected = selectedPaymentMethod == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF1F5F9))
                                .clickable { selectedPaymentMethod = mode }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Discount Adjustment
                Text(
                    text = "Discount Percentage:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                text = "${disc.toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Customer info
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("Customer Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Calculated Summary Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Discount Applied", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text("-₹${String.format(Locale.US, "%.2f", discountAmount)}", fontSize = 12.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tax (${String.format(Locale.US, "%.1f", bill.taxPercent)}%)", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text("₹${String.format(Locale.US, "%.2f", taxAmount)}", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Adjusted Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "₹${String.format(Locale.US, "%.2f", updatedTotal)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save Updated Bill
                Button(
                    onClick = {
                        val updated = bill.copy(
                            paymentMethod = selectedPaymentMethod.name,
                            discountPercent = discountPercent,
                            discountAmount = discountAmount,
                            taxAmount = taxAmount,
                            finalTotal = updatedTotal,
                            customerName = customerName,
                            customerPhone = customerPhone
                        )
                        onSaveUpdate(updated)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save Bill Corrections")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Void / Delete options
                if (showVoidConfirm) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFFBEB),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Void this Bill?",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = voidReason,
                                onValueChange = { voidReason = it },
                                label = { Text("Reason for Voiding") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(onClick = { showVoidConfirm = false }) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onVoidBill(bill.id, voidReason) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                                ) {
                                    Text("Confirm Void")
                                }
                            }
                        }
                    }
                } else if (showDeleteConfirm) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Permanently Delete Bill?",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                            Text(
                                "This will permanently remove Bill #${bill.billNumber} from sales reports and database.",
                                fontSize = 12.sp,
                                color = Color(0xFF7F1D1D)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onDeleteBill(bill.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                                ) {
                                    Text("Delete Permanently")
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showVoidConfirm = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB45309))
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Void Bill", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete Bill", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
