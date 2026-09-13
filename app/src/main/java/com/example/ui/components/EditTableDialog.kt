package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.local.TableEntity
import com.example.model.SeatingArea

@Composable
fun EditTableDialog(
    table: TableEntity,
    onDismiss: () -> Unit,
    onSaveUpdate: (TableEntity) -> Unit,
    onDeleteTable: (TableEntity) -> Unit
) {
    var tableName by remember(table) { mutableStateOf(table.name) }
    var selectedArea by remember(table) {
        val area = try {
            SeatingArea.valueOf(table.area)
        } catch (_: Exception) {
            SeatingArea.HALL
        }
        mutableStateOf(area)
    }
    var capacity by remember(table) { mutableIntStateOf(table.capacity) }
    var isOccupied by remember(table) { mutableStateOf(table.isOccupied) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

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
                        Text(
                            text = "Edit Table / Cabin",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Area Selection (Hall, Cabin, Terrace, Takeaway)
                Text(
                    text = "Seating Section (Hall or Cabin):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(SeatingArea.HALL, SeatingArea.CABIN, SeatingArea.TERRACE, SeatingArea.TAKEAWAY).forEach { area ->
                        val isSelected = selectedArea == area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF1F5F9))
                                .clickable { selectedArea = area }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (area) {
                                    SeatingArea.HALL -> "Hall"
                                    SeatingArea.CABIN -> "Cabin"
                                    SeatingArea.TERRACE -> "Terrace"
                                    SeatingArea.TAKEAWAY -> "Counter"
                                },
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Name field
                OutlinedTextField(
                    value = tableName,
                    onValueChange = { tableName = it },
                    label = { Text("Table / Cabin Name") },
                    placeholder = { Text("e.g. Table 4 or Cabin VIP 2") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Seating Capacity
                Text(
                    text = "Seating Capacity ($capacity Pax):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2, 4, 6, 8, 12).forEach { cap ->
                        val isSelected = capacity == cap
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF1F5F9))
                                .clickable { capacity = cap }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$cap Pax",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Occupancy toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Occupancy Status",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = if (isOccupied) "Occupied (Serving Order)" else "Vacant (Ready for Guests)",
                            fontSize = 11.sp,
                            color = if (isOccupied) Color(0xFFEA580C) else Color(0xFF16A34A)
                        )
                    }
                    Switch(
                        checked = isOccupied,
                        onCheckedChange = { isOccupied = it }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Delete & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteConfirmation = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (tableName.isNotBlank()) {
                                onSaveUpdate(
                                    table.copy(
                                        name = tableName.trim(),
                                        area = selectedArea.name,
                                        capacity = capacity,
                                        isOccupied = isOccupied
                                    )
                                )
                            }
                        },
                        enabled = tableName.isNotBlank(),
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save Changes", fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete ${table.name}?") },
            text = { Text("Are you sure you want to remove this ${table.area} entry from your restaurant floor plan?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteTable(table)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Yes, Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
