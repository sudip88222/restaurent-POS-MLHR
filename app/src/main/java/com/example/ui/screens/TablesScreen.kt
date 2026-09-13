package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material.icons.filled.TakeoutDining
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.OrderEntity
import com.example.data.local.TableEntity
import com.example.model.SeatingArea
import com.example.ui.PosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TablesScreen(
    viewModel: PosViewModel,
    onOpenPrinterSettings: () -> Unit
) {
    val tables by viewModel.filteredTables.collectAsState()
    val activeOrders by viewModel.activeOrders.collectAsState()
    val selectedArea by viewModel.selectedArea.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val selectedPrinter by viewModel.selectedPrinter.collectAsState()
    val restaurantSettings by viewModel.restaurantSettings.collectAsState()

    val totalCount = tables.size
    val occupiedCount = tables.count { it.isOccupied }
    val freeCount = totalCount - occupiedCount

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // App Bar with Quick Status
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
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = restaurantSettings.restaurantName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Sync indicator pill
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (syncState.isOnline) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (syncState.isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                            contentDescription = null,
                                            tint = if (syncState.isOnline) Color(0xFF16A34A) else Color(0xFFD97706),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (syncState.isOnline) "Synced" else "Offline",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (syncState.isOnline) Color(0xFF166534) else Color(0xFF92400E)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (restaurantSettings.address.isNotBlank()) "${restaurantSettings.address} • Floor Plan" else "Cabin, Hall & Table Floor Plan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Permanent GST Quick Toggle
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (restaurantSettings.isGstPermanentlyEnabled) Color(0xFFEFF6FF) else Color(0xFFFEF2F2),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (restaurantSettings.isGstPermanentlyEnabled) Color(0xFF93C5FD) else Color(0xFFFCA5A5)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.setGstPermanently(!restaurantSettings.isGstPermanentlyEnabled)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Receipt,
                                        contentDescription = "GST",
                                        tint = if (restaurantSettings.isGstPermanentlyEnabled) Color(0xFF2563EB) else Color(0xFFDC2626),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (restaurantSettings.isGstPermanentlyEnabled) "GST: ON (${restaurantSettings.defaultTaxPercent}%)" else "GST: OFF (0%)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (restaurantSettings.isGstPermanentlyEnabled) Color(0xFF1E40AF) else Color(0xFFB91C1C)
                                    )
                                }
                            }

                            // Restaurant Settings / Details Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.showRestaurantSettingsDialog.value = true }
                            ) {
                                Box(
                                    modifier = Modifier.padding(7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Storefront,
                                        contentDescription = "Restaurant Settings",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Printer config button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedPrinter != null) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onOpenPrinterSettings() }
                            ) {
                                Box(
                                    modifier = Modifier.padding(7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Print,
                                        contentDescription = "Printer",
                                        tint = if (selectedPrinter != null) Color(0xFF2563EB) else Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Seating Area Tabs (All, Hall, Cabin, Terrace, Takeaway)
                    val areaTabs: List<Pair<String, SeatingArea?>> = listOf(
                        "All" to null,
                        "Hall" to SeatingArea.HALL,
                        "Cabin" to SeatingArea.CABIN,
                        "Terrace" to SeatingArea.TERRACE,
                        "Takeaway" to SeatingArea.TAKEAWAY
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(areaTabs) { (label, area) ->
                            val isSelected = selectedArea == area
                            val count = if (area == null) {
                                viewModel.allTables.value.size
                            } else {
                                viewModel.allTables.value.count { it.area.equals(area.name, ignoreCase = true) }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.selectedArea.value = area }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = getAreaIcon(area),
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(0xFF475569),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$label ($count)",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Status summary metrics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusPill(
                    label = "AVAILABLE",
                    count = freeCount,
                    color = Color(0xFF16A34A),
                    bgColor = Color(0xFFDCFCE7),
                    modifier = Modifier.weight(1f)
                )
                StatusPill(
                    label = "OCCUPIED (SERVING)",
                    count = occupiedCount,
                    color = Color(0xFFEA580C),
                    bgColor = Color(0xFFFFEDD5),
                    modifier = Modifier.weight(1f)
                )
            }

            // Tables Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(tables) { table ->
                    val activeOrder = activeOrders.find { it.tableId == table.id }
                    TableCardItem(
                        table = table,
                        activeOrder = activeOrder,
                        onClick = { viewModel.onSelectTable(table) },
                        onEditClick = {
                            viewModel.tableToEdit.value = table
                            viewModel.showEditTableDialog.value = true
                        }
                    )
                }
            }
        }

        // Floating Action Button to Add New Table or Cabin
        FloatingActionButton(
            onClick = { viewModel.showAddTableDialog.value = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Table / Cabin", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun StatusPill(
    label: String,
    count: Int,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = "$count", fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
private fun TableCardItem(
    table: TableEntity,
    activeOrder: OrderEntity?,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val isOccupied = table.isOccupied
    val borderColor = if (isOccupied) Color(0xFFF97316) else Color(0xFFE2E8F0)
    val cardBg = if (isOccupied) Color(0xFFFFF7ED) else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOccupied) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Table name, Status badge, and Edit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (isOccupied) Color(0xFFEA580C) else Color(0xFF16A34A), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = table.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onEditClick() }
                ) {
                    Box(
                        modifier = Modifier.padding(5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Table / Cabin",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Area & Capacity
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${table.area} • ${table.capacity} Seats",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isOccupied && activeOrder != null) {
                // Occupied details
                val itemsCount = activeOrder.items.sumOf { it.quantity }
                val timeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(activeOrder.createdAt))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFEDD5),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "$itemsCount Items", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9A3412))
                            Text(text = "Since $timeFormatted", fontSize = 10.sp, color = Color(0xFFC2410C))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", activeOrder.totalAmount)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFC2410C)
                        )
                    }
                }
            } else {
                // Free table badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Tap to Take Order",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }
        }
    }
}

private fun getAreaIcon(area: SeatingArea?): ImageVector {
    return when (area) {
        SeatingArea.HALL -> Icons.Default.Restaurant
        SeatingArea.CABIN -> Icons.Default.MeetingRoom
        SeatingArea.TERRACE -> Icons.Default.WbSunny
        SeatingArea.TAKEAWAY -> Icons.Default.TakeoutDining
        null -> Icons.Default.TableBar
    }
}
