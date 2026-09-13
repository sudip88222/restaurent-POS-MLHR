package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import com.example.data.local.MenuItemEntity
import java.util.Locale

@Composable
fun AddEditMenuItemDialog(
    itemToEdit: MenuItemEntity?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (id: Long, name: String, category: String, price: Double, isVeg: Boolean, imageUrl: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var category by remember { mutableStateOf(itemToEdit?.category ?: "Main Course") }
    var priceText by remember { mutableStateOf(itemToEdit?.let { String.format(Locale.US, "%.0f", it.price) } ?: "") }
    var isVeg by remember { mutableStateOf(itemToEdit?.isVeg ?: true) }
    var imageUrl by remember { mutableStateOf(itemToEdit?.imageUrl ?: "icon:curry") }
    var description by remember { mutableStateOf(itemToEdit?.description ?: "") }

    val standardCategories = listOf("Starters", "Main Course", "Rice & Biryani", "Breads", "Fast Food", "Beverages", "Desserts")
    val combinedCategories = remember(categories) {
        (categories + standardCategories).filter { it.isNotBlank() && it != "All" && it != "🔥 Most Used" }.distinct()
    }
    var isCustomCategoryInput by remember {
        mutableStateOf(category.isNotBlank() && category !in standardCategories)
    }
    var customCategoryText by remember {
        mutableStateOf(if (category.isNotBlank() && category !in standardCategories) category else "")
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageUrl = uri.toString()
        }
    }

    val presetIcons = listOf(
        "icon:curry", "icon:starter", "icon:chicken", "icon:pizza", "icon:burger",
        "icon:noodles", "icon:rice", "icon:bread", "icon:coffee", "icon:drink", "icon:dessert"
    )

    fun autoAssignIcon(itemName: String, itemCat: String): String {
        val lower = "$itemName $itemCat".lowercase()
        return when {
            lower.contains("pizza") -> "icon:pizza"
            lower.contains("burger") -> "icon:burger"
            lower.contains("noodle") || lower.contains("pasta") -> "icon:noodles"
            lower.contains("rice") || lower.contains("biryani") -> "icon:rice"
            lower.contains("chicken") || lower.contains("meat") || lower.contains("mutton") -> "icon:chicken"
            lower.contains("coffee") || lower.contains("tea") -> "icon:coffee"
            lower.contains("drink") || lower.contains("soda") || lower.contains("juice") -> "icon:drink"
            lower.contains("bread") || lower.contains("naan") || lower.contains("roti") -> "icon:bread"
            lower.contains("cake") || lower.contains("dessert") || lower.contains("ice") -> "icon:dessert"
            lower.contains("starter") || lower.contains("tikka") -> "icon:starter"
            else -> "icon:curry"
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
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (itemToEdit == null) "Add New Food Item" else "Edit Food Item",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Item Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (imageUrl.startsWith("icon:")) {
                            imageUrl = autoAssignIcon(it, category)
                        }
                    },
                    label = { Text("Dish / Item Name") },
                    placeholder = { Text("e.g. Tandoori Chicken") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Selection
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(combinedCategories) { cat ->
                        val isSelected = !isCustomCategoryInput && category.equals(cat, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF1F5F9))
                                .clickable {
                                    isCustomCategoryInput = false
                                    category = cat
                                    if (imageUrl.startsWith("icon:")) {
                                        imageUrl = autoAssignIcon(name, cat)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFF334155)
                            )
                        }
                    }

                    // + Custom Category Chip
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCustomCategoryInput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer)
                                .clickable {
                                    isCustomCategoryInput = true
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "+ Custom Category",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCustomCategoryInput) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // If user selected Custom Category, show text field to enter it
                if (isCustomCategoryInput) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customCategoryText,
                        onValueChange = {
                            customCategoryText = it
                            category = it.trim()
                        },
                        label = { Text("Enter Custom Category") },
                        placeholder = { Text("e.g. Mocktails, South Indian, Pizza") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Price and Veg/Non-Veg
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Price (₹)") },
                        placeholder = { Text("240") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        VegNonVegBadge(isVeg = isVeg)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isVeg) "Veg" else "Non-Veg",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = isVeg,
                            onCheckedChange = { isVeg = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF16A34A),
                                checkedTrackColor = Color(0xFFDCFCE7),
                                uncheckedThumbColor = Color(0xFFDC2626),
                                uncheckedTrackColor = Color(0xFFFEE2E2)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Image Selection (Auto or Manual)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Item Icon / Photo",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Custom Photo", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Preset Icons Carousel
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetIcons) { iconTag ->
                        val isSelected = imageUrl == iconTag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    2.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { imageUrl = iconTag }
                                .padding(2.dp)
                        ) {
                            FoodItemIcon(imageUrl = iconTag, category = category, size = 40.dp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Fresh ingredients, spices, and aroma") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Save Action
                Button(
                    onClick = {
                        val parsedPrice = priceText.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank() && parsedPrice > 0) {
                            onSave(
                                itemToEdit?.id ?: 0L,
                                name,
                                category,
                                parsedPrice,
                                isVeg,
                                imageUrl,
                                description
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && (priceText.toDoubleOrNull() ?: 0.0) > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (itemToEdit == null) "Add Item to Menu" else "Save Changes")
                }
            }
        }
    }
}
