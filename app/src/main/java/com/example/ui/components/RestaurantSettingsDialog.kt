package com.example.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.RestaurantSettings
import com.example.service.QrCodeGenerator

@Composable
fun RestaurantSettingsDialog(
    initialSettings: RestaurantSettings,
    onDismiss: () -> Unit,
    onSave: (RestaurantSettings) -> Unit
) {
    var restaurantName by remember { mutableStateOf(initialSettings.restaurantName) }
    var tagline by remember { mutableStateOf(initialSettings.tagline) }
    var address by remember { mutableStateOf(initialSettings.address) }
    var phone by remember { mutableStateOf(initialSettings.phone) }
    var gstin by remember { mutableStateOf(initialSettings.gstin) }
    var fssaiNumber by remember { mutableStateOf(initialSettings.fssai) }
    var billFooterMessage by remember { mutableStateOf(initialSettings.billFooter) }

    // Permanent GST Switch
    var isGstPermanentlyEnabled by remember { mutableStateOf(initialSettings.isGstPermanentlyEnabled) }
    var defaultTaxPercent by remember { mutableDoubleStateOf(initialSettings.defaultTaxPercent) }

    // Large UPI QR Settings
    var upiId by remember { mutableStateOf(initialSettings.upiId) }
    var upiPayeeName by remember { mutableStateOf(initialSettings.upiPayeeName) }
    var isUpiQrEnabledOnBill by remember { mutableStateOf(initialSettings.isUpiQrEnabledOnBill) }
    var largeQrSize by remember { mutableStateOf(initialSettings.isLargeQr) }

    // Dynamic QR Preview
    val previewQrBitmap = remember(upiId, upiPayeeName, restaurantName, largeQrSize) {
        if (upiId.isNotBlank()) {
            val uri = QrCodeGenerator.buildUpiPaymentUri(
                upiId = upiId,
                payeeName = upiPayeeName.ifBlank { restaurantName },
                amount = 0.0,
                billNumber = ""
            )
            val size = if (largeQrSize) 320 else 240
            QrCodeGenerator.generateQrBitmap(uri, size, size)
        } else {
            null
        }
    }

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
                                Icons.Default.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Restaurant & Bill Setup",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Name, GST, Bill Header & UPI QR",
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    // SECTION 1: PERMANENT GST / TAX ON/OFF TOGGLE
                    Surface(
                        color = if (isGstPermanentlyEnabled) Color(0xFFEFF6FF) else Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (isGstPermanentlyEnabled) Color(0xFF3B82F6) else Color(0xFFEF4444)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = if (isGstPermanentlyEnabled) Color(0xFF1D4ED8) else Color(0xFFB91C1C)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Permanent GST / Tax",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isGstPermanentlyEnabled) Color(0xFF1E3A8A) else Color(0xFF7F1D1D)
                                        )
                                        Text(
                                            text = if (isGstPermanentlyEnabled) "GST is ACTIVE on all bills (${defaultTaxPercent}%)" else "GST is OFF (0% Tax permanently)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isGstPermanentlyEnabled) Color(0xFF2563EB) else Color(0xFFDC2626)
                                        )
                                    }
                                }
                                Switch(
                                    checked = isGstPermanentlyEnabled,
                                    onCheckedChange = { isGstPermanentlyEnabled = it }
                                )
                            }

                            if (isGstPermanentlyEnabled) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("GST Rate:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    listOf(2.5, 5.0, 12.0, 18.0).forEach { rate ->
                                        val isSelected = defaultTaxPercent == rate
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) Color(0xFF1D4ED8) else Color.White,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF93C5FD)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { defaultTaxPercent = rate }
                                        ) {
                                            Text(
                                                text = "${rate}%",
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else Color(0xFF1E3A8A)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // SECTION 2: RESTAURANT PROFILE DETAILS (FOR BILL PRINTING)
                    Text(
                        text = "RESTAURANT DETAILS (PRINTED ON BILL)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = restaurantName,
                        onValueChange = { restaurantName = it },
                        label = { Text("Restaurant Name *") },
                        placeholder = { Text("e.g. Royal Palace Restaurant & Cafe") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = tagline,
                        onValueChange = { tagline = it },
                        label = { Text("Tagline / Cuisine Subtitle") },
                        placeholder = { Text("e.g. Fine Dining & Family Restaurant") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Restaurant Address") },
                        placeholder = { Text("e.g. 12 High Street, MG Road, Pune") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Contact Phone") },
                            placeholder = { Text("+91 98765 43210") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = gstin,
                            onValueChange = { gstin = it },
                            label = { Text("GSTIN No.") },
                            placeholder = { Text("27AAAAA0000A1Z5") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = fssaiNumber,
                            onValueChange = { fssaiNumber = it },
                            label = { Text("FSSAI Lic. No.") },
                            placeholder = { Text("10012345678901") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = billFooterMessage,
                            onValueChange = { billFooterMessage = it },
                            label = { Text("Bill Footer Message") },
                            placeholder = { Text("Thank you! Visit again.") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION 3: LARGE UPI ID & QR INTEGRATION
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "UPI QR CODE PAYMENT (PRINT & SCREEN SCAN)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F766E)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it.trim() },
                        label = { Text("UPI ID (VPA) *") },
                        placeholder = { Text("e.g. restaurant@okaxis or 9876543210@paytm") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFF0F766E))
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = upiPayeeName,
                        onValueChange = { upiPayeeName = it },
                        label = { Text("UPI Payee / Business Name") },
                        placeholder = { Text("e.g. Royal Palace POS") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // UPI QR Switches
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0FDF4))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Print UPI QR on 58mm Thermal Bill",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF14532D)
                            )
                            Text(
                                text = "Customers can scan from printed paper bill to pay directly",
                                fontSize = 11.sp,
                                color = Color(0xFF166534)
                            )
                        }
                        Switch(
                            checked = isUpiQrEnabledOnBill,
                            onCheckedChange = { isUpiQrEnabledOnBill = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Large QR Size",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "High-contrast large QR code for quick scanning by phone cameras",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = largeQrSize,
                            onCheckedChange = { largeQrSize = it }
                        )
                    }

                    // QR Preview Card if UPI is present
                    if (previewQrBitmap != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Icon(Icons.Default.QrCode, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "LIVE UPI QR PREVIEW (SCANNABLE)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F766E)
                                    )
                                }

                                Image(
                                    bitmap = previewQrBitmap.asImageBitmap(),
                                    contentDescription = "Live UPI QR Preview",
                                    modifier = Modifier.size(if (largeQrSize) 180.dp else 140.dp)
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = upiId,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Scan with GPay, PhonePe, Paytm, BHIM",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Actions
                Button(
                    onClick = {
                        onSave(
                            RestaurantSettings(
                                restaurantName = restaurantName.trim().ifBlank { "The Grand Bistro" },
                                tagline = tagline.trim(),
                                address = address.trim(),
                                phone = phone.trim(),
                                gstin = gstin.trim(),
                                fssai = fssaiNumber.trim(),
                                billFooter = billFooterMessage.trim(),
                                isGstPermanentlyEnabled = isGstPermanentlyEnabled,
                                defaultTaxPercent = defaultTaxPercent,
                                upiId = upiId.trim(),
                                upiPayeeName = upiPayeeName.trim().ifBlank { restaurantName.trim() },
                                isUpiQrEnabledOnBill = isUpiQrEnabledOnBill,
                                isLargeQr = largeQrSize
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Restaurant & Billing Settings", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
