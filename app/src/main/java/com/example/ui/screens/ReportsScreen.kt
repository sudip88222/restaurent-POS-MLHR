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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ExpenseEntity
import com.example.ui.PosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: PosViewModel
) {
    val bills by viewModel.allBills.collectAsState()
    val expenses by viewModel.allExpenses.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    val activeBills = bills.filter { !it.isVoided }
    val totalSales = activeBills.sumOf { it.finalTotal }
    val totalExpenses = expenses.sumOf { it.amount }
    val netProfit = totalSales - totalExpenses
    val totalBillsCount = activeBills.size

    val todayStr = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())

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
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Business Reports",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sales, Expenses & Accounting ($todayStr)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Multi-device sync badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (syncState.isOnline) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.triggerSync() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (syncState.isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = if (syncState.isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (syncState.isOnline) Color(0xFF16A34A) else Color(0xFFD97706),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (syncState.isSyncing) "Syncing..." else if (syncState.isOnline) "Auto-Sync ON" else "Offline",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (syncState.isOnline) Color(0xFF166534) else Color(0xFF92400E)
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Executive Summary Financial Cards (2x2 grid)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReportMetricCard(
                    title = "NET DAILY SALES",
                    value = "₹${String.format(Locale.US, "%.2f", totalSales)}",
                    subtitle = "$totalBillsCount paid bills",
                    bgColor = Color(0xFFECFDF5),
                    textColor = Color(0xFF065F46),
                    modifier = Modifier.weight(1f)
                )
                ReportMetricCard(
                    title = "TOTAL EXPENSES",
                    value = "₹${String.format(Locale.US, "%.2f", totalExpenses)}",
                    subtitle = "${expenses.size} expense entries",
                    bgColor = Color(0xFFFEF2F2),
                    textColor = Color(0xFF991B1B),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReportMetricCard(
                    title = "OPERATING PROFIT",
                    value = "₹${String.format(Locale.US, "%.2f", netProfit)}",
                    subtitle = if (netProfit >= 0) "Net profit earned" else "Operating loss",
                    bgColor = if (netProfit >= 0) Color(0xFFF0FDF4) else Color(0xFFFFF1F2),
                    textColor = if (netProfit >= 0) Color(0xFF15803D) else Color(0xFFBE123C),
                    modifier = Modifier.weight(1f)
                )
                ReportMetricCard(
                    title = "TOTAL ORDERS",
                    value = "$totalBillsCount",
                    subtitle = "Average ₹${if (totalBillsCount > 0) String.format(Locale.US, "%.0f", totalSales / totalBillsCount) else "0"}/bill",
                    bgColor = Color(0xFFEFF6FF),
                    textColor = Color(0xFF1E40AF),
                    modifier = Modifier.weight(1f)
                )
            }

            // 2. Export and Share Actions (WhatsApp PDF & Excel)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Export & Share Daily Reports",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Instantly share with management or CA on WhatsApp and Excel",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // WhatsApp PDF Share Button
                    Button(
                        onClick = { viewModel.shareDailyPdfReport(preferWhatsApp = true) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Daily Report (PDF) on WhatsApp", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Excel Export Button
                    OutlinedButton(
                        onClick = { viewModel.exportDailyExcelReport(preferWhatsApp = false) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Daily Sales Report to Excel (.csv)", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                    }
                }
            }

            // 3. Multi-Device Synchronization Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFE0E7FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF4338CA), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Multi-Device Offline Sync",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = syncState.syncMessage,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.triggerSync() },
                        enabled = !syncState.isSyncing
                    ) {
                        Text("Sync Now", fontSize = 11.sp)
                    }
                }
            }

            // 4. Daily Operating Expenses Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Operating Expenses",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Track groceries, dairy, salaries & fuel",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }

                        Button(
                            onClick = { viewModel.showAddExpenseDialog.value = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (expenses.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No expenses recorded today. Tap + Add to record cash/online expenses.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            expenses.forEach { expense ->
                                ExpenseRowItem(
                                    expense = expense,
                                    onDelete = { viewModel.deleteExpense(expense.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportMetricCard(
    title: String,
    value: String,
    subtitle: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = bgColor
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = textColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 11.sp, color = textColor.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun ExpenseRowItem(
    expense: ExpenseEntity,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "${expense.category} • ${expense.paymentMode} ${if (expense.notes.isNotBlank()) "• ${expense.notes}" else ""}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", expense.amount)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = Color(0xFFDC2626)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
